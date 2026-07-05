package com.example.fitto.core.ui

import com.google.android.material.chip.Chip

fun Chip.setUniformCornerRadius(radiusPx: Float) {
    shapeAppearanceModel = shapeAppearanceModel
        .toBuilder()
        .setAllCornerSizes(radiusPx)
        .build()
}
