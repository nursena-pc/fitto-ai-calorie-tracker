package com.example.fitto.feature_home.presentation.social

enum class SharedContentType {
    MEDIA,
    LINK,
    FILE
}

data class SharedContentUiModel(
    val id: String,
    val type: SharedContentType,
    val title: String,
    val subtitle: String,
    val url: String,
    val createdAt: Long
)