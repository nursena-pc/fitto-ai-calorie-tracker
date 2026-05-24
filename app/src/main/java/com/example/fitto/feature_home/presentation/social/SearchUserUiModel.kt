package com.example.fitto.feature_home.presentation.social

data class SearchUserUiModel(
    val id: String,
    val userName: String,
    val bio: String?,
    val photoUrl: String?,
    val isVerified: Boolean = false,
    val isDietitian: Boolean = false,
    val isTrainer: Boolean = false,
    val isNewMember: Boolean = false,
    val isFollowed: Boolean = false,
    val engagementScore: Int = 0,
    val postCount: Int = 0,
    val lastActiveAt: Long = 0L,
    val searchKeywords: String = ""
)
