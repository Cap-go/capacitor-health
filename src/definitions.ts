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
  | 'workouts';

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
  | 'centimeter';

export interface AuthorizationOptions {
  /** Data types that should be readable after authorization. */
  read?: HealthDataType[];
  /** Data types that should be writable after authorization. */
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
  /** Platform-specific unique identifier (HealthKit UUID on iOS, Health Connect metadata ID on Android). */
  platformId?: string;
  /** For sleep data, indicates the sleep state (e.g., 'asleep', 'awake', 'rem', 'deep', 'light'). */
  sleepState?: SleepState;
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
}

export interface QueryWorkoutsResult {
  workouts: Workout[];
  /**
   * Anchor for the next page of results. Pass this value as the anchor parameter in the next query
   * to continue pagination. If undefined or null, there are no more results.
   */
  anchor?: string;
}

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
