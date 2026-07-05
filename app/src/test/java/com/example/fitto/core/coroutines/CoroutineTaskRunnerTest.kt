package com.example.fitto.core.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineTaskRunnerTest {

    @Test
    fun `run executes work through injected dispatcher`() = runBlocking {
        val dispatcher = RecordingDispatcher()
        val runner = CoroutineTaskRunner(dispatcher)

        val result = runner.run { "completed" }

        assertEquals("completed", result)
        assertTrue(dispatcher.dispatchCount > 0)
    }

    @Test
    fun `run propagates work failure`() {
        val runner = CoroutineTaskRunner(RecordingDispatcher())

        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                runner.run<Unit> { throw IllegalArgumentException("invalid task") }
            }
        }

        assertEquals("invalid task", error.message)
    }

    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatchCount: Int = 0
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatchCount += 1
            block.run()
        }
    }
}
