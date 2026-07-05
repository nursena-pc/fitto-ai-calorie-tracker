package com.example.fitto

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FittoApplicationSmokeTest {

    @Test
    fun application_startsWithExpectedCompositionRoot() {
        val application = ApplicationProvider.getApplicationContext<FittoApplication>()

        assertEquals("com.example.fitto", application.packageName)
        assertNotNull(application.appContainer)
    }
}
