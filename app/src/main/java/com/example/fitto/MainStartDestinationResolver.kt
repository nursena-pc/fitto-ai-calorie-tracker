package com.example.fitto

internal enum class MainStartDestination {
    HOME,
    WELCOME
}

internal object MainStartDestinationResolver {

    fun resolve(
        hasAuthenticatedUser: Boolean,
        email: String?,
        isEmailVerified: Boolean
    ): MainStartDestination {
        val canOpenHome = hasAuthenticatedUser && (
                email.isNullOrBlank() || isEmailVerified
                )

        return if (canOpenHome) {
            MainStartDestination.HOME
        } else {
            MainStartDestination.WELCOME
        }
    }
}
