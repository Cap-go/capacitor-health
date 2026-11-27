# Changelog

All notable changes to this project are documented in the GitHub Releases page.

**[Click here to view all releases →](../../releases)**

## Unreleased

### Fixed

- **iOS percent scale:** `oxygenSaturation` and `bodyFat` values now use a 0-100 scale in `readSamples` and `saveSample`, matching Android and the documented `percent` unit. Previously iOS returned HealthKit's 0.0-1.0 fraction (for example `0.97` for 97% SpO2). Apps that relied on the old iOS scale should divide stored iOS values by 100 or re-read from HealthKit after upgrading.
