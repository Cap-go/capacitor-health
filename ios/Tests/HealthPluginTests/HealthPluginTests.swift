import XCTest
@testable import HealthPlugin

class HealthTests: XCTestCase {
    func testReadSamplePayloadIncludesFiniteValue() throws {
        let implementation = Health()
        let startDate = Date(timeIntervalSince1970: 0)
        let endDate = Date(timeIntervalSince1970: 60)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]

        let pluginValue = HealthDataType.oxygenSaturation.pluginValue(fromHealthKit: 0.96)
        let payload = implementation.readSamplePayload(
            dataType: .oxygenSaturation,
            value: pluginValue,
            startDate: startDate,
            endDate: endDate
        )

        XCTAssertEqual(payload?["dataType"] as? String, "oxygenSaturation")
        XCTAssertEqual(payload?["value"] as? Double, 96.0)
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

    func testPluginPercentScaleConvertsHealthKitFractionToZeroToHundred() {
        XCTAssertEqual(HealthDataType.oxygenSaturation.pluginValue(fromHealthKit: 0.97), 97.0, accuracy: 0.0001)
        XCTAssertEqual(HealthDataType.bodyFat.pluginValue(fromHealthKit: 0.215), 21.5, accuracy: 0.0001)
        XCTAssertEqual(HealthDataType.heartRate.pluginValue(fromHealthKit: 72.0), 72.0, accuracy: 0.0001)
    }

    func testHealthKitPercentScaleConvertsPluginZeroToHundredToFraction() throws {
        XCTAssertEqual(try HealthDataType.oxygenSaturation.healthKitValue(fromPlugin: 97.0), 0.97, accuracy: 0.0001)
        XCTAssertEqual(try HealthDataType.bodyFat.healthKitValue(fromPlugin: 21.5), 0.215, accuracy: 0.0001)
    }

    func testValidatePluginPercentValueRejectsOutOfRange() {
        XCTAssertThrowsError(try HealthDataType.oxygenSaturation.validatePluginValue(101.0))
        XCTAssertThrowsError(try HealthDataType.bodyFat.validatePluginValue(-0.1))
    }
}
