import XCTest
@testable import HealthPlugin

class HealthTests: XCTestCase {
    func testReadSamplePayloadIncludesFiniteValue() throws {
        let implementation = Health()
        let startDate = Date(timeIntervalSince1970: 0)
        let endDate = Date(timeIntervalSince1970: 60)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]

        let payload = implementation.readSamplePayload(
            dataType: .oxygenSaturation,
            value: 0.96,
            startDate: startDate,
            endDate: endDate
        )

        XCTAssertEqual(payload?["dataType"] as? String, "oxygenSaturation")
        XCTAssertEqual(payload?["value"] as? Double, 0.96)
        XCTAssertEqual(payload?["unit"] as? String, "percent")

        let startDateString = try XCTUnwrap(payload?["startDate"] as? String)
        let endDateString = try XCTUnwrap(payload?["endDate"] as? String)
        let parsedStartDate = try XCTUnwrap(formatter.date(from: startDateString))
        let parsedEndDate = try XCTUnwrap(formatter.date(from: endDateString))

        XCTAssertEqual(parsedStartDate.timeIntervalSince1970, startDate.timeIntervalSince1970, accuracy: 0.001)
        XCTAssertEqual(parsedEndDate.timeIntervalSince1970, endDate.timeIntervalSince1970, accuracy: 0.001)
    }

    func testReadSamplePayloadRejectsNaNValue() {
        let implementation = Health()
        let startDate = Date(timeIntervalSince1970: 0)

        let payload = implementation.readSamplePayload(
            dataType: .oxygenSaturation,
            value: .nan,
            startDate: startDate,
            endDate: startDate
        )

        XCTAssertNil(payload)
    }

    func testReadSamplePayloadRejectsInfiniteValue() {
        let implementation = Health()
        let startDate = Date(timeIntervalSince1970: 0)

        let payload = implementation.readSamplePayload(
            dataType: .bloodGlucose,
            value: .infinity,
            startDate: startDate,
            endDate: startDate
        )

        XCTAssertNil(payload)
    }

    func testReadSamplePayloadRejectsNegativeInfiniteValue() {
        let implementation = Health()
        let startDate = Date(timeIntervalSince1970: 0)

        let payload = implementation.readSamplePayload(
            dataType: .bloodGlucose,
            value: -.infinity,
            startDate: startDate,
            endDate: startDate
        )

        XCTAssertNil(payload)
    }

    func testSaveSampleRejectsLeanBodyMassAsUnsupported() {
        let implementation = Health()
        XCTAssertThrowsError(
            try implementation.saveSample(
                dataTypeIdentifier: "leanBodyMass",
                value: 60,
                unitIdentifier: nil,
                startDateString: nil,
                endDateString: nil,
                metadata: nil,
                systolic: nil,
                diastolic: nil
            ) { _ in }
        ) { error in
            guard let healthError = error as? HealthManagerError else {
                return XCTFail("Expected HealthManagerError, got \(error)")
            }
            if case let .invalidDataType(identifier) = healthError {
                XCTAssertEqual(identifier, "leanBodyMass")
            } else {
                XCTFail("Expected invalidDataType, got \(healthError)")
            }
        }
    }

    func testSaveSampleRejectsBoneMassAsUnsupported() {
        let implementation = Health()
        XCTAssertThrowsError(
            try implementation.saveSample(
                dataTypeIdentifier: "boneMass",
                value: 2.8,
                unitIdentifier: nil,
                startDateString: nil,
                endDateString: nil,
                metadata: nil,
                systolic: nil,
                diastolic: nil
            ) { _ in }
        ) { error in
            guard let healthError = error as? HealthManagerError else {
                return XCTFail("Expected HealthManagerError, got \(error)")
            }
            if case let .invalidDataType(identifier) = healthError {
                XCTAssertEqual(identifier, "boneMass")
            } else {
                XCTFail("Expected invalidDataType, got \(healthError)")
            }
        }
    }

    func testReadSamplesRejectsLeanBodyMassAsUnsupported() {
        let implementation = Health()
        XCTAssertThrowsError(
            try implementation.readSamples(
                dataTypeIdentifier: "leanBodyMass",
                startDateString: nil,
                endDateString: nil,
                limit: nil,
                ascending: false
            ) { _ in }
        ) { error in
            guard let healthError = error as? HealthManagerError else {
                return XCTFail("Expected HealthManagerError, got \(error)")
            }
            if case let .invalidDataType(identifier) = healthError {
                XCTAssertEqual(identifier, "leanBodyMass")
            } else {
                XCTFail("Expected invalidDataType, got \(healthError)")
            }
        }
    }

    func testReadSamplesRejectsBoneMassAsUnsupported() {
        let implementation = Health()
        XCTAssertThrowsError(
            try implementation.readSamples(
                dataTypeIdentifier: "boneMass",
                startDateString: nil,
                endDateString: nil,
                limit: nil,
                ascending: false
            ) { _ in }
        ) { error in
            guard let healthError = error as? HealthManagerError else {
                return XCTFail("Expected HealthManagerError, got \(error)")
            }
            if case let .invalidDataType(identifier) = healthError {
                XCTAssertEqual(identifier, "boneMass")
            } else {
                XCTFail("Expected invalidDataType, got \(healthError)")
            }
        }
    }

    func testReadSamplesRejectsBodyWaterMassAsUnsupported() throws {
        let implementation = Health()
        XCTAssertThrowsError(
            try implementation.readSamples(
                dataTypeIdentifier: "bodyWaterMass",
                startDateString: nil,
                endDateString: nil,
                limit: nil,
                ascending: false
            ) { _ in }
        ) { error in
            guard let healthError = error as? HealthManagerError else {
                return XCTFail("Expected HealthManagerError, got \(error)")
            }
            if case let .invalidDataType(identifier) = healthError {
                XCTAssertEqual(identifier, "bodyWaterMass")
            } else {
                XCTFail("Expected invalidDataType, got \(healthError)")
            }
        }
    }
}
