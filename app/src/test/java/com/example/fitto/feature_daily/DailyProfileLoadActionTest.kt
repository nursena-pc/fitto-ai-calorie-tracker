package com.example.fitto.feature_daily

import com.example.fitto.data.local.entity.UserProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyProfileLoadActionTest {

    @Test
    fun `cached profile is returned without syncing`() = runBlocking {
        val cached = profile("cached")
        var syncCalled = false
        val action = DailyProfileLoadAction(
            getCachedProfile = { cached },
            syncProfile = {
                syncCalled = true
                profile("synced")
            }
        )

        val result = action.load()

        assertSame(cached, result)
        assertFalse(syncCalled)
    }

    @Test
    fun `missing cache falls back to synced profile`() = runBlocking {
        val synced = profile("synced")
        var syncCalled = false
        val action = DailyProfileLoadAction(
            getCachedProfile = { null },
            syncProfile = {
                syncCalled = true
                synced
            }
        )

        val result = action.load()

        assertSame(synced, result)
        assertTrue(syncCalled)
    }

    @Test
    fun `missing cache and remote profile returns null`() = runBlocking {
        val action = DailyProfileLoadAction(
            getCachedProfile = { null },
            syncProfile = { null }
        )

        assertNull(action.load())
    }

    private fun profile(id: String): UserProfileEntity {
        return testProfile(userId = id)
    }
}
