# @capgo/capacitor-health

<a href="https://capgo.app/">
  <img
    src="https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png"
    alt="Capgo - Instant updates for capacitor"
  />
</a>

<div align="center">
  <h2>
    <a href="https://capgo.app/?ref=plugin_health"> ➡️ Get Instant updates for your App with Capgo</a>
  </h2>
  <h2>
    <a href="https://capgo.app/consulting/?ref=plugin_health"> Missing a feature? We’ll build the plugin for you 💪</a>
  </h2>
</div>

Capacitor plugin to read and write health metrics via Apple HealthKit (iOS) and Health Connect (Android). The TypeScript API keeps the same data types and units across platforms so you can build once and deploy everywhere.

## Why Capacitor Health?

The only **free**, **unified** health data plugin for Capacitor supporting the latest native APIs:

- **Health Connect (Android)** - Uses Google's newest health platform (replaces deprecated Google Fit)
- **HealthKit (iOS)** - Full integration with Apple's health framework
- **Unified API** - Same TypeScript interface across platforms with consistent units
- **Multiple metrics** - Steps, distance, calories, heart rate, weight
- **Read & Write** - Query historical data and save new health entries
- **Modern standards** - Supports Android 8.0+ and iOS 14+
- **Modern package management** - Supports both Swift Package Manager (SPM) and CocoaPods (SPM-ready for Capacitor 8)

Perfect for fitness apps, health trackers, wellness platforms, and medical applications.

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/health/

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.\*.\*       | v8.\*.\*                | ✅          |
| v7.\*.\*       | v7.\*.\*                | On demand   |
| v6.\*.\*       | v6.\*.\*                | ❌          |
| v5.\*.\*       | v5.\*.\*                | ❌          |

> **Note:** The major version of this plugin follows the major version of Capacitor. Use the version that matches your Capacitor installation (e.g., plugin v8 for Capacitor 8). Only the latest major version is actively maintained.

## Install

```bash
npm install @capgo/capacitor-health
npx cap sync
```

## iOS Setup

1. Open your Capacitor application's Xcode workspace and enable the **HealthKit** capability.
2. Provide usage descriptions in `Info.plist` (update the copy for your product):

```xml
<key>NSHealthShareUsageDescription</key>
<string>This app reads your health data to personalise your experience.</string>
<key>NSHealthUpdateUsageDescription</key>
<string>This app writes new health entries that you explicitly create.</string>
```

## Android Setup

This plugin now uses [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) instead of Google Fit. Make sure your app meets the requirements below:

1. **Min SDK 26+.** Health Connect is only available on Android 8.0 (API 26) and above. The plugin's Gradle setup already targets this level.
2. **Declare Health permissions.** The plugin manifest ships with the required `<uses-permission>` declarations for basic data types (`READ_/WRITE_STEPS`, `READ_/WRITE_DISTANCE`, `READ_/WRITE_ACTIVE_CALORIES_BURNED`, `READ_/WRITE_HEART_RATE`, `READ_/WRITE_WEIGHT`, `READ_/WRITE_SLEEP`, `READ_/WRITE_RESPIRATORY_RATE`, `READ_/WRITE_OXYGEN_SATURATION`, `READ_/WRITE_RESTING_HEART_RATE`, `READ_/WRITE_HEART_RATE_VARIABILITY`). Your app does not need to duplicate them, but you must surface a user-facing rationale because the permissions are considered health sensitive.
3. **Ensure Health Connect is installed.** Devices on Android 14+ include it by default. For earlier versions the user must install _Health Connect by Android_ from the Play Store. The `Health.isAvailable()` helper exposes the current status so you can prompt accordingly.
4. **Request runtime access.** The plugin opens the Health Connect permission UI when you call `requestAuthorization`. You should still handle denial flows (e.g., show a message if `checkAuthorization` reports missing scopes).
5. **Provide a Privacy Policy.** Health Connect requires apps to display a privacy policy explaining how health data is used. See the [Privacy Policy Setup](#privacy-policy-setup) section below.

If you already used Google Fit in your project you can remove the associated dependencies (`play-services-fitness`, `play-services-auth`, OAuth configuration, etc.).

### Privacy Policy Setup

Health Connect requires your app to provide a privacy policy that explains how you handle health data. When users tap "Privacy policy" in the Health Connect permissions dialog, your app must display this information.

**Option 1: HTML file in assets (recommended for simple cases)**

Place an HTML file at `android/app/src/main/assets/public/privacypolicy.html`:

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Privacy Policy</title>
  </head>
  <body>
    <h1>Privacy Policy</h1>
    <p>Your privacy policy content here...</p>
    <h2>Health Data</h2>
    <p>Explain how you collect, use, and protect health data...</p>
  </body>
</html>
```

**Option 2: Custom URL (recommended for hosted privacy policies)**

Add a string resource to your app's `android/app/src/main/res/values/strings.xml`:

```xml
<resources>
    <!-- Your other strings... -->
    <string name="health_connect_privacy_policy_url">https://yourapp.com/privacy-policy</string>
</resources>
```

This URL will be loaded in a WebView when the user requests to see your privacy policy.

**Programmatic access:**

You can also show the privacy policy or open Health Connect settings from your app:

```ts
// Show the privacy policy screen
await Health.showPrivacyPolicy();

// Open Health Connect settings (useful for managing permissions)
await Health.openHealthConnectSettings();
```

## Usage

```ts
import { Health } from '@capgo/capacitor-health';

// Verify that the native health SDK is present on this device
const availability = await Health.isAvailable();
if (!availability.available) {
  console.warn('Health access unavailable:', availability.reason);
}

// Ask for separate read/write access scopes
await Health.requestAuthorization({
  read: ['steps', 'heartRate', 'weight'],
  write: ['weight'],
});

// Query the last 50 step samples from the past 24 hours
const { samples } = await Health.readSamples({
  dataType: 'steps',
  startDate: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
  limit: 50,
});

// Persist a new body-weight entry (kilograms by default)
await Health.saveSample({
  dataType: 'weight',
  value: 74.3,
});
```

### Supported data types

| Identifier              | Default unit  | Notes                                                    |
| ----------------------- | ------------- | -------------------------------------------------------- |
| `steps`                 | `count`       | Step count deltas                                        |
| `distance`              | `meter`       | Walking / running distance                               |
| `calories`              | `kilocalorie` | Active energy burned                                     |
| `heartRate`             | `bpm`         | Beats per minute                                         |
| `weight`                | `kilogram`    | Body mass                                                |
| `sleep`                 | `minute`      | Sleep sessions with duration and states                  |
| `respiratoryRate`       | `bpm`         | Breaths per minute                                       |
| `oxygenSaturation`      | `percent`     | Blood oxygen saturation (SpO2)                           |
| `restingHeartRate`      | `bpm`         | Resting heart rate                                       |
| `heartRateVariability`  | `millisecond` | Heart rate variability (HRV)                             |
| `bloodPressure`         | `mmHg`        | Blood pressure (requires systolic/diastolic values)      |
| `bloodGlucose`          | `mg/dL`       | Blood glucose level                                      |
| `bodyTemperature`       | `celsius`     | Body temperature                                         |
| `height`                | `centimeter`  | Body height                                              |
| `flightsClimbed`        | `count`       | Floors / flights of stairs climbed                       |
| `exerciseTime`          | `minute`      | Apple Exercise Time (iOS only)                           |
| `distanceCycling`       | `meter`       | Cycling distance                                         |
| `bodyFat`               | `percent`     | Body fat percentage                                      |
| `basalBodyTemperature`  | `celsius`     | Basal body temperature                                   |
| `basalCalories`         | `kilocalorie` | Basal metabolic rate / resting energy                    |
| `totalCalories`         | `kilocalorie` | Total energy burned (active + basal)                     |
| `mindfulness`           | `minute`      | Mindfulness / meditation sessions                        |
| `workouts`              | N/A           | Workout sessions (read-only, use with `queryWorkouts()`) |

All write operations expect the default unit shown above. On Android the `metadata` option is currently ignored by Health Connect.

**Blood Pressure:** Blood pressure requires both systolic and diastolic values:

```ts
await Health.saveSample({
  dataType: 'bloodPressure',
  value: 120, // systolic value (used as main value)
  systolic: 120,
  diastolic: 80,
  startDate: new Date().toISOString(),
});

// Reading blood pressure returns samples with systolic/diastolic fields
const { samples } = await Health.readSamples({
  dataType: 'bloodPressure',
  startDate: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
});

samples.forEach((sample) => {
  console.log(`BP: ${sample.systolic}/${sample.diastolic} mmHg`);
});
```

**Note about workouts:** To query workout data using `queryWorkouts()`, you need to explicitly request `workouts` permission:

```ts
await Health.requestAuthorization({
  read: ['steps', 'workouts'], // Include 'workouts' to access workout sessions
});
```

**Pagination example:** Use the `anchor` parameter to paginate through workout results:

```ts
// First page: get the first 10 workouts
let result = await Health.queryWorkouts({
  startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(), // Last 30 days
  endDate: new Date().toISOString(),
  limit: 10,
});

console.log(`Found ${result.workouts.length} workouts`);

// If there are more results, the anchor will be set
while (result.anchor) {
  // Next page: use the anchor to continue from where we left off
  result = await Health.queryWorkouts({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    endDate: new Date().toISOString(),
    limit: 10,
    anchor: result.anchor, // Continue from the last result
  });

  console.log(`Found ${result.workouts.length} more workouts`);
}
```

### New Data Types Examples

**Sleep data:**
```ts
// Request permission for sleep data
await Health.requestAuthorization({
  read: ['sleep'],
});

// Read sleep sessions from the past week
const { samples } = await Health.readSamples({
  dataType: 'sleep',
  startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
});

samples.forEach(sample => {
  console.log(`Sleep: ${sample.value} minutes, state: ${sample.sleepState}`);
});
```

**Respiratory rate, oxygen saturation, and HRV:**
```ts
// Request permission
await Health.requestAuthorization({
  read: ['respiratoryRate', 'oxygenSaturation', 'restingHeartRate', 'heartRateVariability'],
});

// Read respiratory rate
const { samples: respiratoryRate } = await Health.readSamples({
  dataType: 'respiratoryRate',
  startDate: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
});

// Read oxygen saturation (SpO2)
const { samples: oxygenSat } = await Health.readSamples({
  dataType: 'oxygenSaturation',
  startDate: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
});
```

### Aggregated Queries

For large date ranges, use `queryAggregated()` to get aggregated data efficiently instead of fetching individual samples:

```ts
// Get daily step totals for the past month
const { samples } = await Health.queryAggregated({
  dataType: 'steps',
  startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
  bucket: 'day',        // Options: 'hour', 'day', 'week', 'month'
  aggregation: 'sum',   // Options: 'sum', 'average', 'min', 'max'
});

samples.forEach(sample => {
  console.log(`${sample.startDate}: ${sample.value} ${sample.unit}`);
});

// Get average heart rate by day
const { samples: avgHR } = await Health.queryAggregated({
  dataType: 'heartRate',
  startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
  endDate: new Date().toISOString(),
  bucket: 'day',
  aggregation: 'average',
});
```

**Note:** Aggregated queries are not supported for sleep, respiratory rate, oxygen saturation, and heart rate variability data types. These are instantaneous measurements and should use `readSamples()` instead. Aggregation is supported for: steps, distance, calories, heart rate, weight, and resting heart rate.

## API

<docgen-index>

* [`isAvailable()`](#isavailable)
* [`requestAuthorization(...)`](#requestauthorization)
* [`checkAuthorization(...)`](#checkauthorization)
* [`readSamples(...)`](#readsamples)
* [`saveSample(...)`](#savesample)
* [`getPluginVersion()`](#getpluginversion)
* [`openHealthConnectSettings()`](#openhealthconnectsettings)
* [`showPrivacyPolicy()`](#showprivacypolicy)
* [`queryWorkouts(...)`](#queryworkouts)
* [`queryAggregated(...)`](#queryaggregated)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### isAvailable()

```typescript
isAvailable() => Promise<AvailabilityResult>
```

Returns whether the current platform supports the native health SDK.

**Returns:** <code>Promise&lt;<a href="#availabilityresult">AvailabilityResult</a>&gt;</code>

--------------------


### requestAuthorization(...)

```typescript
requestAuthorization(options: AuthorizationOptions) => Promise<AuthorizationStatus>
```

Requests read/write access to the provided data types.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#authorizationoptions">AuthorizationOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#authorizationstatus">AuthorizationStatus</a>&gt;</code>

--------------------


### checkAuthorization(...)

```typescript
checkAuthorization(options: AuthorizationOptions) => Promise<AuthorizationStatus>
```

Checks authorization status for the provided data types without prompting the user.

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#authorizationoptions">AuthorizationOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#authorizationstatus">AuthorizationStatus</a>&gt;</code>

--------------------


### readSamples(...)

```typescript
readSamples(options: QueryOptions) => Promise<ReadSamplesResult>
```

Reads samples for the given data type within the specified time frame.

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#queryoptions">QueryOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#readsamplesresult">ReadSamplesResult</a>&gt;</code>

--------------------


### saveSample(...)

```typescript
saveSample(options: WriteSampleOptions) => Promise<void>
```

Writes a single sample to the native health store.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#writesampleoptions">WriteSampleOptions</a></code> |

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<{ version: string; }>
```

Get the native Capacitor plugin version

**Returns:** <code>Promise&lt;{ version: string; }&gt;</code>

--------------------


### openHealthConnectSettings()

```typescript
openHealthConnectSettings() => Promise<void>
```

Opens the Health Connect settings screen (Android only).
On iOS, this method does nothing.

Use this to direct users to manage their Health Connect permissions
or to install Health Connect if not available.

--------------------


### showPrivacyPolicy()

```typescript
showPrivacyPolicy() => Promise<void>
```

Shows the app's privacy policy for Health Connect (Android only).
On iOS, this method does nothing.

This displays the same privacy policy screen that Health Connect shows
when the user taps "Privacy policy" in the permissions dialog.

The privacy policy URL can be configured by adding a string resource
named "health_connect_privacy_policy_url" in your app's strings.xml,
or by placing an HTML file at www/privacypolicy.html in your assets.

--------------------


### queryWorkouts(...)

```typescript
queryWorkouts(options: QueryWorkoutsOptions) => Promise<QueryWorkoutsResult>
```

Queries workout sessions from the native health store.
Supported on iOS (HealthKit) and Android (Health Connect).

| Param         | Type                                                                  | Description                                                                             |
| ------------- | --------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#queryworkoutsoptions">QueryWorkoutsOptions</a></code> | Query options including optional workout type filter, date range, limit, and sort order |

**Returns:** <code>Promise&lt;<a href="#queryworkoutsresult">QueryWorkoutsResult</a>&gt;</code>

--------------------


### queryAggregated(...)

```typescript
queryAggregated(options: QueryAggregatedOptions) => Promise<QueryAggregatedResult>
```

Queries aggregated health data from the native health store.
Aggregates data into time buckets (hour, day, week, month) with operations like sum, average, min, or max.
This is more efficient than fetching individual samples for large date ranges.

Supported on iOS (HealthKit) and Android (Health Connect).

| Param         | Type                                                                      | Description                                                                      |
| ------------- | ------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| **`options`** | <code><a href="#queryaggregatedoptions">QueryAggregatedOptions</a></code> | Query options including data type, date range, bucket size, and aggregation type |

**Returns:** <code>Promise&lt;<a href="#queryaggregatedresult">QueryAggregatedResult</a>&gt;</code>

--------------------


### Interfaces


#### AvailabilityResult

| Prop            | Type                                     | Description                                            |
| --------------- | ---------------------------------------- | ------------------------------------------------------ |
| **`available`** | <code>boolean</code>                     |                                                        |
| **`platform`**  | <code>'ios' \| 'android' \| 'web'</code> | Platform specific details (for debugging/diagnostics). |
| **`reason`**    | <code>string</code>                      |                                                        |


#### AuthorizationStatus

| Prop                  | Type                          |
| --------------------- | ----------------------------- |
| **`readAuthorized`**  | <code>HealthDataType[]</code> |
| **`readDenied`**      | <code>HealthDataType[]</code> |
| **`writeAuthorized`** | <code>HealthDataType[]</code> |
| **`writeDenied`**     | <code>HealthDataType[]</code> |


#### AuthorizationOptions

| Prop        | Type                          | Description                                             |
| ----------- | ----------------------------- | ------------------------------------------------------- |
| **`read`**  | <code>HealthDataType[]</code> | Data types that should be readable after authorization. |
| **`write`** | <code>HealthDataType[]</code> | Data types that should be writable after authorization. |


#### ReadSamplesResult

| Prop          | Type                        |
| ------------- | --------------------------- |
| **`samples`** | <code>HealthSample[]</code> |


#### HealthSample

| Prop             | Type                                                      | Description                                                                                  |
| ---------------- | --------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| **`dataType`**   | <code><a href="#healthdatatype">HealthDataType</a></code> |                                                                                              |
| **`value`**      | <code>number</code>                                       |                                                                                              |
| **`unit`**       | <code><a href="#healthunit">HealthUnit</a></code>         |                                                                                              |
| **`startDate`**  | <code>string</code>                                       |                                                                                              |
| **`endDate`**    | <code>string</code>                                       |                                                                                              |
| **`sourceName`** | <code>string</code>                                       |                                                                                              |
| **`sourceId`**   | <code>string</code>                                       |                                                                                              |
| **`sleepState`** | <code><a href="#sleepstate">SleepState</a></code>         | For sleep data, indicates the sleep state (e.g., 'asleep', 'awake', 'rem', 'deep', 'light'). |
| **`systolic`**   | <code>number</code>                                       | For blood pressure data, the systolic value in mmHg.                                         |
| **`diastolic`**  | <code>number</code>                                       | For blood pressure data, the diastolic value in mmHg.                                        |


#### QueryOptions

| Prop            | Type                                                      | Description                                                        |
| --------------- | --------------------------------------------------------- | ------------------------------------------------------------------ |
| **`dataType`**  | <code><a href="#healthdatatype">HealthDataType</a></code> | The type of data to retrieve from the health store.                |
| **`startDate`** | <code>string</code>                                       | Inclusive ISO 8601 start date (defaults to now - 1 day).           |
| **`endDate`**   | <code>string</code>                                       | Exclusive ISO 8601 end date (defaults to now).                     |
| **`limit`**     | <code>number</code>                                       | Maximum number of samples to return (defaults to 100).             |
| **`ascending`** | <code>boolean</code>                                      | Return results sorted ascending by start date (defaults to false). |


#### WriteSampleOptions

| Prop            | Type                                                            | Description                                                                                                                                                                                       |
| --------------- | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`dataType`**  | <code><a href="#healthdatatype">HealthDataType</a></code>       |                                                                                                                                                                                                   |
| **`value`**     | <code>number</code>                                             |                                                                                                                                                                                                   |
| **`unit`**      | <code><a href="#healthunit">HealthUnit</a></code>               | Optional unit override. If omitted, the default unit for the data type is used (count for `steps`, meter for `distance`, kilocalorie for `calories`, bpm for `heartRate`, kilogram for `weight`). |
| **`startDate`** | <code>string</code>                                             | ISO 8601 start date for the sample. Defaults to now.                                                                                                                                              |
| **`endDate`**   | <code>string</code>                                             | ISO 8601 end date for the sample. Defaults to startDate.                                                                                                                                          |
| **`metadata`**  | <code><a href="#record">Record</a>&lt;string, string&gt;</code> | Metadata key-value pairs forwarded to the native APIs where supported.                                                                                                                            |
| **`systolic`**  | <code>number</code>                                             | For blood pressure data, the systolic value in mmHg. Required when dataType is 'bloodPressure'.                                                                                                   |
| **`diastolic`** | <code>number</code>                                             | For blood pressure data, the diastolic value in mmHg. Required when dataType is 'bloodPressure'.                                                                                                  |


#### QueryWorkoutsResult

| Prop           | Type                   | Description                                                                                                                                                             |
| -------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`workouts`** | <code>Workout[]</code> |                                                                                                                                                                         |
| **`anchor`**   | <code>string</code>    | Anchor for the next page of results. Pass this value as the anchor parameter in the next query to continue pagination. If undefined or null, there are no more results. |


#### Workout

| Prop                    | Type                                                            | Description                                         |
| ----------------------- | --------------------------------------------------------------- | --------------------------------------------------- |
| **`workoutType`**       | <code><a href="#workouttype">WorkoutType</a></code>             | The type of workout.                                |
| **`duration`**          | <code>number</code>                                             | Duration of the workout in seconds.                 |
| **`totalEnergyBurned`** | <code>number</code>                                             | Total energy burned in kilocalories (if available). |
| **`totalDistance`**     | <code>number</code>                                             | Total distance in meters (if available).            |
| **`startDate`**         | <code>string</code>                                             | ISO 8601 start date of the workout.                 |
| **`endDate`**           | <code>string</code>                                             | ISO 8601 end date of the workout.                   |
| **`sourceName`**        | <code>string</code>                                             | Source name that recorded the workout.              |
| **`sourceId`**          | <code>string</code>                                             | Source bundle identifier.                           |
| **`metadata`**          | <code><a href="#record">Record</a>&lt;string, string&gt;</code> | Additional metadata (if available).                 |


#### QueryWorkoutsOptions

| Prop              | Type                                                | Description                                                                                                                                                                                                                           |
| ----------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`workoutType`** | <code><a href="#workouttype">WorkoutType</a></code> | Optional workout type filter. If omitted, all workout types are returned.                                                                                                                                                             |
| **`startDate`**   | <code>string</code>                                 | Inclusive ISO 8601 start date (defaults to now - 1 day).                                                                                                                                                                              |
| **`endDate`**     | <code>string</code>                                 | Exclusive ISO 8601 end date (defaults to now).                                                                                                                                                                                        |
| **`limit`**       | <code>number</code>                                 | Maximum number of workouts to return (defaults to 100).                                                                                                                                                                               |
| **`ascending`**   | <code>boolean</code>                                | Return results sorted ascending by start date (defaults to false).                                                                                                                                                                    |
| **`anchor`**      | <code>string</code>                                 | Anchor for pagination. Use the anchor returned from a previous query to continue from that point. On iOS, this uses HKQueryAnchor. On Android, this uses Health Connect's pageToken. Omit this parameter to start from the beginning. |


#### QueryAggregatedResult

| Prop          | Type                            |
| ------------- | ------------------------------- |
| **`samples`** | <code>AggregatedSample[]</code> |


#### AggregatedSample

| Prop            | Type                                              | Description                        |
| --------------- | ------------------------------------------------- | ---------------------------------- |
| **`startDate`** | <code>string</code>                               | ISO 8601 start date of the bucket. |
| **`endDate`**   | <code>string</code>                               | ISO 8601 end date of the bucket.   |
| **`value`**     | <code>number</code>                               | Aggregated value for the bucket.   |
| **`unit`**      | <code><a href="#healthunit">HealthUnit</a></code> | Unit of the aggregated value.      |


#### QueryAggregatedOptions

| Prop              | Type                                                        | Description                                              |
| ----------------- | ----------------------------------------------------------- | -------------------------------------------------------- |
| **`dataType`**    | <code><a href="#healthdatatype">HealthDataType</a></code>   | The type of data to aggregate from the health store.     |
| **`startDate`**   | <code>string</code>                                         | Inclusive ISO 8601 start date (defaults to now - 1 day). |
| **`endDate`**     | <code>string</code>                                         | Exclusive ISO 8601 end date (defaults to now).           |
| **`bucket`**      | <code><a href="#buckettype">BucketType</a></code>           | Time bucket for aggregation (defaults to 'day').         |
| **`aggregation`** | <code><a href="#aggregationtype">AggregationType</a></code> | Aggregation operation to perform (defaults to 'sum').    |


### Type Aliases


#### HealthDataType

<code>'steps' | 'distance' | 'calories' | 'heartRate' | 'weight' | 'sleep' | 'respiratoryRate' | 'oxygenSaturation' | 'restingHeartRate' | 'heartRateVariability' | 'bloodPressure' | 'bloodGlucose' | 'bodyTemperature' | 'height' | 'flightsClimbed' | 'exerciseTime' | 'distanceCycling' | 'bodyFat' | 'basalBodyTemperature' | 'basalCalories' | 'totalCalories' | 'mindfulness'</code>


#### HealthUnit

<code>'count' | 'meter' | 'kilocalorie' | 'bpm' | 'kilogram' | 'minute' | 'percent' | 'millisecond' | 'mmHg' | 'mg/dL' | 'celsius' | 'fahrenheit' | 'centimeter'</code>


#### SleepState

<code>'inBed' | 'asleep' | 'awake' | 'rem' | 'deep' | 'light'</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>


#### WorkoutType

<code>'running' | 'cycling' | 'walking' | 'swimming' | 'yoga' | 'strengthTraining' | 'hiking' | 'tennis' | 'basketball' | 'soccer' | 'americanFootball' | 'baseball' | 'crossTraining' | 'elliptical' | 'rowing' | 'stairClimbing' | 'traditionalStrengthTraining' | 'waterFitness' | 'waterPolo' | 'waterSports' | 'wrestling' | 'other'</code>


#### BucketType

<code>'hour' | 'day' | 'week' | 'month'</code>


#### AggregationType

<code>'sum' | 'average' | 'min' | 'max'</code>

</docgen-api>

### Credits:

this plugin was inspired by the work of https://github.com/perfood/capacitor-healthkit/ for ios and https://github.com/perfood/capacitor-google-fit for Android
