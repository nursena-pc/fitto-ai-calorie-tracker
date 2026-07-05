package com.example.fitto.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Coroutine dispatchers shared by application-level composition roots and feature factories. */
data class AppCoroutineDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher
) {
    companion object {
        fun production(): AppCoroutineDispatchers = AppCoroutineDispatchers(
            io = Dispatchers.IO,
            default = Dispatchers.Default,
            main = Dispatchers.Main.immediate
        )
    }
}
