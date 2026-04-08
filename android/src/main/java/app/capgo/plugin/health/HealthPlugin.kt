package app.capgo.plugin.health

import android.content.Intent
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import app.capgo.plugin.health.background.BackgroundHealthPermissionChecker
import app.capgo.plugin.health.background.BackgroundHealthPreferences
import app.capgo.plugin.health.background.BackgroundHealthScheduler
import app.capgo.plugin.health.background.BackgroundSyncApiRequestConfig
import app.capgo.plugin.health.background.BackgroundSyncConfig
import app.capgo.plugin.health.background.BackgroundSyncInterval
import java.time.Instant
import java.time.Duration
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@CapacitorPlugin(
    name = "Health",
    permissions = [
        Permission(
            alias = "readHealthDataInBackground",
            strings = ["android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"]
        )
    ]
)
class HealthPlugin : Plugin() {
    private val pluginVersion = "7.2.14"

    private val manager = HealthManager()
    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val permissionContract = PermissionController.createRequestPermissionResultContract()
    private val backgroundPreferences by lazy { BackgroundHealthPreferences(context) }
    private val backgroundScheduler by lazy { BackgroundHealthScheduler(context) }
    private val backgroundPermissionChecker by lazy { BackgroundHealthPermissionChecker(context, manager) }

    // Store pending request data for callback
    private var pendingReadTypes: List<HealthDataType> = emptyList()
    private var pendingWriteTypes: List<HealthDataType> = emptyList()
    private var pendingIncludeWorkouts: Boolean = false
    private var pendingConfigureBackgroundSync: Boolean = false

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
            pendingConfigureBackgroundSync = false
            pendingReadTypes = emptyList()
            pendingWriteTypes = emptyList()
            pendingIncludeWorkouts = false
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
            if (pendingConfigureBackgroundSync) {
                pendingConfigureBackgroundSync = false
                continueConfigureBackgroundSync(call, client)
            } else {
                val status = manager.authorizationStatus(client, readTypes, writeTypes, includeWorkouts)
                call.resolve(status)
            }
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
        
        val systolic = call.getDouble("systolic")
        val diastolic = call.getDouble("diastolic")

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            try {
                manager.saveSample(client, dataType, value, startInstant, endInstant, metadata, systolic, diastolic)
                call.resolve()
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to save sample.", null, e)
            }
        }
    }

    @PluginMethod
    fun configureBackgroundSync(call: PluginCall) {
        rejectIfBackgroundSyncUnavailable(call)?.let { return }
        val config = try {
            parseBackgroundSyncConfig(call)
        } catch (e: Exception) {
            call.reject(e.message ?: "Failed to configure background sync.", null, e)
            return
        }

        backgroundPreferences.saveConfig(config)
        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            val requiredHealthPermissions = manager.permissionsFor(config.dataTypes, emptyList())
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            if (!grantedPermissions.containsAll(requiredHealthPermissions)) {
                pendingConfigureBackgroundSync = true
                pendingReadTypes = config.dataTypes
                pendingWriteTypes = emptyList()
                pendingIncludeWorkouts = false
                val intent = permissionContract.createIntent(context, requiredHealthPermissions)
                try {
                    startActivityForResult(call, intent, "handlePermissionResult")
                } catch (e: Exception) {
                    pendingConfigureBackgroundSync = false
                    pendingReadTypes = emptyList()
                    call.reject("Failed to launch Health Connect permission request.", null, e)
                }
                return@launch
            }

            continueConfigureBackgroundSync(call, client)
        }
    }

    @PluginMethod
    fun startBackgroundSync(call: PluginCall) {
        rejectIfBackgroundSyncUnavailable(call)?.let { return }
        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            val config = try {
                backgroundPreferences.requireConfig()
            } catch (e: Exception) {
                call.reject(e.message ?: "Background sync is not configured.", null, e)
                return@launch
            }
            if (!backgroundPermissionChecker.hasHealthConnectPermissions(client, config)) {
                call.reject("Background sync requires Health Connect read permissions for all configured dataTypes.")
                return@launch
            }
            if (!backgroundPermissionChecker.hasBackgroundHealthRuntimePermissions()) {
                call.reject("Background sync requires Android background health permissions.")
                return@launch
            }

            backgroundPreferences.saveConfig(config.withEnabled(true))
            backgroundScheduler.schedule(config)
            call.resolve(buildBackgroundSyncStatus(config, client))
        }
    }

    @PluginMethod
    fun stopBackgroundSync(call: PluginCall) {
        rejectIfBackgroundSyncUnavailable(call)?.let { return }
        pluginScope.launch {
            try {
                backgroundScheduler.cancel()
                backgroundPreferences.setEnabled(false)
                val client = if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                    HealthConnectClient.getOrCreate(context)
                } else {
                    null
                }
                val config = backgroundPreferences.getConfig()
                call.resolve(buildBackgroundSyncStatus(config, client))
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to stop background sync.", null, e)
            }
        }
    }

    @PluginMethod
    fun getBackgroundSyncStatus(call: PluginCall) {
        pluginScope.launch {
            try {
                val config = backgroundPreferences.getConfig()
                val isBgSyncAvailable = isBackgroundSyncAvailable()
                val client = if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                    HealthConnectClient.getOrCreate(context)
                } else {
                    null
                }
                val isPermissionsGranted = if (isBgSyncAvailable && config != null && client != null) {
                    backgroundPermissionChecker.hasAllPermissionsForBackgroundSync(client, config)
                } else {
                    false
                }
                call.resolve(
                    buildBackgroundSyncStatus(config, client, isBgSyncAvailable, isPermissionsGranted)
                )
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to get background sync status.", null, e)
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

    private fun parseBackgroundSyncConfig(call: PluginCall): BackgroundSyncConfig {
        val subjectId = call.getString("subjectId")
        if (subjectId.isNullOrBlank()) {
            throw IllegalArgumentException("subjectId is required.")
        }
        val getLastSync = parseBackgroundSyncRequest(call.getObject("getLastSync"), "getLastSync")
        val postSamples = parseBackgroundSyncRequest(call.getObject("postSamples"), "postSamples")
        val dataTypes = parseTypeList(call, "dataTypes").distinct()
        if (dataTypes.isEmpty()) {
            throw IllegalArgumentException("Background sync requires at least one dataType.")
        }
        val interval = BackgroundSyncInterval.from(call.getString("interval"))
            ?: throw IllegalArgumentException("interval must be one of: 15min, 30min, 1hour, 8hours, 24hours.")

        return BackgroundSyncConfig(
            subjectId = subjectId,
            getLastSync = getLastSync,
            postSamples = postSamples,
            dataTypes = dataTypes,
            interval = interval,
            enabled = backgroundPreferences.getConfig()?.enabled ?: false
        )
    }

    private fun parseBackgroundSyncRequest(rawRequest: JSObject?, key: String): BackgroundSyncApiRequestConfig {
        val request = rawRequest ?: throw IllegalArgumentException("$key configuration is required.")
        val url = request.getString("url")
        if (url.isNullOrBlank()) {
            throw IllegalArgumentException("$key.url is required.")
        }
        val headersObject = request.optJSONObject("headers")
        val headers = mutableMapOf<String, String>()
        headersObject?.let { rawHeaders ->
            val keys = rawHeaders.keys()
            while (keys.hasNext()) {
                val headerKey = keys.next()
                val value = rawHeaders.optString(headerKey)
                if (value.isNotBlank()) {
                    headers[headerKey] = value
                }
            }
        }
        return BackgroundSyncApiRequestConfig(url = url, headers = headers)
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

    private fun isBackgroundSyncAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE &&
            backgroundPermissionChecker.isBackgroundSyncSupported()
    }

    private fun rejectIfBackgroundSyncUnavailable(call: PluginCall): Unit? {
        if (isBackgroundSyncAvailable()) {
            return null
        }
        call.reject(backgroundSyncUnavailableReason())
        return Unit
    }

    private fun backgroundSyncUnavailableReason(): String {
        if (!backgroundPermissionChecker.isBackgroundSyncSupported()) {
            return "Background sync requires Android API level 35 or higher (Android 15+)."
        }
        val status = HealthConnectClient.getSdkStatus(context)
        return availabilityReason(status)
    }

    private suspend fun continueConfigureBackgroundSync(call: PluginCall, client: HealthConnectClient) {
        val config = try {
            backgroundPreferences.requireConfig()
        } catch (e: Exception) {
            call.reject(e.message ?: "Background sync is not configured.", null, e)
            return
        }

        val hasHc = backgroundPermissionChecker.hasHealthConnectPermissions(client, config)
        if (!hasHc) {
            call.reject("Background sync requires Health Connect read permissions for all configured dataTypes.")
            return
        }

        val hasRuntime = backgroundPermissionChecker.hasBackgroundHealthRuntimePermissions()

        if (!hasRuntime) {
            if (android.os.Build.VERSION.SDK_INT >= 35) {
                requestPermissionForAliases(
                    arrayOf("readHealthDataInBackground"),
                    call,
                    "handleBackgroundRuntimePermissionResult"
                )
                return
            }
        }

        if (!backgroundPermissionChecker.hasBackgroundHealthRuntimePermissions()) {
            call.reject("Background sync requires Android background health permissions.")
            return
        }

        call.resolve(buildBackgroundSyncStatus(config, client, isPermissionsGranted = true))
    }

    @PermissionCallback
    private fun handleBackgroundRuntimePermissionResult(call: PluginCall) {
        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            if (!backgroundPermissionChecker.hasBackgroundHealthRuntimePermissions()) {
                val config = try {
                    backgroundPreferences.requireConfig()
                } catch (e: Exception) {
                    call.reject(e.message ?: "Background sync is not configured.", null, e)
                    return@launch
                }
                call.resolve(buildBackgroundSyncStatus(config, client, isPermissionsGranted = false))
                return@launch
            }
            continueConfigureBackgroundSync(call, client)
        }
    }

    private suspend fun buildBackgroundSyncStatus(
        config: BackgroundSyncConfig?,
        client: HealthConnectClient?,
        isBgSyncAvailable: Boolean = isBackgroundSyncAvailable(),
        isPermissionsGranted: Boolean? = null
    ): JSObject {
        val bgPermissionsGranted = isPermissionsGranted ?: if (isBgSyncAvailable && config != null && client != null) {
            backgroundPermissionChecker.hasAllPermissionsForBackgroundSync(client, config)
        } else {
            false
        }
        // True after startBackgroundSync() persisted enabled; false after stopBackgroundSync().
        val isBgSyncScheduled = config?.enabled == true
        return JSObject().apply {
            put("isBgSyncAvailable", isBgSyncAvailable)
            put("isBgPermissionsGranted", bgPermissionsGranted)
            put("isBgSyncScheduled", isBgSyncScheduled)
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

        pluginScope.launch {
            val client = getClientOrReject(call) ?: return@launch
            try {
                val result = manager.queryWorkouts(client, workoutType, startInstant, endInstant, limit, ascending, anchor)
                call.resolve(result)
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to query workouts.", null, e)
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
        val aggregation = call.getString("aggregation") ?: "sum"

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
                val result = manager.queryAggregated(client, dataType, startInstant, endInstant, bucket, aggregation)
                call.resolve(result)
            } catch (e: IllegalArgumentException) {
                call.reject(e.message ?: "Unsupported aggregation.", null, e)
            } catch (e: Exception) {
                call.reject(e.message ?: "Failed to query aggregated data.", null, e)
            }
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 100
        private val DEFAULT_PAST_DURATION: Duration = Duration.ofDays(1)
        private const val HEALTH_CONNECT_SETTINGS_ACTION = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    }
}
