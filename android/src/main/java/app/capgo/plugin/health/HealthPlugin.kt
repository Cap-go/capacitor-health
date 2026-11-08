package app.capgo.plugin.health

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import java.time.Instant
import java.time.Duration
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "Health")
class HealthPlugin : Plugin() {
    private val pluginVersion = "8.10.0"
    private val manager = HealthManager()
    private val workoutWriter = WorkoutWriter()
    private val sampleDeleter = SampleDeleter()
    private val pluginScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, throwable ->
                Log.e(TAG, "Unhandled coroutine failure", throwable)
            },
    )
    private val permissionContract = PermissionController.createRequestPermissionResultContract()

    // Store pending request data for callback
    private var pendingReadTypes: List<HealthDataType> = emptyList()
    private var pendingWriteTypes = WriteTypes()
    private var pendingIncludeWorkouts: Boolean = false
    private var pendingIncludeHistoryAccess: Boolean = false

    /**
     * Launch work on [pluginScope] and always settle [this] PluginCall on failure.
     * Without this, exceptions escaping a plain `launch` hit the thread's default
     * handler and kill the host app (e.g. Health Connect IPC rate limits).
     */
    private fun PluginCall.launchSafely(block: suspend PluginCall.() -> Unit) {
        val call = this
        pluginScope.launch {
            try {
                call.block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Health Connect call failed", e)
                val exception = e as? Exception ?: Exception(e)
                call.reject(e.message ?: "Health Connect call failed", null, exception)
            }
        }
    }

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        pluginScope.cancel()
    }

    @PluginMethod
    fun isAvailable(call: PluginCall) {
        val status = HealthConnectClient.getSdkStatus(context)
        call.resolve(availabilityPayload(status))
    }

    @PluginMethod
    fun requestAuthorization(call: PluginCall) {
        val (readTypes, includeWorkouts) = try {
            parseTypeListWithWorkouts(call, "read")
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        val writeTypes = try {
            parseWriteTypeList(call)
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        val includeHistoryAccess = call.getBoolean("requestHistoryAccess") ?: false

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            // Only request the history permission when the provider actually supports it.
            // On an older-but-supported provider it can never be granted, which would leave
            // granted.containsAll(permissions) permanently false and reopen the permission
            // sheet on every call. The normal data scopes are still requested either way;
            // authorizationStatus reports historyAccessAvailable so callers can react.
            val requestHistoryAccess = includeHistoryAccess && manager.isHistoryAccessAvailable(client)
            val permissions = manager.permissionsFor(
                readTypes,
                writeTypes.dataTypes,
                includeWorkouts,
                requestHistoryAccess,
                includeWorkoutWrite = writeTypes.workouts,
                includeWorkoutRouteWrite = writeTypes.workoutRoute,
            )

            if (permissions.isEmpty()) {
                val status = authorizationStatus(client, readTypes, writeTypes, includeWorkouts, includeHistoryAccess)
                resolve(status)
                return@launchSafely
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                val status = authorizationStatus(client, readTypes, writeTypes, includeWorkouts, includeHistoryAccess)
                resolve(status)
                return@launchSafely
            }

            // Store types for callback
            pendingReadTypes = readTypes
            pendingWriteTypes = writeTypes
            pendingIncludeWorkouts = includeWorkouts
            pendingIncludeHistoryAccess = includeHistoryAccess

            // Create intent using the Health Connect permission contract
            val intent = permissionContract.createIntent(context, permissions)

            try {
                startActivityForResult(this, intent, "handlePermissionResult")
            } catch (e: Exception) {
                pendingReadTypes = emptyList()
                pendingWriteTypes = WriteTypes()
                reject("Failed to launch Health Connect permission request.", null, e)
            }
        }
    }

    @ActivityCallback
    private fun handlePermissionResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            return
        }

        val readTypes = pendingReadTypes
        val writeTypes = pendingWriteTypes
        val includeWorkouts = pendingIncludeWorkouts
        val includeHistoryAccess = pendingIncludeHistoryAccess
        pendingReadTypes = emptyList()
        pendingWriteTypes = WriteTypes()
        pendingIncludeWorkouts = false
        pendingIncludeHistoryAccess = false

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            val status = authorizationStatus(client, readTypes, writeTypes, includeWorkouts, includeHistoryAccess)
            resolve(status)
        }
    }

    @PluginMethod
    fun checkAuthorization(call: PluginCall) {
        val (readTypes, includeWorkouts) = try {
            parseTypeListWithWorkouts(call, "read")
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        val writeTypes = try {
            parseWriteTypeList(call)
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        val includeHistoryAccess = call.getBoolean("requestHistoryAccess") ?: false

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            val status = authorizationStatus(client, readTypes, writeTypes, includeWorkouts, includeHistoryAccess)
            resolve(status)
        }
    }

    @PluginMethod
    fun readSamples(call: PluginCall) {
        val identifier = call.getString("dataType")
        if (identifier.isNullOrBlank()) {
            call.reject("dataType is required")
            return
        }

        val dataType = HealthDataType.from(identifier)
        if (dataType == null) {
            call.reject("Unsupported data type: $identifier")
            return
        }

        val limit = (call.getInt("limit") ?: DEFAULT_LIMIT).coerceAtLeast(0)
        val ascending = call.getBoolean("ascending") ?: false

        val startInstant = try {
            manager.parseInstant(call.getString("startDate"), Instant.now().minus(DEFAULT_PAST_DURATION))
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        val endInstant = try {
            manager.parseInstant(call.getString("endDate"), Instant.now())
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        if (endInstant.isBefore(startInstant)) {
            call.reject("endDate must be greater than or equal to startDate")
            return
        }

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            try {
                val samples = manager.readSamples(client, dataType, startInstant, endInstant, limit, ascending)
                val result = JSObject().apply { put("samples", samples) }
                resolve(result)
            } catch (e: Exception) {
                reject(e.message ?: "Failed to read samples.", null, e)
            }
        }
    }

    @PluginMethod
    fun saveSample(call: PluginCall) {
        val identifier = call.getString("dataType")
        if (identifier.isNullOrBlank()) {
            call.reject("dataType is required")
            return
        }

        val dataType = HealthDataType.from(identifier)
        if (dataType == null) {
            call.reject("Unsupported data type: $identifier")
            return
        }

        val value = call.getDouble("value")
        if (value == null) {
            call.reject("value is required")
            return
        }

        val unit = call.getString("unit")
        if (unit != null && unit != dataType.unit) {
            call.reject("Unsupported unit $unit for ${dataType.identifier}. Expected ${dataType.unit}.")
            return
        }

        val startInstant = try {
            manager.parseInstant(call.getString("startDate"), Instant.now())
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        val endInstant = try {
            manager.parseInstant(call.getString("endDate"), startInstant)
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        if (endInstant.isBefore(startInstant)) {
            call.reject("endDate must be greater than or equal to startDate")
            return
        }

        val metadataObj = call.getObject("metadata")
        val metadata = metadataObj?.let { obj ->
            val iterator = obj.keys()
            val map = mutableMapOf<String, String>()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val rawValue = obj.opt(key)
                if (rawValue is String) {
                    map[key] = rawValue
                }
            }
            map.takeIf { it.isNotEmpty() }
        }
        
        val systolic = call.getDouble("systolic")
        val diastolic = call.getDouble("diastolic")
        val mindfulnessSessionType = call.getString("mindfulnessSessionType")

        val clientRecordId = call.getString("clientRecordId")
        if (clientRecordId != null && clientRecordId.isBlank()) {
            call.reject("clientRecordId must not be blank")
            return
        }
        // Omitted keeps the previous behaviour: a manual entry.
        val manualEntry = when (val recordingMethod = call.getString("recordingMethod")) {
            null, "manualEntry" -> true
            "activelyRecorded" -> false
            else -> {
                call.reject("Unsupported recordingMethod: $recordingMethod. Expected 'activelyRecorded' or 'manualEntry'.")
                return
            }
        }

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            try {
                manager.saveSample(
                    client, dataType, value, startInstant, endInstant, metadata, systolic, diastolic, mindfulnessSessionType,
                    clientRecordId = clientRecordId,
                    manualEntry = manualEntry,
                )
                resolve()
            } catch (e: Exception) {
                reject(e.message ?: "Failed to save sample.", null, e)
            }
        }
    }

    @PluginMethod
    fun deleteSamples(call: PluginCall) {
        val input = try {
            sampleDeleter.parse(call.data)
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            try {
                resolve(sampleDeleter.delete(client, input))
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                reject(e.message ?: "Permission denied for deleting samples.", "permission-denied", e)
            } catch (e: IllegalArgumentException) {
                reject(e.message ?: "Invalid delete request.", null, e)
            } catch (e: Exception) {
                Log.w(TAG, "Deleting samples failed: ${e.message}", e)
                reject(e.message ?: "Failed to delete samples.", "delete-samples-failed", e)
            }
        }
    }

    /** Parsed `write` list: regular data types plus the `workouts` / `workoutRoute` pseudo types. */
    private data class WriteTypes(
        val dataTypes: List<HealthDataType> = emptyList(),
        val workouts: Boolean = false,
        val workoutRoute: Boolean = false,
    )

    private fun parseWriteTypeList(call: PluginCall): WriteTypes {
        val array = call.getArray("write") ?: JSArray()
        val result = mutableListOf<HealthDataType>()
        var includeWorkouts = false
        var includeWorkoutRoute = false
        for (i in 0 until array.length()) {
            val identifier = array.optString(i, null) ?: continue
            when (identifier) {
                "workouts" -> includeWorkouts = true
                "workoutRoute" -> includeWorkoutRoute = true
                else -> {
                    val dataType = HealthDataType.from(identifier)
                        ?: throw IllegalArgumentException("Unsupported data type: $identifier")
                    result.add(dataType)
                }
            }
        }
        return WriteTypes(result, includeWorkouts, includeWorkoutRoute)
    }

    private suspend fun authorizationStatus(
        client: HealthConnectClient,
        readTypes: List<HealthDataType>,
        writeTypes: WriteTypes,
        includeWorkouts: Boolean,
        includeHistoryAccess: Boolean,
    ): JSObject = manager.authorizationStatus(
        client,
        readTypes,
        writeTypes.dataTypes,
        includeWorkouts,
        includeHistoryAccess,
        includeWorkoutWrite = writeTypes.workouts,
        includeWorkoutRouteWrite = writeTypes.workoutRoute,
    )

    private fun parseTypeListWithWorkouts(call: PluginCall, key: String): Pair<List<HealthDataType>, Boolean> {
        val array = call.getArray(key) ?: JSArray()
        val result = mutableListOf<HealthDataType>()
        var includeWorkouts = false
        for (i in 0 until array.length()) {
            val identifier = array.optString(i, null) ?: continue
            if (identifier == "workouts") {
                includeWorkouts = true
            } else {
                val dataType = HealthDataType.from(identifier)
                    ?: throw IllegalArgumentException("Unsupported data type: $identifier")
                result.add(dataType)
            }
        }
        return Pair(result, includeWorkouts)
    }

    private fun getClientOrReject(call: PluginCall): HealthConnectClient? {
        val status = HealthConnectClient.getSdkStatus(context)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            call.reject(availabilityReason(status))
            return null
        }
        return HealthConnectClient.getOrCreate(context)
    }

    private fun availabilityPayload(status: Int): JSObject {
        val payload = JSObject()
        payload.put("platform", "android")
        payload.put("available", status == HealthConnectClient.SDK_AVAILABLE)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            payload.put("reason", availabilityReason(status))
        }
        return payload
    }

    private fun availabilityReason(status: Int): String {
        return when (status) {
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect needs an update."
            HealthConnectClient.SDK_UNAVAILABLE -> "Health Connect is unavailable on this device."
            else -> "Health Connect availability unknown."
        }
    }

    @PluginMethod
    fun getPluginVersion(call: PluginCall) {
        try {
            val ret = JSObject()
            ret.put("version", pluginVersion)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("Could not get plugin version", e)
        }
    }

    @PluginMethod
    fun openHealthConnectSettings(call: PluginCall) {
        try {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to open Health Connect settings", null, e)
        }
    }

    @PluginMethod
    fun showPrivacyPolicy(call: PluginCall) {
        try {
            val intent = Intent(context, PermissionsRationaleActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to show privacy policy", null, e)
        }
    }

    @PluginMethod
    fun queryWorkouts(call: PluginCall) {
        val workoutType = call.getString("workoutType")
        val limit = (call.getInt("limit") ?: DEFAULT_LIMIT).coerceAtLeast(0)
        val ascending = call.getBoolean("ascending") ?: false
        val anchor = call.getString("anchor")

        val startInstant = try {
            manager.parseInstant(call.getString("startDate"), Instant.now().minus(DEFAULT_PAST_DURATION))
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        val endInstant = try {
            manager.parseInstant(call.getString("endDate"), Instant.now())
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        if (endInstant.isBefore(startInstant)) {
            call.reject("endDate must be greater than or equal to startDate")
            return
        }

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            try {
                val result = manager.queryWorkouts(client, workoutType, startInstant, endInstant, limit, ascending, anchor)
                resolve(result)
            } catch (e: Exception) {
                reject(e.message ?: "Failed to query workouts.", null, e)
            }
        }
    }

    @PluginMethod
    fun saveWorkout(call: PluginCall) {
        val input = try {
            workoutWriter.parse(call.data)
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            try {
                resolve(workoutWriter.save(client, input))
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                reject(e.message ?: "Permission denied for saving workouts.", "permission-denied", e)
            } catch (e: IllegalArgumentException) {
                reject(e.message ?: "Invalid workout.", null, e)
            } catch (e: Exception) {
                Log.w(TAG, "Saving workout failed: ${e.message}", e)
                reject(e.message ?: "Failed to save workout.", "save-workout-failed", e)
            }
        }
    }

    @PluginMethod
    fun queryAggregated(call: PluginCall) {
        val identifier = call.getString("dataType")
        if (identifier.isNullOrBlank()) {
            call.reject("dataType is required")
            return
        }

        val dataType = HealthDataType.from(identifier)
        if (dataType == null) {
            call.reject("Unsupported data type: $identifier")
            return
        }

        val bucket = call.getString("bucket") ?: "day"
        // `aggregation` may be a single string or an array of strings. Fall back to a
        // sensible per-data-type default when omitted.
        val aggregations: List<String> = call.getArray("aggregation")?.let { array ->
            try {
                (0 until array.length()).map { array.getString(it) }
            } catch (e: org.json.JSONException) {
                call.reject("aggregation array must contain only strings", null, e)
                return
            }
        } ?: call.getString("aggregation")?.let { listOf(it) } ?: listOf(
            when (dataType) {
                HealthDataType.STEPS,
                HealthDataType.DISTANCE,
                HealthDataType.CALORIES -> "sum"
                HealthDataType.HEART_RATE,
                HealthDataType.WEIGHT,
                HealthDataType.RESTING_HEART_RATE -> "average"
                else -> "sum"
            },
        )

        if (aggregations.isEmpty()) {
            call.reject("aggregation must not be empty")
            return
        }

        val startInstant = try {
            manager.parseInstant(call.getString("startDate"), Instant.now().minus(DEFAULT_PAST_DURATION))
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        val endInstant = try {
            manager.parseInstant(call.getString("endDate"), Instant.now())
        } catch (e: DateTimeParseException) {
            call.reject(e.message, null, e)
            return
        }

        if (endInstant.isBefore(startInstant)) {
            call.reject("endDate must be greater than or equal to startDate")
            return
        }

        call.launchSafely {
            val client = getClientOrReject(this) ?: return@launchSafely
            try {
                val result = manager.queryAggregated(client, dataType, startInstant, endInstant, bucket, aggregations)
                resolve(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission denied for aggregation: ${e.message}", e)
                reject(e.message ?: "Permission denied for aggregated data.", "permission-denied", e)
            } catch (e: IllegalArgumentException) {
                reject(e.message ?: "Unsupported aggregation.", null, e)
            } catch (e: Exception) {
                Log.w(TAG, "Aggregation failed: ${e.message}", e)
                reject(e.message ?: "Failed to query aggregated data.", "query-aggregated-failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "HealthPlugin"
        private const val DEFAULT_LIMIT = 100
        private val DEFAULT_PAST_DURATION: Duration = Duration.ofDays(1)
    }
}
