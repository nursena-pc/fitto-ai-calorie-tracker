package com.example.fitto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppStartupCoordinatorTest {

    @Test
    fun `start runs application startup tasks in order`() {
        val calls = mutableListOf<String>()
        val coordinator = AppStartupCoordinator(
            importSeedData = { calls += "seed" },
            ensureNotificationChannels = { calls += "channels" },
            startSocialNotificationObserver = { calls += "social" }
        )

        coordinator.start()

        assertEquals(listOf("seed", "channels", "social"), calls)
    }

    @Test
    fun `start stops when a startup task fails`() {
        val calls = mutableListOf<String>()
        val coordinator = AppStartupCoordinator(
            importSeedData = { calls += "seed" },
            ensureNotificationChannels = {
                calls += "channels"
                error("channel failure")
            },
            startSocialNotificationObserver = { calls += "social" }
        )

        assertThrows(IllegalStateException::class.java) {
            coordinator.start()
        }

        assertEquals(listOf("seed", "channels"), calls)
    }
}
