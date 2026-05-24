package com.example.fitto.feature_home.presentation.notifications

import android.content.Context
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

object FittoNotificationBadgeBinder {

    fun bind(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        badgeText: TextView,
        includeMessages: Boolean = true
    ) {
        val appContext = context.applicationContext
        renderBadge(
            badgeText = badgeText,
            count = FittoNotificationStore.getUnreadCount(appContext)
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FittoNotificationStore.changeToken.collect {
                    renderBadge(
                        badgeText = badgeText,
                        count = FittoNotificationStore.getUnreadCount(appContext)
                    )
                }
            }
        }
    }

    fun renderBadge(badgeText: TextView, count: Int) {
        badgeText.isVisible = count > 0
        badgeText.text = when {
            count > 99 -> "99+"
            count > 0 -> count.toString()
            else -> ""
        }
    }
}
