package com.example.fitto.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Executes blocking or CPU-bound feature work on an injected dispatcher. */
class CoroutineTaskRunner(
    private val dispatcher: CoroutineDispatcher
) {
    suspend fun <T> run(block: suspend () -> T): T = withContext(dispatcher) {
        block()
    }
}
