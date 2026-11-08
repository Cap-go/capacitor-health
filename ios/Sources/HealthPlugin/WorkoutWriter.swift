import Foundation
import HealthKit
import CoreLocation
import Capacitor

/// Errors of `saveWorkout` and `deleteSamples`, with plugin error codes aligned with Android.
enum HealthWriteError: LocalizedError {
    case invalidOption(String)
    case permissionDenied(String)
    case saveFailed(String)
    /// HealthKit saved the workout but could not return it (the device is locked).
    case savedButUnavailable(String)
    case deleteFailed(String)

    var errorDescription: String? {
        switch self {
        case let .invalidOption(message), let .permissionDenied(message), let .saveFailed(message),
             let .savedButUnavailable(message), let .deleteFailed(message):
            return message
        }
    }

    /// Plugin error code; validation errors carry no code.
    var code: String? {
        switch self {
        case .invalidOption:
            return nil
        case .permissionDenied:
            return "permission-denied"
        case .saveFailed:
            return "save-workout-failed"
        case .savedButUnavailable:
            return "workout-saved-unavailable"
        case .deleteFailed:
            return "delete-samples-failed"
        }
    }

    /// Code for any error: ours, `permission-denied` for HealthKit authorization errors, nil for
    /// validation errors, otherwise `fallback`.
    static func code(for error: Error, fallback: String) -> String? {
        if let writeError = error as? HealthWriteError {
            return writeError.code
        }
        if error is HealthManagerError {
            return nil
        }
        if let healthKitError = error as? HKError,
           healthKitError.code == .errorAuthorizationDenied || healthKitError.code == .errorAuthorizationNotDetermined {
            return "permission-denied"
        }
        return fallback
    }
}

/// Writes a complete workout to HealthKit for `saveWorkout`.
///
/// An `HKWorkoutBuilder` collects the optional distance, active-energy and heart-rate samples and the
/// pause / resume events, then saves the workout. The GPS route goes through the builder's
/// `HKWorkoutRouteBuilder` and is attached after the workout is saved. Steps are never written.
///
/// Only workout share access is required. A companion sample whose share access is missing is left
/// out and reported in `skippedRecords`; a route without share access is left out (`routeSaved: false`).
///
/// Idempotency: with a `clientRecordId`, every object gets `HKMetadataKeySyncIdentifier` (the id for
/// the workout, `<id>:distance`, `<id>:energy`, `<id>:hr:<n>` and `<id>:route` for the rest; with pauses
/// the distance and energy are split per active interval as `<id>:distance:<n>` / `<id>:energy:<n>`) and an
/// increasing `HKMetadataKeySyncVersion` (`SyncMetadata.currentVersion()`), so HealthKit replaces the
/// earlier copy of each object written again.
final class WorkoutWriter {
    private let healthStore: HKHealthStore

    /// Uses the plugin's existing store (one `HKHealthStore` per app, as Apple recommends).
    init(healthStore: HKHealthStore) {
        self.healthStore = healthStore
    }

    /// Validates `options` and saves the workout. `completion` is called exactly once: synchronously for
    /// validation and permission errors, otherwise on a HealthKit background queue.
    func saveWorkout(options: JSObject, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(HealthManagerError.healthDataUnavailable))
            return
        }

        let input: WorkoutInput
        do {
            input = try WorkoutInput(options: options)
        } catch {
            completion(.failure(error))
            return
        }

        guard isSharingAuthorized(HKObjectType.workoutType()) else {
            completion(.failure(HealthWriteError.permissionDenied(
                "Missing HealthKit write permission for workouts. Request write access to 'workouts' with requestAuthorization() first."
            )))
            return
        }

        save(input, plan: makePlan(for: input), completion: completion)
    }

    // MARK: - Plan

    /// Everything handed to HealthKit for one save.
    private struct SavePlan {
        let device: HKDevice?
        let samples: [HKSample]
        let events: [HKWorkoutEvent]
        let metadata: [String: Any]
        /// Empty when there is no route or no route share access.
        let route: [CLLocation]
        let routeMetadata: [String: Any]?
        let skippedRecords: [String]
        let droppedRouteLocations: Int
        let droppedHeartRateSamples: Int
    }

    private func makePlan(for input: WorkoutInput) -> SavePlan {
        // Actively recorded workouts come from this phone; manual entries have no device.
        let device: HKDevice? = input.manualEntry ? nil : HKDevice.local()
        let version = SyncMetadata.currentVersion()
        let metadata = { (idSuffix: String?) -> [String: Any] in
            WorkoutWriter.objectMetadata(for: input, idSuffix: idSuffix, version: version)
        }
        let companions = companionSamples(for: input, device: device, metadata: metadata)
        let canWriteRoute = !input.route.isEmpty && isSharingAuthorized(HKSeriesType.workoutRoute())
        let routeMetadata = metadata("route")

        var workoutMetadata: [String: Any] = input.metadata
        workoutMetadata.merge(metadata(nil)) { _, pluginValue in pluginValue }

        return SavePlan(
            device: device,
            samples: companions.samples,
            events: WorkoutWriter.pauseEvents(for: input.pauses),
            metadata: workoutMetadata,
            route: canWriteRoute ? input.route : [],
            routeMetadata: routeMetadata.isEmpty ? nil : routeMetadata,
            skippedRecords: companions.skipped,
            droppedRouteLocations: input.droppedRouteLocations,
            droppedHeartRateSamples: input.droppedHeartRateSamples
        )
    }

    /// Distance, active-energy and heart-rate samples whose share access is granted, plus the names of the
    /// provided ones left out (`skippedRecords`).
    private func companionSamples(for input: WorkoutInput, device: HKDevice?,
                                  metadata: (String?) -> [String: Any]) -> (samples: [HKSample], skipped: [String]) {
        var samples: [HKSample] = []
        var skippedRecords: [String] = []
        let activeIntervals = WorkoutWriter.activeIntervals(start: input.startDate, end: input.endDate, pauses: input.pauses)

        if let meters = input.totalDistanceMeters {
            if let type = WorkoutWriter.distanceType(for: input.workoutType.hkWorkoutActivityType()), isSharingAuthorized(type) {
                samples += WorkoutWriter.totalSamples(type: type, unit: HKUnit.meter(), total: meters, over: activeIntervals,
                                                      idSuffix: "distance", device: device, metadata: metadata)
            } else {
                skippedRecords.append("distance")
            }
        }

        if let kilocalories = input.totalEnergyKilocalories {
            if let type = HKObjectType.quantityType(forIdentifier: .activeEnergyBurned), isSharingAuthorized(type) {
                samples += WorkoutWriter.totalSamples(type: type, unit: HKUnit.kilocalorie(), total: kilocalories, over: activeIntervals,
                                                      idSuffix: "energy", device: device, metadata: metadata)
            } else {
                skippedRecords.append("energy")
            }
        }

        if !input.heartRateSamples.isEmpty {
            if let type = HKObjectType.quantityType(forIdentifier: .heartRate), isSharingAuthorized(type) {
                let unit = HKUnit.count().unitDivided(by: HKUnit.minute())
                for (index, reading) in input.heartRateSamples.enumerated() {
                    let quantity = HKQuantity(unit: unit, doubleValue: reading.bpm)
                    samples.append(HKQuantitySample(type: type, quantity: quantity, start: reading.date, end: reading.date,
                                                    device: device, metadata: metadata("hr:\(index)")))
                }
            } else {
                skippedRecords.append("heartRate")
            }
        }

        return (samples, skippedRecords)
    }

    private func isSharingAuthorized(_ type: HKObjectType) -> Bool {
        return healthStore.authorizationStatus(for: type) == .sharingAuthorized
    }

    /// HealthKit has one distance type per kind of movement; workouts without one skip the distance sample.
    private static func distanceType(for activityType: HKWorkoutActivityType) -> HKQuantityType? {
        switch activityType {
        case .running, .walking, .hiking:
            return HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)
        case .cycling:
            return HKObjectType.quantityType(forIdentifier: .distanceCycling)
        default:
            return nil
        }
    }

    /// The workout window minus the pauses (sorted, non-overlapping and inside the window, as
    /// `WorkoutInput` validates them), leaving out empty gaps. A workout paused from start to end has
    /// no active time: the whole window is returned so the total is still saved.
    private static func activeIntervals(start: Date, end: Date, pauses: [DateInterval]) -> [DateInterval] {
        var intervals: [DateInterval] = []
        var cursor = start
        for pause in pauses {
            if pause.start > cursor {
                intervals.append(DateInterval(start: cursor, end: pause.start))
            }
            cursor = max(cursor, pause.end)
        }
        if end > cursor {
            intervals.append(DateInterval(start: cursor, end: end))
        }
        return intervals.isEmpty ? [DateInterval(start: start, end: end)] : intervals
    }

    /// Splits `total` over `intervals` (non-empty, each longer than zero) in proportion to their
    /// durations. The last share takes the floating-point remainder, so the shares add up to `total`.
    private static func split(_ total: Double, over intervals: [DateInterval]) -> [Double] {
        let activeDuration = intervals.reduce(0) { $0 + $1.duration }
        var shares: [Double] = []
        var assigned = 0.0
        for interval in intervals.dropLast() {
            let share = total * (interval.duration / activeDuration)
            shares.append(share)
            assigned += share
        }
        shares.append(max(0, total - assigned))
        return shares
    }

    /// A workout total (distance or active energy) as one sample per active interval. HealthKit leaves
    /// paused time out of the workout's statistics and prorates a sample that spans a pause, so a single
    /// sample over the whole window would count only part of the total. One piece keeps `<id>:<idSuffix>`;
    /// several pieces get `<id>:<idSuffix>:<n>` (from 0, in time order).
    private static func totalSamples(type: HKQuantityType, unit: HKUnit, total: Double, over intervals: [DateInterval],
                                     idSuffix: String, device: HKDevice?,
                                     metadata: (String?) -> [String: Any]) -> [HKSample] {
        let shares = split(total, over: intervals)
        var samples: [HKSample] = []
        for index in intervals.indices {
            let interval = intervals[index]
            let suffix = intervals.count == 1 ? idSuffix : "\(idSuffix):\(index)"
            let quantity = HKQuantity(unit: unit, doubleValue: shares[index])
            samples.append(HKQuantitySample(type: type, quantity: quantity, start: interval.start, end: interval.end,
                                            device: device, metadata: metadata(suffix)))
        }
        return samples
    }

    private static func objectMetadata(for input: WorkoutInput, idSuffix: String?, version: NSNumber) -> [String: Any] {
        let syncIdentifier = input.clientRecordId.map { clientRecordId in
            idSuffix.map { "\(clientRecordId):\($0)" } ?? clientRecordId
        }
        return SyncMetadata.metadata(syncIdentifier: syncIdentifier, version: version, userEntered: input.manualEntry)
    }

    /// Each pause becomes a zero-length pause event and a resume event; HealthKit excludes the time
    /// between them from the workout duration.
    private static func pauseEvents(for pauses: [DateInterval]) -> [HKWorkoutEvent] {
        return pauses.flatMap { pause in
            [
                HKWorkoutEvent(type: .pause, dateInterval: DateInterval(start: pause.start, duration: 0), metadata: nil),
                HKWorkoutEvent(type: .resume, dateInterval: DateInterval(start: pause.end, duration: 0), metadata: nil)
            ]
        }
    }

    // MARK: - Save

    /// One asynchronous `HKWorkoutBuilder` call. `action` names it in the error when HealthKit
    /// reports failure without an error.
    private struct BuilderStep {
        let action: String
        let run: (@escaping (Bool, Error?) -> Void) -> Void
    }

    private func save(_ input: WorkoutInput, plan: SavePlan, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = input.workoutType.hkWorkoutActivityType()
        configuration.locationType = input.route.isEmpty ? .unknown : .outdoor
        switch input.workoutType {
        case .swimmingPool:
            configuration.swimmingLocationType = .pool
        case .swimmingOpenWater:
            // The shared mapping (used by queryWorkouts filters) turns swimmingOpenWater into waterSports;
            // HealthKit records an open-water swim as a swimming workout with an open-water location.
            configuration.activityType = .swimming
            configuration.swimmingLocationType = .openWater
        default:
            break
        }
        let builder = HKWorkoutBuilder(healthStore: healthStore, configuration: configuration, device: plan.device)

        // begin -> events -> metadata -> samples, so the pauses are known before the samples arrive.
        var steps = [BuilderStep(action: "start the workout") { done in
            builder.beginCollection(withStart: input.startDate, completion: done)
        }]
        if !plan.events.isEmpty {
            steps.append(BuilderStep(action: "add the workout pauses") { done in
                builder.addWorkoutEvents(plan.events, completion: done)
            })
        }
        if !plan.metadata.isEmpty {
            steps.append(BuilderStep(action: "add the workout metadata") { done in
                builder.addMetadata(plan.metadata, completion: done)
            })
        }
        if !plan.samples.isEmpty {
            steps.append(BuilderStep(action: "add the workout samples") { done in
                builder.add(plan.samples, completion: done)
            })
        }

        WorkoutWriter.run(steps[...]) { error in
            if let error = error {
                // Still collecting (endCollection not called yet), so discarding is valid here.
                builder.discardWorkout()
                completion(.failure(error))
                return
            }
            WorkoutWriter.insertRoute(plan.route, into: builder) { routeBuilder in
                WorkoutWriter.finish(builder, endDate: input.endDate, plan: plan, routeBuilder: routeBuilder, completion: completion)
            }
        }
    }

    /// Runs `steps` in order and stops at the first failure. `completion` is called exactly once.
    private static func run(_ steps: ArraySlice<BuilderStep>, completion: @escaping (Error?) -> Void) {
        guard let step = steps.first else {
            completion(nil)
            return
        }
        step.run { success, error in
            if let error = error {
                completion(error)
                return
            }
            guard success else {
                completion(HealthWriteError.saveFailed("HealthKit could not \(step.action)."))
                return
            }
            WorkoutWriter.run(steps.dropFirst(), completion: completion)
        }
    }

    /// Hands `locations` to the builder's route builder. Calls back with the route builder, or nil when
    /// there is no route or HealthKit refuses the locations (the workout is then saved without a route).
    /// The route builder is never `discard()`ed: any later call on a discarded builder raises, and an
    /// unfinished route builder just drops its data when it is released.
    private static func insertRoute(_ locations: [CLLocation], into builder: HKWorkoutBuilder,
                                    completion: @escaping (HKWorkoutRouteBuilder?) -> Void) {
        guard !locations.isEmpty,
              let routeBuilder = builder.seriesBuilder(for: HKSeriesType.workoutRoute()) as? HKWorkoutRouteBuilder else {
            completion(nil)
            return
        }
        routeBuilder.insertRouteData(locations) { success, error in
            completion(success && error == nil ? routeBuilder : nil)
        }
    }

    private static func finish(_ builder: HKWorkoutBuilder, endDate: Date, plan: SavePlan, routeBuilder: HKWorkoutRouteBuilder?,
                               completion: @escaping (Result<[String: Any], Error>) -> Void) {
        builder.endCollection(withEnd: endDate) { success, error in
            guard success, error == nil else {
                builder.discardWorkout()
                completion(.failure(error ?? HealthWriteError.saveFailed("HealthKit could not end the workout.")))
                return
            }
            builder.finishWorkout { workout, error in
                if let error = error {
                    completion(.failure(error))
                    return
                }
                guard let workout = workout else {
                    // Apple: no workout and no error means the workout was saved but is not available
                    // because the device is locked. Without it the route cannot be attached.
                    completion(.failure(HealthWriteError.savedButUnavailable(
                        "HealthKit saved the workout but could not return it because the device is locked, so its route was not saved. "
                            + "Retrying with the same clientRecordId replaces it."
                    )))
                    return
                }
                // Apple: finish the route after the workout is saved, so it can be associated with it.
                WorkoutWriter.finishRoute(routeBuilder, workout: workout, metadata: plan.routeMetadata) { routeSaved in
                    completion(.success([
                        "platformId": workout.uuid.uuidString,
                        "routeSaved": routeSaved,
                        "skippedRecords": plan.skippedRecords,
                        "droppedRouteLocations": plan.droppedRouteLocations,
                        "droppedHeartRateSamples": plan.droppedHeartRateSamples
                    ]))
                }
            }
        }
    }

    /// Calls back with whether the route was saved. A route HealthKit cannot save never fails the
    /// already saved workout.
    private static func finishRoute(_ routeBuilder: HKWorkoutRouteBuilder?, workout: HKWorkout, metadata: [String: Any]?,
                                    completion: @escaping (Bool) -> Void) {
        guard let routeBuilder = routeBuilder else {
            completion(false)
            return
        }
        routeBuilder.finishRoute(with: workout, metadata: metadata) { route, error in
            completion(route != nil && error == nil)
        }
    }
}

// MARK: - Sync metadata

/// Sync and recording metadata shared by `saveWorkout` and `saveSample`.
enum SyncMetadata {
    private static let versionCounter = SyncVersionCounter()

    /// `HKMetadataKeySyncVersion` for a save: the save time in milliseconds, but always greater than the
    /// previous version handed out, so two saves in the same millisecond, or after the clock moved back
    /// while the app runs, still replace each other. HealthKit replaces an object with the same
    /// `HKMetadataKeySyncIdentifier` only when the new sync version is greater.
    static func currentVersion() -> NSNumber {
        return NSNumber(value: versionCounter.next(now: Int64((Date().timeIntervalSince1970 * 1000).rounded())))
    }

    static func metadata(syncIdentifier: String?, version: NSNumber, userEntered: Bool) -> [String: Any] {
        var metadata: [String: Any] = [:]
        if let syncIdentifier = syncIdentifier {
            metadata[HKMetadataKeySyncIdentifier] = syncIdentifier
            metadata[HKMetadataKeySyncVersion] = version
        }
        if userEntered {
            metadata[HKMetadataKeyWasUserEntered] = true
        }
        return metadata
    }

    /// `saveSample`'s `clientRecordId` and `recordingMethod`. Omitting both adds nothing, as before;
    /// only `manualEntry` sets `HKMetadataKeyWasUserEntered`.
    static func forSample(clientRecordId: String?, recordingMethod: String?) throws -> [String: Any] {
        if let clientRecordId = clientRecordId, clientRecordId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            throw HealthWriteError.invalidOption("clientRecordId must not be blank")
        }
        switch recordingMethod {
        case nil, "activelyRecorded"?, "manualEntry"?:
            break
        case let other?:
            throw HealthWriteError.invalidOption(
                "Unsupported recordingMethod: \(other). Expected 'activelyRecorded' or 'manualEntry'."
            )
        }
        return metadata(syncIdentifier: clientRecordId, version: currentVersion(), userEntered: recordingMethod == "manualEntry")
    }

    /// Metadata for a part of a sample saved with `forSample` metadata (the systolic and diastolic samples
    /// of a blood pressure correlation): `<clientRecordId>:<part>` as sync identifier with the same sync
    /// version, so saving again replaces the parts along with the correlation. Nil without a
    /// `clientRecordId`, so the parts are saved without metadata as before.
    static func part(_ name: String, of sampleMetadata: [String: Any]) -> [String: Any]? {
        guard let syncIdentifier = sampleMetadata[HKMetadataKeySyncIdentifier] as? String,
              let version = sampleMetadata[HKMetadataKeySyncVersion] as? NSNumber else {
            return nil
        }
        let userEntered = sampleMetadata[HKMetadataKeyWasUserEntered] as? Bool ?? false
        return metadata(syncIdentifier: "\(syncIdentifier):\(name)", version: version, userEntered: userEntered)
    }
}

/// Hands out strictly increasing sync versions. Thread-safe, since plugin calls can run on different threads.
private final class SyncVersionCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var last: Int64 = 0

    func next(now: Int64) -> Int64 {
        lock.lock()
        defer { lock.unlock() }
        last = max(now, last + 1)
        return last
    }
}

// MARK: - Authorization

/// Write access for `saveWorkout`, used by `requestAuthorization` / `checkAuthorization`.
extension WorkoutWriter {
    /// Share type behind the `workouts` and `workoutRoute` write identifiers; nil for any other identifier.
    static func shareType(forWriteIdentifier identifier: String) -> HKSampleType? {
        switch identifier {
        case "workouts":
            return HKObjectType.workoutType()
        case "workoutRoute":
            return HKSeriesType.workoutRoute()
        default:
            return nil
        }
    }

    /// Splits `write` identifiers into regular data type identifiers and the workout ones handled here.
    static func splitWriteIdentifiers(_ identifiers: [String]) -> (dataTypes: [String], workouts: [String]) {
        let workouts = identifiers.filter { shareType(forWriteIdentifier: $0) != nil }
        let dataTypes = identifiers.filter { shareType(forWriteIdentifier: $0) == nil }
        return (dataTypes, workouts)
    }

    /// Share types to request. Apple ("Creating a workout route"): route data needs permission to read
    /// and share both `HKWorkout` and `HKWorkoutRoute`, so `workoutRoute` also shares workouts.
    static func shareTypes(forWriteIdentifiers identifiers: [String]) -> Set<HKSampleType> {
        var types = Set(identifiers.compactMap { shareType(forWriteIdentifier: $0) })
        if identifiers.contains("workoutRoute") {
            types.insert(HKObjectType.workoutType())
        }
        return types
    }

    /// Read types to request alongside: `workoutRoute` needs read access to workouts and routes (see above).
    static func readTypes(forWriteIdentifiers identifiers: [String]) -> Set<HKObjectType> {
        guard identifiers.contains("workoutRoute") else {
            return []
        }
        return [HKObjectType.workoutType(), HKSeriesType.workoutRoute()]
    }

    /// Adds the share status of the workout write identifiers to `payload`.
    static func addingWriteStatus(of identifiers: [String], to payload: AuthorizationStatusPayload,
                                  healthStore: HKHealthStore) -> AuthorizationStatusPayload {
        guard !identifiers.isEmpty else {
            return payload
        }
        var authorized = payload.writeAuthorized
        var denied = payload.writeDenied
        for identifier in identifiers {
            if let type = shareType(forWriteIdentifier: identifier),
               healthStore.authorizationStatus(for: type) == .sharingAuthorized {
                authorized.append(identifier)
            } else {
                denied.append(identifier)
            }
        }
        return AuthorizationStatusPayload(
            readAuthorized: payload.readAuthorized,
            readDenied: payload.readDenied,
            writeAuthorized: authorized,
            writeDenied: denied
        )
    }
}

// MARK: - Input

/// Validated `saveWorkout` options, with the same rules as Android's `WorkoutWriter.parse`.
///
/// Invalid values throw `HealthWriteError.invalidOption` naming the field. Heart-rate readings and
/// route locations outside the workout (`startDate` inclusive, `endDate` exclusive), and route
/// locations in the same millisecond as an earlier one, are dropped and counted instead of failing the workout.
struct WorkoutInput {
    struct HeartRateReading {
        let date: Date
        let bpm: Double
    }

    let workoutType: WorkoutType
    let startDate: Date
    let endDate: Date
    let clientRecordId: String?
    let totalDistanceMeters: Double?
    let totalEnergyKilocalories: Double?
    let heartRateSamples: [HeartRateReading]
    let route: [CLLocation]
    let pauses: [DateInterval]
    let manualEntry: Bool
    /// String values of the `metadata` option, saved on the workout.
    let metadata: [String: String]
    let droppedHeartRateSamples: Int
    let droppedRouteLocations: Int

    init(options: JSObject) throws {
        let typeName = try WorkoutInput.requiredString(options["workoutType"], path: "workoutType")
        guard let type = WorkoutType(rawValue: typeName) else {
            throw HealthWriteError.invalidOption("Unsupported workoutType: \(typeName)")
        }
        let start = try WorkoutInput.requiredDate(options["startDate"], path: "startDate")
        let end = try WorkoutInput.requiredDate(options["endDate"], path: "endDate")
        guard end > start else {
            throw HealthWriteError.invalidOption("endDate must be after startDate")
        }
        let recordId = try WorkoutInput.optionalString(options["clientRecordId"], path: "clientRecordId")
        if let recordId = recordId, recordId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            throw HealthWriteError.invalidOption("clientRecordId must not be blank")
        }
        // HealthKit workouts have no title: the option is validated like on Android, then not saved.
        _ = try WorkoutInput.optionalString(options["title"], path: "title")
        let distance = try WorkoutInput.parseTotal(options["totalDistance"], path: "totalDistance", unit: "meters")
        let energy = try WorkoutInput.parseTotal(options["totalEnergyBurned"], path: "totalEnergyBurned", unit: "kilocalories")
        let isManualEntry = try WorkoutInput.parseManualEntry(options["recordingMethod"])
        let window = start..<end
        let heartRate = try WorkoutInput.parseHeartRateSamples(options["heartRateSamples"], window: window)
        let routeResult = try WorkoutInput.parseRoute(options["route"], window: window)
        let pauseIntervals = try WorkoutInput.parsePauses(options["pauses"], workout: DateInterval(start: start, end: end))
        let userMetadata = try WorkoutInput.parseMetadata(options["metadata"])

        workoutType = type
        startDate = start
        endDate = end
        clientRecordId = recordId
        totalDistanceMeters = distance
        totalEnergyKilocalories = energy
        heartRateSamples = heartRate.readings
        droppedHeartRateSamples = heartRate.dropped
        route = routeResult.locations
        droppedRouteLocations = routeResult.dropped
        pauses = pauseIntervals
        manualEntry = isManualEntry
        metadata = userMetadata
    }

    // MARK: Options

    /// `totalDistance` / `totalEnergyBurned`: 0 to 1000000 (Health Connect's limit, so both platforms
    /// accept the same values).
    private static func parseTotal(_ value: JSValue?, path: String, unit: String) throws -> Double? {
        let total = try optionalNumber(value, path: path)
        if let total = total, !(0...1_000_000).contains(total) {
            throw HealthWriteError.invalidOption("\(path) must be between 0 and 1000000 \(unit), got \(total)")
        }
        return total
    }

    private static func parseManualEntry(_ value: JSValue?) throws -> Bool {
        switch try optionalString(value, path: "recordingMethod") {
        case nil, "activelyRecorded"?:
            return false
        case "manualEntry"?:
            return true
        case let other?:
            throw HealthWriteError.invalidOption(
                "Unsupported recordingMethod: \(other). Expected 'activelyRecorded' or 'manualEntry'."
            )
        }
    }

    private static func parseHeartRateSamples(_ value: JSValue?, window: Range<Date>) throws -> (readings: [HeartRateReading], dropped: Int) {
        let items = try optionalArray(value, path: "heartRateSamples")
        var readings: [HeartRateReading] = []
        for (index, item) in items.enumerated() {
            let path = "heartRateSamples[\(index)]"
            let object = try requiredObject(item, path: path)
            let date = try requiredDate(object["date"], path: "\(path).date")
            let bpm = try requiredNumber(object["bpm"], path: "\(path).bpm")
            guard (1.0...300.0).contains(bpm) else {
                throw HealthWriteError.invalidOption("\(path).bpm must be between 1 and 300, got \(bpm)")
            }
            if window.contains(date) {
                readings.append(HeartRateReading(date: date, bpm: bpm))
            }
        }
        return (sortedByDate(readings) { $0.date }, items.count - readings.count)
    }

    private static func parseRoute(_ value: JSValue?, window: Range<Date>) throws -> (locations: [CLLocation], dropped: Int) {
        let items = try optionalArray(value, path: "route")
        var inWorkout: [CLLocation] = []
        for (index, item) in items.enumerated() {
            let location = try parseLocation(item, path: "route[\(index)]")
            if window.contains(location.timestamp) {
                inWorkout.append(location)
            }
        }
        // Keep the first location given for a millisecond, like Android (Health Connect stores route times in
        // milliseconds and needs them to increase).
        var unique: [CLLocation] = []
        for location in sortedByDate(inWorkout, { $0.timestamp }) {
            if let previous = unique.last, epochMilliseconds(previous.timestamp) == epochMilliseconds(location.timestamp) {
                continue
            }
            unique.append(location)
        }
        return (unique, items.count - unique.count)
    }

    /// Whole milliseconds since 1970, rounded down like Java's `Instant.toEpochMilli()`.
    private static func epochMilliseconds(_ date: Date) -> Int64 {
        return Int64((date.timeIntervalSince1970 * 1000).rounded(.down))
    }

    private static func parseLocation(_ value: JSValue, path: String) throws -> CLLocation {
        let object = try requiredObject(value, path: path)
        let date = try requiredDate(object["date"], path: "\(path).date")
        let latitude = try requiredNumber(object["latitude"], path: "\(path).latitude")
        guard (-90.0...90.0).contains(latitude) else {
            throw HealthWriteError.invalidOption("\(path).latitude must be between -90 and 90, got \(latitude)")
        }
        let longitude = try requiredNumber(object["longitude"], path: "\(path).longitude")
        guard (-180.0...180.0).contains(longitude) else {
            throw HealthWriteError.invalidOption("\(path).longitude must be between -180 and 180, got \(longitude)")
        }
        let altitude = try optionalNumber(object["altitude"], path: "\(path).altitude")
        let horizontalAccuracy = try accuracy(object["horizontalAccuracy"], path: "\(path).horizontalAccuracy")
        let verticalAccuracy = try accuracy(object["verticalAccuracy"], path: "\(path).verticalAccuracy")
        // CoreLocation reads a negative accuracy as "invalid": an unknown altitude gets verticalAccuracy -1,
        // and an unknown horizontal accuracy is stored as 0 so the coordinate stays valid.
        return CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            altitude: altitude ?? 0,
            horizontalAccuracy: horizontalAccuracy ?? 0,
            verticalAccuracy: altitude == nil ? -1 : (verticalAccuracy ?? 0),
            timestamp: date
        )
    }

    private static func accuracy(_ value: JSValue?, path: String) throws -> Double? {
        let accuracy = try optionalNumber(value, path: path)
        if let accuracy = accuracy, accuracy < 0 {
            throw HealthWriteError.invalidOption("\(path) must be 0 or more, got \(accuracy)")
        }
        return accuracy
    }

    private static func parsePauses(_ value: JSValue?, workout: DateInterval) throws -> [DateInterval] {
        let items = try optionalArray(value, path: "pauses")
        var pauses: [DateInterval] = []
        for (index, item) in items.enumerated() {
            let path = "pauses[\(index)]"
            let object = try requiredObject(item, path: path)
            let start = try requiredDate(object["startDate"], path: "\(path).startDate")
            let end = try requiredDate(object["endDate"], path: "\(path).endDate")
            guard end > start else {
                throw HealthWriteError.invalidOption("\(path).endDate must be after \(path).startDate")
            }
            guard start >= workout.start, end <= workout.end else {
                throw HealthWriteError.invalidOption("\(path) must lie within the workout (between startDate and endDate)")
            }
            pauses.append(DateInterval(start: start, end: end))
        }
        let sorted = pauses.sorted { $0.start < $1.start }
        for (previous, next) in zip(sorted, sorted.dropFirst()) where previous.end > next.start {
            throw HealthWriteError.invalidOption(
                "pauses must not overlap: the pause ending at \(isoString(previous.end)) overlaps the one starting at \(isoString(next.start))"
            )
        }
        return sorted
    }

    private static func parseMetadata(_ value: JSValue?) throws -> [String: String] {
        if isAbsent(value) {
            return [:]
        }
        guard let object = value as? JSObject else {
            throw HealthWriteError.invalidOption("metadata must be an object")
        }
        var metadata: [String: String] = [:]
        for (key, entry) in object {
            guard !key.hasPrefix("HK") else {
                throw HealthWriteError.invalidOption("metadata.\(key): keys starting with HK are reserved by HealthKit")
            }
            // Only string values are kept, like saveSample.
            if let string = entry as? String {
                metadata[key] = string
            }
        }
        return metadata
    }

    // MARK: Values

    private static let fractionalSecondsFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    private static let wholeSecondsFormatter = ISO8601DateFormatter()

    private static func isoString(_ date: Date) -> String {
        return fractionalSecondsFormatter.string(from: date)
    }

    /// Sorts by date and keeps the input order for equal dates.
    private static func sortedByDate<Element>(_ elements: [Element], _ date: (Element) -> Date) -> [Element] {
        return elements.enumerated()
            .sorted { (date($0.element), $0.offset) < (date($1.element), $1.offset) }
            .map { $0.element }
    }

    /// A missing key and JSON `null` both mean "not provided".
    private static func isAbsent(_ value: JSValue?) -> Bool {
        guard let value = value else {
            return true
        }
        return value is NSNull
    }

    private static func optionalString(_ value: JSValue?, path: String) throws -> String? {
        if isAbsent(value) {
            return nil
        }
        guard let string = value as? String else {
            throw HealthWriteError.invalidOption("\(path) must be a string")
        }
        return string
    }

    private static func requiredString(_ value: JSValue?, path: String) throws -> String {
        guard let string = try optionalString(value, path: path),
              !string.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw HealthWriteError.invalidOption("\(path) is required")
        }
        return string
    }

    private static func optionalNumber(_ value: JSValue?, path: String) throws -> Double? {
        if isAbsent(value) {
            return nil
        }
        // JSON numbers arrive as NSNumber. Booleans do too, and are rejected.
        guard let number = value as? NSNumber, CFGetTypeID(number) != CFBooleanGetTypeID() else {
            throw HealthWriteError.invalidOption("\(path) must be a number")
        }
        guard number.doubleValue.isFinite else {
            throw HealthWriteError.invalidOption("\(path) must be a finite number")
        }
        return number.doubleValue
    }

    private static func requiredNumber(_ value: JSValue?, path: String) throws -> Double {
        guard let number = try optionalNumber(value, path: path) else {
            throw HealthWriteError.invalidOption("\(path) is required")
        }
        return number
    }

    /// Accepts ISO 8601 dates with or without fractional seconds (`toISOString()` has them).
    private static func requiredDate(_ value: JSValue?, path: String) throws -> Date {
        let string = try requiredString(value, path: path)
        guard let date = fractionalSecondsFormatter.date(from: string) ?? wholeSecondsFormatter.date(from: string) else {
            throw HealthWriteError.invalidOption("\(path) must be an ISO 8601 date, got \(string)")
        }
        return date
    }

    private static func optionalArray(_ value: JSValue?, path: String) throws -> JSArray {
        if isAbsent(value) {
            return []
        }
        guard let array = value as? JSArray else {
            throw HealthWriteError.invalidOption("\(path) must be an array")
        }
        return array
    }

    private static func requiredObject(_ value: JSValue, path: String) throws -> JSObject {
        guard let object = value as? JSObject else {
            throw HealthWriteError.invalidOption("\(path) must be an object")
        }
        return object
    }
}
