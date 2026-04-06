package app.capgo.plugin.health.background

import app.capgo.plugin.health.HealthDataType
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import org.json.JSONArray
import org.json.JSONObject

data class BackgroundSyncApiRequestConfig(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("url", url)
        put("headers", JSONObject(headers))
    }

    fun toJSObject(): JSObject = JSObject().apply {
        put("url", url)
        put("headers", JSObject().apply {
            headers.forEach { (key, value) -> put(key, value) }
        })
    }

    companion object {
        fun fromJson(json: JSONObject): BackgroundSyncApiRequestConfig {
            val url = json.optString("url")
            require(url.isNotBlank()) { "Background sync URL is required." }

            val headersJson = json.optJSONObject("headers") ?: JSONObject()
            val headers = mutableMapOf<String, String>()
            val keys = headersJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                headers[key] = headersJson.optString(key)
            }
            return BackgroundSyncApiRequestConfig(url = url, headers = headers)
        }
    }
}

enum class BackgroundSyncInterval(
    val identifier: String,
    val intervalMinutes: Long
) {
    FIFTEEN_MINUTES("15min", 15L),
    THIRTY_MINUTES("30min", 30L),
    ONE_HOUR("1hour", 60L),
    EIGHT_HOURS("8hours", 480L),
    TWENTY_FOUR_HOURS("24hours", 1440L);

    companion object {
        fun from(identifier: String?): BackgroundSyncInterval? {
            return entries.firstOrNull { it.identifier == identifier }
        }
    }
}

data class BackgroundSyncConfig(
    val subjectId: String,
    val getLastSync: BackgroundSyncApiRequestConfig,
    val postSamples: BackgroundSyncApiRequestConfig,
    val dataTypes: List<HealthDataType>,
    val interval: BackgroundSyncInterval,
    val enabled: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("subjectId", subjectId)
        put("getLastSync", getLastSync.toJson())
        put("postSamples", postSamples.toJson())
        put("dataTypes", JSONArray().apply {
            dataTypes.forEach { put(it.identifier) }
        })
        put("interval", interval.identifier)
        put("enabled", enabled)
    }

    fun toOptionsJSObject(): JSObject = JSObject().apply {
        put("subjectId", subjectId)
        put("getLastSync", getLastSync.toJSObject())
        put("postSamples", postSamples.toJSObject())
        put("dataTypes", JSArray().apply {
            dataTypes.forEach { put(it.identifier) }
        })
        put("interval", interval.identifier)
    }

    fun withEnabled(enabled: Boolean): BackgroundSyncConfig = copy(enabled = enabled)

    companion object {
        fun fromJson(json: JSONObject): BackgroundSyncConfig {
            val dataTypesJson = json.optJSONArray("dataTypes") ?: JSONArray()
            val dataTypes = mutableListOf<HealthDataType>()
            for (index in 0 until dataTypesJson.length()) {
                val identifier = dataTypesJson.optString(index)
                val dataType = HealthDataType.from(identifier)
                    ?: throw IllegalArgumentException("Unsupported background sync data type: $identifier")
                dataTypes.add(dataType)
            }

            val interval = BackgroundSyncInterval.from(json.optString("interval"))
                ?: throw IllegalArgumentException("Background sync interval must be one of: 15min, 30min, 1hour, 8hours, 24hours.")
            require(dataTypes.isNotEmpty()) { "Background sync requires at least one data type." }

            return BackgroundSyncConfig(
                subjectId = json.optString("subjectId").also {
                    require(it.isNotBlank()) { "Background sync subjectId is required." }
                },
                getLastSync = BackgroundSyncApiRequestConfig.fromJson(
                    json.optJSONObject("getLastSync")
                        ?: throw IllegalArgumentException("Background sync getLastSync configuration is required.")
                ),
                postSamples = BackgroundSyncApiRequestConfig.fromJson(
                    json.optJSONObject("postSamples")
                        ?: throw IllegalArgumentException("Background sync postSamples configuration is required.")
                ),
                dataTypes = dataTypes.distinct(),
                interval = interval,
                enabled = json.optBoolean("enabled", false)
            )
        }
    }
}
