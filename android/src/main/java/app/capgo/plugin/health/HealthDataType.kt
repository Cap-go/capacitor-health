package app.capgo.plugin.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
    SLEEP("sleep", SleepSessionRecord::class, "minute"),
    RESPIRATORY_RATE("respiratoryRate", RespiratoryRateRecord::class, "bpm"),
    OXYGEN_SATURATION("oxygenSaturation", OxygenSaturationRecord::class, "percent"),
    RESTING_HEART_RATE("restingHeartRate", RestingHeartRateRecord::class, "bpm"),
    HEART_RATE_VARIABILITY("heartRateVariability", HeartRateVariabilityRmssdRecord::class, "millisecond"),
    BLOOD_PRESSURE("bloodPressure", BloodPressureRecord::class, "mmHg"),
    BLOOD_GLUCOSE("bloodGlucose", BloodGlucoseRecord::class, "mg/dL"),
    BODY_TEMPERATURE("bodyTemperature", BodyTemperatureRecord::class, "celsius"),
    HEIGHT("height", HeightRecord::class, "centimeter"),
    FLIGHTS_CLIMBED("flightsClimbed", FloorsClimbedRecord::class, "count"),
    DISTANCE_CYCLING("distanceCycling", DistanceRecord::class, "meter"),
    BODY_FAT("bodyFat", BodyFatRecord::class, "percent"),
    BASAL_BODY_TEMPERATURE("basalBodyTemperature", BasalBodyTemperatureRecord::class, "celsius"),
    BASAL_CALORIES("basalCalories", BasalMetabolicRateRecord::class, "kilocalorie"),
    TOTAL_CALORIES("totalCalories", TotalCaloriesBurnedRecord::class, "kilocalorie"),
    MINDFULNESS("mindfulness", MindfulnessSessionRecord::class, "minute");

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
