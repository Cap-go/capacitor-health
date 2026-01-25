package app.capgo.plugin.health

import androidx.health.connect.client.records.ExerciseSessionRecord

object WorkoutType {
    fun fromString(type: String?): Int? {
        if (type.isNullOrBlank()) return null
        
        return when (type) {
            // Common types
            "americanFootball" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
            "australianFootball" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN
            "badminton" -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            "baseball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL
            "basketball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            "bowling" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT // No direct mapping
            "boxing" -> ExerciseSessionRecord.EXERCISE_TYPE_BOXING
            "climbing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING
            "cricket" -> ExerciseSessionRecord.EXERCISE_TYPE_CRICKET
            "crossTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "curling" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT // No direct mapping
            "cycling" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            "dance" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "elliptical" -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            "fencing" -> ExerciseSessionRecord.EXERCISE_TYPE_FENCING
            "functionalStrengthTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "golf" -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
            "gymnastics" -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
            "handball" -> ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL
            "hiking" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            "hockey" -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY
            "jumpRope" -> ExerciseSessionRecord.EXERCISE_TYPE_JUMP_ROPE
            "kickboxing" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "lacrosse" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT // No direct mapping
            "martialArts" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "pilates" -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            "racquetball" -> ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL
            "rowing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
            "rugby" -> ExerciseSessionRecord.EXERCISE_TYPE_RUGBY
            "running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "sailing" -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
            "skatingSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            "skiing" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "snowboarding" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
            "soccer" -> ExerciseSessionRecord.EXERCISE_TYPE_SOCCER
            "softball" -> ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL
            "squash" -> ExerciseSessionRecord.EXERCISE_TYPE_SQUASH
            "stairClimbing" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            "strengthTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "surfing" -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            "swimming" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "swimmingPool" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "swimmingOpenWater" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            "tableTennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
            "tennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
            "trackAndField" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT // No direct mapping
            "traditionalStrengthTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "volleyball" -> ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL
            "walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "waterFitness" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "waterPolo" -> ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO
            "waterSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            "weightlifting" -> ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING
            "wheelchair" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            
            // iOS-specific types (map to OTHER or closest match)
            "archery" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "barre" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "cooldown" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "coreTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            "crossCountrySkiing" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "discSports" -> ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC
            "downhillSkiing" -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
            "equestrianSports" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "fishing" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "fitnessGaming" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "flexibility" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "handCycling" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "highIntensityIntervalTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            "hunting" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "mindAndBody" -> ExerciseSessionRecord.EXERCISE_TYPE_MEDITATION
            "mixedCardio" -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            "paddleSports" -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
            "pickleball" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "play" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "preparationAndRecovery" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "snowSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
            "stepTraining" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
            "surfingSports" -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
            "taiChi" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            "transition" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            "wheelchairRunPace" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "wheelchairWalkPace" -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            "wrestling" -> ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            
            // Android-specific types
            "backExtension" -> ExerciseSessionRecord.EXERCISE_TYPE_BACK_EXTENSION
            "barbellShoulderPress" -> ExerciseSessionRecord.EXERCISE_TYPE_BARBELL_SHOULDER_PRESS
            "benchPress" -> ExerciseSessionRecord.EXERCISE_TYPE_BENCH_PRESS
            "benchSitUp" -> ExerciseSessionRecord.EXERCISE_TYPE_BENCH_SIT_UP
            "bikingStationary" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
            "bootCamp" -> ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP
            "burpee" -> ExerciseSessionRecord.EXERCISE_TYPE_BURPEE
            "calisthenics" -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
            "crunch" -> ExerciseSessionRecord.EXERCISE_TYPE_CRUNCH
            "dancing" -> ExerciseSessionRecord.EXERCISE_TYPE_DANCING
            "deadlift" -> ExerciseSessionRecord.EXERCISE_TYPE_DEADLIFT
            "dumbbellCurlLeftArm" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_CURL_LEFT_ARM
            "dumbbellCurlRightArm" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_CURL_RIGHT_ARM
            "dumbbellFrontRaise" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_FRONT_RAISE
            "dumbbellLateralRaise" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_LATERAL_RAISE
            "dumbbellTricepsExtensionLeftArm" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM
            "dumbbellTricepsExtensionRightArm" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM
            "dumbbellTricepsExtensionTwoArm" -> ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM
            "exerciseClass" -> ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS
            "forwardTwist" -> ExerciseSessionRecord.EXERCISE_TYPE_FORWARD_TWIST
            "frisbeedisc" -> ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC
            "guidedBreathing" -> ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING
            "iceHockey" -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY
            "iceSkating" -> ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING
            "jumpingJack" -> ExerciseSessionRecord.EXERCISE_TYPE_JUMPING_JACK
            "latPullDown" -> ExerciseSessionRecord.EXERCISE_TYPE_LAT_PULL_DOWN
            "lunge" -> ExerciseSessionRecord.EXERCISE_TYPE_LUNGE
            "meditation" -> ExerciseSessionRecord.EXERCISE_TYPE_MEDITATION
            "paddling" -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
            "paraGliding" -> ExerciseSessionRecord.EXERCISE_TYPE_PARA_GLIDING
            "plank" -> ExerciseSessionRecord.EXERCISE_TYPE_PLANK
            "rockClimbing" -> ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING
            "rollerHockey" -> ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY
            "rowingMachine" -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE
            "runningTreadmill" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
            "scubaDiving" -> ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING
            "skating" -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
            "snowshoeing" -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
            "stairClimbingMachine" -> ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
            "stretching" -> ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            "upperTwist" -> ExerciseSessionRecord.EXERCISE_TYPE_UPPER_TWIST
            
            "other" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            else -> null
        }
    }

    fun toWorkoutTypeString(exerciseType: Int): String {
        return when (exerciseType) {
            // Common mappings
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
            ExerciseSessionRecord.EXERCISE_TYPE_JUMP_ROPE -> "jumpRope"
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
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> "iceSkating"
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
            
            // Android-specific types
            ExerciseSessionRecord.EXERCISE_TYPE_BACK_EXTENSION -> "backExtension"
            ExerciseSessionRecord.EXERCISE_TYPE_BARBELL_SHOULDER_PRESS -> "barbellShoulderPress"
            ExerciseSessionRecord.EXERCISE_TYPE_BENCH_PRESS -> "benchPress"
            ExerciseSessionRecord.EXERCISE_TYPE_BENCH_SIT_UP -> "benchSitUp"
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> "bootCamp"
            ExerciseSessionRecord.EXERCISE_TYPE_BURPEE -> "burpee"
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "calisthenics"
            ExerciseSessionRecord.EXERCISE_TYPE_CRUNCH -> "crunch"
            ExerciseSessionRecord.EXERCISE_TYPE_DEADLIFT -> "deadlift"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_CURL_LEFT_ARM -> "dumbbellCurlLeftArm"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_CURL_RIGHT_ARM -> "dumbbellCurlRightArm"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_FRONT_RAISE -> "dumbbellFrontRaise"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_LATERAL_RAISE -> "dumbbellLateralRaise"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM -> "dumbbellTricepsExtensionLeftArm"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM -> "dumbbellTricepsExtensionRightArm"
            ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM -> "dumbbellTricepsExtensionTwoArm"
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "exerciseClass"
            ExerciseSessionRecord.EXERCISE_TYPE_FORWARD_TWIST -> "forwardTwist"
            ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> "frisbeedisc"
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> "guidedBreathing"
            ExerciseSessionRecord.EXERCISE_TYPE_JUMPING_JACK -> "jumpingJack"
            ExerciseSessionRecord.EXERCISE_TYPE_LAT_PULL_DOWN -> "latPullDown"
            ExerciseSessionRecord.EXERCISE_TYPE_LUNGE -> "lunge"
            ExerciseSessionRecord.EXERCISE_TYPE_MEDITATION -> "meditation"
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "paddling"
            ExerciseSessionRecord.EXERCISE_TYPE_PARA_GLIDING -> "paraGliding"
            ExerciseSessionRecord.EXERCISE_TYPE_PLANK -> "plank"
            ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> "rollerHockey"
            ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> "scubaDiving"
            ExerciseSessionRecord.EXERCISE_TYPE_UPPER_TWIST -> "upperTwist"
            
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "other"
            else -> "other"
        }
    }
}
