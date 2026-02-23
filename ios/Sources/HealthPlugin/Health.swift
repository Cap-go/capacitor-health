import Foundation
import HealthKit

enum HealthManagerError: LocalizedError {
    case healthDataUnavailable
    case invalidDataType(String)
    case invalidDate(String)
    case dataTypeUnavailable(String)
    case invalidDateRange
    case operationFailed(String)

    var errorDescription: String? {
        switch self {
        case .healthDataUnavailable:
            return "Health data is not available on this device."
        case let .invalidDataType(identifier):
            return "Unsupported health data type: \(identifier)."
        case let .invalidDate(dateString):
            return "Invalid ISO 8601 date value: \(dateString)."
        case let .dataTypeUnavailable(identifier):
            return "The health data type \(identifier) is not available on this device."
        case .invalidDateRange:
            return "endDate must be greater than or equal to startDate."
        case let .operationFailed(message):
            return message
        }
    }
}

enum WorkoutType: String, CaseIterable {
    case running
    case cycling
    case walking
    case swimming
    case yoga
    case strengthTraining
    case hiking
    case tennis
    case basketball
    case soccer
    case americanFootball
    case baseball
    case crossTraining
    case elliptical
    case rowing
    case stairClimbing
    case traditionalStrengthTraining
    case waterFitness
    case waterPolo
    case waterSports
    case wrestling
    case other

    func hkWorkoutActivityType() -> HKWorkoutActivityType {
        switch self {
        case .running:
            return .running
        case .cycling:
            return .cycling
        case .walking:
            return .walking
        case .swimming:
            return .swimming
        case .yoga:
            return .yoga
        case .strengthTraining:
            return .traditionalStrengthTraining
        case .hiking:
            return .hiking
        case .tennis:
            return .tennis
        case .basketball:
            return .basketball
        case .soccer:
            return .soccer
        case .americanFootball:
            return .americanFootball
        case .baseball:
            return .baseball
        case .crossTraining:
            return .crossTraining
        case .elliptical:
            return .elliptical
        case .rowing:
            return .rowing
        case .stairClimbing:
            return .stairClimbing
        case .traditionalStrengthTraining:
            return .traditionalStrengthTraining
        case .waterFitness:
            return .waterFitness
        case .waterPolo:
            return .waterPolo
        case .waterSports:
            return .waterSports
        case .wrestling:
            return .wrestling
        case .other:
            return .other
        }
    }

    static func fromHKWorkoutActivityType(_ hkType: HKWorkoutActivityType) -> WorkoutType {
        switch hkType {
        case .running:
            return .running
        case .cycling:
            return .cycling
        case .walking:
            return .walking
        case .swimming:
            return .swimming
        case .yoga:
            return .yoga
        case .traditionalStrengthTraining:
            // Map back to strengthTraining for consistency (both map to the same HK type)
            return .strengthTraining
        case .hiking:
            return .hiking
        case .tennis:
            return .tennis
        case .basketball:
            return .basketball
        case .soccer:
            return .soccer
        case .americanFootball:
            return .americanFootball
        case .baseball:
            return .baseball
        case .crossTraining:
            return .crossTraining
        case .elliptical:
            return .elliptical
        case .rowing:
            return .rowing
        case .stairClimbing:
            return .stairClimbing
        case .waterFitness:
            return .waterFitness
        case .waterPolo:
            return .waterPolo
        case .waterSports:
            return .waterSports
        case .wrestling:
            return .wrestling
        default:
            return .other
        }
    }
}

enum HealthDataType: String, CaseIterable {
    case steps
    case distance
    case calories
    case heartRate
    case weight
    case sleep
    case respiratoryRate
    case oxygenSaturation
    case restingHeartRate
    case heartRateVariability
    case bloodPressure
    case bloodGlucose
    case bodyTemperature
    case height
    case flightsClimbed
    case exerciseTime
    case distanceCycling
    case bodyFat
    case basalBodyTemperature
    case basalCalories
    case totalCalories
    case mindfulness

    func sampleType() throws -> HKSampleType {
        switch self {
        case .sleep:
            guard let type = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) else {
                throw HealthManagerError.dataTypeUnavailable(rawValue)
            }
            return type
        case .bloodPressure:
            guard let type = HKObjectType.correlationType(forIdentifier: .bloodPressure) else {
                throw HealthManagerError.dataTypeUnavailable(rawValue)
            }
            return type
        case .mindfulness:
            guard let type = HKObjectType.categoryType(forIdentifier: .mindfulSession) else {
                throw HealthManagerError.dataTypeUnavailable(rawValue)
            }
            return type
        default:
            return try quantityType()
        }
    }

    func quantityType() throws -> HKQuantityType {
        let identifier: HKQuantityTypeIdentifier
        switch self {
        case .steps:
            identifier = .stepCount
        case .distance:
            identifier = .distanceWalkingRunning
        case .calories:
            identifier = .activeEnergyBurned
        case .heartRate:
            identifier = .heartRate
        case .weight:
            identifier = .bodyMass
        case .respiratoryRate:
            identifier = .respiratoryRate
        case .oxygenSaturation:
            identifier = .oxygenSaturation
        case .restingHeartRate:
            identifier = .restingHeartRate
        case .heartRateVariability:
            identifier = .heartRateVariabilitySDNN
        case .bloodGlucose:
            identifier = .bloodGlucose
        case .bodyTemperature:
            identifier = .bodyTemperature
        case .height:
            identifier = .height
        case .flightsClimbed:
            identifier = .flightsClimbed
        case .exerciseTime:
            identifier = .appleExerciseTime
        case .distanceCycling:
            identifier = .distanceCycling
        case .bodyFat:
            identifier = .bodyFatPercentage
        case .basalBodyTemperature:
            identifier = .basalBodyTemperature
        case .basalCalories:
            identifier = .basalEnergyBurned
        case .totalCalories:
            identifier = .activeEnergyBurned
        case .sleep:
            throw HealthManagerError.invalidDataType("Sleep is a category type, not a quantity type")
        case .bloodPressure:
            throw HealthManagerError.invalidDataType("Blood pressure is a correlation type, not a quantity type")
        case .mindfulness:
            throw HealthManagerError.invalidDataType("Mindfulness is a category type, not a quantity type")
        }

        guard let type = HKObjectType.quantityType(forIdentifier: identifier) else {
            throw HealthManagerError.dataTypeUnavailable(rawValue)
        }
        return type
    }

    var defaultUnit: HKUnit {
        switch self {
        case .steps:
            return HKUnit.count()
        case .distance:
            return HKUnit.meter()
        case .calories:
            return HKUnit.kilocalorie()
        case .heartRate, .restingHeartRate:
            return HKUnit.count().unitDivided(by: HKUnit.minute())
        case .weight:
            return HKUnit.gramUnit(with: .kilo)
        case .respiratoryRate:
            return HKUnit.count().unitDivided(by: HKUnit.minute())
        case .oxygenSaturation:
            return HKUnit.percent()
        case .heartRateVariability:
            return HKUnit.secondUnit(with: .milli)
        case .sleep:
            return HKUnit.minute()
        case .bloodPressure:
            return HKUnit.millimeterOfMercury()
        case .bloodGlucose:
            return HKUnit.gramUnit(with: .milli).unitDivided(by: HKUnit.literUnit(with: .deci))
        case .bodyTemperature, .basalBodyTemperature:
            return HKUnit.degreeCelsius()
        case .height:
            return HKUnit.meterUnit(with: .centi)
        case .flightsClimbed:
            return HKUnit.count()
        case .exerciseTime:
            return HKUnit.minute()
        case .distanceCycling:
            return HKUnit.meter()
        case .bodyFat:
            return HKUnit.percent()
        case .basalCalories:
            return HKUnit.kilocalorie()
        case .totalCalories:
            return HKUnit.kilocalorie()
        case .mindfulness:
            return HKUnit.minute()
        }
    }

    var unitIdentifier: String {
        switch self {
        case .steps:
            return "count"
        case .distance:
            return "meter"
        case .calories:
            return "kilocalorie"
        case .heartRate, .restingHeartRate, .respiratoryRate:
            return "bpm"
        case .weight:
            return "kilogram"
        case .oxygenSaturation:
            return "percent"
        case .heartRateVariability:
            return "millisecond"
        case .sleep:
            return "minute"
        case .bloodPressure:
            return "mmHg"
        case .bloodGlucose:
            return "mg/dL"
        case .bodyTemperature, .basalBodyTemperature:
            return "celsius"
        case .height:
            return "centimeter"
        case .flightsClimbed:
            return "count"
        case .exerciseTime:
            return "minute"
        case .distanceCycling:
            return "meter"
        case .bodyFat:
            return "percent"
        case .basalCalories:
            return "kilocalorie"
        case .totalCalories:
            return "kilocalorie"
        case .mindfulness:
            return "minute"
        }
    }

    static func parseMany(_ identifiers: [String]) throws -> [HealthDataType] {
        try identifiers.map { identifier in
            guard let type = HealthDataType(rawValue: identifier) else {
                throw HealthManagerError.invalidDataType(identifier)
            }
            return type
        }
    }
}

struct AuthorizationStatusPayload {
    let readAuthorized: [HealthDataType]
    let readDenied: [HealthDataType]
    let writeAuthorized: [HealthDataType]
    let writeDenied: [HealthDataType]

    func toDictionary() -> [String: Any] {
        return [
            "readAuthorized": readAuthorized.map { $0.rawValue },
            "readDenied": readDenied.map { $0.rawValue },
            "writeAuthorized": writeAuthorized.map { $0.rawValue },
            "writeDenied": writeDenied.map { $0.rawValue }
        ]
    }
}

final class Health {
    private let healthStore = HKHealthStore()
    private let isoFormatter: ISO8601DateFormatter
    
    /// Small time offset (in seconds) added to the last workout's end date to avoid duplicate results in pagination
    private let paginationOffsetSeconds: TimeInterval = 0.001

    init() {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        isoFormatter = formatter
    }

    func availabilityPayload() -> [String: Any] {
        let available = HKHealthStore.isHealthDataAvailable()
        if available {
            return [
                "available": true,
                "platform": "ios"
            ]
        }

        return [
            "available": false,
            "platform": "ios",
            "reason": "Health data is not available on this device."
        ]
    }

    func requestAuthorization(readIdentifiers: [String], writeIdentifiers: [String], completion: @escaping (Result<AuthorizationStatusPayload, Error>) -> Void) {
        guard HKHealthStore.isHealthDataAvailable() else {
            completion(.failure(HealthManagerError.healthDataUnavailable))
            return
        }

        do {
            // Separate "workouts" from regular health data types
            let (readTypes, includeWorkouts) = try parseTypesWithWorkouts(readIdentifiers)
            let writeTypes = try HealthDataType.parseMany(writeIdentifiers)

            var readObjectTypes = try objectTypes(for: readTypes)
            // Include workout type if explicitly requested
            if includeWorkouts {
                readObjectTypes.insert(HKObjectType.workoutType())
            }
            let writeSampleTypes = try sampleTypes(for: writeTypes)

            healthStore.requestAuthorization(toShare: writeSampleTypes, read: readObjectTypes) { [weak self] success, error in
                guard let self = self else { return }

                if let error = error {
                    completion(.failure(error))
                    return
                }

                if success {
                    self.evaluateAuthorizationStatus(readTypes: readTypes, writeTypes: writeTypes) { result in
                        completion(.success(result))
                    }
                } else {
                    completion(.failure(HealthManagerError.operationFailed("Authorization request was not granted.")))
                }
            }
        } catch {
            completion(.failure(error))
        }
    }

    func checkAuthorization(readIdentifiers: [String], writeIdentifiers: [String], completion: @escaping (Result<AuthorizationStatusPayload, Error>) -> Void) {
        do {
            let (readTypes, _) = try parseTypesWithWorkouts(readIdentifiers)
            let writeTypes = try HealthDataType.parseMany(writeIdentifiers)

            evaluateAuthorizationStatus(readTypes: readTypes, writeTypes: writeTypes) { payload in
                completion(.success(payload))
            }
        } catch {
            completion(.failure(error))
        }
    }

    func readSamples(dataTypeIdentifier: String, startDateString: String?, endDateString: String?, limit: Int?, ascending: Bool, completion: @escaping (Result<[[String: Any]], Error>) -> Void) throws {
        let dataType = try parseDataType(identifier: dataTypeIdentifier)
        let sampleType = try dataType.sampleType()

        let startDate = try parseDate(startDateString, defaultValue: Date().addingTimeInterval(-86400))
        let endDate = try parseDate(endDateString, defaultValue: Date())

        guard endDate >= startDate else {
            throw HealthManagerError.invalidDateRange
        }

        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: [])
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: ascending)
        let queryLimit = limit ?? 100

        // Handle sleep as a category sample
        if dataType == .sleep {
            let query = HKSampleQuery(sampleType: sampleType, predicate: predicate, limit: queryLimit, sortDescriptors: [sortDescriptor]) { [weak self] _, samples, error in
                guard let self = self else { return }

                if let error = error {
                    completion(.failure(error))
                    return
                }

                guard let categorySamples = samples as? [HKCategorySample] else {
                    completion(.success([]))
                    return
                }

                let results = categorySamples.map { sample -> [String: Any] in
                    let sleepValue = sample.value
                    let durationMinutes = sample.endDate.timeIntervalSince(sample.startDate) / 60.0
                    
                    var payload: [String: Any] = [
                        "dataType": dataType.rawValue,
                        "value": durationMinutes,
                        "unit": dataType.unitIdentifier,
                        "startDate": self.isoFormatter.string(from: sample.startDate),
                        "endDate": self.isoFormatter.string(from: sample.endDate)
                    ]
                    
                    // Map HKCategoryValueSleepAnalysis to sleep state
                    let sleepState = self.sleepStateFromValue(sleepValue)
                    if let sleepState = sleepState {
                        payload["sleepState"] = sleepState
                    }

                    let source = sample.sourceRevision.source
                    payload["sourceName"] = source.name
                    payload["sourceId"] = source.bundleIdentifier

                    return payload
                }

                completion(.success(results))
            }
            healthStore.execute(query)
            return
        }
        
        // Handle mindfulness as a category sample
        if dataType == .mindfulness {
            let query = HKSampleQuery(sampleType: sampleType, predicate: predicate, limit: queryLimit, sortDescriptors: [sortDescriptor]) { [weak self] _, samples, error in
                guard let self = self else { return }

                if let error = error {
                    completion(.failure(error))
                    return
                }

                guard let categorySamples = samples as? [HKCategorySample] else {
                    completion(.success([]))
                    return
                }

                let results = categorySamples.map { sample -> [String: Any] in
                    let durationMinutes = sample.endDate.timeIntervalSince(sample.startDate) / 60.0
                    
                    var payload: [String: Any] = [
                        "dataType": dataType.rawValue,
                        "value": durationMinutes,
                        "unit": dataType.unitIdentifier,
                        "startDate": self.isoFormatter.string(from: sample.startDate),
                        "endDate": self.isoFormatter.string(from: sample.endDate)
                    ]

                    let source = sample.sourceRevision.source
                    payload["sourceName"] = source.name
                    payload["sourceId"] = source.bundleIdentifier

                    return payload
                }

                completion(.success(results))
            }
            healthStore.execute(query)
            return
        }
        
        // Handle blood pressure as a correlation sample
        if dataType == .bloodPressure {
            let query = HKSampleQuery(sampleType: sampleType, predicate: predicate, limit: queryLimit, sortDescriptors: [sortDescriptor]) { [weak self] _, samples, error in
                guard let self = self else { return }

                if let error = error {
                    completion(.failure(error))
                    return
                }

                guard let correlations = samples as? [HKCorrelation] else {
                    completion(.success([]))
                    return
                }

                let results = correlations.compactMap { correlation -> [String: Any]? in
                    guard let systolicType = HKObjectType.quantityType(forIdentifier: .bloodPressureSystolic),
                          let diastolicType = HKObjectType.quantityType(forIdentifier: .bloodPressureDiastolic),
                          let systolicSample = correlation.objects(for: systolicType).first as? HKQuantitySample,
                          let diastolicSample = correlation.objects(for: diastolicType).first as? HKQuantitySample else {
                        return nil
                    }
                    
                    let systolicValue = systolicSample.quantity.doubleValue(for: HKUnit.millimeterOfMercury())
                    let diastolicValue = diastolicSample.quantity.doubleValue(for: HKUnit.millimeterOfMercury())
                    
                    var payload: [String: Any] = [
                        "dataType": dataType.rawValue,
                        "value": systolicValue,
                        "unit": dataType.unitIdentifier,
                        "startDate": self.isoFormatter.string(from: correlation.startDate),
                        "endDate": self.isoFormatter.string(from: correlation.endDate),
                        "systolic": systolicValue,
                        "diastolic": diastolicValue
                    ]

                    let source = correlation.sourceRevision.source
                    payload["sourceName"] = source.name
                    payload["sourceId"] = source.bundleIdentifier

                    return payload
                }

                completion(.success(results))
            }
            healthStore.execute(query)
            return
        }

        // Handle quantity samples
        let query = HKSampleQuery(sampleType: sampleType, predicate: predicate, limit: queryLimit, sortDescriptors: [sortDescriptor]) { [weak self] _, samples, error in
            guard let self = self else { return }

            if let error = error {
                completion(.failure(error))
                return
            }

            guard let quantitySamples = samples as? [HKQuantitySample] else {
                completion(.success([]))
                return
            }

            let results = quantitySamples.map { sample -> [String: Any] in
                let value = sample.quantity.doubleValue(for: dataType.defaultUnit)
                var payload: [String: Any] = [
                    "dataType": dataType.rawValue,
                    "value": value,
                    "unit": dataType.unitIdentifier,
                    "startDate": self.isoFormatter.string(from: sample.startDate),
                    "endDate": self.isoFormatter.string(from: sample.endDate)
                ]

                let source = sample.sourceRevision.source
                payload["sourceName"] = source.name
                payload["sourceId"] = source.bundleIdentifier

                return payload
            }

            completion(.success(results))
        }

        healthStore.execute(query)
    }
    
    private func sleepStateFromValue(_ value: Int) -> String? {
        switch value {
        case HKCategoryValueSleepAnalysis.inBed.rawValue:
            return "inBed"
        case HKCategoryValueSleepAnalysis.asleep.rawValue:
            return "asleep"
        case HKCategoryValueSleepAnalysis.awake.rawValue:
            return "awake"
        default:
            // Handle iOS 16+ sleep states
            if #available(iOS 16.0, *) {
                switch value {
                case HKCategoryValueSleepAnalysis.asleepUnspecified.rawValue:
                    return "asleep"
                case HKCategoryValueSleepAnalysis.asleepCore.rawValue:
                    return "light"
                case HKCategoryValueSleepAnalysis.asleepDeep.rawValue:
                    return "deep"
                case HKCategoryValueSleepAnalysis.asleepREM.rawValue:
                    return "rem"
                default:
                    return nil
                }
            }
            return nil
        }
    }

    func saveSample(dataTypeIdentifier: String, value: Double, unitIdentifier: String?, startDateString: String?, endDateString: String?, metadata: [String: String]?, systolic: Double?, diastolic: Double?, completion: @escaping (Result<Void, Error>) -> Void) throws {
        guard HKHealthStore.isHealthDataAvailable() else {
            throw HealthManagerError.healthDataUnavailable
        }

        let dataType = try parseDataType(identifier: dataTypeIdentifier)

        let startDate = try parseDate(startDateString, defaultValue: Date())
        let endDate = try parseDate(endDateString, defaultValue: startDate)

        guard endDate >= startDate else {
            throw HealthManagerError.invalidDateRange
        }

        var metadataDictionary: [String: Any]?
        if let metadata = metadata, !metadata.isEmpty {
            metadataDictionary = metadata.reduce(into: [String: Any]()) { result, entry in
                result[entry.key] = entry.value
            }
        }

        // Handle sleep as a category sample
        if dataType == .sleep {
            guard let categoryType = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) else {
                throw HealthManagerError.dataTypeUnavailable(dataTypeIdentifier)
            }
            // For sleep, the value parameter should represent a sleep state value from HKCategoryValueSleepAnalysis
            // If value is 0 or not specified, default to asleep (HKCategoryValueSleepAnalysis.asleep.rawValue)
            let sleepValue = Int(value) == 0 ? HKCategoryValueSleepAnalysis.asleep.rawValue : Int(value)
            let sample = HKCategorySample(type: categoryType, value: sleepValue, start: startDate, end: endDate, metadata: metadataDictionary)
            
            healthStore.save(sample) { success, error in
                if let error = error {
                    completion(.failure(error))
                    return
                }
                
                if success {
                    completion(.success(()))
                } else {
                    completion(.failure(HealthManagerError.operationFailed("Failed to save the sample.")))
                }
            }
            return
        }
        
        // Handle mindfulness as a category sample
        if dataType == .mindfulness {
            guard let categoryType = HKObjectType.categoryType(forIdentifier: .mindfulSession) else {
                throw HealthManagerError.dataTypeUnavailable(dataTypeIdentifier)
            }
            let sample = HKCategorySample(type: categoryType, value: 0, start: startDate, end: endDate, metadata: metadataDictionary)
            
            healthStore.save(sample) { success, error in
                if let error = error {
                    completion(.failure(error))
                    return
                }
                
                if success {
                    completion(.success(()))
                } else {
                    completion(.failure(HealthManagerError.operationFailed("Failed to save the sample.")))
                }
            }
            return
        }
        
        // Handle blood pressure as a correlation sample
        if dataType == .bloodPressure {
            guard let systolicValue = systolic, let diastolicValue = diastolic else {
                throw HealthManagerError.operationFailed("Blood pressure requires both systolic and diastolic values")
            }
            
            guard let systolicType = HKObjectType.quantityType(forIdentifier: .bloodPressureSystolic),
                  let diastolicType = HKObjectType.quantityType(forIdentifier: .bloodPressureDiastolic),
                  let correlationType = HKObjectType.correlationType(forIdentifier: .bloodPressure) else {
                throw HealthManagerError.dataTypeUnavailable(dataTypeIdentifier)
            }
            
            let systolicQuantity = HKQuantity(unit: HKUnit.millimeterOfMercury(), doubleValue: systolicValue)
            let diastolicQuantity = HKQuantity(unit: HKUnit.millimeterOfMercury(), doubleValue: diastolicValue)
            
            let systolicSample = HKQuantitySample(type: systolicType, quantity: systolicQuantity, start: startDate, end: endDate)
            let diastolicSample = HKQuantitySample(type: diastolicType, quantity: diastolicQuantity, start: startDate, end: endDate)
            
            let correlation = HKCorrelation(type: correlationType, start: startDate, end: endDate, objects: [systolicSample, diastolicSample], metadata: metadataDictionary)
            
            healthStore.save(correlation) { success, error in
                if let error = error {
                    completion(.failure(error))
                    return
                }
                
                if success {
                    completion(.success(()))
                } else {
                    completion(.failure(HealthManagerError.operationFailed("Failed to save the sample.")))
                }
            }
            return
        }

        // Handle quantity samples
        let sampleType = try dataType.quantityType()
        let unit = unit(for: unitIdentifier, dataType: dataType)
        let quantity = HKQuantity(unit: unit, doubleValue: value)
        let sample = HKQuantitySample(type: sampleType, quantity: quantity, start: startDate, end: endDate, metadata: metadataDictionary)

        healthStore.save(sample) { success, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            if success {
                completion(.success(()))
            } else {
                completion(.failure(HealthManagerError.operationFailed("Failed to save the sample.")))
            }
        }
    }

    private func evaluateAuthorizationStatus(readTypes: [HealthDataType], writeTypes: [HealthDataType], completion: @escaping (AuthorizationStatusPayload) -> Void) {
        let writeStatus = writeAuthorizationStatus(for: writeTypes)

        readAuthorizationStatus(for: readTypes) { readAuthorized, readDenied in
            let payload = AuthorizationStatusPayload(
                readAuthorized: readAuthorized,
                readDenied: readDenied,
                writeAuthorized: writeStatus.authorized,
                writeDenied: writeStatus.denied
            )
            completion(payload)
        }
    }

    private func writeAuthorizationStatus(for types: [HealthDataType]) -> (authorized: [HealthDataType], denied: [HealthDataType]) {
        var authorized: [HealthDataType] = []
        var denied: [HealthDataType] = []

        for type in types {
            guard let sampleType = try? type.sampleType() else {
                denied.append(type)
                continue
            }

            switch healthStore.authorizationStatus(for: sampleType) {
            case .sharingAuthorized:
                authorized.append(type)
            case .sharingDenied, .notDetermined:
                denied.append(type)
            @unknown default:
                denied.append(type)
            }
        }

        return (authorized, denied)
    }

    private func readAuthorizationStatus(for types: [HealthDataType], completion: @escaping ([HealthDataType], [HealthDataType]) -> Void) {
        guard !types.isEmpty else {
            completion([], [])
            return
        }

        let group = DispatchGroup()
        let lock = NSLock()
        var authorized: [HealthDataType] = []
        var denied: [HealthDataType] = []

        for type in types {
            guard let objectType = try? type.sampleType() else {
                denied.append(type)
                continue
            }

            group.enter()
            let readSet = Set<HKObjectType>([objectType])
            healthStore.getRequestStatusForAuthorization(toShare: Set<HKSampleType>(), read: readSet) { status, error in
                defer { group.leave() }

                if error != nil {
                    lock.lock(); denied.append(type); lock.unlock()
                    return
                }

                switch status {
                case .unnecessary:
                    lock.lock(); authorized.append(type); lock.unlock()
                case .shouldRequest, .unknown:
                    lock.lock(); denied.append(type); lock.unlock()
                @unknown default:
                    lock.lock(); denied.append(type); lock.unlock()
                }
            }
        }

        group.notify(queue: .main) {
            completion(authorized, denied)
        }
    }

    private func parseDataType(identifier: String) throws -> HealthDataType {
        guard let type = HealthDataType(rawValue: identifier) else {
            throw HealthManagerError.invalidDataType(identifier)
        }
        return type
    }

    private func parseTypesWithWorkouts(_ identifiers: [String]) throws -> ([HealthDataType], Bool) {
        var types: [HealthDataType] = []
        var includeWorkouts = false
        
        for identifier in identifiers {
            if identifier == "workouts" {
                includeWorkouts = true
            } else {
                guard let type = HealthDataType(rawValue: identifier) else {
                    throw HealthManagerError.invalidDataType(identifier)
                }
                types.append(type)
            }
        }
        
        return (types, includeWorkouts)
    }

    private func parseDate(_ string: String?, defaultValue: Date) throws -> Date {
        guard let value = string else {
            return defaultValue
        }

        if let date = isoFormatter.date(from: value) {
            return date
        }

        throw HealthManagerError.invalidDate(value)
    }

    private func unit(for identifier: String?, dataType: HealthDataType) -> HKUnit {
        guard let identifier = identifier else {
            return dataType.defaultUnit
        }

        switch identifier {
        case "count":
            return HKUnit.count()
        case "meter":
            return HKUnit.meter()
        case "kilocalorie":
            return HKUnit.kilocalorie()
        case "bpm":
            return HKUnit.count().unitDivided(by: HKUnit.minute())
        case "kilogram":
            return HKUnit.gramUnit(with: .kilo)
        case "percent":
            return HKUnit.percent()
        case "millisecond":
            return HKUnit.secondUnit(with: .milli)
        case "minute":
            return HKUnit.minute()
        default:
            return dataType.defaultUnit
        }
    }

    private func objectTypes(for dataTypes: [HealthDataType]) throws -> Set<HKObjectType> {
        var set = Set<HKObjectType>()
        for dataType in dataTypes {
            let type = try dataType.sampleType()
            set.insert(type)
        }
        return set
    }

    private func sampleTypes(for dataTypes: [HealthDataType]) throws -> Set<HKSampleType> {
        var set = Set<HKSampleType>()
        for dataType in dataTypes {
            let type = try dataType.sampleType()
            set.insert(type)
        }
        return set
    }

    func queryAggregated(dataTypeIdentifier: String, startDateString: String?, endDateString: String?, bucketString: String?, aggregationString: String?, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        do {
            let dataType = try parseDataType(identifier: dataTypeIdentifier)
            
            // Sleep aggregation is not supported as it's categorical data
            if dataType == .sleep {
                completion(.failure(HealthManagerError.operationFailed("Aggregated queries are not supported for sleep data. Use readSamples instead.")))
                return
            }
            
            // Instantaneous measurement types don't support meaningful aggregation
            // These should use readSamples instead
            if dataType == .respiratoryRate || dataType == .oxygenSaturation || dataType == .heartRateVariability {
                completion(.failure(HealthManagerError.operationFailed("Aggregated queries are not supported for \(dataType.rawValue). Use readSamples instead.")))
                return
            }
            
            let quantityType = try dataType.quantityType()
            
            let startDate = try parseDate(startDateString, defaultValue: Date().addingTimeInterval(-86400))
            let endDate = try parseDate(endDateString, defaultValue: Date())
            
            guard endDate >= startDate else {
                completion(.failure(HealthManagerError.invalidDateRange))
                return
            }
            
            // Default bucket is "day" and default aggregation is "sum" (consistent with TypeScript defaults)
            let bucket = bucketString ?? "day"
            let aggregation = aggregationString ?? "sum"
            
            // Determine the anchor date and interval based on bucket type
            var anchorComponents = Calendar.current.dateComponents([.year, .month, .day], from: startDate)
            var intervalComponents = DateComponents()
            
            switch bucket {
            case "hour":
                anchorComponents.hour = 0
                intervalComponents.hour = 1
            case "day":
                intervalComponents.day = 1
            case "week":
                intervalComponents.day = 7
            case "month":
                // Note: Using 30 days as an approximation. This may not align exactly with calendar months
                // but provides consistent bucket sizes. For calendar-month accuracy, consider using
                // readSamples with appropriate date ranges instead.
                intervalComponents.day = 30
            default:
                intervalComponents.day = 1
            }
            
            guard let anchor = Calendar.current.date(from: anchorComponents) else {
                completion(.failure(HealthManagerError.operationFailed("Failed to create anchor date")))
                return
            }
            
            // Determine the statistics options based on aggregation type
            var options: HKStatisticsOptions = []
            switch aggregation {
            case "sum":
                options = .cumulativeSum
            case "average":
                options = .discreteAverage
            case "min":
                options = .discreteMin
            case "max":
                options = .discreteMax
            default:
                options = .cumulativeSum
            }
            
            let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
            
            let query = HKStatisticsCollectionQuery(
                quantityType: quantityType,
                quantitySamplePredicate: predicate,
                options: options,
                anchorDate: anchor,
                intervalComponents: intervalComponents
            )
            
            query.initialResultsHandler = { [weak self] _, collection, error in
                guard let self = self else { return }
                
                if let error = error {
                    completion(.failure(error))
                    return
                }
                
                guard let collection = collection else {
                    completion(.success(["samples": []]))
                    return
                }
                
                var samples: [[String: Any]] = []
                
                collection.enumerateStatistics(from: startDate, to: endDate) { statistics, _ in
                    var value: Double?
                    
                    switch aggregation {
                    case "sum":
                        value = statistics.sumQuantity()?.doubleValue(for: dataType.defaultUnit)
                    case "average":
                        value = statistics.averageQuantity()?.doubleValue(for: dataType.defaultUnit)
                    case "min":
                        value = statistics.minimumQuantity()?.doubleValue(for: dataType.defaultUnit)
                    case "max":
                        value = statistics.maximumQuantity()?.doubleValue(for: dataType.defaultUnit)
                    default:
                        value = statistics.sumQuantity()?.doubleValue(for: dataType.defaultUnit)
                    }
                    
                    if let value = value {
                        let sample: [String: Any] = [
                            "startDate": self.isoFormatter.string(from: statistics.startDate),
                            "endDate": self.isoFormatter.string(from: statistics.endDate),
                            "value": value,
                            "unit": dataType.unitIdentifier
                        ]
                        samples.append(sample)
                    }
                }
                
                completion(.success(["samples": samples]))
            }
            
            healthStore.execute(query)
        } catch {
            completion(.failure(error))
        }
    }

    func queryWorkouts(workoutTypeString: String?, startDateString: String?, endDateString: String?, limit: Int?, ascending: Bool, anchorString: String?, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        let startDate = (try? parseDate(startDateString, defaultValue: Date().addingTimeInterval(-86400))) ?? Date().addingTimeInterval(-86400)
        let endDate = (try? parseDate(endDateString, defaultValue: Date())) ?? Date()

        guard endDate >= startDate else {
            completion(.failure(HealthManagerError.invalidDateRange))
            return
        }

        // If anchor is provided, use it as the continuation point for pagination.
        // The anchor is the ISO 8601 date string of the last workout's end date from the previous query.
        let effectiveStartDate: Date
        if let anchorString = anchorString, let anchorDate = try? parseDate(anchorString, defaultValue: startDate) {
            effectiveStartDate = anchorDate
        } else {
            effectiveStartDate = startDate
        }

        var predicate = HKQuery.predicateForSamples(withStart: effectiveStartDate, end: endDate, options: [])

        // Filter by workout type if specified
        if let workoutTypeString = workoutTypeString, let workoutType = WorkoutType(rawValue: workoutTypeString) {
            let hkWorkoutType = workoutType.hkWorkoutActivityType()
            let typePredicate = HKQuery.predicateForWorkouts(with: hkWorkoutType)
            predicate = NSCompoundPredicate(andPredicateWithSubpredicates: [predicate, typePredicate])
        }

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: ascending)
        let queryLimit = limit ?? 100

        let workoutSampleType = HKObjectType.workoutType()

        let query = HKSampleQuery(sampleType: workoutSampleType, predicate: predicate, limit: queryLimit, sortDescriptors: [sortDescriptor]) { [weak self] _, samples, error in
            guard let self = self else { return }

            if let error = error {
                completion(.failure(error))
                return
            }

            guard let workouts = samples as? [HKWorkout] else {
                completion(.success(["workouts": []]))
                return
            }

            let results = workouts.map { workout -> [String: Any] in
                var payload: [String: Any] = [
                    "workoutType": WorkoutType.fromHKWorkoutActivityType(workout.workoutActivityType).rawValue,
                    "duration": Int(workout.duration),
                    "startDate": self.isoFormatter.string(from: workout.startDate),
                    "endDate": self.isoFormatter.string(from: workout.endDate)
                ]

                // Add total energy burned if available
                if let totalEnergyBurned = workout.totalEnergyBurned {
                    let energyInKilocalories = totalEnergyBurned.doubleValue(for: HKUnit.kilocalorie())
                    payload["totalEnergyBurned"] = energyInKilocalories
                }

                // Add total distance if available
                if let totalDistance = workout.totalDistance {
                    let distanceInMeters = totalDistance.doubleValue(for: HKUnit.meter())
                    payload["totalDistance"] = distanceInMeters
                }

                // Add source information
                let source = workout.sourceRevision.source
                payload["sourceName"] = source.name
                payload["sourceId"] = source.bundleIdentifier

                // Add metadata if available
                if let metadata = workout.metadata, !metadata.isEmpty {
                    var metadataDict: [String: String] = [:]
                    for (key, value) in metadata {
                        if let stringValue = value as? String {
                            metadataDict[key] = stringValue
                        } else if let numberValue = value as? NSNumber {
                            metadataDict[key] = numberValue.stringValue
                        }
                    }
                    if !metadataDict.isEmpty {
                        payload["metadata"] = metadataDict
                    }
                }

                return payload
            }

            // Generate next anchor if we have results and reached the limit
            var response: [String: Any] = ["workouts": results]
            if !workouts.isEmpty && workouts.count >= queryLimit {
                // Use the last workout's end date as the anchor for the next page
                let lastWorkout = workouts.last!
                // Add a small offset to avoid getting the same workout again
                let nextAnchorDate = lastWorkout.endDate.addingTimeInterval(self.paginationOffsetSeconds)
                response["anchor"] = self.isoFormatter.string(from: nextAnchorDate)
            }

            completion(.success(response))
        }

        healthStore.execute(query)
    }
}
