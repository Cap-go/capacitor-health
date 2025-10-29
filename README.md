# @capgo/capacitor-health
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin_health"> ➡️ Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin_health"> Missing a feature? We’ll build the plugin for you 💪</a></h2>
</div>

Capacitor plugin to read and write health metrics via Apple HealthKit (iOS) and Health Connect (Android). The TypeScript API keeps the same data types and units across platforms so you can build once and deploy everywhere.

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/health/

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

1. **Min SDK 26+.** Health Connect is only available on Android 8.0 (API 26) and above. The plugin’s Gradle setup already targets this level.
2. **Declare Health permissions.** The plugin manifest ships with the required `<uses-permission>` declarations (`READ_/WRITE_STEPS`, `READ_/WRITE_DISTANCE`, `READ_/WRITE_ACTIVE_CALORIES_BURNED`, `READ_/WRITE_HEART_RATE`, `READ_/WRITE_WEIGHT`). Your app does not need to duplicate them, but you must surface a user-facing rationale because the permissions are considered health sensitive.
3. **Ensure Health Connect is installed.** Devices on Android 14+ include it by default. For earlier versions the user must install *Health Connect by Android* from the Play Store. The `Health.isAvailable()` helper exposes the current status so you can prompt accordingly.
4. **Request runtime access.** The plugin opens the Health Connect permission UI when you call `requestAuthorization`. You should still handle denial flows (e.g., show a message if `checkAuthorization` reports missing scopes).

If you already used Google Fit in your project you can remove the associated dependencies (`play-services-fitness`, `play-services-auth`, OAuth configuration, etc.).

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

| Identifier | Default unit  | Notes |
| ---------- | ------------- | ----- |
| `steps`    | `count`       | Step count deltas |
| `distance` | `meter`       | Walking / running distance |
| `calories` | `kilocalorie` | Active energy burned |
| `heartRate`| `bpm`         | Beats per minute |
| `weight`   | `kilogram`    | Body mass |

All write operations expect the default unit shown above. On Android the `metadata` option is currently ignored by Health Connect.

## API

<docgen-index>

* [`isAvailable()`](#isavailable)
* [`requestAuthorization(...)`](#requestauthorization)
* [`checkAuthorization(...)`](#checkauthorization)
* [`readSamples(...)`](#readsamples)
* [`saveSample(...)`](#savesample)
* [`getPluginVersion()`](#getpluginversion)
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

| Prop             | Type                                                      |
| ---------------- | --------------------------------------------------------- |
| **`dataType`**   | <code><a href="#healthdatatype">HealthDataType</a></code> |
| **`value`**      | <code>number</code>                                       |
| **`unit`**       | <code><a href="#healthunit">HealthUnit</a></code>         |
| **`startDate`**  | <code>string</code>                                       |
| **`endDate`**    | <code>string</code>                                       |
| **`sourceName`** | <code>string</code>                                       |
| **`sourceId`**   | <code>string</code>                                       |


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


### Type Aliases


#### HealthDataType

<code>'steps' | 'distance' | 'calories' | 'heartRate' | 'weight'</code>


#### HealthUnit

<code>'count' | 'meter' | 'kilocalorie' | 'bpm' | 'kilogram'</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>

</docgen-api>

### Credits:

this plugin was inspired by the work of https://github.com/perfood/capacitor-healthkit/ for ios and https://github.com/perfood/capacitor-google-fit for Android
