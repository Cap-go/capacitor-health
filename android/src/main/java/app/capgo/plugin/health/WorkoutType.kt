package app.capgo.plugin.health

import androidx.health.connect.client.records.ExerciseSessionRecord

object WorkoutType {
    fun fromString(type: String?): Int? {
        if (type.isNullOrBlank()) return null
        
        return when (type) {
            "running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "cycling" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            "walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "swimming" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "strengthTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "hiking" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            "tennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
            "basketball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            "soccer" -> ExerciseSessionRecord.EXERCISE_TYPE_SOCCER
            "americanFootball" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
            "baseball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL
            "crossTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "elliptical" -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            "rowing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
            "stairClimbing" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            "traditionalStrengthTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "waterFitness" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "waterPolo" -> ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO
            "waterSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            "wrestling" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "other" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            else -> null
        }
    }

    fun toWorkoutTypeString(exerciseType: Int): String {
        return when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "running"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walking"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "strengthTraining"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "tennis"
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "basketball"
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "soccer"
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> "americanFootball"
            ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> "baseball"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "crossTraining"
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "elliptical"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "rowing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "rowing"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "stairClimbing"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "stairClimbing"
            ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> "waterPolo"
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "wrestling"
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "other"
            else -> "other"
        }
    }
}
