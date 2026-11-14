package app.capgo.plugin.health

import androidx.health.connect.client.records.NutritionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HealthDataTypeTest {

    @Test
    fun dietaryProteinIsTheProteinOfNutritionRecordsInGrams() {
        val type = HealthDataType.from("dietaryProtein")
        assertSame(HealthDataType.DIETARY_PROTEIN, type)
        assertEquals(NutritionRecord::class, type?.recordClass)
        assertEquals("gram", type?.unit)
    }

    @Test
    fun dietaryProteinSharesTheNutritionPermissionsWithDietaryEnergy() {
        assertEquals(HealthDataType.DIETARY_ENERGY.recordClass, HealthDataType.DIETARY_PROTEIN.recordClass)
        assertEquals(HealthDataType.DIETARY_ENERGY.readPermission, HealthDataType.DIETARY_PROTEIN.readPermission)
        assertEquals(HealthDataType.DIETARY_ENERGY.writePermission, HealthDataType.DIETARY_PROTEIN.writePermission)
    }
}
