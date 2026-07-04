package com.example.fitto.feature_home.presentation.social

data class MessageHiddenRange(
    val userId: String,
    val startAt: Long,
    val endAt: Long?
)

data class DirectThreadBlockState(
    val isBlocked: Boolean,
    val hiddenRanges: List<MessageHiddenRange>
)
