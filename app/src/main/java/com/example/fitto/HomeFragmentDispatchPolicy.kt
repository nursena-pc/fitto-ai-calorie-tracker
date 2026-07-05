package com.example.fitto

internal object HomeFragmentDispatchPolicy {
    private const val MAX_RETRY_ATTEMPT = 6
    private const val RETRY_DELAY_MILLIS = 120L

    fun shouldRetry(attempt: Int): Boolean = attempt < MAX_RETRY_ATTEMPT

    fun delayMillis(attempt: Int): Long {
        return if (attempt == 0) 0L else RETRY_DELAY_MILLIS
    }
}
