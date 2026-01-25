package app.capgo.plugin.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import kotlin.reflect.KClass

enum class HealthDataType(
    val identifier: String,
    val recordClass: KClass<out Record>,
    val unit: String
) {
    STEPS("steps", StepsRecord::class, "count"),
    DISTANCE("distance", DistanceRecord::class, "meter"),
    CALORIES("calories", ActiveCaloriesBurnedRecord::class, "kilocalorie"),
    HEART_RATE("heartRate", HeartRateRecord::class, "bpm"),
    WEIGHT("weight", WeightRecord::class, "kilogram"),
    SLEEP_ANALYSIS("sleepAnalysis", SleepSessionRecord::class, "minute"),
    RESPIRATORY_RATE("respiratoryRate", RespiratoryRateRecord::class, "breathsPerMinute"),
    OXYGEN_SATURATION("oxygenSaturation", OxygenSaturationRecord::class, "percent"),
    RESTING_HEART_RATE("restingHeartRate", RestingHeartRateRecord::class, "bpm"),
    HEART_RATE_VARIABILITY("heartRateVariability", HeartRateVariabilityRmssdRecord::class, "millisecond");

    val readPermission: String
        get() = HealthPermission.getReadPermission(recordClass)

    val writePermission: String
        get() = HealthPermission.getWritePermission(recordClass)

    companion object {
        fun from(identifier: String): HealthDataType? {
            return entries.firstOrNull { it.identifier == identifier }
        }
    }
}
