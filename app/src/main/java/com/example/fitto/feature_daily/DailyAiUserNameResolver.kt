package com.example.fitto.feature_daily

object DailyAiUserNameResolver {

    fun resolve(
        displayName: String?,
        userName: String?,
        fallbackName: String
    ): String {
        return listOf(
            displayName,
            userName,
            fallbackName
        ).firstOrNull { value ->
            !value.isNullOrBlank()
        }?.trim().orEmpty()
    }
}
