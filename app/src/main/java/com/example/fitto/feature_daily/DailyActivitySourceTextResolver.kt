package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R

object DailyActivitySourceTextResolver {

    @StringRes
    fun resolveStatusTextRes(source: DailyActivityPolicy.Source): Int {
        return when (source) {
            DailyActivityPolicy.Source.HEALTH_CONNECT -> R.string.daily_activity_source_health_connect
            DailyActivityPolicy.Source.FITTO_COUNTER -> R.string.daily_activity_source_fitto_counter
            DailyActivityPolicy.Source.MANUAL_ONLY -> R.string.daily_activity_source_manual_only
            DailyActivityPolicy.Source.CONNECT -> R.string.daily_activity_source_connect
        }
    }
}
