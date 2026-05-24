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
}
