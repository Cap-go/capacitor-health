export type HealthDataType = 'steps' | 'distance' | 'calories' | 'heartRate' | 'weight';

export type HealthUnit = 'count' | 'meter' | 'kilocalorie' | 'bpm' | 'kilogram';

export interface AuthorizationOptions {
  /** Data types that should be readable after authorization. */
  read?: HealthDataType[];
  /** Data types that should be writable after authorization. */
  write?: HealthDataType[];
}

export interface AuthorizationStatus {
  readAuthorized: HealthDataType[];
  readDenied: HealthDataType[];
  writeAuthorized: HealthDataType[];
  writeDenied: HealthDataType[];
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

export interface HealthSample {
  dataType: HealthDataType;
  value: number;
  unit: HealthUnit;
  startDate: string;
  endDate: string;
  sourceName?: string;
  sourceId?: string;
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
  | 'stepTraining'
  | 'surfingSports'
  | 'taiChi'
  | 'transition'
  | 'wheelchairRunPace'
  | 'wheelchairWalkPace'
  | 'wrestling'
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
   * Anchor for pagination or incremental queries.
   * Use the anchor returned in QueryWorkoutsResult to fetch the next page or incremental set of results.
   *
   * **iOS behavior**: Uses HKAnchoredObjectQuery. When an anchor is provided with a date range,
   * HealthKit returns workouts that match the predicate and haven't been delivered since the anchor was created.
   * This enables efficient incremental queries for NEW workouts, but does NOT track modifications to existing workouts.
   *
   * **Android behavior**: Uses Health Connect's pagination tokens for simple pagination through a result set.
   * This does NOT track changes - it simply continues reading through results matching the query.
   * To detect truly new/modified workouts on Android, you would need Health Connect's change tracking API.
   *
   * **Important**: The `ascending` parameter is ignored when using anchored queries to maintain consistency.
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
  /** Additional metadata (if available). */
  metadata?: Record<string, string>;
}

export interface QueryWorkoutsResult {
  workouts: Workout[];
  /**
   * Anchor for pagination or incremental queries. Save this value and pass it to the next queryWorkouts call.
   *
   * **iOS**: Returns an HKQueryAnchor (as base64 string) that can be used for incremental queries.
   * The anchor is returned even when the workouts array is empty, allowing subsequent queries to detect new data.
   *
   * **Android**: Returns a pagination token to continue reading through the result set.
   * The token may be absent when there are no more results to fetch.
   *
   * **Platform differences**:
   * - iOS: Enables incremental queries - returns NEW workouts since the anchor was created
   * - Android: Simple pagination - continues through the same result set
   *
   * The anchor may be omitted when:
   * - The platform doesn't support anchors (web implementation)
   * - There are no more results (Android pagination exhausted)
   *
   * Always check for the presence of anchor before using it for pagination.
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
}

export interface HealthPlugin {
  /** Returns whether the current platform supports the native health SDK. */
  isAvailable(): Promise<AvailabilityResult>;
  /** Requests read/write access to the provided data types. */
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
}
