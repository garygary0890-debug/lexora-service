package com.lexora.service.feature.vehicles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleValidationTest {
    @Test
    fun emptyOptionalFieldsDoNotBlockSaving() {
        val draft = VehicleDraft(make = "Toyota", model = "Camry", color = "Чёрный")

        assertNull(requiredVehicleField(draft))
    }

    @Test
    fun makeModelAndColorAreRequired() {
        assertEquals(VehicleRequiredField.MAKE, requiredVehicleField(VehicleDraft(model = "Camry", color = "Чёрный")))
        assertEquals(VehicleRequiredField.MODEL, requiredVehicleField(VehicleDraft(make = "Toyota", color = "Чёрный")))
        assertEquals(VehicleRequiredField.COLOR, requiredVehicleField(VehicleDraft(make = "Toyota", model = "Camry")))
    }

    @Test
    fun registrationLineIsShownOnlyWhenFilled() {
        assertFalse(shouldShowVehicleRegistration(""))
        assertFalse(shouldShowVehicleRegistration("   "))
        assertTrue(shouldShowVehicleRegistration("A123AA"))
    }
}
