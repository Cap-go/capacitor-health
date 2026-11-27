package app.capgo.plugin.health

/**
 * Cross-platform percent values use a 0-100 scale in the JS API.
 * Health Connect already stores [Percentage] this way; iOS converts at the native boundary.
 */
object PercentValueScale {
    const val MIN = 0.0
    const val MAX = 100.0

    fun requirePluginPercentInRange(value: Double) {
        require(value.isFinite() && value in MIN..MAX) {
            "Percent values must be between 0 and 100, got $value."
        }
    }

    /** Health Connect -> plugin (identity; documented for parity with iOS tests). */
    fun toPluginValue(healthConnectValue: Double): Double = healthConnectValue

    /** Plugin -> Health Connect before [androidx.health.connect.client.units.Percentage]. */
    fun toHealthConnectValue(pluginValue: Double): Double {
        requirePluginPercentInRange(pluginValue)
        return pluginValue
    }
}