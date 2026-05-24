import XCTest
@testable import HealthPlugin

class HealthTests: XCTestCase {
    func testReadSamplePayloadIncludesFiniteValue() {
        let implementation = Health()
        let startDate = Date(timeIntervalSince1970: 0)
        let endDate = Date(timeIntervalSince1970: 60)

        let payload = implementation.readSamplePayload(
            dataType: .oxygenSaturation,
            value: 0.96,
            startDate: startDate,
            endDate: endDate
        )

        XCTAssertEqual(payload?["dataType"] as? String, "oxygenSaturation")
        XCTAssertEqual(payload?["value"] as? Double, 0.96)
        XCTAssertEqual(payload?["unit"] as? String, "percent")
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
}
