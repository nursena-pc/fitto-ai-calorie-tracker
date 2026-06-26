package com.example.fitto.feature_dietplans

import com.example.fitto.feature_meal.model.MealType

class CreateDietPlanStateStore(
    initialGoal: CreatePlanGoal = CreatePlanGoal.WEIGHT_LOSS,
    initialPrivacy: CreatePlanPrivacy = CreatePlanPrivacy.PRIVATE,
    initialDailyCalories: Int = DEFAULT_DAILY_CALORIES,
    initialDurationDays: Int = DEFAULT_DURATION_DAYS,
    initialMealTypes: Collection<MealType> = DEFAULT_MEAL_TYPES,
    initialPreferences: Collection<String> = emptyList()
) {
    var goal: CreatePlanGoal = initialGoal
        private set
    var privacy: CreatePlanPrivacy = initialPrivacy
        private set
    var dailyCalories: Int = initialDailyCalories
        private set
    var durationDays: Int = initialDurationDays
        private set

    private val selectedMealTypes = linkedSetOf<MealType>()
    private val selectedPreferences = mutableSetOf<String>()

    init {
        setMealTypes(initialMealTypes)
        setPreferences(initialPreferences)
    }

    fun applyDefaults(defaults: CreateDietPlanDefaults) {
        dailyCalories = defaults.dailyCalories
        goal = defaults.goal
    }

    fun applyEditableState(state: CreateDietPlanEditableState) {
        goal = state.goal
        privacy = state.privacy
        dailyCalories = state.dailyCalories
        durationDays = state.durationDays
        setPreferences(state.preferences)
        setMealTypes(state.mealTypes)
    }

    fun setPrivacy(value: CreatePlanPrivacy) {
        privacy = value
    }

    fun setGoal(value: CreatePlanGoal) {
        goal = value
    }

    fun setDailyCalories(value: Int) {
        dailyCalories = value
    }

    fun setDurationDays(value: Int) {
        durationDays = value
    }

    fun decreaseCalories() {
        dailyCalories = CreateDietPlanNumberRules.decreaseCalories(dailyCalories)
    }

    fun increaseCalories() {
        dailyCalories = CreateDietPlanNumberRules.increaseCalories(dailyCalories)
    }

    fun decreaseDuration() {
        durationDays = CreateDietPlanNumberRules.decreaseDuration(durationDays)
    }

    fun increaseDuration() {
        durationDays = CreateDietPlanNumberRules.increaseDuration(durationDays)
    }

    fun togglePreference(preferenceKey: String, isChecked: Boolean) {
        setPreferences(
            CreateDietPlanSelectionReducer.togglePreference(
                currentPreferences = selectedPreferences,
                preferenceKey = preferenceKey,
                isChecked = isChecked
            )
        )
    }

    fun toggleMealType(mealType: MealType, isChecked: Boolean) {
        setMealTypes(
            CreateDietPlanSelectionReducer.toggleMealType(
                currentMealTypes = selectedMealTypes,
                mealType = mealType,
                isChecked = isChecked
            )
        )
    }

    fun toRenderState(): CreateDietPlanRenderState {
        return CreateDietPlanRenderState(
            dailyCalories = dailyCalories,
            durationDays = durationDays,
            selectedGoal = goal,
            selectedPrivacy = privacy,
            selectedMealTypes = selectedMealTypes.toList(),
            selectedPreferences = selectedPreferences.toSet()
        )
    }

    fun toFormState(
        planName: String,
        description: String,
        exclusions: List<String>
    ): CreateDietPlanFormState {
        return CreateDietPlanFormState(
            planName = planName,
            description = description,
            goal = goal,
            privacy = privacy,
            dailyCalories = dailyCalories,
            durationDays = durationDays,
            mealTypes = selectedMealTypes.toList(),
            preferences = selectedPreferences.toList(),
            exclusions = exclusions
        )
    }

    private fun setMealTypes(mealTypes: Collection<MealType>) {
        selectedMealTypes.clear()
        selectedMealTypes.addAll(mealTypes)
    }

    private fun setPreferences(preferences: Collection<String>) {
        selectedPreferences.clear()
        selectedPreferences.addAll(preferences)
    }

    companion object {
        private const val DEFAULT_DAILY_CALORIES = 1800
        private const val DEFAULT_DURATION_DAYS = 30
        private val DEFAULT_MEAL_TYPES = listOf(
            MealType.BREAKFAST,
            MealType.SNACK,
            MealType.LUNCH,
            MealType.DINNER
        )
    }
}
