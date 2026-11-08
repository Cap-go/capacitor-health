package app.capgo.plugin.health

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlin.math.roundToLong
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Writes a complete workout to Health Connect for `saveWorkout`.
 *
 * One [ExerciseSessionRecord] (with the optional route and pause segments) plus the optional
 * distance, active-energy and heart-rate records are inserted in a single `insertRecords` call,
 * so the save is atomic. Steps are never written.
 *
 * Idempotency: when the caller passes a `clientRecordId`, the session uses it and the derived
 * records use `<id>:distance`, `<id>:energy` and `<id>:hr`. Every save gets a higher
 * `clientRecordVersion` than the previous one ([nextRecordVersion]), so a retry replaces the earlier
 * copy instead of duplicating it, and derived records the new save leaves out are deleted.
 */
class WorkoutWriter {

    /** Validated `saveWorkout` options, built by [parse]. */
    class WorkoutInput internal constructor(
        val exerciseType: Int,
        val startTime: Instant,
        val endTime: Instant,
        val clientRecordId: String?,
        val title: String?,
        val totalDistanceMeters: Double?,
        val totalEnergyKilocalories: Double?,
        val heartRateSamples: List<HeartRateRecord.Sample>,
        val route: List<ExerciseRoute.Location>,
        val pauses: List<ExerciseSegment>,
        val manualEntry: Boolean,
        val droppedHeartRateSamples: Int,
        val droppedRouteLocations: Int,
    )

    /**
     * Validates the `saveWorkout` options.
     *
     * Invalid values are rejected with an [IllegalArgumentException] that names the field and the
     * reason. Heart-rate readings and route locations outside the workout window
     * (`startDate` inclusive, `endDate` exclusive, which is what Health Connect enforces for routes)
     * are dropped and counted instead: device clocks and GPS fix times routinely land a moment
     * outside the session, and failing the whole workout for that would lose it. Route locations
     * in the same millisecond as an earlier one are dropped too, since Health Connect stores route
     * times in milliseconds and requires them to strictly increase.
     */
    fun parse(options: JSONObject): WorkoutInput {
        val workoutType = requiredString(options, "workoutType", "workoutType")
        val exerciseType = WorkoutType.fromString(workoutType)
            ?: throw IllegalArgumentException("Unsupported workoutType: $workoutType")

        val startTime = requiredInstant(options, "startDate", "startDate")
        val endTime = requiredInstant(options, "endDate", "endDate")
        if (!endTime.isAfter(startTime)) {
            throw IllegalArgumentException("endDate must be after startDate")
        }
        val isInWorkout = { time: Instant -> !time.isBefore(startTime) && time.isBefore(endTime) }

        val clientRecordId = optionalString(options, "clientRecordId", "clientRecordId")
        if (clientRecordId != null && clientRecordId.isBlank()) {
            throw IllegalArgumentException("clientRecordId must not be blank")
        }
        val title = optionalString(options, "title", "title")

        val totalDistanceMeters = optionalNumber(options, "totalDistance", "totalDistance")
        if (totalDistanceMeters != null && (totalDistanceMeters < 0.0 || totalDistanceMeters > MAX_DISTANCE_METERS)) {
            throw IllegalArgumentException("totalDistance must be between 0 and 1000000 meters, got $totalDistanceMeters")
        }

        val totalEnergyKilocalories = optionalNumber(options, "totalEnergyBurned", "totalEnergyBurned")
        if (totalEnergyKilocalories != null && (totalEnergyKilocalories < 0.0 || totalEnergyKilocalories > MAX_ENERGY_KILOCALORIES)) {
            throw IllegalArgumentException("totalEnergyBurned must be between 0 and 1000000 kilocalories, got $totalEnergyKilocalories")
        }

        val manualEntry = when (val recordingMethod = optionalString(options, "recordingMethod", "recordingMethod")) {
            null, "activelyRecorded" -> false
            "manualEntry" -> true
            else -> throw IllegalArgumentException(
                "Unsupported recordingMethod: $recordingMethod. Expected 'activelyRecorded' or 'manualEntry'."
            )
        }

        val heartRateSamples = mutableListOf<HeartRateRecord.Sample>()
        var droppedHeartRateSamples = 0
        optionalArray(options, "heartRateSamples", "heartRateSamples")?.let { array ->
            for (index in 0 until array.length()) {
                val path = "heartRateSamples[$index]"
                val item = requiredObject(array, index, path)
                val time = requiredInstant(item, "date", "$path.date")
                val bpm = requiredNumber(item, "bpm", "$path.bpm")
                if (bpm < MIN_BPM || bpm > MAX_BPM) {
                    throw IllegalArgumentException("$path.bpm must be between 1 and 300, got $bpm")
                }
                if (isInWorkout(time)) {
                    heartRateSamples.add(HeartRateRecord.Sample(time = time, beatsPerMinute = bpm.roundToLong()))
                } else {
                    droppedHeartRateSamples++
                }
            }
        }
        heartRateSamples.sortBy { it.time }

        val locations = mutableListOf<ExerciseRoute.Location>()
        var droppedRouteLocations = 0
        optionalArray(options, "route", "route")?.let { array ->
            for (index in 0 until array.length()) {
                val path = "route[$index]"
                val item = requiredObject(array, index, path)
                val time = requiredInstant(item, "date", "$path.date")
                val latitude = requiredNumber(item, "latitude", "$path.latitude")
                if (latitude < -90.0 || latitude > 90.0) {
                    throw IllegalArgumentException("$path.latitude must be between -90 and 90, got $latitude")
                }
                val longitude = requiredNumber(item, "longitude", "$path.longitude")
                if (longitude < -180.0 || longitude > 180.0) {
                    throw IllegalArgumentException("$path.longitude must be between -180 and 180, got $longitude")
                }
                val altitude = optionalNumber(item, "altitude", "$path.altitude")
                val horizontalAccuracy = optionalNumber(item, "horizontalAccuracy", "$path.horizontalAccuracy")
                if (horizontalAccuracy != null && horizontalAccuracy < 0.0) {
                    throw IllegalArgumentException("$path.horizontalAccuracy must be 0 or more, got $horizontalAccuracy")
                }
                val verticalAccuracy = optionalNumber(item, "verticalAccuracy", "$path.verticalAccuracy")
                if (verticalAccuracy != null && verticalAccuracy < 0.0) {
                    throw IllegalArgumentException("$path.verticalAccuracy must be 0 or more, got $verticalAccuracy")
                }
                if (!isInWorkout(time)) {
                    droppedRouteLocations++
                    continue
                }
                locations.add(
                    ExerciseRoute.Location(
                        time = time,
                        latitude = latitude,
                        longitude = longitude,
                        horizontalAccuracy = horizontalAccuracy?.let { Length.meters(it) },
                        verticalAccuracy = verticalAccuracy?.let { Length.meters(it) },
                        altitude = altitude?.let { Length.meters(it) },
                    )
                )
            }
        }
        // Health Connect stores route times in milliseconds and needs them to strictly increase, so
        // locations are deduplicated per millisecond. sortedBy is stable, so distinctBy keeps the
        // first location given for a millisecond.
        val route = locations.sortedBy { it.time }.distinctBy { it.time.toEpochMilli() }
        droppedRouteLocations += locations.size - route.size

        val pauses = mutableListOf<ExerciseSegment>()
        optionalArray(options, "pauses", "pauses")?.let { array ->
            for (index in 0 until array.length()) {
                val path = "pauses[$index]"
                val item = requiredObject(array, index, path)
                val pauseStart = requiredInstant(item, "startDate", "$path.startDate")
                val pauseEnd = requiredInstant(item, "endDate", "$path.endDate")
                if (!pauseEnd.isAfter(pauseStart)) {
                    throw IllegalArgumentException("$path.endDate must be after $path.startDate")
                }
                if (pauseStart.isBefore(startTime) || pauseEnd.isAfter(endTime)) {
                    throw IllegalArgumentException("$path must lie within the workout (between startDate and endDate)")
                }
                // Pause is a universal segment type in Health Connect: valid for every exercise type.
                pauses.add(
                    ExerciseSegment(
                        startTime = pauseStart,
                        endTime = pauseEnd,
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
                    )
                )
            }
        }
        pauses.sortBy { it.startTime }
        pauses.zipWithNext().forEach { (previous, next) ->
            if (previous.endTime.isAfter(next.startTime)) {
                throw IllegalArgumentException(
                    "pauses must not overlap: the pause ending at ${previous.endTime} overlaps the one starting at ${next.startTime}"
                )
            }
        }

        return WorkoutInput(
            exerciseType = exerciseType,
            startTime = startTime,
            endTime = endTime,
            clientRecordId = clientRecordId,
            title = title,
            totalDistanceMeters = totalDistanceMeters,
            totalEnergyKilocalories = totalEnergyKilocalories,
            heartRateSamples = heartRateSamples,
            route = route,
            pauses = pauses,
            manualEntry = manualEntry,
            droppedHeartRateSamples = droppedHeartRateSamples,
            droppedRouteLocations = droppedRouteLocations,
        )
    }

    /**
     * Saves [input] in one `insertRecords` call and returns the `saveWorkout` result.
     *
     * Only the exercise write permission is required: without it this throws
     * [SecurityException] and nothing is written. The Health Connect sheet lets the user untick
     * single types, so a missing distance / active-calories / heart-rate permission does not fail
     * the save: that record is left out and reported in `skippedRecords`. A missing route
     * permission likewise only drops the route (`routeSaved: false`).
     *
     * With a `clientRecordId`, the derived records this save does not write (for example a distance
     * the caller no longer sends) are then deleted, for the types the app can write, so a
     * replacement does not keep them from the earlier save.
     */
    suspend fun save(client: HealthConnectClient, input: WorkoutInput): JSObject =
        WRITE_LOCK.withLock { insert(client, input) }

    private suspend fun insert(client: HealthConnectClient, input: WorkoutInput): JSObject {
        val granted = client.permissionController.getGrantedPermissions()

        if (EXERCISE_WRITE_PERMISSION !in granted) {
            throw SecurityException(
                "Missing Health Connect write permission for workouts. " +
                    "Request write access to 'workouts' with requestAuthorization() first."
            )
        }

        val writeDistance = input.totalDistanceMeters != null && DISTANCE_WRITE_PERMISSION in granted
        val writeEnergy = input.totalEnergyKilocalories != null && ACTIVE_CALORIES_WRITE_PERMISSION in granted
        val writeHeartRate = input.heartRateSamples.isNotEmpty() && HEART_RATE_WRITE_PERMISSION in granted
        val skippedRecords = JSArray().apply {
            if (input.totalDistanceMeters != null && !writeDistance) put("distance")
            if (input.totalEnergyKilocalories != null && !writeEnergy) put("energy")
            if (input.heartRateSamples.isNotEmpty() && !writeHeartRate) put("heartRate")
        }

        val routeSaved = input.route.isNotEmpty() && HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE in granted
        val version = nextRecordVersion()
        val startZoneOffset = zoneOffset(input.startTime)
        val endZoneOffset = zoneOffset(input.endTime)

        val records = mutableListOf<Record>()
        records.add(
            ExerciseSessionRecord(
                startTime = input.startTime,
                startZoneOffset = startZoneOffset,
                endTime = input.endTime,
                endZoneOffset = endZoneOffset,
                metadata = metadata(input, null, version),
                exerciseType = input.exerciseType,
                title = input.title,
                segments = input.pauses,
                exerciseRoute = if (routeSaved) ExerciseRoute(input.route) else null,
            )
        )
        input.totalDistanceMeters?.takeIf { writeDistance }?.let { meters ->
            records.add(
                DistanceRecord(
                    startTime = input.startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = input.endTime,
                    endZoneOffset = endZoneOffset,
                    distance = Length.meters(meters),
                    metadata = metadata(input, DISTANCE_ID_SUFFIX, version),
                )
            )
        }
        input.totalEnergyKilocalories?.takeIf { writeEnergy }?.let { kilocalories ->
            records.add(
                ActiveCaloriesBurnedRecord(
                    startTime = input.startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = input.endTime,
                    endZoneOffset = endZoneOffset,
                    energy = Energy.kilocalories(kilocalories),
                    metadata = metadata(input, ENERGY_ID_SUFFIX, version),
                )
            )
        }
        if (writeHeartRate) {
            records.add(
                HeartRateRecord(
                    startTime = input.startTime,
                    startZoneOffset = startZoneOffset,
                    endTime = input.endTime,
                    endZoneOffset = endZoneOffset,
                    samples = input.heartRateSamples,
                    metadata = metadata(input, HEART_RATE_ID_SUFFIX, version),
                )
            )
        }

        // Health Connect returns the ids in the order of the inserted records: the session is first.
        val response = client.insertRecords(records)
        val platformId = response.recordIdsList.firstOrNull()
            ?: throw IllegalStateException("Health Connect did not return an id for the saved workout.")

        input.clientRecordId?.let { id ->
            val written = setOfNotNull(
                DistanceRecord::class.takeIf { writeDistance },
                ActiveCaloriesBurnedRecord::class.takeIf { writeEnergy },
                HeartRateRecord::class.takeIf { writeHeartRate },
            )
            COMPANION_RECORDS.forEach { (recordClass, idSuffix) ->
                if (recordClass !in written && HealthPermission.getWritePermission(recordClass) in granted) {
                    deleteCompanionRecords(client, recordClass, listOf(companionClientRecordId(id, idSuffix)))
                }
            }
        }

        return JSObject().apply {
            put("platformId", platformId)
            put("routeSaved", routeSaved)
            put("skippedRecords", skippedRecords)
            put("droppedRouteLocations", input.droppedRouteLocations)
            put("droppedHeartRateSamples", input.droppedHeartRateSamples)
        }
    }

    private fun metadata(input: WorkoutInput, idSuffix: String?, version: Long): Metadata {
        val clientRecordId = input.clientRecordId?.let { id -> if (idSuffix == null) id else companionClientRecordId(id, idSuffix) }
        return recordMetadata(clientRecordId, input.manualEntry, version)
    }

    private fun zoneOffset(instant: Instant): ZoneOffset? {
        return ZoneId.systemDefault().rules.getOffset(instant)
    }

    /** JSON `null` and a missing key both mean "not provided". */
    private fun valueOrNull(obj: JSONObject, key: String): Any? {
        val value = obj.opt(key)
        return if (value == null || value == JSONObject.NULL) null else value
    }

    private fun optionalString(obj: JSONObject, key: String, path: String): String? {
        val value = valueOrNull(obj, key) ?: return null
        return value as? String ?: throw IllegalArgumentException("$path must be a string")
    }

    private fun requiredString(obj: JSONObject, key: String, path: String): String {
        val value = optionalString(obj, key, path)
        if (value.isNullOrBlank()) {
            throw IllegalArgumentException("$path is required")
        }
        return value
    }

    private fun optionalNumber(obj: JSONObject, key: String, path: String): Double? {
        val value = valueOrNull(obj, key) ?: return null
        val number = (value as? Number)?.toDouble() ?: throw IllegalArgumentException("$path must be a number")
        if (number.isNaN() || number.isInfinite()) {
            throw IllegalArgumentException("$path must be a finite number")
        }
        return number
    }

    private fun requiredNumber(obj: JSONObject, key: String, path: String): Double {
        return optionalNumber(obj, key, path) ?: throw IllegalArgumentException("$path is required")
    }

    private fun requiredInstant(obj: JSONObject, key: String, path: String): Instant {
        val value = requiredString(obj, key, path)
        return try {
            Instant.parse(value)
        } catch (e: DateTimeParseException) {
            // Older Android runtimes only accept the `Z` suffix in Instant.parse; accept offsets too.
            try {
                OffsetDateTime.parse(value).toInstant()
            } catch (fallback: DateTimeParseException) {
                throw IllegalArgumentException("$path must be an ISO 8601 date, got $value")
            }
        }
    }

    private fun optionalArray(obj: JSONObject, key: String, path: String): JSONArray? {
        val value = valueOrNull(obj, key) ?: return null
        return value as? JSONArray ?: throw IllegalArgumentException("$path must be an array")
    }

    private fun requiredObject(array: JSONArray, index: Int, path: String): JSONObject {
        return array.opt(index) as? JSONObject ?: throw IllegalArgumentException("$path must be an object")
    }

    companion object {
        // Limits enforced by Health Connect itself: HeartRateRecord.Sample, DistanceRecord.MAX_DISTANCE
        // and ActiveCaloriesBurnedRecord.MAX_ENERGY. Checked here so the error names the option.
        private const val MIN_BPM = 1.0
        private const val MAX_BPM = 300.0
        private const val MAX_DISTANCE_METERS = 1_000_000.0
        private const val MAX_ENERGY_KILOCALORIES = 1_000_000.0

        private val PHONE_DEVICE = Device(type = Device.TYPE_PHONE)

        private const val TAG = "WorkoutWriter"

        /** Suffixes of the companion records' clientRecordIds (`<id>:distance`, ...). */
        private const val DISTANCE_ID_SUFFIX = "distance"
        private const val ENERGY_ID_SUFFIX = "energy"
        private const val HEART_RATE_ID_SUFFIX = "hr"

        /** Records saved next to the exercise session, with their clientRecordId suffix; SampleDeleter uses them too. */
        internal val COMPANION_RECORDS: List<Pair<KClass<out Record>, String>> = listOf(
            DistanceRecord::class to DISTANCE_ID_SUFFIX,
            ActiveCaloriesBurnedRecord::class to ENERGY_ID_SUFFIX,
            HeartRateRecord::class to HEART_RATE_ID_SUFFIX,
        )

        /**
         * Serializes saveWorkout and deleteSamples. Both make several Health Connect calls (the
         * insert or delete, then the companion records), so a save and a delete of the same
         * `clientRecordId` must not interleave: the delete could remove the new save's companions.
         */
        internal val WRITE_LOCK = Mutex()

        internal fun companionClientRecordId(clientRecordId: String, idSuffix: String): String = "$clientRecordId:$idSuffix"

        /**
         * Deletes the companion records with [clientRecordIds] of [recordClass], skipping the ids that
         * match nothing.
         *
         * A workout only has the companions it was saved with, and Health Connect documents that a
         * delete fails as a whole when an id does not exist (the platform on Android 14+ ignores such
         * ids). So when the call fails, each id is deleted on its own and a failing one is logged and
         * skipped: the workout itself is already saved or deleted at this point.
         */
        internal suspend fun deleteCompanionRecords(
            client: HealthConnectClient,
            recordClass: KClass<out Record>,
            clientRecordIds: List<String>,
        ) {
            if (clientRecordIds.isEmpty() || tryDelete(client, recordClass, clientRecordIds) || clientRecordIds.size == 1) {
                return
            }
            clientRecordIds.forEach { id -> tryDelete(client, recordClass, listOf(id)) }
        }

        private suspend fun tryDelete(client: HealthConnectClient, recordClass: KClass<out Record>, clientRecordIds: List<String>): Boolean {
            return try {
                client.deleteRecords(recordClass, emptyList(), clientRecordIds)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Skipped deleting ${recordClass.simpleName} $clientRecordIds (it may not exist): ${e.message}")
                false
            }
        }

        private var lastRecordVersion = 0L

        /**
         * `clientRecordVersion` for a save (saveWorkout and saveSample): the current time in
         * milliseconds, but always higher than the previous version handed out, so two saves in the
         * same millisecond, or after the clock moved back while the app runs, still replace each
         * other. Health Connect keeps the record with the highest version.
         */
        @Synchronized
        internal fun nextRecordVersion(): Long {
            lastRecordVersion = maxOf(System.currentTimeMillis(), lastRecordVersion + 1)
            return lastRecordVersion
        }

        /**
         * Health Connect metadata for a record this plugin writes (saveWorkout and saveSample).
         * With a [clientRecordId], [version] makes a later save replace the earlier one. Actively
         * recorded records are attributed to a phone device; manual entries have no device.
         */
        internal fun recordMetadata(clientRecordId: String?, manualEntry: Boolean, version: Long): Metadata {
            return if (manualEntry) {
                if (clientRecordId != null) Metadata.manualEntry(clientRecordId, version) else Metadata.manualEntry()
            } else {
                if (clientRecordId != null) {
                    Metadata.activelyRecorded(PHONE_DEVICE, clientRecordId, version)
                } else {
                    Metadata.activelyRecorded(PHONE_DEVICE)
                }
            }
        }

        private val EXERCISE_WRITE_PERMISSION = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        private val DISTANCE_WRITE_PERMISSION = HealthPermission.getWritePermission(DistanceRecord::class)
        private val ACTIVE_CALORIES_WRITE_PERMISSION = HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
        private val HEART_RATE_WRITE_PERMISSION = HealthPermission.getWritePermission(HeartRateRecord::class)
    }
}
