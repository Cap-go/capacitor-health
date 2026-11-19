package app.capgo.plugin.health

import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun leanBodyMassMapsToLeanBodyMassRecordInKilograms() {
        val type = HealthDataType.from("leanBodyMass")
        assertSame(HealthDataType.LEAN_BODY_MASS, type)
        assertEquals(LeanBodyMassRecord::class, type?.recordClass)
        assertEquals("kilogram", type?.unit)
        assertTrue(type!!.readPermission.contains("READ_LEAN_BODY_MASS"))
        assertTrue(type.writePermission.contains("WRITE_LEAN_BODY_MASS"))
    }

    @Test
    fun boneMassMapsToBoneMassRecordInKilograms() {
        val type = HealthDataType.from("boneMass")
        assertSame(HealthDataType.BONE_MASS, type)
        assertEquals(BoneMassRecord::class, type?.recordClass)
        assertEquals("kilogram", type?.unit)
        assertTrue(type!!.readPermission.contains("READ_BONE_MASS"))
        assertTrue(type.writePermission.contains("WRITE_BONE_MASS"))
    }

    @Test
    fun bodyWaterMassMapsToBodyWaterMassRecordInKilograms() {
        val type = HealthDataType.from("bodyWaterMass")
        assertSame(HealthDataType.BODY_WATER_MASS, type)
        assertEquals(BodyWaterMassRecord::class, type?.recordClass)
        assertEquals("kilogram", type?.unit)
        assertTrue(type!!.readPermission.contains("READ_BODY_WATER_MASS"))
        assertTrue(type.writePermission.contains("WRITE_BODY_WATER_MASS"))
    }
}
