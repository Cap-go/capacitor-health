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
            // New comprehensive workout type mappings
            "archery" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "australianFootball" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN
            "badminton" -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            "barre" -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            "bowling" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "boxing" -> ExerciseSessionRecord.EXERCISE_TYPE_BOXING
            "climbing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING
            "cooldown" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "coreTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "cricket" -> ExerciseSessionRecord.EXERCISE_TYPE_CRICKET
            "crossCountrySkiing" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "curling" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "dance" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "discSports" -> ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC
            "downhillSkiing" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "equestrianSports" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "fencing" -> ExerciseSessionRecord.EXERCISE_TYPE_FENCING
            "fishing" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "fitnessGaming" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "flexibility" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "functionalStrengthTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "golf" -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
            "gymnastics" -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            "handball" -> ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL
            "handCycling" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "highIntensityIntervalTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "hockey" -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY
            "hunting" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "jumpRope" -> ExerciseSessionRecord.EXERCISE_TYPE_JUMPING_ROPE
            "kickboxing" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "lacrosse" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "martialArts" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "mindAndBody" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "mixedCardio" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "paddleSports" -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
            "pickleball" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "pilates" -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            "play" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "preparationAndRecovery" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "racquetball" -> ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL
            "rugby" -> ExerciseSessionRecord.EXERCISE_TYPE_RUGBY
            "sailing" -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
            "skatingSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            "snowboarding" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
            "snowSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
            "softball" -> ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL
            "squash" -> ExerciseSessionRecord.EXERCISE_TYPE_SQUASH
            "stairs" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            "stepTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
            "surfingSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            "tableTennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
            "taiChi" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "trackAndField" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "transition" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "underwaterDiving" -> ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING
            "volleyball" -> ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL
            "wheelchairRunPace" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "wheelchairWalkPace" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "cardioDance" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "socialDance" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
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
            // Keep backward compatible mapping for existing types
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "crossTraining"
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "elliptical"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "rowing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "rowing"
            // Keep stairClimbing as primary mapping for backward compatibility
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "stairClimbing"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "stepTraining"
            ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> "waterPolo"
            // Keep wrestling as primary mapping for backward compatibility
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "wrestling"
            // Comprehensive Android to TypeScript workout type mappings
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "australianFootball"
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "badminton"
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "pilates"
            ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "boxing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> "climbing"
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "flexibility"
            ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> "cricket"
            ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> "downhillSkiing"
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "dance"
            ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> "discSports"
            ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> "fencing"
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> "golf"
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "gymnastics"
            ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> "handball"
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> "wheelchairWalkPace"
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> "hockey"
            ExerciseSessionRecord.EXERCISE_TYPE_JUMPING_ROPE -> "jumpRope"
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "paddleSports"
            ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> "racquetball"
            ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> "rugby"
            ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> "sailing"
            ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> "skatingSports"
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> "snowboarding"
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> "snowSports"
            ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> "softball"
            ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> "squash"
            ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> "surfingSports"
            ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "tableTennis"
            ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> "underwaterDiving"
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "volleyball"
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "other"
            else -> "other"
        }
    }
}
