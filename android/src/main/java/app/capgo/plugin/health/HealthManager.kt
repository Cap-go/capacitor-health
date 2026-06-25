package app.capgo.plugin.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.collections.buildSet
import kotlin.math.min
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthManager {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    /**
     * Whether the connected Health Connect provider supports the
     * READ_HEALTH_DATA_HISTORY permission. The permission only exists on sufficiently
     * new providers (Android 14 extension 13+ or Health Connect APK 171302+); on older
     * but otherwise supported providers it can never be granted, so it must not be added
     * to a permission request (it would never appear in getGrantedPermissions() and would
     * leave the request flow permanently unsatisfiable).
     *
     * See https://developer.android.com/health-and-fitness/health-connect/features/availability
     */
    fun isHistoryAccessAvailable(client: HealthConnectClient): Boolean =
        client.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    fun permissionsFor(
        readTypes: Collection<HealthDataType>,
        writeTypes: Collection<HealthDataType>,
        includeWorkouts: Boolean = false,
        includeHistoryAccess: Boolean = false
    ): Set<String> = buildSet {
        readTypes.forEach { add(it.readPermission) }
        writeTypes.forEach { add(it.writePermission) }
        // Include workout read permission if explicitly requested
        if (includeWorkouts) {
            add(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
        }
        // Include history-access permission if explicitly requested so reads can
        // exceed Health Connect's default 30-day window. Requires the consumer to
        // declare READ_HEALTH_DATA_HISTORY in their AndroidManifest. Callers must
        // gate this on isHistoryAccessAvailable(): adding the permission on a provider
        // that does not support it makes the request flow permanently unsatisfiable.
        if (includeHistoryAccess) {
            add(READ_HEALTH_DATA_HISTORY_PERMISSION)
        }
    }

    suspend fun authorizationStatus(
        client: HealthConnectClient,
        readTypes: Collection<HealthDataType>,
        writeTypes: Collection<HealthDataType>,
        includeWorkouts: Boolean = false,
        includeHistoryAccess: Boolean = false
    ): JSObject {
        val granted = client.permissionController.getGrantedPermissions()

        val readAuthorized = JSArray()
        val readDenied = JSArray()
        readTypes.forEach { type ->
            if (granted.contains(type.readPermission)) {
                readAuthorized.put(type.identifier)
            } else {
                readDenied.put(type.identifier)
            }
        }

        // Check workout permission if requested
        if (includeWorkouts) {
            val workoutPermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
            if (granted.contains(workoutPermission)) {
                readAuthorized.put("workouts")
            } else {
                readDenied.put("workouts")
            }
        }

        val writeAuthorized = JSArray()
        val writeDenied = JSArray()
        writeTypes.forEach { type ->
            if (granted.contains(type.writePermission)) {
                writeAuthorized.put(type.identifier)
            } else {
                writeDenied.put(type.identifier)
            }
        }

        return JSObject().apply {
            put("readAuthorized", readAuthorized)
            put("readDenied", readDenied)
            put("writeAuthorized", writeAuthorized)
            put("writeDenied", writeDenied)
            // Report history-access status as top-level flags, only when it was requested.
            // historyAccessAvailable distinguishes "the provider is too old to ever grant
            // this" from "the user denied it"; historyAccessAuthorized is necessarily false
            // when the feature is unavailable.
            if (includeHistoryAccess) {
                val historyAccessAvailable = isHistoryAccessAvailable(client)
                put("historyAccessAvailable", historyAccessAvailable)
                put(
                    "historyAccessAuthorized",
                    historyAccessAvailable && granted.contains(READ_HEALTH_DATA_HISTORY_PERMISSION)
                )
            }
        }
    }

    suspend fun readSamples(
        client: HealthConnectClient,
        dataType: HealthDataType,
        startTime: Instant,
        endTime: Instant,
        limit: Int,
        ascending: Boolean
    ): JSArray {
        val samples = mutableListOf<Pair<Instant, JSObject>>()
        when (dataType) {
            HealthDataType.STEPS -> readRecords(client, StepsRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    record.count.toDouble(),
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
            HealthDataType.DISTANCE -> readRecords(client, DistanceRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    record.distance.inMeters,
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
            HealthDataType.CALORIES -> readRecords(client, ActiveCaloriesBurnedRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    record.energy.inKilocalories,
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
            HealthDataType.WEIGHT -> readRecords(client, WeightRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.weight.inKilograms,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.HEART_RATE -> readRecords(client, HeartRateRecord::class, startTime, endTime, limit) { record ->
                record.samples.forEach { sample ->
                    val payload = createSamplePayload(
                        dataType,
                        sample.time,
                        sample.time,
                        sample.beatsPerMinute.toDouble(),
                        record.metadata
                    )
                    samples.add(sample.time to payload)
                }
            }
            HealthDataType.SLEEP -> readRecords(client, SleepSessionRecord::class, startTime, endTime, limit) { record ->
                val durationMinutes = Duration.between(record.startTime, record.endTime).toMinutes().toDouble()
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    durationMinutes,
                    record.metadata
                )
                // Expose individual sleep stages when present (Samsung Health, Mi Health,
                // Apple Health, and other writers typically populate SleepSessionRecord.stages).
                val stagesArray = JSArray()
                record.stages.forEach { stage ->
                    val stageString = mapSleepStageToString(stage.stage) ?: return@forEach
                    val stageObj = JSObject()
                    stageObj.put("startDate", formatter.format(stage.startTime))
                    stageObj.put("endDate", formatter.format(stage.endTime))
                    stageObj.put("stage", stageString)
                    stageObj.put("durationMinutes", Duration.between(stage.startTime, stage.endTime).toMinutes().toDouble())
                    stagesArray.put(stageObj)
                }
                val hasNormalizedStages = stagesArray.length() > 0
                if (hasNormalizedStages) {
                    payload.put("stages", stagesArray)
                }
                payload.put("hasStageData", hasNormalizedStages)

                samples.add(record.startTime to payload)
            }
            HealthDataType.RESPIRATORY_RATE -> readRecords(client, RespiratoryRateRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.rate,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.OXYGEN_SATURATION -> readRecords(client, OxygenSaturationRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.percentage.value,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.RESTING_HEART_RATE -> readRecords(client, RestingHeartRateRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.beatsPerMinute.toDouble(),
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.HEART_RATE_VARIABILITY -> readRecords(client, HeartRateVariabilityRmssdRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.heartRateVariabilityMillis,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.VO2_MAX -> readRecords(client, Vo2MaxRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.vo2MillilitersPerMinuteKilogram,
                    record.metadata
                )
                payload.put("measurementMethod", record.measurementMethod)
                samples.add(record.time to payload)
            }
            HealthDataType.BLOOD_PRESSURE -> readRecords(client, BloodPressureRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.systolic.inMillimetersOfMercury,
                    record.metadata
                )
                payload.put("systolic", record.systolic.inMillimetersOfMercury)
                payload.put("diastolic", record.diastolic.inMillimetersOfMercury)
                samples.add(record.time to payload)
            }
            HealthDataType.BLOOD_GLUCOSE -> readRecords(client, BloodGlucoseRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.level.inMilligramsPerDeciliter,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.BODY_TEMPERATURE -> readRecords(client, BodyTemperatureRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.temperature.inCelsius,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.HEIGHT -> readRecords(client, HeightRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.height.inMeters * 100.0, // Convert to centimeters
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.FLIGHTS_CLIMBED -> readRecords(client, FloorsClimbedRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    record.floors,
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
            HealthDataType.DISTANCE_CYCLING -> readRecords(client, DistanceRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    record.distance.inMeters,
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
            HealthDataType.BODY_FAT -> readRecords(client, BodyFatRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.percentage.value,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.BASAL_BODY_TEMPERATURE -> readRecords(client, BasalBodyTemperatureRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.temperature.inCelsius,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.BASAL_CALORIES -> readRecords(client, BasalMetabolicRateRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.time,
                    record.time,
                    record.basalMetabolicRate.inKilocaloriesPerDay,
                    record.metadata
                )
                samples.add(record.time to payload)
            }
            HealthDataType.TOTAL_CALORIES -> readRecords(client, TotalCaloriesBurnedRecord::class, startTime, endTime, limit) { record ->
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    record.energy.inKilocalories,
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
            HealthDataType.MINDFULNESS -> readRecords(client, MindfulnessSessionRecord::class, startTime, endTime, limit) { record ->
                val durationMinutes = Duration.between(record.startTime, record.endTime).toMinutes().toDouble()
                val payload = createSamplePayload(
                    dataType,
                    record.startTime,
                    record.endTime,
                    durationMinutes,
                    record.metadata
                )
                samples.add(record.startTime to payload)
            }
        }

        val sorted = samples.sortedBy { it.first }
        val ordered = if (ascending) sorted else sorted.asReversed()
        val limited = if (limit > 0) ordered.take(limit) else ordered

        val array = JSArray()
        limited.forEach { array.put(it.second) }
        return array
    }

    private suspend fun <T : Record> readRecords(
        client: HealthConnectClient,
        recordClass: kotlin.reflect.KClass<T>,
        startTime: Instant,
        endTime: Instant,
        limit: Int,
        consumer: (record: T) -> Unit
    ) {
        var pageToken: String? = null
        val pageSize = if (limit > 0) min(limit, MAX_PAGE_SIZE) else DEFAULT_PAGE_SIZE
        var fetched = 0

        do {
            val request = ReadRecordsRequest(
                recordType = recordClass,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                pageSize = pageSize,
                pageToken = pageToken
            )
            val response = client.readRecords(request)
            response.records.forEach { record ->
                consumer(record)
            }
            fetched += response.records.size
            pageToken = response.pageToken
        } while (pageToken != null && (limit <= 0 || fetched < limit))
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun saveSample(
        client: HealthConnectClient,
        dataType: HealthDataType,
        value: Double,
        startTime: Instant,
        endTime: Instant,
        metadata: Map<String, String>?,
        systolic: Double?,
        diastolic: Double?,
        mindfulnessSessionType: String?
    ) {
        val recordMetadata = Metadata.manualEntry()

        when (dataType) {
            HealthDataType.STEPS -> {
                val record = StepsRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    count = value.toLong().coerceAtLeast(0),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.DISTANCE -> {
                val record = DistanceRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    distance = Length.meters(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.CALORIES -> {
                val record = ActiveCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    energy = Energy.kilocalories(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.WEIGHT -> {
                val record = WeightRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    weight = Mass.kilograms(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.HEART_RATE -> {
                val samples = listOf(HeartRateRecord.Sample(time = startTime, beatsPerMinute = value.toBpmLong()))
                val record = HeartRateRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    samples = samples,
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.SLEEP -> {
                val record = SleepSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.RESPIRATORY_RATE -> {
                val record = RespiratoryRateRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    rate = value,
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.OXYGEN_SATURATION -> {
                val record = OxygenSaturationRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    percentage = Percentage(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.RESTING_HEART_RATE -> {
                val record = RestingHeartRateRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    beatsPerMinute = value.toBpmLong(),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.HEART_RATE_VARIABILITY -> {
                val record = HeartRateVariabilityRmssdRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    heartRateVariabilityMillis = value,
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.VO2_MAX -> {
                val record = Vo2MaxRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    metadata = recordMetadata,
                    vo2MillilitersPerMinuteKilogram = value
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.BLOOD_PRESSURE -> {
                if (systolic == null || diastolic == null) {
                    throw IllegalArgumentException("Blood pressure requires both systolic and diastolic values")
                }
                val record = BloodPressureRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    metadata = recordMetadata,
                    systolic = androidx.health.connect.client.units.Pressure.millimetersOfMercury(systolic),
                    diastolic = androidx.health.connect.client.units.Pressure.millimetersOfMercury(diastolic)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.BLOOD_GLUCOSE -> {
                val record = BloodGlucoseRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    metadata = recordMetadata,
                    level = androidx.health.connect.client.units.BloodGlucose.milligramsPerDeciliter(value)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.BODY_TEMPERATURE -> {
                val record = BodyTemperatureRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    metadata = recordMetadata,
                    temperature = androidx.health.connect.client.units.Temperature.celsius(value)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.HEIGHT -> {
                val record = HeightRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    height = Length.meters(value / 100.0), // Convert from centimeters to meters
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.FLIGHTS_CLIMBED -> {
                val record = FloorsClimbedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    floors = value,
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.DISTANCE_CYCLING -> {
                val record = DistanceRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    distance = Length.meters(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.BODY_FAT -> {
                val record = BodyFatRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    percentage = Percentage(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.BASAL_BODY_TEMPERATURE -> {
                val record = BasalBodyTemperatureRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    metadata = recordMetadata,
                    temperature = androidx.health.connect.client.units.Temperature.celsius(value)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.BASAL_CALORIES -> {
                val record = BasalMetabolicRateRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    basalMetabolicRate = Power.kilocaloriesPerDay(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.TOTAL_CALORIES -> {
                val record = TotalCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    energy = Energy.kilocalories(value),
                    metadata = recordMetadata
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.MINDFULNESS -> {
                val record = MindfulnessSessionRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    metadata = recordMetadata,
                    mindfulnessSessionType = mindfulnessSessionType.toMindfulnessSessionType()
                )
                client.insertRecords(listOf(record))
            }
        }
    }

    fun parseInstant(value: String?, defaultInstant: Instant): Instant {
        if (value.isNullOrBlank()) {
            return defaultInstant
        }
        return Instant.parse(value)
    }
private fun String?.toMindfulnessSessionType(): Int {
        val normalized = this?.trim()
        if (normalized.isNullOrEmpty()) {
            return MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
        }
        return MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_STRING_TO_INT_MAP[normalized]
            ?: throw IllegalArgumentException("Unsupported mindfulnessSessionType: $normalized")
    }
private fun mapSleepStageToString(stage: Int): String? {
        return when (stage) {
            SleepSessionRecord.STAGE_TYPE_AWAKE -> "awake"
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "awake"
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> "asleep"
            SleepSessionRecord.STAGE_TYPE_LIGHT -> "light"
            SleepSessionRecord.STAGE_TYPE_DEEP -> "deep"
            SleepSessionRecord.STAGE_TYPE_REM -> "rem"
            else -> null  // OUT_OF_BED, UNKNOWN: filtered out by caller
        }
    }
private fun createSamplePayload(
        dataType: HealthDataType,
        startTime: Instant,
        endTime: Instant,
        value: Double,
        metadata: Metadata
    ): JSObject {
        val payload = JSObject()
        payload.put("dataType", dataType.identifier)
        payload.put("value", value)
        payload.put("unit", dataType.unit)
        payload.put("startDate", formatter.format(startTime))
        payload.put("endDate", formatter.format(endTime))

        val dataOrigin = metadata.dataOrigin
        payload.put("sourceId", dataOrigin.packageName)
        payload.put("sourceName", dataOrigin.packageName)
        metadata.device?.let { device ->
            val manufacturer = device.manufacturer?.takeIf { it.isNotBlank() }
            val model = device.model?.takeIf { it.isNotBlank() }
            val label = listOfNotNull(manufacturer, model).joinToString(" ").trim()
            if (label.isNotEmpty()) {
                payload.put("sourceName", label)
            }
        }

        payload.put("platformId", metadata.id)

        return payload
    }

    private fun zoneOffset(instant: Instant): ZoneOffset? {
        return ZoneId.systemDefault().rules.getOffset(instant)
    }

    private fun Double.toBpmLong(): Long {
        return java.lang.Math.round(this.coerceAtLeast(0.0))
    }

    private fun validateAggregation(dataType: HealthDataType, aggregation: String) {
        val supportedAggregations = when (dataType) {
            HealthDataType.STEPS,
            HealthDataType.DISTANCE,
            HealthDataType.CALORIES -> setOf("sum")
            HealthDataType.HEART_RATE,
            HealthDataType.WEIGHT,
            HealthDataType.RESTING_HEART_RATE -> setOf("average", "min", "max")
            else -> emptySet()
        }

        if (aggregation !in supportedAggregations) {
            throw IllegalArgumentException("Unsupported aggregation '$aggregation' for ${dataType.identifier}")
        }
    }

    private fun aggregateMetrics(dataType: HealthDataType) = when (dataType) {
        HealthDataType.STEPS -> setOf(StepsRecord.COUNT_TOTAL)
        HealthDataType.DISTANCE -> setOf(DistanceRecord.DISTANCE_TOTAL)
        HealthDataType.CALORIES -> setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        HealthDataType.HEART_RATE -> setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MAX, HeartRateRecord.BPM_MIN)
        HealthDataType.WEIGHT -> setOf(WeightRecord.WEIGHT_AVG, WeightRecord.WEIGHT_MAX, WeightRecord.WEIGHT_MIN)
        HealthDataType.RESTING_HEART_RATE -> setOf(RestingHeartRateRecord.BPM_AVG, RestingHeartRateRecord.BPM_MAX, RestingHeartRateRecord.BPM_MIN)
        else -> throw IllegalArgumentException("Unsupported data type for aggregation: ${dataType.identifier}")
    }

    private fun aggregateValue(dataType: HealthDataType, aggregation: String, result: AggregationResult): Double? {
        return when (dataType) {
            HealthDataType.STEPS -> result[StepsRecord.COUNT_TOTAL]?.toDouble()
            HealthDataType.DISTANCE -> result[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            HealthDataType.CALORIES -> result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            HealthDataType.HEART_RATE -> when (aggregation) {
                "average" -> result[HeartRateRecord.BPM_AVG]?.toDouble()
                "max" -> result[HeartRateRecord.BPM_MAX]?.toDouble()
                "min" -> result[HeartRateRecord.BPM_MIN]?.toDouble()
                else -> null
            }
            HealthDataType.WEIGHT -> when (aggregation) {
                "average" -> result[WeightRecord.WEIGHT_AVG]?.inKilograms
                "max" -> result[WeightRecord.WEIGHT_MAX]?.inKilograms
                "min" -> result[WeightRecord.WEIGHT_MIN]?.inKilograms
                else -> null
            }
            HealthDataType.RESTING_HEART_RATE -> when (aggregation) {
                "average" -> result[RestingHeartRateRecord.BPM_AVG]?.toDouble()
                "max" -> result[RestingHeartRateRecord.BPM_MAX]?.toDouble()
                "min" -> result[RestingHeartRateRecord.BPM_MIN]?.toDouble()
                else -> null
            }
            else -> null
        }
    }

    suspend fun queryAggregated(
        client: HealthConnectClient,
        dataType: HealthDataType,
        startTime: Instant,
        endTime: Instant,
        bucket: String,
        aggregation: String
    ): JSObject {
        // Sleep aggregation is not directly supported like other metrics
        if (dataType == HealthDataType.SLEEP) {
            throw IllegalArgumentException("Aggregated queries are not supported for sleep data. Use readSamples instead.")
        }
        
        // Instantaneous measurement records don't support aggregation in Health Connect
        // These data types should use readSamples instead
        if (dataType == HealthDataType.RESPIRATORY_RATE || 
            dataType == HealthDataType.OXYGEN_SATURATION || 
            dataType == HealthDataType.HEART_RATE_VARIABILITY ||
            dataType == HealthDataType.VO2_MAX) {
            throw IllegalArgumentException("Aggregated queries are not supported for ${dataType.identifier}. Use readSamples instead.")
        }

        validateAggregation(dataType, aggregation)

        val metrics = aggregateMetrics(dataType)
        val samples = JSArray()

        if (bucket == "month") {
            val zoneId = ZoneId.systemDefault()
            val endDateTime = LocalDateTime.ofInstant(endTime, zoneId)
            var currentStart = LocalDateTime.ofInstant(startTime, zoneId)

            while (currentStart.isBefore(endDateTime)) {
                val currentEnd = currentStart.plusMonths(MAX_AGGREGATE_GROUP_BUCKETS.toLong()).let {
                    if (it.isAfter(endDateTime)) endDateTime else it
                }

                val aggregateRequest = AggregateGroupByPeriodRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(currentStart, currentEnd),
                    timeRangeSlicer = Period.ofMonths(1)
                )

                val groupedResults = client.aggregateGroupByPeriod(aggregateRequest)

                for (groupedResult in groupedResults) {
                    val value = aggregateValue(dataType, aggregation, groupedResult.result)
                    if (value != null) {
                        samples.put(JSObject().apply {
                            put("startDate", formatter.format(groupedResult.startTime.atZone(zoneId).toInstant()))
                            put("endDate", formatter.format(groupedResult.endTime.atZone(zoneId).toInstant()))
                            put("value", value)
                            put("unit", dataType.unit)
                        })
                    }
                }

                currentStart = currentEnd
            }

            return JSObject().apply {
                put("samples", samples)
            }
        }

        val bucketDuration = when (bucket) {
            "hour" -> Duration.ofHours(1)
            "day" -> Duration.ofDays(1)
            "week" -> Duration.ofDays(7)
            else -> throw IllegalArgumentException("Unsupported bucket: $bucket")
        }

        // Health Connect rejects grouped aggregation requests with more than 5000 buckets.
        val maxChunkDuration = bucketDuration.multipliedBy(MAX_AGGREGATE_GROUP_BUCKETS.toLong())
        
        var currentStart = startTime
        while (currentStart.isBefore(endTime)) {
            val currentEnd = currentStart.plus(maxChunkDuration).let {
                if (it.isAfter(endTime)) endTime else it
            }

            val aggregateRequest = AggregateGroupByDurationRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(currentStart, currentEnd),
                timeRangeSlicer = bucketDuration
            )

            val groupedResults = client.aggregateGroupByDuration(aggregateRequest)

            for (groupedResult in groupedResults) {
                val value = aggregateValue(dataType, aggregation, groupedResult.result)
                // Only add the sample if we have a value
                if (value != null) {
                    val sample = JSObject().apply {
                        put("startDate", formatter.format(groupedResult.startTime))
                        put("endDate", formatter.format(groupedResult.endTime))
                        put("value", value)
                        put("unit", dataType.unit)
                    }
                    samples.put(sample)
                }
            }
            
            currentStart = currentEnd
        }
        
        return JSObject().apply {
            put("samples", samples)
        }
    }

    suspend fun queryWorkouts(
        client: HealthConnectClient,
        workoutType: String?,
        startTime: Instant,
        endTime: Instant,
        limit: Int,
        ascending: Boolean,
        anchor: String?
    ): JSObject {
        val workouts = mutableListOf<Pair<Instant, JSObject>>()
        
        var pageToken: String? = anchor  // Use anchor as initial pageToken (leverages Health Connect's native pagination)
        val pageSize = if (limit > 0) min(limit, MAX_PAGE_SIZE) else DEFAULT_PAGE_SIZE
        var fetched = 0
        
        val exerciseTypeFilter = WorkoutType.fromString(workoutType)
        
        do {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                pageSize = pageSize,
                pageToken = pageToken
            )
            val response = client.readRecords(request)
            
            response.records.forEach { record ->
                val session = record as ExerciseSessionRecord
                
                // Filter by exercise type if specified
                if (exerciseTypeFilter != null && session.exerciseType != exerciseTypeFilter) {
                    return@forEach
                }
                
                // Aggregate calories and distance for this workout session
                val aggregatedData = aggregateWorkoutData(client, session)
                val payload = createWorkoutPayload(session, aggregatedData)
                workouts.add(session.startTime to payload)
            }
            
            fetched += response.records.size
            pageToken = response.pageToken
        } while (pageToken != null && (limit <= 0 || fetched < limit))
        
        val sorted = workouts.sortedBy { it.first }
        val ordered = if (ascending) sorted else sorted.asReversed()
        val limited = if (limit > 0) ordered.take(limit) else ordered
        
        val array = JSArray()
        limited.forEach { array.put(it.second) }
        
        // Return result with workouts and next anchor (pageToken)
        val result = JSObject()
        result.put("workouts", array)
        // Only include anchor if there might be more results
        if (pageToken != null) {
            result.put("anchor", pageToken)
        }
        return result
    }
    
    private suspend fun aggregateWorkoutData(
        client: HealthConnectClient,
        session: ExerciseSessionRecord
    ): WorkoutAggregatedData {
        val timeRange = TimeRangeFilter.between(session.startTime, session.endTime)
        // Don't filter by dataOrigin - distance might come from different sources
        // than the workout session itself (e.g., fitness tracker vs workout app)
        
        // Aggregate distance and calories in a single request for efficiency
        var distanceAggregate: Double? = null
        var caloriesAggregate: Double? = null
        
        try {
            val aggregateRequest = AggregateRequest(
                metrics = setOf(
                    DistanceRecord.DISTANCE_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                ),
                timeRangeFilter = timeRange
                // Removed dataOriginFilter to get data from all sources during workout time
            )
            val result = client.aggregate(aggregateRequest)
            distanceAggregate = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            caloriesAggregate = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
        } catch (e: CancellationException) {
            // Rethrow cancellation to allow coroutine cancellation to propagate
            throw e
        } catch (e: SecurityException) {
            // Permission not granted for one or both metrics
            android.util.Log.d("HealthManager", "Permission denied for workout data aggregation: ${e.message}", e)
        } catch (e: Exception) {
            // Other errors (e.g., no data available)
            android.util.Log.d("HealthManager", "Workout data aggregation failed: ${e.message}", e)
        }

        if (caloriesAggregate == null) {
            try {
                val fallbackRequest = AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = timeRange
                )
                val fallbackResult = client.aggregate(fallbackRequest)
                caloriesAggregate = fallbackResult[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                android.util.Log.d("HealthManager", "Permission denied for workout total calories aggregation: ${e.message}", e)
            } catch (e: Exception) {
                android.util.Log.d("HealthManager", "Workout total calories aggregation failed: ${e.message}", e)
            }
        }
        
        return WorkoutAggregatedData(
            totalDistance = distanceAggregate,
            totalEnergyBurned = caloriesAggregate
        )
    }
    
    private data class WorkoutAggregatedData(
        val totalDistance: Double?,
        val totalEnergyBurned: Double?
    )
    
    private fun createWorkoutPayload(session: ExerciseSessionRecord, aggregatedData: WorkoutAggregatedData): JSObject {
        val payload = JSObject()
        
        // Workout type
        payload.put("workoutType", WorkoutType.toWorkoutTypeString(session.exerciseType))
        
        // Duration in seconds
        val durationSeconds = Duration.between(session.startTime, session.endTime).seconds.toInt()
        payload.put("duration", durationSeconds)
        
        // Start and end dates
        payload.put("startDate", formatter.format(session.startTime))
        payload.put("endDate", formatter.format(session.endTime))
        
        // Total distance (aggregated from DistanceRecord)
        aggregatedData.totalDistance?.let { distance ->
            payload.put("totalDistance", distance)
        }
        
        // Total energy burned (aggregated from ActiveCaloriesBurnedRecord)
        aggregatedData.totalEnergyBurned?.let { energy ->
            payload.put("totalEnergyBurned", energy)
        }
        
        // Source information
        val dataOrigin = session.metadata.dataOrigin
        payload.put("sourceId", dataOrigin.packageName)
        payload.put("sourceName", dataOrigin.packageName)
        session.metadata.device?.let { device ->
            val manufacturer = device.manufacturer?.takeIf { it.isNotBlank() }
            val model = device.model?.takeIf { it.isNotBlank() }
            val label = listOfNotNull(manufacturer, model).joinToString(" ").trim()
            if (label.isNotEmpty()) {
                payload.put("sourceName", label)
            }
        }
        
        payload.put("platformId", session.metadata.id)

        // Note: customMetadata is not available on Metadata in Health Connect
        // Metadata only contains dataOrigin, device, and lastModifiedTime

        return payload
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE = 500
        private const val MAX_AGGREGATE_GROUP_BUCKETS = 5000
        // Permission that lifts Health Connect's default 30-day read window.
        private const val READ_HEALTH_DATA_HISTORY_PERMISSION = "android.permission.health.READ_HEALTH_DATA_HISTORY"
    }
}
