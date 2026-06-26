package com.example.fitto.feature_dietplans

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanSaveValidatorTest {

    @Test
    fun validate_returnsMissingNameBeforeDuplicateName() {
        assertEquals(
            CreateDietPlanSaveValidation.MISSING_NAME,
            CreateDietPlanSaveValidator.validate(
                planName = "   ",
                hasDuplicateTitle = true
            )
        )
    }

    @Test
    fun validate_returnsDuplicateNameWhenTitleAlreadyExists() {
        assertEquals(
            CreateDietPlanSaveValidation.DUPLICATE_NAME,
            CreateDietPlanSaveValidator.validate(
                planName = "My plan",
                hasDuplicateTitle = true
            )
        )
    }

    @Test
    fun validate_returnsValidWhenNameIsPresentAndUnique() {
        assertEquals(
            CreateDietPlanSaveValidation.VALID,
            CreateDietPlanSaveValidator.validate(
                planName = "My plan",
                hasDuplicateTitle = false
            )
        )
    }
}
