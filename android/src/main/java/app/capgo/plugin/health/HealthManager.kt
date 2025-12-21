package app.capgo.plugin.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Duration
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.collections.buildSet

class HealthManager {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    fun permissionsFor(readTypes: Collection<HealthDataType>, writeTypes: Collection<HealthDataType>, includeWorkouts: Boolean = false): Set<String> = buildSet {
        readTypes.forEach { add(it.readPermission) }
        writeTypes.forEach { add(it.writePermission) }
        // Include workout read permission if explicitly requested
        if (includeWorkouts) {
            add(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
        }
    }

    suspend fun authorizationStatus(
        client: HealthConnectClient,
        readTypes: Collection<HealthDataType>,
        writeTypes: Collection<HealthDataType>,
        includeWorkouts: Boolean = false
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
        metadata: Map<String, String>?
    ) {
        when (dataType) {
            HealthDataType.STEPS -> {
                val record = StepsRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    count = value.toLong().coerceAtLeast(0)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.DISTANCE -> {
                val record = DistanceRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    distance = Length.meters(value)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.CALORIES -> {
                val record = ActiveCaloriesBurnedRecord(
                    startTime = startTime,
                    startZoneOffset = zoneOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = zoneOffset(endTime),
                    energy = Energy.kilocalories(value)
                )
                client.insertRecords(listOf(record))
            }
            HealthDataType.WEIGHT -> {
                val record = WeightRecord(
                    time = startTime,
                    zoneOffset = zoneOffset(startTime),
                    weight = Mass.kilograms(value)
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
                    samples = samples
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

        return payload
    }

    private fun zoneOffset(instant: Instant): ZoneOffset? {
        return ZoneId.systemDefault().rules.getOffset(instant)
    }

    private fun Double.toBpmLong(): Long {
        return java.lang.Math.round(this.coerceAtLeast(0.0))
    }

    suspend fun queryWorkouts(
        client: HealthConnectClient,
        workoutType: String?,
        startTime: Instant,
        endTime: Instant,
        limit: Int,
        ascending: Boolean
    ): JSArray {
        val workouts = mutableListOf<Pair<Instant, JSObject>>()
        
        var pageToken: String? = null
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
        return array
    }
    
    private suspend fun aggregateWorkoutData(
        client: HealthConnectClient,
        session: ExerciseSessionRecord
    ): WorkoutAggregatedData {
        val timeRange = TimeRangeFilter.between(session.startTime, session.endTime)
        // Don't filter by dataOrigin - distance might come from different sources
        // than the workout session itself (e.g., fitness tracker vs workout app)
        
        // Aggregate distance
        val distanceAggregate = try {
            val aggregateRequest = AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = timeRange
                // Removed dataOriginFilter to get distance from all sources during workout time
            )
            val result = client.aggregate(aggregateRequest)
            result[DistanceRecord.DISTANCE_TOTAL]?.inMeters
        } catch (e: Exception) {
            null // Permission might not be granted or no data available
        }
        
        return WorkoutAggregatedData(
            totalDistance = distanceAggregate
        )
    }
    
    private data class WorkoutAggregatedData(
        val totalDistance: Double?
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
        
        // Note: customMetadata is not available on Metadata in Health Connect
        // Metadata only contains dataOrigin, device, and lastModifiedTime
        
        return payload
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val MAX_PAGE_SIZE = 500
    }
}
