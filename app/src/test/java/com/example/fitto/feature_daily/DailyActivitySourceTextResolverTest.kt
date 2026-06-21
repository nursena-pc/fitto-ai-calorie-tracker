package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivitySourceTextResolverTest {

    @Test
    fun resolveStatusTextRes_mapsActivitySourcesToTextResources() {
        assertEquals(
            R.string.daily_activity_source_health_connect,
            DailyActivitySourceTextResolver.resolveStatusTextRes(
                DailyActivityPolicy.Source.HEALTH_CONNECT
            )
        )
        assertEquals(
            R.string.daily_activity_source_fitto_counter,
            DailyActivitySourceTextResolver.resolveStatusTextRes(
                DailyActivityPolicy.Source.FITTO_COUNTER
            )
        )
        assertEquals(
            R.string.daily_activity_source_manual_only,
            DailyActivitySourceTextResolver.resolveStatusTextRes(
                DailyActivityPolicy.Source.MANUAL_ONLY
            )
        )
        assertEquals(
            R.string.daily_activity_source_connect,
            DailyActivitySourceTextResolver.resolveStatusTextRes(
                DailyActivityPolicy.Source.CONNECT
            )
        )
    }
}
