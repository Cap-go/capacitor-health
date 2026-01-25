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
    // Common types (supported on both platforms)
    case americanFootball
    case australianFootball
    case badminton
    case baseball
    case basketball
    case bowling
    case boxing
    case climbing
    case cricket
    case crossTraining
    case curling
    case cycling
    case dance
    case elliptical
    case fencing
    case functionalStrengthTraining
    case golf
    case gymnastics
    case handball
    case hiking
    case hockey
    case jumpRope
    case kickboxing
    case lacrosse
    case martialArts
    case pilates
    case racquetball
    case rowing
    case rugby
    case running
    case sailing
    case skatingSports
    case skiing
    case snowboarding
    case soccer
    case softball
    case squash
    case stairClimbing
    case strengthTraining
    case surfing
    case swimming
    case swimmingPool
    case swimmingOpenWater
    case tableTennis
    case tennis
    case trackAndField
    case traditionalStrengthTraining
    case volleyball
    case walking
    case waterFitness
    case waterPolo
    case waterSports
    case weightlifting
    case wheelchair
    case yoga
    // iOS specific types
    case archery
    case barre
    case cooldown
    case coreTraining
    case crossCountrySkiing
    case discSports
    case downhillSkiing
    case equestrianSports
    case fishing
    case fitnessGaming
    case flexibility
    case handCycling
    case highIntensityIntervalTraining
    case hunting
    case mindAndBody
    case mixedCardio
    case paddleSports
    case pickleball
    case play
    case preparationAndRecovery
    case snowSports
    case stepTraining
    case surfingSports
    case taiChi
    case transition
    case wheelchairRunPace
    case wheelchairWalkPace
    case wrestling
    // Android specific types (map to other on iOS)
    case backExtension
    case barbellShoulderPress
    case benchPress
    case benchSitUp
    case bikingStationary
    case bootCamp
    case burpee
    case calisthenics
    case crunch
    case dancing
    case deadlift
    case dumbbellCurlLeftArm
    case dumbbellCurlRightArm
    case dumbbellFrontRaise
    case dumbbellLateralRaise
    case dumbbellTricepsExtensionLeftArm
    case dumbbellTricepsExtensionRightArm
    case dumbbellTricepsExtensionTwoArm
    case exerciseClass
    case forwardTwist
    case frisbeedisc
    case guidedBreathing
    case iceHockey
    case iceSkating
    case jumpingJack
    case latPullDown
    case lunge
    case meditation
    case paddling
    case paraGliding
    case plank
    case rockClimbing
    case rollerHockey
    case rowingMachine
    case runningTreadmill
    case scubaDiving
    case skating
    case snowshoeing
    case stairClimbingMachine
    case stretching
    case upperTwist
    case other

    func hkWorkoutActivityType() -> HKWorkoutActivityType {
        switch self {
        case .americanFootball:
            return .americanFootball
        case .archery:
            return .archery
        case .australianFootball:
            return .australianFootball
        case .badminton:
            return .badminton
        case .barre:
            return .barre
        case .baseball:
            return .baseball
        case .basketball:
            return .basketball
        case .bowling:
            return .bowling
        case .boxing:
            return .boxing
        case .climbing:
            return .climbing
        case .cooldown:
            return .cooldown
        case .coreTraining:
            return .coreTraining
        case .cricket:
            return .cricket
        case .crossCountrySkiing:
            return .crossCountrySkiing
        case .crossTraining:
            return .crossTraining
        case .curling:
            return .curling
        case .cycling:
            return .cycling
        case .dance:
            return .dance
        case .discSports:
            return .discSports
        case .downhillSkiing:
            return .downhillSkiing
        case .elliptical:
            return .elliptical
        case .equestrianSports:
            return .equestrianSports
        case .fencing:
            return .fencing
        case .fishing:
            return .fishing
        case .fitnessGaming:
            return .fitnessGaming
        case .flexibility:
            return .flexibility
        case .functionalStrengthTraining:
            return .functionalStrengthTraining
        case .golf:
            return .golf
        case .gymnastics:
            return .gymnastics
        case .handball:
            return .handball
        case .handCycling:
            return .handCycling
        case .highIntensityIntervalTraining:
            return .highIntensityIntervalTraining
        case .hiking:
            return .hiking
        case .hockey:
            return .hockey
        case .hunting:
            return .hunting
        case .jumpRope:
            return .jumpRope
        case .kickboxing:
            return .kickboxing
        case .lacrosse:
            return .lacrosse
        case .martialArts:
            return .martialArts
        case .mindAndBody:
            return .mindAndBody
        case .mixedCardio:
            return .mixedCardio
        case .paddleSports:
            return .paddleSports
        case .pickleball:
            return .pickleball
        case .pilates:
            return .pilates
        case .play:
            return .play
        case .preparationAndRecovery:
            return .preparationAndRecovery
        case .racquetball:
            return .racquetball
        case .rowing:
            return .rowing
        case .rugby:
            return .rugby
        case .running:
            return .running
        case .sailing:
            return .sailing
        case .skatingSports:
            return .skatingSports
        case .skiing:
            // iOS doesn't have a generic 'skiing' type, map to downhillSkiing as most common
            return .downhillSkiing
        case .snowboarding:
            return .snowboarding
        case .snowSports:
            return .snowSports
        case .soccer:
            return .soccer
        case .softball:
            return .softball
        case .squash:
            return .squash
        case .stairClimbing:
            return .stairClimbing
        case .stepTraining:
            return .stepTraining
        case .strengthTraining:
            return .traditionalStrengthTraining
        case .surfing:
            // Generic 'surfing' type for cross-platform compatibility
            return .surfingSports
        case .surfingSports:
            // iOS-specific surfing type
            return .surfingSports
        case .swimming:
            return .swimming
        case .swimmingPool:
            return .swimming
        case .swimmingOpenWater:
            return .swimmingOpenWater
        case .tableTennis:
            return .tableTennis
        case .taiChi:
            return .taiChi
        case .tennis:
            return .tennis
        case .trackAndField:
            return .trackAndField
        case .traditionalStrengthTraining:
            return .traditionalStrengthTraining
        case .transition:
            return .transition
        case .volleyball:
            return .volleyball
        case .walking:
            return .walking
        case .waterFitness:
            return .waterFitness
        case .waterPolo:
            return .waterPolo
        case .waterSports:
            return .waterSports
        case .weightlifting:
            // iOS doesn't have a specific weightlifting type, map to functionalStrengthTraining
            return .functionalStrengthTraining
        case .wheelchair:
            return .wheelchair
        case .wheelchairRunPace:
            return .wheelchairRunPace
        case .wheelchairWalkPace:
            return .wheelchairWalkPace
        case .wrestling:
            return .wrestling
        case .yoga:
            return .yoga
        // Android-specific types that don't have direct iOS equivalents
        // Map to .other since iOS doesn't support these granular exercise types
        case .backExtension, .barbellShoulderPress, .benchPress, .benchSitUp,
             .bikingStationary, .bootCamp, .burpee, .calisthenics, .crunch,
             .dancing, .deadlift, .dumbbellCurlLeftArm, .dumbbellCurlRightArm,
             .dumbbellFrontRaise, .dumbbellLateralRaise, .dumbbellTricepsExtensionLeftArm,
             .dumbbellTricepsExtensionRightArm, .dumbbellTricepsExtensionTwoArm,
             .exerciseClass, .forwardTwist, .frisbeedisc, .guidedBreathing,
             .iceHockey, .iceSkating, .jumpingJack, .latPullDown, .lunge,
             .meditation, .paddling, .paraGliding, .plank, .rockClimbing,
             .rollerHockey, .rowingMachine, .runningTreadmill, .scubaDiving,
             .skating, .snowshoeing, .stairClimbingMachine, .stretching, .upperTwist:
            return .other
        case .other:
            return .other
        }
    }

    static func fromHKWorkoutActivityType(_ hkType: HKWorkoutActivityType) -> WorkoutType {
        switch hkType {
        case .americanFootball:
            return .americanFootball
        case .archery:
            return .archery
        case .australianFootball:
            return .australianFootball
        case .badminton:
            return .badminton
        case .barre:
            return .barre
        case .baseball:
            return .baseball
        case .basketball:
            return .basketball
        case .bowling:
            return .bowling
        case .boxing:
            return .boxing
        case .climbing:
            return .climbing
        case .cooldown:
            return .cooldown
        case .coreTraining:
            return .coreTraining
        case .cricket:
            return .cricket
        case .crossCountrySkiing:
            return .crossCountrySkiing
        case .crossTraining:
            return .crossTraining
        case .curling:
            return .curling
        case .cycling:
            return .cycling
        case .dance:
            return .dance
        case .discSports:
            return .discSports
        case .downhillSkiing:
            return .downhillSkiing
        case .elliptical:
            return .elliptical
        case .equestrianSports:
            return .equestrianSports
        case .fencing:
            return .fencing
        case .fishing:
            return .fishing
        case .fitnessGaming:
            return .fitnessGaming
        case .flexibility:
            return .flexibility
        case .functionalStrengthTraining:
            return .functionalStrengthTraining
        case .golf:
            return .golf
        case .gymnastics:
            return .gymnastics
        case .handball:
            return .handball
        case .handCycling:
            return .handCycling
        case .highIntensityIntervalTraining:
            return .highIntensityIntervalTraining
        case .hiking:
            return .hiking
        case .hockey:
            return .hockey
        case .hunting:
            return .hunting
        case .jumpRope:
            return .jumpRope
        case .kickboxing:
            return .kickboxing
        case .lacrosse:
            return .lacrosse
        case .martialArts:
            return .martialArts
        case .mindAndBody:
            return .mindAndBody
        case .mixedCardio:
            return .mixedCardio
        case .paddleSports:
            return .paddleSports
        case .pickleball:
            return .pickleball
        case .pilates:
            return .pilates
        case .play:
            return .play
        case .preparationAndRecovery:
            return .preparationAndRecovery
        case .racquetball:
            return .racquetball
        case .rowing:
            return .rowing
        case .rugby:
            return .rugby
        case .running:
            return .running
        case .sailing:
            return .sailing
        case .skatingSports:
            return .skatingSports
        case .snowboarding:
            return .snowboarding
        case .snowSports:
            return .snowSports
        case .soccer:
            return .soccer
        case .softball:
            return .softball
        case .squash:
            return .squash
        case .stairClimbing:
            return .stairClimbing
        case .stepTraining:
            return .stepTraining
        case .surfingSports:
            return .surfingSports
        case .swimming:
            return .swimming
        case .swimmingOpenWater:
            return .swimmingOpenWater
        case .tableTennis:
            return .tableTennis
        case .taiChi:
            return .taiChi
        case .tennis:
            return .tennis
        case .trackAndField:
            return .trackAndField
        case .traditionalStrengthTraining:
            // Both strengthTraining and traditionalStrengthTraining map to the same HK type,
            // but we prefer the more explicit 'traditionalStrengthTraining' when reading from iOS
            return .traditionalStrengthTraining
        case .transition:
            return .transition
        case .volleyball:
            return .volleyball
        case .walking:
            return .walking
        case .waterFitness:
            return .waterFitness
        case .waterPolo:
            return .waterPolo
        case .waterSports:
            return .waterSports
        case .wheelchair:
            return .wheelchair
        case .wheelchairRunPace:
            return .wheelchairRunPace
        case .wheelchairWalkPace:
            return .wheelchairWalkPace
        case .wrestling:
            return .wrestling
        case .yoga:
            return .yoga
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

    func sampleType() throws -> HKQuantityType {
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
        case .heartRate:
            return HKUnit.count().unitDivided(by: HKUnit.minute())
        case .weight:
            return HKUnit.gramUnit(with: .kilo)
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
        case .heartRate:
            return "bpm"
        case .weight:
            return "kilogram"
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

    func saveSample(dataTypeIdentifier: String, value: Double, unitIdentifier: String?, startDateString: String?, endDateString: String?, metadata: [String: String]?, completion: @escaping (Result<Void, Error>) -> Void) throws {
        guard HKHealthStore.isHealthDataAvailable() else {
            throw HealthManagerError.healthDataUnavailable
        }

        let dataType = try parseDataType(identifier: dataTypeIdentifier)
        let sampleType = try dataType.sampleType()

        let startDate = try parseDate(startDateString, defaultValue: Date())
        let endDate = try parseDate(endDateString, defaultValue: startDate)

        guard endDate >= startDate else {
            throw HealthManagerError.invalidDateRange
        }

        let unit = unit(for: unitIdentifier, dataType: dataType)
        let quantity = HKQuantity(unit: unit, doubleValue: value)

        var metadataDictionary: [String: Any]?
        if let metadata = metadata, !metadata.isEmpty {
            metadataDictionary = metadata.reduce(into: [String: Any]()) { result, entry in
                result[entry.key] = entry.value
            }
        }

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
            let type = try dataType.sampleType() as HKSampleType
            set.insert(type)
        }
        return set
    }

    func queryWorkouts(workoutTypeString: String?, startDateString: String?, endDateString: String?, limit: Int?, ascending: Bool, anchorString: String?, completion: @escaping (Result<[String: Any], Error>) -> Void) {
        let startDate = (try? parseDate(startDateString, defaultValue: Date().addingTimeInterval(-86400))) ?? Date().addingTimeInterval(-86400)
        let endDate = (try? parseDate(endDateString, defaultValue: Date())) ?? Date()

        guard endDate >= startDate else {
            completion(.failure(HealthManagerError.invalidDateRange))
            return
        }

        var predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: [])

        // Filter by workout type if specified
        if let workoutTypeString = workoutTypeString, let workoutType = WorkoutType(rawValue: workoutTypeString) {
            let hkWorkoutType = workoutType.hkWorkoutActivityType()
            let typePredicate = HKQuery.predicateForWorkouts(with: hkWorkoutType)
            predicate = NSCompoundPredicate(andPredicateWithSubpredicates: [predicate, typePredicate])
        }

        guard let workoutSampleType = HKObjectType.workoutType() as? HKSampleType else {
            completion(.failure(HealthManagerError.operationFailed("Workout type is not available.")))
            return
        }

        // Decode anchor if provided
        var anchor: HKQueryAnchor? = nil
        if let anchorString = anchorString, let anchorData = Data(base64Encoded: anchorString) {
            anchor = try? NSKeyedUnarchiver.unarchivedObject(ofClass: HKQueryAnchor.self, from: anchorData)
        }
        
        // Use HKAnchoredObjectQuery for efficient pagination
        // Default to 100 workouts if no limit specified (matches Android and documentation)
        let queryLimit = limit ?? 100
        
        let anchoredQuery = HKAnchoredObjectQuery(
            type: workoutSampleType,
            predicate: predicate,
            anchor: anchor,
            limit: queryLimit
        ) { [weak self] _, samplesOrNil, deletedObjectsOrNil, newAnchor, error in
            guard let self = self else { return }

            if let error = error {
                completion(.failure(error))
                return
            }

            guard let samples = samplesOrNil as? [HKWorkout] else {
                // Return empty results with new anchor
                var result: [String: Any] = ["workouts": []]
                if let newAnchor = newAnchor,
                   let anchorData = try? NSKeyedArchiver.archivedData(withRootObject: newAnchor, requiringSecureCoding: true) {
                    result["anchor"] = anchorData.base64EncodedString()
                }
                completion(.success(result))
                return
            }

            // Note: We don't sort results from HKAnchoredObjectQuery to preserve anchor-based
            // pagination consistency. Sorting could cause the same workout to appear in multiple
            // result sets across paginated queries.
            let workoutPayloads = samples.map { workout -> [String: Any] in
                self.workoutToPayload(workout)
            }

            var result: [String: Any] = ["workouts": workoutPayloads]
            
            // Encode and return the new anchor for pagination
            if let newAnchor = newAnchor,
               let anchorData = try? NSKeyedArchiver.archivedData(withRootObject: newAnchor, requiringSecureCoding: true) {
                result["anchor"] = anchorData.base64EncodedString()
            }

            completion(.success(result))
        }

        healthStore.execute(anchoredQuery)
    }
    
    private func workoutToPayload(_ workout: HKWorkout) -> [String: Any] {
        var payload: [String: Any] = [
            "workoutType": WorkoutType.fromHKWorkoutActivityType(workout.workoutActivityType).rawValue,
            "duration": Int(workout.duration),
            "startDate": isoFormatter.string(from: workout.startDate),
            "endDate": isoFormatter.string(from: workout.endDate)
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
}
