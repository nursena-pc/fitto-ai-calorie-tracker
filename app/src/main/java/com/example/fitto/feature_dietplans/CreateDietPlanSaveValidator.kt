package com.example.fitto.feature_dietplans

object CreateDietPlanSaveValidator {

    fun validate(
        planName: String,
        hasDuplicateTitle: Boolean
    ): CreateDietPlanSaveValidation {
        return when {
            planName.isBlank() -> CreateDietPlanSaveValidation.MISSING_NAME
            hasDuplicateTitle -> CreateDietPlanSaveValidation.DUPLICATE_NAME
            else -> CreateDietPlanSaveValidation.VALID
        }
    }
}

enum class CreateDietPlanSaveValidation {
    VALID,
    MISSING_NAME,
    DUPLICATE_NAME
}
