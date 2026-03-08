package app.capgo.plugin.health

import androidx.health.connect.client.records.ExerciseSessionRecord

/**
 * Maps plugin workout strings to Android Health Connect exercise types.
 *
 * Several plugin workout types intentionally share the same Android exercise type. The plugin
 * accepts both cross-platform names and platform-specific aliases when filtering workouts.
 */
object WorkoutType {
    fun fromString(type: String?): Int? {
        if (type.isNullOrBlank()) return null

        return when (type) {
            "americanFootball" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
            "australianFootball" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN
            "badminton" -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            "baseball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL
            "basketball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            "bowling" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "boxing" -> ExerciseSessionRecord.EXERCISE_TYPE_BOXING
            "climbing", "rockClimbing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING
            "cricket" -> ExerciseSessionRecord.EXERCISE_TYPE_CRICKET
            "crossTraining", "highIntensityIntervalTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "curling" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "cycling" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            "dancing", "dance", "cardioDance", "socialDance" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "elliptical" -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            "fencing" -> ExerciseSessionRecord.EXERCISE_TYPE_FENCING
            "functionalStrengthTraining", "strengthTraining", "traditionalStrengthTraining", "coreTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "golf" -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
            "gymnastics" -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            "handball" -> ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL
            "hiking" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            "hockey", "iceHockey" -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY
            "jumpRope" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "kickboxing", "martialArts", "taiChi", "wrestling" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "lacrosse" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "mindAndBody", "meditation" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "mixedCardio", "calisthenics" -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            "paddleSports", "paddling" -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
            "pickleball" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "pilates" -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            "play" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "preparationAndRecovery", "flexibility", "stretching" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "racquetball" -> ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL
            "rowing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
            "rowingMachine" -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE
            "rugby" -> ExerciseSessionRecord.EXERCISE_TYPE_RUGBY
            "running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "runningTreadmill" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
            "sailing" -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
            "skatingSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            "skating" -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            "skiing", "crossCountrySkiing", "downhillSkiing" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "snowboarding" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
            "snowSports", "snowshoeing" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
            "soccer" -> ExerciseSessionRecord.EXERCISE_TYPE_SOCCER
            "softball" -> ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL
            "squash" -> ExerciseSessionRecord.EXERCISE_TYPE_SQUASH
            "stairClimbing", "stairs" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            "stepTraining", "stairClimbingMachine" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
            "surfing", "surfingSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            "swimming", "swimmingPool", "waterFitness" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "swimmingOpenWater", "waterSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            "tableTennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
            "tennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
            "trackAndField" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "underwaterDiving", "scubaDiving" -> ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING
            "volleyball" -> ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL
            "walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "waterPolo" -> ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO
            "weightlifting" -> ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING
            "wheelchair", "wheelchairRunPace", "wheelchairWalkPace", "handCycling" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "archery" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "barre" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "cooldown" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "discSports", "frisbeedisc" -> ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC
            "equestrianSports" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "fishing" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "fitnessGaming" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "hunting" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "transition" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "backExtension",
            "barbellShoulderPress",
            "benchPress",
            "benchSitUp",
            "crunch",
            "deadlift",
            "dumbbellCurlLeftArm",
            "dumbbellCurlRightArm",
            "dumbbellFrontRaise",
            "dumbbellLateralRaise",
            "dumbbellTricepsExtensionLeftArm",
            "dumbbellTricepsExtensionRightArm",
            "dumbbellTricepsExtensionTwoArm",
            "forwardTwist",
            "latPullDown",
            "lunge",
            "plank",
            "upperTwist" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "bikingStationary" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
            "bootCamp" -> ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP
            "burpee", "jumpingJack" -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            "exerciseClass" -> ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS
            "guidedBreathing" -> ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING
            "iceSkating" -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING
            "paraGliding" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "rollerHockey" -> ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY
            "other" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            else -> null
        }
    }

    fun toWorkoutTypeString(exerciseType: Int): String {
        return when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> "americanFootball"
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "australianFootball"
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "badminton"
            ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> "baseball"
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "basketball"
            ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "boxing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> "climbing"
            ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> "cricket"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "crossTraining"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "bikingStationary"
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "dancing"
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "elliptical"
            ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> "fencing"
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> "golf"
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "gymnastics"
            ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> "handball"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> "iceHockey"
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> "iceSkating"
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "martialArts"
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "pilates"
            ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> "racquetball"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "rowing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "rowingMachine"
            ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> "rugby"
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "running"
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "runningTreadmill"
            ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> "sailing"
            ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> "skating"
            ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> "skiing"
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> "snowboarding"
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> "snowshoeing"
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "soccer"
            ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> "softball"
            ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> "squash"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "stairClimbing"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "stairClimbingMachine"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "strengthTraining"
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "stretching"
            ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> "surfing"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "swimmingPool"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "swimmingOpenWater"
            ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "tableTennis"
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "tennis"
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "volleyball"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walking"
            ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> "waterPolo"
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "weightlifting"
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> "wheelchair"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> "bootCamp"
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "calisthenics"
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "exerciseClass"
            ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> "frisbeedisc"
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> "guidedBreathing"
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "paddling"
            ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> "rollerHockey"
            ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> "scubaDiving"
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "other"
            else -> "other"
        }
    }
}
