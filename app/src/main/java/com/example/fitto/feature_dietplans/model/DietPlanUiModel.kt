package com.example.fitto.feature_dietplans.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DietPlanUiModel(
    val id: String,
    @StringRes val titleRes: Int? = null,
    @StringRes val descriptionRes: Int? = null,
    val titleText: String? = null,
    val descriptionText: String? = null,
    val kcalPerDay: Int,
    val durationDays: Int,
    val isFavorite: Boolean = false,
    val isApplied: Boolean = false,
    @StringRes val tagRes: Int? = null,
    @DrawableRes val imageResId: Int? = null,
    val isDraft: Boolean = false
)