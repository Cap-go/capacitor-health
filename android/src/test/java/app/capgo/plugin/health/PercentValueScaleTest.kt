package app.capgo.plugin.health

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentValueScaleTest {

    @Test
    fun toPluginValuePassesThroughHealthConnectZeroToHundredScale() {
        assertEquals(97.0, PercentValueScale.toPluginValue(97.0), 0.0001)
        assertEquals(21.5, PercentValueScale.toPluginValue(21.5), 0.0001)
    }

    @Test
    fun toHealthConnectValueAcceptsZeroToHundred() {
        assertEquals(97.0, PercentValueScale.toHealthConnectValue(97.0), 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toHealthConnectValueRejectsAboveHundred() {
        PercentValueScale.toHealthConnectValue(101.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toHealthConnectValueRejectsNegative() {
        PercentValueScale.toHealthConnectValue(-1.0)
    }
}