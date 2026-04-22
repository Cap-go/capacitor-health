package app.capgo.plugin.health.background

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.work.ListenableWorker
import app.capgo.plugin.health.HealthDataType
import app.capgo.plugin.health.HealthManager
import com.getcapacitor.JSArray
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BackgroundHealthCoordinator(
    private val context: Context,
    private val preferences: BackgroundHealthPreferences = BackgroundHealthPreferences(context),
    private val healthManager: HealthManager = HealthManager(),
    private val permissionChecker: BackgroundHealthPermissionChecker =
        BackgroundHealthPermissionChecker(context, healthManager),
    private val apiClient: BackgroundHealthApiClient = BackgroundHealthApiClient()
) {
    suspend fun run(): ListenableWorker.Result {
        val config = preferences.getConfig() ?: return ListenableWorker.Result.success()
        if (!config.enabled) {
            return ListenableWorker.Result.success()
        }

        val status = HealthConnectClient.getSdkStatus(context)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            return ListenableWorker.Result.retry()
        }

        val client = HealthConnectClient.getOrCreate(context)
        if (!permissionChecker.hasAllPermissionsForBackgroundSync(client, config)) {
            return ListenableWorker.Result.retry()
        }

        val lastSyncMap = try {
            apiClient.fetchLastSyncMap(config.getLastSync, config.subjectId)
        } catch (error: Exception) {
            Log.w(TAG, "Background sync failed fetching last-sync state.", error)
            return ListenableWorker.Result.retry()
        }

        val uploadedSamples = JSArray()
        var successfulReadCount = 0

        config.dataTypes.forEach { dataType ->
            try {
                val window = resolveReadWindow(lastSyncMap, dataType)
                val samples = healthManager.readSamples(
                    client = client,
                    dataType = dataType,
                    startTime = window.start,
                    endTime = window.end,
                    limit = 0,
                    ascending = true
                )
                for (index in 0 until samples.length()) {
                    uploadedSamples.put(samples.opt(index))
                }
                successfulReadCount += 1
            } catch (error: Exception) {
                Log.w(TAG, "Background sync read failed for ${dataType.identifier}. Continuing with partial upload.", error)
            }
        }

        if (successfulReadCount == 0) {
            return ListenableWorker.Result.retry()
        }

        if (uploadedSamples.length() == 0) {
            // POST /api/health/raw-payloads requires non-empty `samples`; nothing new from Health Connect this run.
            return ListenableWorker.Result.success()
        }

        return try {
            apiClient.uploadSamples(config.postSamples, config.subjectId, uploadedSamples)
            ListenableWorker.Result.success()
        } catch (error: Exception) {
            Log.w(TAG, "Background sync upload failed.", error)
            ListenableWorker.Result.retry()
        }
    }

    /**
     * Resolves the Health Connect read interval for one [dataType].
     *
     * Uses [Instant.now] at call time as the upper bound where applicable.
     *
     * 1. No last-sync entry: start = local midnight today, end = now.
     * 2. Last sync more than 24h before now: start = stored instant, end = that instant + 24h (backfill chunk).
     * 3. Otherwise: start = stored instant, end = now.
     */
    private fun resolveReadWindow(
        lastSyncMap: Map<HealthDataType, String>,
        dataType: HealthDataType
    ): ReadWindow {
        val now = Instant.now()
        val raw = lastSyncMap[dataType]
        if (raw == null) {
            val zone = ZoneId.systemDefault()
            val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            return ReadWindow(start = startOfToday, end = now)
        }

        val lastSyncTimestamp = try {
            Instant.parse(raw)
        } catch (error: Exception) {
            throw IllegalArgumentException("Invalid ISO timestamp for ${dataType.identifier}: $raw", error)
        }

        val cutoff = now.minus(MAX_WINDOW)
        return if (lastSyncTimestamp.isBefore(cutoff)) {
            ReadWindow(start = lastSyncTimestamp, end = lastSyncTimestamp.plus(MAX_WINDOW))
        } else {
            ReadWindow(start = lastSyncTimestamp, end = now)
        }
    }

    private data class ReadWindow(val start: Instant, val end: Instant)

    companion object {
        private val MAX_WINDOW: Duration = Duration.ofHours(24)
        private const val TAG = "BackgroundHealthSync"
    }
}
