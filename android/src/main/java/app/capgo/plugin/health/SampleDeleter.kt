package app.capgo.plugin.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import com.getcapacitor.JSObject
import kotlin.reflect.KClass
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Deletes records this app wrote, for `deleteSamples`, by Health Connect record id (`platformIds`)
 * and/or `clientRecordIds`.
 *
 * Health Connect only lets an app delete its own records and needs the type's write permission.
 * `deleteRecords` does not report how many records it deleted, so the result is the number of ids
 * requested. The requested ids are passed as given: Health Connect documents that an id matching
 * no record fails the whole call (the platform on Android 14+ ignores it).
 */
class SampleDeleter {

    /** Validated `deleteSamples` options, built by [parse]. */
    class DeleteInput internal constructor(
        val dataType: String,
        val recordClass: KClass<out Record>,
        val isWorkout: Boolean,
        val clientRecordIds: List<String>,
        val platformIds: List<String>,
    )

    /** Throws [IllegalArgumentException] naming the offending option and the reason. */
    fun parse(options: JSONObject): DeleteInput {
        val dataType = (options.opt("dataType") as? String)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("dataType is required")
        val recordClass: KClass<out Record> = when (dataType) {
            "workouts" -> ExerciseSessionRecord::class
            "workoutRoute" -> throw IllegalArgumentException(
                "workoutRoute cannot be deleted on its own: delete the workout with dataType 'workouts'"
            )
            else -> HealthDataType.from(dataType)?.recordClass
                ?: throw IllegalArgumentException("Unsupported data type: $dataType")
        }

        val clientRecordIds = idList(options, "clientRecordIds")
        val platformIds = idList(options, "platformIds")
        if (clientRecordIds.isEmpty() && platformIds.isEmpty()) {
            throw IllegalArgumentException("Provide at least one id in clientRecordIds or platformIds")
        }
        val count = clientRecordIds.size + platformIds.size
        if (count > MAX_IDS) {
            throw IllegalArgumentException("At most $MAX_IDS ids can be deleted per call, got $count")
        }

        return DeleteInput(dataType, recordClass, dataType == "workouts", clientRecordIds, platformIds)
    }

    /**
     * Deletes the records. Throws [SecurityException] when the type's write permission is missing.
     * For workouts, the companion records saveWorkout wrote for the given clientRecordIds
     * (`<id>:distance`, `<id>:energy`, `<id>:hr`) are deleted too, for the types the app can still
     * write; a companion the workout was saved without is skipped.
     */
    suspend fun delete(client: HealthConnectClient, input: DeleteInput): JSObject = WorkoutWriter.WRITE_LOCK.withLock {
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(input.recordClass) !in granted) {
            throw SecurityException(
                "Missing Health Connect write permission for ${input.dataType}. " +
                    "Deleting needs write access to '${input.dataType}': request it with requestAuthorization() first."
            )
        }

        client.deleteRecords(input.recordClass, input.platformIds, input.clientRecordIds)

        if (input.isWorkout && input.clientRecordIds.isNotEmpty()) {
            WorkoutWriter.COMPANION_RECORDS.forEach { (recordClass, idSuffix) ->
                if (HealthPermission.getWritePermission(recordClass) in granted) {
                    WorkoutWriter.deleteCompanionRecords(
                        client,
                        recordClass,
                        input.clientRecordIds.map { WorkoutWriter.companionClientRecordId(it, idSuffix) },
                    )
                }
            }
        }

        JSObject().apply { put("deleted", input.clientRecordIds.size + input.platformIds.size) }
    }

    /** A missing key or JSON `null` is an empty list; duplicates are dropped. */
    private fun idList(options: JSONObject, key: String): List<String> {
        val value = options.opt(key)
        if (value == null || value == JSONObject.NULL) {
            return emptyList()
        }
        val array = value as? JSONArray ?: throw IllegalArgumentException("$key must be an array of strings")
        return (0 until array.length()).map { index ->
            (array.opt(index) as? String)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("$key[$index] must be a non-empty string")
        }.distinct()
    }

    companion object {
        private const val MAX_IDS = 100
    }
}
