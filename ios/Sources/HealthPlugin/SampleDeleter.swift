import Foundation
import HealthKit
import Capacitor

/// Deletes records this app saved, for `deleteSamples`.
///
/// Records are matched by `HKMetadataKeySyncIdentifier` (the `clientRecordId` given when saving) and/or
/// UUID (`platformId`), limited to this app's source: HealthKit only lets an app delete the objects it
/// saved, and only while it has share access to their type.
///
/// Apple does not document that deleting a workout also deletes its associated samples or route, so
/// `workouts` deletes the distance, active-energy and heart-rate samples and the route associated with
/// each workout first (for the types the app can still share), then the workouts. Blood pressure deletes
/// the correlation together with its systolic and diastolic samples.
final class SampleDeleter {
    private let healthStore: HKHealthStore

    /// Uses the plugin's existing store.
    init(healthStore: HKHealthStore) {
        self.healthStore = healthStore
    }

    /// Validates `options` and deletes the matching records. `completion` is called exactly once:
    /// synchronously for validation and permission errors, otherwise on a HealthKit background queue.
    func deleteSamples(options: JSObject, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(HealthManagerError.healthDataUnavailable))
            return
        }

        let request: DeleteRequest
        do {
            request = try DeleteRequest(options: options)
        } catch {
            completion(.failure(error))
            return
        }

        guard canDelete(request.target) else {
            completion(.failure(HealthWriteError.permissionDenied(
                "Missing HealthKit write permission for \(request.dataType). Deleting needs write access to "
                    + "'\(request.dataType)': request it with requestAuthorization() first."
            )))
            return
        }

        switch request.target {
        case .workouts:
            deleteWorkouts(matching: request.predicate, completion: completion)
        case let .correlations(type):
            deleteCorrelations(of: type, matching: request.predicate, completion: completion)
        case let .samples(type):
            healthStore.deleteObjects(of: type, predicate: request.predicate) { success, deletedCount, error in
                SampleDeleter.finish(success: success, count: deletedCount, error: error, dataType: request.dataType,
                                     completion: completion)
            }
        }
    }

    // MARK: - Targets

    private func canDelete(_ target: DeleteRequest.Target) -> Bool {
        switch target {
        case .workouts:
            return isSharingAuthorized(HKObjectType.workoutType())
        case .correlations:
            // Blood pressure share access is tracked on the systolic / diastolic types, like saveSample.
            guard let systolicType = HKObjectType.quantityType(forIdentifier: .bloodPressureSystolic),
                  let diastolicType = HKObjectType.quantityType(forIdentifier: .bloodPressureDiastolic) else {
                return false
            }
            return isSharingAuthorized(systolicType) && isSharingAuthorized(diastolicType)
        case let .samples(type):
            return isSharingAuthorized(type)
        }
    }

    private func isSharingAuthorized(_ type: HKObjectType) -> Bool {
        return healthStore.authorizationStatus(for: type) == .sharingAuthorized
    }

    /// Finds the workouts, deletes what is associated with them, then deletes the workouts in one call.
    private func deleteWorkouts(matching predicate: NSPredicate, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        query(HKObjectType.workoutType(), matching: predicate) { result in
            switch result {
            case let .failure(error):
                completion(.failure(SampleDeleter.lookupError(error, dataType: "workouts")))
            case let .success(samples):
                let workouts = samples.compactMap { $0 as? HKWorkout }
                guard !workouts.isEmpty else {
                    completion(.success(["deleted": 0]))
                    return
                }
                let associatedPredicate = NSCompoundPredicate(andPredicateWithSubpredicates: [
                    NSCompoundPredicate(orPredicateWithSubpredicates: workouts.map { HKQuery.predicateForObjects(from: $0) }),
                    HKQuery.predicateForObjects(from: HKSource.default())
                ])
                self.deleteAssociated(self.associatedTypes()[...], matching: associatedPredicate) { error in
                    if let error = error {
                        completion(.failure(error))
                        return
                    }
                    self.healthStore.delete(workouts) { success, error in
                        SampleDeleter.finish(success: success, count: workouts.count, error: error, dataType: "workouts",
                                             completion: completion)
                    }
                }
            }
        }
    }

    /// Types saveWorkout associates with a workout that the app can still share (and so delete).
    private func associatedTypes() -> [HKSampleType] {
        let identifiers: [HKQuantityTypeIdentifier] = [.distanceWalkingRunning, .distanceCycling, .activeEnergyBurned, .heartRate]
        var types: [HKSampleType] = identifiers.compactMap { HKObjectType.quantityType(forIdentifier: $0) }
        types.append(HKSeriesType.workoutRoute())
        return types.filter { isSharingAuthorized($0) }
    }

    /// Deletes the objects of each type matching `predicate`, one type after another.
    /// `completion` is called exactly once, with the first error if any.
    private func deleteAssociated(_ types: ArraySlice<HKSampleType>, matching predicate: NSPredicate,
                                  completion: @escaping (Error?) -> Void) {
        guard let type = types.first else {
            completion(nil)
            return
        }
        healthStore.deleteObjects(of: type, predicate: predicate) { success, _, error in
            if let error = error, !SampleDeleter.isNoData(error) {
                completion(error)
                return
            }
            guard success || error != nil else {
                completion(HealthWriteError.deleteFailed("HealthKit could not delete the workout's \(type.identifier) samples."))
                return
            }
            self.deleteAssociated(types.dropFirst(), matching: predicate, completion: completion)
        }
    }

    /// Deletes blood pressure correlations together with their systolic and diastolic samples.
    private func deleteCorrelations(of type: HKCorrelationType, matching predicate: NSPredicate,
                                    completion: @escaping (Result<[String: Any], Error>) -> Void) {
        query(type, matching: predicate) { result in
            switch result {
            case let .failure(error):
                completion(.failure(SampleDeleter.lookupError(error, dataType: "bloodPressure")))
            case let .success(samples):
                let correlations = samples.compactMap { $0 as? HKCorrelation }
                guard !correlations.isEmpty else {
                    completion(.success(["deleted": 0]))
                    return
                }
                var objects: [HKObject] = correlations
                for correlation in correlations {
                    for sample in correlation.objects {
                        objects.append(sample)
                    }
                }
                self.healthStore.delete(objects) { success, error in
                    SampleDeleter.finish(success: success, count: correlations.count, error: error, dataType: "bloodPressure",
                                         completion: completion)
                }
            }
        }
    }

    // MARK: - HealthKit calls

    private func query(_ type: HKSampleType, matching predicate: NSPredicate,
                       completion: @escaping (Result<[HKSample], Error>) -> Void) {
        let sampleQuery = HKSampleQuery(
            sampleType: type,
            predicate: predicate,
            limit: HKObjectQueryNoLimit,
            sortDescriptors: nil
        ) { _, samples, error in
            if let error = error {
                completion(.failure(error))
                return
            }
            completion(.success(samples ?? []))
        }
        healthStore.execute(sampleQuery)
    }

    /// Resolves with `count` deleted records. Apple does not document what `deleteObjects` reports when
    /// nothing matches, so a "no data" error also counts as zero deleted.
    private static func finish(success: Bool, count: Int, error: Error?, dataType: String,
                               completion: @escaping (Result<[String: Any], Error>) -> Void) {
        if let error = error {
            if isNoData(error) {
                completion(.success(["deleted": 0]))
            } else {
                completion(.failure(error))
            }
            return
        }
        guard success else {
            completion(.failure(HealthWriteError.deleteFailed("HealthKit could not delete the \(dataType) records.")))
            return
        }
        completion(.success(["deleted": count]))
    }

    private static func isNoData(_ error: Error) -> Bool {
        return (error as? HKError)?.code == .errorNoData
    }

    /// Looking records up needs read authorization to have been requested for the type; HealthKit
    /// answers `errorAuthorizationNotDetermined` otherwise.
    private static func lookupError(_ error: Error, dataType: String) -> Error {
        guard (error as? HKError)?.code == .errorAuthorizationNotDetermined else {
            return error
        }
        return HealthWriteError.permissionDenied(
            "Deleting \(dataType) first looks the records up, which needs read access to '\(dataType)': "
                + "request read: ['\(dataType)'] with requestAuthorization() first."
        )
    }
}

/// Validated `deleteSamples` options.
struct DeleteRequest {
    enum Target {
        case workouts
        case correlations(HKCorrelationType)
        case samples(HKSampleType)
    }

    static let maxIds = 100

    let dataType: String
    let target: Target
    /// Matches the requested sync identifiers or UUIDs, among the objects saved by this app.
    let predicate: NSPredicate

    init(options: JSObject) throws {
        guard let dataType = options["dataType"] as? String, !dataType.isEmpty else {
            throw HealthWriteError.invalidOption("dataType is required")
        }
        let target = try DeleteRequest.resolveTarget(for: dataType)

        let clientRecordIds = try DeleteRequest.idList(options["clientRecordIds"], path: "clientRecordIds")
        let platformIds = try DeleteRequest.idList(options["platformIds"], path: "platformIds")
        guard !clientRecordIds.isEmpty || !platformIds.isEmpty else {
            throw HealthWriteError.invalidOption("Provide at least one id in clientRecordIds or platformIds")
        }
        let count = clientRecordIds.count + platformIds.count
        guard count <= DeleteRequest.maxIds else {
            throw HealthWriteError.invalidOption("At most \(DeleteRequest.maxIds) ids can be deleted per call, got \(count)")
        }

        var uuids = Set<UUID>()
        for (index, platformId) in platformIds.enumerated() {
            guard let uuid = UUID(uuidString: platformId) else {
                throw HealthWriteError.invalidOption("platformIds[\(index)] is not a HealthKit UUID: \(platformId)")
            }
            uuids.insert(uuid)
        }

        var idPredicates: [NSPredicate] = []
        if !clientRecordIds.isEmpty {
            idPredicates.append(HKQuery.predicateForObjects(withMetadataKey: HKMetadataKeySyncIdentifier, allowedValues: clientRecordIds))
        }
        if !uuids.isEmpty {
            idPredicates.append(HKQuery.predicateForObjects(with: uuids))
        }

        self.dataType = dataType
        self.target = target
        predicate = NSCompoundPredicate(andPredicateWithSubpredicates: [
            NSCompoundPredicate(orPredicateWithSubpredicates: idPredicates),
            HKQuery.predicateForObjects(from: HKSource.default())
        ])
    }

    private static func resolveTarget(for dataType: String) throws -> Target {
        switch dataType {
        case "workouts":
            return .workouts
        case "workoutRoute":
            throw HealthWriteError.invalidOption(
                "workoutRoute cannot be deleted on its own: delete the workout with dataType 'workouts'"
            )
        default:
            guard let type = HealthDataType(rawValue: dataType) else {
                throw HealthManagerError.invalidDataType(dataType)
            }
            if type == .bloodPressure {
                guard let correlationType = HKObjectType.correlationType(forIdentifier: .bloodPressure) else {
                    throw HealthManagerError.dataTypeUnavailable(dataType)
                }
                return .correlations(correlationType)
            }
            return .samples(try type.sampleType())
        }
    }

    /// A missing key or JSON `null` is an empty list; duplicates are dropped.
    private static func idList(_ value: JSValue?, path: String) throws -> [String] {
        guard let value = value, !(value is NSNull) else {
            return []
        }
        guard let array = value as? JSArray else {
            throw HealthWriteError.invalidOption("\(path) must be an array of strings")
        }
        var ids: [String] = []
        for (index, item) in array.enumerated() {
            guard let id = item as? String, !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                throw HealthWriteError.invalidOption("\(path)[\(index)] must be a non-empty string")
            }
            if !ids.contains(id) {
                ids.append(id)
            }
        }
        return ids
    }
}
