package app.capgo.plugin.health.background

import android.util.Log
import app.capgo.plugin.health.HealthDataType
import com.getcapacitor.JSArray
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class BackgroundHealthApiClient {
    fun fetchLastSyncMap(config: BackgroundSyncApiRequestConfig, subjectId: String): Map<HealthDataType, String> {
        val url = urlWithSubjectPath(config.url, subjectId)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
        config.headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }
        return connection.useJsonConnection { response ->
            val json = JSONObject(response)
            val data = json.optJSONObject("data")
                ?: throw IllegalArgumentException("Missing data object in last sync response.")
            val items = data.optJSONArray("items")
                ?: throw IllegalArgumentException("Missing data.items in last sync response.")
            buildMap {
                for (i in 0 until items.length()) {
                    val row = items.optJSONObject(i) ?: continue
                    val key = row.optString("dataType", "")
                    if (key.isBlank()) {
                        continue
                    }
                    val dataType = HealthDataType.from(key)
                    if (dataType == null) {
                        Log.w(TAG, "Skipping unknown last-sync key (not in HealthDataType): $key")
                        continue
                    }
                    val timestamp = row.optString("lastSyncAt", "")
                    if (timestamp.isBlank()) {
                        throw IllegalArgumentException("Missing lastSyncAt for health data type: $key")
                    }
                    put(dataType, timestamp)
                }
            }
        }
    }

    fun uploadSamples(config: BackgroundSyncApiRequestConfig, subjectId: String, samples: JSArray) {
        val body = JSONObject().apply {
            put(
                "data",
                JSONObject().apply {
                    put("healthSubjectId", subjectId)
                    put("sourcePlatform", "HEALTH_CONNECT")
                    put("samples", samples)
                }
            )
        }
        val connection = openConnection(config, "POST")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        BufferedWriter(OutputStreamWriter(connection.outputStream)).use { writer ->
            writer.write(body.toString())
        }
        connection.useJsonConnection { }
    }

    /** MindMend zone-health: `{base}/{subjectId}` (path subject; not `?subjectId=`). */
    private fun urlWithSubjectPath(baseUrl: String, subjectId: String): String {
        val encoded = URLEncoder.encode(subjectId, StandardCharsets.UTF_8)
        return "${baseUrl.trimEnd('/')}/$encoded"
    }

    private fun openConnection(config: BackgroundSyncApiRequestConfig, method: String): HttpURLConnection {
        val connection = (URL(config.url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
        config.headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }
        return connection
    }

    private inline fun <T> HttpURLConnection.useJsonConnection(block: (String) -> T): T {
        return try {
            val statusCode = responseCode
            val stream = if (statusCode in 200..299) inputStream else errorStream
            val responseBody = stream?.let { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()
            if (statusCode !in 200..299) {
                throw IllegalStateException(
                    "Background sync API request failed with status $statusCode." +
                        if (responseBody.isNotBlank()) " Response: $responseBody" else ""
                )
            }
            block(responseBody)
        } finally {
            disconnect()
        }
    }

    companion object {
        private const val TAG = "BackgroundHealthApi"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
