export type HealthDataType =
  | 'steps'
  | 'distance'
  | 'calories'
  | 'heartRate'
  | 'weight'
  | 'sleep'
  | 'respiratoryRate'
  | 'oxygenSaturation'
  | 'restingHeartRate'
  | 'heartRateVariability'
  | 'vo2Max'
  | 'bloodPressure'
  | 'bloodGlucose'
  | 'bodyTemperature'
  | 'height'
  | 'flightsClimbed'
  | 'exerciseTime'
  | 'distanceCycling'
  | 'bodyFat'
  | 'basalBodyTemperature'
  | 'appleSleepingWristTemperature'
  | 'basalCalories'
  | 'totalCalories'
  | 'mindfulness'
  | 'appleStandHour'
  | 'dietaryWater'
  | 'dietaryEnergyConsumed'
  | 'stateOfMind'
  | 'workouts'
  | 'workoutRoute';

export type HealthUnit =
  | 'count'
  | 'meter'
  | 'kilocalorie'
  | 'bpm'
  | 'kilogram'
  | 'minute'
  | 'percent'
  | 'millisecond'
  | 'mL/min/kg'
  | 'mmHg'
  | 'mg/dL'
  | 'celsius'
  | 'fahrenheit'
  | 'centimeter'
  | 'liter'
  | 'valence';

export interface AuthorizationOptions {
  /** Data types that should be readable after authorization. */
  read?: HealthDataType[];
  /**
   * Data types that should be writable after authorization.
   *
   * Include `workouts` to save workouts with {@link HealthPlugin.saveWorkout}, and `workoutRoute`
   * to also save their GPS route (Health Connect `WRITE_EXERCISE_ROUTE`, HealthKit
   * `HKSeriesType.workoutRoute()`). `workoutRoute` is write-only. On iOS it also requests read and
   * share access to workouts and read access to routes, which Apple requires for route data.
   */
  write?: HealthDataType[];
  /**
   * Android only: also request the `READ_HEALTH_DATA_HISTORY` permission in the same
   * Health Connect permission sheet. Without it, Health Connect caps reads to roughly
   * the last 30 days; granting it lets you read older data.
   *
   * The consuming app must also declare the permission in its `AndroidManifest.xml`:
   * `<uses-permission android:name="android.permission.health.READ_HEALTH_DATA_HISTORY" />`
   *
   * The permission only exists on sufficiently new Health Connect providers (Android 14
   * extension 13+ or Health Connect APK 171302+). On older but otherwise supported
   * providers it is silently skipped — the normal read/write scopes are still requested —
   * and the returned status reports `historyAccessAvailable: false`.
   *
   * Ignored on iOS (HealthKit has no equivalent permission and no 30-day read cap).
   */
  requestHistoryAccess?: boolean;
}

export interface AuthorizationStatus {
  readAuthorized: HealthDataType[];
  readDenied: HealthDataType[];
  writeAuthorized: HealthDataType[];
  writeDenied: HealthDataType[];
  /**
   * Android only: whether the `READ_HEALTH_DATA_HISTORY` permission is granted. Only
   * present when `requestHistoryAccess` was set on the request; omitted otherwise and
   * always omitted on iOS. Always `false` when `historyAccessAvailable` is `false`,
   * since an unsupported provider can never grant the permission.
   */
  historyAccessAuthorized?: boolean;
  /**
   * Android only: whether the connected Health Connect provider supports the
   * `READ_HEALTH_DATA_HISTORY` permission at all. Only present when `requestHistoryAccess`
   * was set on the request; omitted otherwise and always omitted on iOS.
   *
   * `false` means the provider is too old (pre Android 14 extension 13 / Health Connect APK
   * 171302) to ever grant history access — distinct from the user simply denying it. Use it
   * to avoid re-prompting and to message the user that history access is unavailable on their
   * device.
   */
  historyAccessAvailable?: boolean;
}

export interface AvailabilityResult {
  available: boolean;
  /** Platform specific details (for debugging/diagnostics). */
  platform?: 'ios' | 'android' | 'web';
  reason?: string;
}

export interface QueryOptions {
  /** The type of data to retrieve from the health store. */
  dataType: HealthDataType;
  /** Inclusive ISO 8601 start date (defaults to now - 1 day). */
  startDate?: string;
  /** Exclusive ISO 8601 end date (defaults to now). */
  endDate?: string;
  /** Maximum number of samples to return (defaults to 100). */
  limit?: number;
  /** Return results sorted ascending by start date (defaults to false). */
  ascending?: boolean;
}

export type SleepState = 'inBed' | 'asleep' | 'awake' | 'rem' | 'deep' | 'light';

/** Stage-level sleep segment emitted for sleep samples when platform data is available. */
export interface SleepStage {
  /** Stage segment start date in ISO 8601 format. */
  startDate: string;
  /** Stage segment end date in ISO 8601 format. */
  endDate: string;
  /** Sleep stage label for this segment. */
  stage: SleepState;
  /** Duration of this stage segment in minutes. */
  durationMinutes: number;
}

export interface HealthSample {
  dataType: HealthDataType;
  value: number;
  unit: HealthUnit;
  startDate: string;
  endDate: string;
  sourceName?: string;
  sourceId?: string;
  /**
   * Android only. The recording device's type, when Health Connect reports one:
   * 'watch' | 'phone' | 'scale' | 'ring' | 'headMounted' | 'fitnessBand' |
   * 'chestStrap' | 'smartDisplay'. Useful when a record carries a device type but
   * no manufacturer/model, where `sourceName` falls back to the package name and
   * cannot distinguish a watch from a phone.
   */
  deviceType?: string;
  /** Platform-specific unique identifier (HealthKit UUID on iOS, Health Connect metadata ID on Android). */
  platformId?: string;
  /** For sleep data, indicates the sleep state (e.g., 'asleep', 'awake', 'rem', 'deep', 'light'). */
  sleepState?: SleepState;
  /**
   * For Apple Stand Hour data (iOS only), whether the hour counted as stood or idle.
   * `value` is normalized to 1 for stood and 0 for idle, so summing values per day
   * yields the Activity ring's stand-hour count.
   */
  standState?: 'stood' | 'idle';
  /**
   * For State of Mind data (iOS 18+), whether the sample is a momentary
   * emotion or the day's overall mood. `value` carries the valence in
   * [-1, 1] (unpleasant to pleasant). Omitted for kinds introduced after
   * this plugin version.
   */
  stateOfMindKind?: 'momentaryEmotion' | 'dailyMood';
  /** For sleep data, individual sleep stages when the platform exposes stage-level data. */
  stages?: SleepStage[];
  /** For sleep data, indicates whether stage-level data was emitted. */
  hasStageData?: boolean;
  /** For blood pressure data, the systolic value in mmHg. */
  systolic?: number;
  /** For blood pressure data, the diastolic value in mmHg. */
  diastolic?: number;
  /** For VO2 max data on Android, Health Connect's measurement method enum value. */
  measurementMethod?: number;
}

export interface ReadSamplesResult {
  samples: HealthSample[];
}

export type WorkoutType =
  // Common types (supported on both platforms)
  | 'americanFootball'
  | 'australianFootball'
  | 'badminton'
  | 'baseball'
  | 'basketball'
  | 'bowling'
  | 'boxing'
  | 'climbing'
  | 'cricket'
  | 'crossTraining'
  | 'curling'
  | 'cycling'
  | 'dance'
  | 'elliptical'
  | 'fencing'
  | 'functionalStrengthTraining'
  | 'golf'
  | 'gymnastics'
  | 'handball'
  | 'hiking'
  | 'hockey'
  | 'jumpRope'
  | 'kickboxing'
  | 'lacrosse'
  | 'martialArts'
  | 'pilates'
  | 'racquetball'
  | 'rowing'
  | 'rugby'
  | 'running'
  | 'sailing'
  | 'skatingSports'
  | 'skiing'
  | 'snowboarding'
  | 'soccer'
  | 'softball'
  | 'squash'
  | 'stairClimbing'
  | 'strengthTraining'
  | 'surfing'
  | 'swimming'
  | 'swimmingPool'
  | 'swimmingOpenWater'
  | 'tableTennis'
  | 'tennis'
  | 'trackAndField'
  | 'traditionalStrengthTraining'
  | 'volleyball'
  | 'walking'
  | 'waterFitness'
  | 'waterPolo'
  | 'waterSports'
  | 'weightlifting'
  | 'wheelchair'
  | 'yoga'
  // iOS specific types
  | 'archery'
  | 'barre'
  | 'cooldown'
  | 'coreTraining'
  | 'crossCountrySkiing'
  | 'discSports'
  | 'downhillSkiing'
  | 'equestrianSports'
  | 'fishing'
  | 'fitnessGaming'
  | 'flexibility'
  | 'handCycling'
  | 'highIntensityIntervalTraining'
  | 'hunting'
  | 'mindAndBody'
  | 'mixedCardio'
  | 'paddleSports'
  | 'pickleball'
  | 'play'
  | 'preparationAndRecovery'
  | 'snowSports'
  | 'stairs'
  | 'stepTraining'
  | 'surfingSports'
  | 'taiChi'
  | 'transition'
  | 'underwaterDiving'
  | 'wheelchairRunPace'
  | 'wheelchairWalkPace'
  | 'wrestling'
  | 'cardioDance'
  | 'socialDance'
  // Android specific types
  | 'backExtension'
  | 'barbellShoulderPress'
  | 'benchPress'
  | 'benchSitUp'
  | 'bikingStationary'
  | 'bootCamp'
  | 'burpee'
  | 'calisthenics'
  | 'crunch'
  | 'dancing'
  | 'deadlift'
  | 'dumbbellCurlLeftArm'
  | 'dumbbellCurlRightArm'
  | 'dumbbellFrontRaise'
  | 'dumbbellLateralRaise'
  | 'dumbbellTricepsExtensionLeftArm'
  | 'dumbbellTricepsExtensionRightArm'
  | 'dumbbellTricepsExtensionTwoArm'
  | 'exerciseClass'
  | 'forwardTwist'
  | 'frisbeedisc'
  | 'guidedBreathing'
  | 'iceHockey'
  | 'iceSkating'
  | 'jumpingJack'
  | 'latPullDown'
  | 'lunge'
  | 'meditation'
  | 'paddling'
  | 'paraGliding'
  | 'plank'
  | 'rockClimbing'
  | 'rollerHockey'
  | 'rowingMachine'
  | 'runningTreadmill'
  | 'scubaDiving'
  | 'skating'
  | 'snowshoeing'
  | 'stairClimbingMachine'
  | 'stretching'
  | 'upperTwist'
  | 'other';

export interface QueryWorkoutsOptions {
  /** Optional workout type filter. If omitted, all workout types are returned. */
  workoutType?: WorkoutType;
  /** Inclusive ISO 8601 start date (defaults to now - 1 day). */
  startDate?: string;
  /** Exclusive ISO 8601 end date (defaults to now). */
  endDate?: string;
  /** Maximum number of workouts to return (defaults to 100). */
  limit?: number;
  /** Return results sorted ascending by start date (defaults to false). */
  ascending?: boolean;
  /**
   * Anchor for pagination. Use the anchor returned from a previous query to continue from that point.
   * On iOS, this is the ISO 8601 cursor returned by the previous query. On Android, this uses
   * Health Connect's pageToken.
   * Omit this parameter to start from the beginning.
   */
  anchor?: string;
}

export type WorkoutEventType = 'lap' | 'pause' | 'segment' | 'marker';

export interface WorkoutEvent {
  /**
   * Workout event type.
   * iOS returns lap events from HealthKit (`HKWorkoutEventType.lap`).
   */
  type: WorkoutEventType | string;
  /** ISO 8601 timestamp of the event. */
  date: string;
  /**
   * Duration of the lap interval in seconds until the next lap event or the workout end.
   * Present for lap events returned on iOS.
   */
  durationSeconds?: number;
  /**
   * Lap distance in meters when HealthKit provides it on the workout event metadata
   * (for example `HKMetadataKeyLapLength`).
   */
  distanceMeters?: number;
}

export interface Workout {
  /** The type of workout. */
  workoutType: WorkoutType;
  /** Duration of the workout in seconds. */
  duration: number;
  /** Total energy burned in kilocalories (if available). */
  totalEnergyBurned?: number;
  /** Total distance in meters (if available). */
  totalDistance?: number;
  /** ISO 8601 start date of the workout. */
  startDate: string;
  /** ISO 8601 end date of the workout. */
  endDate: string;
  /** Source name that recorded the workout. */
  sourceName?: string;
  /** Source bundle identifier. */
  sourceId?: string;
  /** Platform-specific unique identifier (HealthKit UUID on iOS, Health Connect metadata ID on Android). */
  platformId?: string;
  /** Additional metadata (if available). */
  metadata?: Record<string, string>;
  /**
   * Lap workout events when available.
   * On iOS, includes HealthKit lap markers with per-lap duration and optional distance.
   */
  workoutEvents?: WorkoutEvent[];
}

export interface QueryWorkoutsResult {
  workouts: Workout[];
  /**
   * Anchor for the next page of results. Pass this value as the anchor parameter in the next query
   * to continue pagination. If undefined or null, there are no more results.
   */
  anchor?: string;
}

/** How a record was recorded: by the app while it happened, or typed in by the user. */
export type RecordingMethod = 'activelyRecorded' | 'manualEntry';

export interface WriteSampleOptions {
  dataType: HealthDataType;
  value: number;
  /**
   * Optional unit override. If omitted, the default unit for the data type is used
   * (count for `steps`, meter for `distance`, kilocalorie for `calories`, bpm for `heartRate`, kilogram for `weight`).
   */
  unit?: HealthUnit;
  /** ISO 8601 start date for the sample. Defaults to now. */
  startDate?: string;
  /** ISO 8601 end date for the sample. Defaults to startDate. */
  endDate?: string;
  /** Metadata key-value pairs forwarded to the native APIs where supported. */
  metadata?: Record<string, string>;
  /** Android mindfulness session type. Defaults to 'meditation' when dataType is 'mindfulness'. */
  mindfulnessSessionType?: 'unknown' | 'meditation' | 'breathing' | 'music' | 'movement' | 'unguided';
  /** For blood pressure data, the systolic value in mmHg. Required when dataType is 'bloodPressure'. */
  systolic?: number;
  /** For blood pressure data, the diastolic value in mmHg. Required when dataType is 'bloodPressure'. */
  diastolic?: number;
  /**
   * Idempotency key. Saving again with the same `clientRecordId` replaces the earlier sample instead
   * of duplicating it, and {@link HealthPlugin.deleteSamples} can delete it later by this id.
   *
   * Android: the Health Connect `clientRecordId`, with the save time in milliseconds as
   * `clientRecordVersion` (always higher than the previous save's).
   * iOS: `HKMetadataKeySyncIdentifier`, with the same kind of version as `HKMetadataKeySyncVersion`
   * (for blood pressure it is set on the correlation, and its systolic and diastolic samples get
   * `<id>:systolic` and `<id>:diastolic`).
   *
   * Ids are unique per data type. Do not give a sample an id the plugin derives for another record:
   * `<workout id>:distance`, `:distance:<n>`, `:energy`, `:energy:<n>`, `:hr`, `:hr:<n>` and `:route` belong to the records saved
   * with a workout (see {@link SaveWorkoutOptions.clientRecordId}).
   */
  clientRecordId?: string;
  /**
   * How the sample was recorded. When omitted nothing changes: Android saves a manual entry and iOS sets
   * no recording flag.
   *
   * `manualEntry`: Android manual entry; iOS `HKMetadataKeyWasUserEntered: true`.
   * `activelyRecorded`: Android actively recorded on a phone device; iOS no recording flag.
   */
  recordingMethod?: RecordingMethod;
}

export interface DeleteSamplesOptions {
  /**
   * Type of the records to delete. Use `workouts` for workouts saved with
   * {@link HealthPlugin.saveWorkout}; `workoutRoute` is not accepted (delete the workout instead).
   */
  dataType: HealthDataType;
  /** `clientRecordId`s given when the records were saved. */
  clientRecordIds?: string[];
  /** `platformId`s of the records (as returned by `saveWorkout`, `readSamples` or `queryWorkouts`). */
  platformIds?: string[];
}

export interface DeleteSamplesResult {
  /**
   * iOS: the number of records of `dataType` actually deleted (for `workouts`, the workouts, not their
   * associated samples).
   *
   * Android: the number of ids requested, since Health Connect does not report how many records it
   * deleted. Android 14 and later ignore ids that match nothing. Health Connect documents that such an
   * id fails the whole call, so on Android 13 and lower (Health Connect app) the call can reject with
   * `delete-samples-failed` and delete nothing.
   */
  deleted: number;
}

/** A heart-rate reading recorded during a workout. */
export interface WorkoutHeartRateSample {
  /** ISO 8601 timestamp of the reading. */
  date: string;
  /** Beats per minute, from 1 to 300. Android rounds it to the nearest integer (Health Connect stores whole bpm). */
  bpm: number;
}

/** A GPS location recorded during a workout. */
export interface WorkoutRouteLocation {
  /** ISO 8601 timestamp of the location. */
  date: string;
  /** Latitude in degrees, from -90 to 90. */
  latitude: number;
  /** Longitude in degrees, from -180 to 180. */
  longitude: number;
  /** Altitude in meters. On iOS a location without altitude is stored with an invalid altitude (vertical accuracy -1). */
  altitude?: number;
  /** Horizontal accuracy in meters (0 or more). On iOS a missing value is stored as 0. */
  horizontalAccuracy?: number;
  /** Vertical accuracy in meters (0 or more). On iOS a missing value is stored as 0 when `altitude` is set. */
  verticalAccuracy?: number;
}

/** A pause inside a workout. */
export interface WorkoutPause {
  /** ISO 8601 start of the pause. */
  startDate: string;
  /** ISO 8601 end of the pause. Must be after `startDate`. */
  endDate: string;
}

/** Records saved alongside a workout by {@link HealthPlugin.saveWorkout}. */
export type WorkoutCompanionRecord = 'distance' | 'energy' | 'heartRate';

export interface SaveWorkoutOptions {
  /**
   * The type of workout. On iOS `swimmingPool` and `swimmingOpenWater` are saved as swimming workouts
   * with a pool or open-water location, so {@link HealthPlugin.queryWorkouts} reports both as `swimming`.
   */
  workoutType: WorkoutType;
  /** ISO 8601 start date of the workout. */
  startDate: string;
  /** ISO 8601 end date of the workout. Must be after `startDate`. */
  endDate: string;
  /**
   * Idempotency key. Saving again with the same `clientRecordId` replaces the previously saved
   * workout instead of creating a duplicate, so a failed or uncertain save can simply be retried.
   *
   * On Android it becomes the Health Connect `clientRecordId` of the exercise session, and the
   * distance, energy and heart-rate records get derived ids (`<id>:distance`, `<id>:energy`,
   * `<id>:hr`). Each save uses the save time in milliseconds as `clientRecordVersion` (always higher
   * than the previous save's), so the latest save wins. Derived records a later save no longer
   * writes are deleted, for the types the app can still write.
   *
   * On iOS it becomes the `HKMetadataKeySyncIdentifier` of the workout, and the other objects get
   * derived ids (`<id>:distance`, `<id>:energy`, `<id>:hr:<n>` per heart-rate reading,
   * `<id>:route`), with the same kind of version as `HKMetadataKeySyncVersion`. When pauses split the
   * workout into several active intervals, the distance and energy pieces get `<id>:distance:<n>` and
   * `<id>:energy:<n>` (from 0, in time order) instead. HealthKit only replaces the objects a later save
   * writes again: a distance, energy or route it leaves out, distance or energy pieces with another id
   * (the pauses changed), and heart-rate readings beyond its count, stay from the earlier save. Retry with the same data, or
   * delete the workout with {@link HealthPlugin.deleteSamples} before saving different contents.
   *
   * Without it, every call inserts new records.
   */
  clientRecordId?: string;
  /** Title of the workout. Not saved on iOS: HealthKit workouts have no title. */
  title?: string;
  /**
   * Total distance in meters. Android writes it as a `DistanceRecord` covering the workout (needs
   * `distance` write access).
   *
   * iOS writes a sample covering the workout with the distance type of the workout: walking, running
   * and hiking use `distanceWalkingRunning` (`distance` write access), cycling uses `distanceCycling`
   * (`distanceCycling` write access). Other workout types have no matching HealthKit distance type, so
   * the distance is not saved and is reported in {@link SaveWorkoutResult.skippedRecords}.
   * With {@link SaveWorkoutOptions.pauses}, iOS writes one sample per active interval instead (each
   * proportional to its duration, adding up to the total): HealthKit leaves paused time out of the
   * workout's statistics, so a sample spanning a pause would only count in part.
   */
  totalDistance?: number;
  /**
   * Active energy burned in kilocalories (not total energy). Android writes it as an
   * `ActiveCaloriesBurnedRecord` covering the workout, iOS as an `activeEnergyBurned` sample (split
   * per active interval when there are pauses, like {@link SaveWorkoutOptions.totalDistance}).
   */
  totalEnergyBurned?: number;
  /**
   * Heart-rate readings recorded during the workout. Android writes them as one `HeartRateRecord`,
   * iOS as one `heartRate` sample per reading.
   * Readings outside the workout (`startDate` inclusive, `endDate` exclusive) are dropped and
   * counted in {@link SaveWorkoutResult.droppedHeartRateSamples}.
   */
  heartRateSamples?: WorkoutHeartRateSample[];
  /**
   * GPS route of the workout. Locations outside the workout (`startDate` inclusive, `endDate`
   * exclusive) or in the same millisecond as an earlier location are dropped and counted in
   * {@link SaveWorkoutResult.droppedRouteLocations}.
   *
   * The route is only saved when `workoutRoute` write access is granted; otherwise the workout is
   * saved without it and {@link SaveWorkoutResult.routeSaved} is `false`. On iOS the route goes
   * through `HKWorkoutRouteBuilder` after the workout is saved; if HealthKit refuses it, the workout
   * stays saved and `routeSaved` is `false`.
   */
  route?: WorkoutRouteLocation[];
  /**
   * Pauses inside the workout. They must lie within the workout and must not overlap.
   * Android stores each one as a Health Connect exercise segment of type pause
   * (`EXERCISE_SEGMENT_TYPE_PAUSE`, valid for every workout type). iOS adds a pause and a resume
   * workout event, and HealthKit leaves the time between them out of the workout duration.
   */
  pauses?: WorkoutPause[];
  /**
   * How the workout was recorded (defaults to `'activelyRecorded'`).
   * On Android this sets the Health Connect recording method; actively recorded workouts are
   * attributed to a phone device. On iOS actively recorded workouts are attributed to this device,
   * and manual entries get `HKMetadataKeyWasUserEntered` and no device.
   */
  recordingMethod?: RecordingMethod;
  /**
   * Metadata key-value pairs. Ignored on Android: Health Connect has no custom metadata.
   * On iOS the string values are saved on the workout; keys starting with `HK` are reserved by
   * HealthKit and rejected.
   */
  metadata?: Record<string, string>;
}

export interface SaveWorkoutResult {
  /**
   * Platform-specific identifier of the saved workout (HealthKit UUID on iOS, Health Connect metadata
   * ID of the exercise session on Android). Matches `platformId` returned by
   * {@link HealthPlugin.queryWorkouts}.
   */
  platformId: string;
  /** Whether a route with at least one location was saved with the workout. */
  routeSaved: boolean;
  /**
   * Companion records that were provided but not saved because their write permission is missing
   * (`distance` for `totalDistance`, `energy` for `totalEnergyBurned`, `heartRate` for
   * `heartRateSamples`), or, for `distance` on iOS, because the workout type has no HealthKit
   * distance type. The workout itself is still saved. Empty when everything was written.
   */
  skippedRecords: WorkoutCompanionRecord[];
  /** Number of route locations dropped because they were outside the workout or repeated an earlier location's millisecond. */
  droppedRouteLocations: number;
  /** Number of heart-rate readings dropped because they were outside the workout. */
  droppedHeartRateSamples: number;
}

export type BucketType = 'hour' | 'day' | 'week' | 'month';

export type AggregationType = 'sum' | 'average' | 'min' | 'max';

export interface QueryAggregatedOptions {
  /** The type of data to aggregate from the health store. */
  dataType: HealthDataType;
  /** Inclusive ISO 8601 start date (defaults to now - 1 day). */
  startDate?: string;
  /** Exclusive ISO 8601 end date (defaults to now). */
  endDate?: string;
  /** Time bucket for aggregation (defaults to 'day'). */
  bucket?: BucketType;
  /**
   * Aggregation operation(s) to perform (defaults to 'sum').
   *
   * Pass a single {@link AggregationType} to compute one aggregation, or an array to
   * compute several in a single query. Each requested aggregation is returned in
   * {@link AggregatedSample.values}, keyed by its name.
   */
  aggregation?: AggregationType | AggregationType[];
}

export interface AggregatedSample {
  /** ISO 8601 start date of the bucket. */
  startDate: string;
  /** ISO 8601 end date of the bucket. */
  endDate: string;
  /**
   * Aggregated value for the bucket. When multiple aggregations are requested, this holds
   * the value of the first requested aggregation that produced a result. See {@link values}
   * for every requested aggregation.
   */
  value: number;
  /** Map of each requested aggregation type to its aggregated value for the bucket. */
  values: Partial<Record<AggregationType, number>>;
  /** Unit of the aggregated value. */
  unit: HealthUnit;
}

export interface QueryAggregatedResult {
  samples: AggregatedSample[];
}

export interface HealthPlugin {
  /** Returns whether the current platform supports the native health SDK. */
  isAvailable(): Promise<AvailabilityResult>;
  /**
   * Requests read/write access to the provided data types.
   *
   * Set `requestHistoryAccess: true` to additionally request Android's
   * `READ_HEALTH_DATA_HISTORY` permission in the same Health Connect permission sheet
   * (see {@link AuthorizationOptions.requestHistoryAccess}). The granted/denied status is
   * reported back as `historyAccessAuthorized` on the result.
   */
  requestAuthorization(options: AuthorizationOptions): Promise<AuthorizationStatus>;
  /** Checks authorization status for the provided data types without prompting the user. */
  checkAuthorization(options: AuthorizationOptions): Promise<AuthorizationStatus>;
  /** Reads samples for the given data type within the specified time frame. */
  readSamples(options: QueryOptions): Promise<ReadSamplesResult>;
  /** Writes a single sample to the native health store. */
  saveSample(options: WriteSampleOptions): Promise<void>;

  /**
   * Deletes records this app saved, by `clientRecordId` and/or `platformId`.
   * Supported on iOS (HealthKit) and Android (Health Connect).
   *
   * Give at least one id, and at most 100 ids in total. Both platforms only let an app delete the
   * records it wrote itself, and deleting needs write access to the type; without it the call rejects
   * with code `permission-denied`. Other failures reject with code `delete-samples-failed`.
   *
   * With `dataType: 'workouts'` the workout's companion records are deleted too:
   * - iOS: the distance, active-energy and heart-rate samples and the route associated with each
   *   workout (for types the app can still write). Finding the workouts needs read access to
   *   workouts: request `read: ['workouts']`, otherwise HealthKit may reject the lookup
   *   (`permission-denied`).
   * - Android: the distance, active-calories and heart-rate records saved by `saveWorkout` for the
   *   given `clientRecordIds` (for types the app can still write). With `platformIds` only the exercise
   *   session is deleted.
   *
   * iOS: `bloodPressure` is looked up first too (to delete the correlation with its systolic and
   * diastolic samples), which needs read access: request `read: ['bloodPressure']`, otherwise the call
   * can reject with `permission-denied`.
   *
   * Android: ids that match nothing are ignored on Android 14 and later; on Android 13 and lower the
   * call can reject instead (see {@link DeleteSamplesResult.deleted}).
   *
   * @param options The record type and the ids to delete
   * @returns A promise that resolves with the number of deleted records (see {@link DeleteSamplesResult.deleted})
   * @throws An error if the options are invalid, the write permission is missing, or the delete fails
   */
  deleteSamples(options: DeleteSamplesOptions): Promise<DeleteSamplesResult>;

  /**
   * Get the native Capacitor plugin version
   *
   * @returns {Promise<{ version: string }>} a Promise with version for this device
   * @throws An error if something went wrong
   */
  getPluginVersion(): Promise<{ version: string }>;

  /**
   * Opens the Health Connect settings screen (Android only).
   * On iOS, this method does nothing.
   *
   * Use this to direct users to manage their Health Connect permissions
   * or to install Health Connect if not available.
   *
   * @throws An error if Health Connect settings cannot be opened
   */
  openHealthConnectSettings(): Promise<void>;

  /**
   * Shows the app's privacy policy for Health Connect (Android only).
   * On iOS, this method does nothing.
   *
   * This displays the same privacy policy screen that Health Connect shows
   * when the user taps "Privacy policy" in the permissions dialog.
   *
   * The privacy policy URL can be configured by adding a string resource
   * named "health_connect_privacy_policy_url" in your app's strings.xml,
   * or by placing an HTML file at www/privacypolicy.html in your assets.
   *
   * @throws An error if the privacy policy cannot be displayed
   */
  showPrivacyPolicy(): Promise<void>;

  /**
   * Queries workout sessions from the native health store.
   * Supported on iOS (HealthKit) and Android (Health Connect).
   *
   * @param options Query options including optional workout type filter, date range, limit, and sort order
   * @returns A promise that resolves with the workout sessions
   * @throws An error if something went wrong
   */
  queryWorkouts(options: QueryWorkoutsOptions): Promise<QueryWorkoutsResult>;

  /**
   * Saves a completed workout to the native health store.
   * Supported on iOS (HealthKit) and Android (Health Connect). Steps are never written.
   *
   * On Android the workout is written in a single atomic Health Connect insert: one
   * `ExerciseSessionRecord` (with the optional route and pauses), plus a `DistanceRecord`, an
   * `ActiveCaloriesBurnedRecord` and a `HeartRateRecord` when the matching options are provided.
   *
   * On iOS an `HKWorkoutBuilder` saves the workout with its distance, active energy and heart-rate
   * samples and its pause / resume events; the route is then attached with the builder's
   * `HKWorkoutRouteBuilder`.
   *
   * Requires write access to `workouts`; without it the call rejects with code `permission-denied`
   * and writes nothing. Distance, active energy and heart rate need `distance` (`distanceCycling`
   * for cycling on iOS), `calories` and `heartRate` write access: when one is missing the workout is
   * still saved without that record, which is reported in {@link SaveWorkoutResult.skippedRecords}.
   * The route needs `workoutRoute` write access; without it the workout is saved without the route
   * (see {@link SaveWorkoutResult.routeSaved}). Other failures reject with code `save-workout-failed`.
   *
   * iOS only: code `workout-saved-unavailable` means HealthKit saved the workout but could not return
   * it because the device is locked, so its route was not saved and no `platformId` is known. With a
   * `clientRecordId` the call can be retried safely: the sync identifier replaces the saved copy.
   *
   * @param options The workout to save
   * @returns A promise that resolves with the saved workout's platform id and what was written
   * @throws An error if the options are invalid, a required permission is missing, or the save fails
   */
  saveWorkout(options: SaveWorkoutOptions): Promise<SaveWorkoutResult>;

  /**
   * Queries aggregated health data from the native health store.
   * Aggregates data into time buckets (hour, day, week, month) with operations like sum, average, min, or max.
   * This is more efficient than fetching individual samples for large date ranges.
   *
   * Supported on iOS (HealthKit) and Android (Health Connect).
   *
   * @param options Query options including data type, date range, bucket size, and aggregation type
   * @returns A promise that resolves with the aggregated samples
   * @throws An error if something went wrong
   */
  queryAggregated(options: QueryAggregatedOptions): Promise<QueryAggregatedResult>;
}
