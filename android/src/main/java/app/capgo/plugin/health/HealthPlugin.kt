package app.capgo.plugin.health

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "Health")
class HealthPlugin : Plugin() {
    private val pluginVersion = "7.2.14"
    private val manager = HealthManager()
    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val permissionContract = PermissionController.createRequestPermissionResultContract()

    // Store pending request data for callback
    private var pendingReadTypes: List<HealthDataType> = emptyList()
    private var pendingWriteTypes: List<HealthDataType> = emptyList()
    private var pendingIncludeWorkouts: Boolean = false

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
            parseTypeList(call, "write")
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            val permissions = manager.permissionsFor(readTypes, writeTypes, includeWorkouts)

            if (permissions.isEmpty()) {
                val status = manager.authorizationStatus(client, readTypes, writeTypes, includeWorkouts)
                call.resolve(status)
                return@launch
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                val status = manager.authorizationStatus(client, readTypes, writeTypes, includeWorkouts)
                call.resolve(status)
                return@launch
            }

            // Store types for callback
            pendingReadTypes = readTypes
            pendingWriteTypes = writeTypes
            pendingIncludeWorkouts = includeWorkouts

            // Create intent using the Health Connect permission contract
            val intent = permissionContract.createIntent(context, permissions)

            try {
                startActivityForResult(call, intent, "handlePermissionResult")
            } catch (e: Exception) {
                pendingReadTypes = emptyList()
                pendingWriteTypes = emptyList()
                call.reject("Failed to launch Health Connect permission request.", null, e)
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
        pendingReadTypes = emptyList()
        pendingWriteTypes = emptyList()
        pendingIncludeWorkouts = false

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            val status = manager.authorizationStatus(client, readTypes, writeTypes, includeWorkouts)
            call.resolve(status)
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
            parseTypeList(call, "write")
        } catch (e: IllegalArgumentException) {
            call.reject(e.message, null, e)
            return
        }

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            val status = manager.authorizationStatus(client, readTypes, writeTypes, includeWorkouts)
            call.resolve(status)
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

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            try {
                val samples = manager.readSamples(client, dataType, startInstant, endInstant, limit, ascending)
                val result = JSObject().apply { put("samples", samples) }
                call.resolve(result)
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to read samples.", null, e)
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

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            try {
                manager.saveSample(client, dataType, value, startInstant, endInstant, metadata)
                call.resolve()
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to save sample.", null, e)
            }
        }
    }

    private fun parseTypeList(call: PluginCall, key: String): List<HealthDataType> {
        val array = call.getArray(key) ?: JSArray()
        val result = mutableListOf<HealthDataType>()
        for (i in 0 until array.length()) {
            val identifier = array.optString(i, null) ?: continue
            val dataType = HealthDataType.from(identifier)
                ?: throw IllegalArgumentException("Unsupported data type: $identifier")
            result.add(dataType)
        }
        return result
    }

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
            val intent = Intent(HEALTH_CONNECT_SETTINGS_ACTION)
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

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            try {
                val workouts = manager.queryWorkouts(client, workoutType, startInstant, endInstant, limit, ascending)
                val result = JSObject().apply { put("workouts", workouts) }
                call.resolve(result)
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to query workouts.", null, e)
            }
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 100
        private val DEFAULT_PAST_DURATION: Duration = Duration.ofDays(1)
        private const val HEALTH_CONNECT_SETTINGS_ACTION = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    }
}
