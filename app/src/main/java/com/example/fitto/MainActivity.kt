package com.example.fitto

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_home.presentation.HomeFragment
import com.example.fitto.feature_home.presentation.notifications.FittoInAppNotificationCenter
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPermission
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPublisher
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_home.presentation.profile.ReminderPreferenceStore
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.profile.reminders.service.ReminderTaskRemovedService
import com.example.fitto.feature_home.presentation.social.FittoNotification
import com.example.fitto.feature_home.presentation.social.RealtimePresenceManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val bannerHandler = Handler(Looper.getMainLooper())
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var observedUserId: String? = null
    private var inAppBannerView: View? = null
    private var hideBannerRunnable: Runnable? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            ProfilePreferenceStore.wrapContextWithSavedLanguage(newBase)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ProfilePreferenceStore.applySavedTheme(this)

        super.onCreate(savedInstanceState)
        ReminderTaskRemovedService.ensureStarted(this)
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_main)
        setupStartDestination()
        setupNotificationAccountGuard()
        ensureCurrentUserScopeOnStartup()
        observeInAppNotifications()
        clearSystemNotificationsOnAppOpen(intent)
        handleSocialNotificationIntent(intent)
        handleReminderNotificationIntent(intent)
    }


    private fun setupStartDestination() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mainNavHost) as NavHostFragment

        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)

        val currentUser = FirebaseAuth.getInstance().currentUser

        val shouldOpenHome = currentUser != null &&
                (
                        currentUser.email.isNullOrBlank() ||
                                currentUser.isEmailVerified
                        )

        graph.setStartDestination(
            if (shouldOpenHome) {
                R.id.homeFragment
            } else {
                R.id.welcomeFragment
            }
        )

        navController.graph = graph
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        clearSystemNotificationsOnAppOpen(intent)
        handleSocialNotificationIntent(intent)
        handleReminderNotificationIntent(intent)
    }

    private fun observeInAppNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                FittoInAppNotificationCenter.events.collect { notification ->
                    showInAppNotificationBanner(notification)
                }
            }
        }
    }

    private fun showInAppNotificationBanner(notification: FittoNotification) {
        val contentRoot = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return
        hideInAppNotificationBanner(animate = false)

        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            isFocusable = true
            elevation = dp(10).toFloat()
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), Color.argb(34, 46, 125, 50))
            }
        }

        val iconView = ImageView(this).apply {
            setImageResource(notification.iconRes)
            setColorFilter(getColorCompat(R.color.fitto_green))
        }
        banner.addView(
            iconView,
            LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(12) }
        )

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val titleView = TextView(this).apply {
            text = notification.title
            setTextColor(getColorCompat(R.color.fitto_text_dark))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        val detailView = TextView(this).apply {
            text = notification.description
            setTextColor(getColorCompat(R.color.fitto_text_secondary))
            textSize = 12f
            maxLines = 2
        }
        textContainer.addView(titleView)
        textContainer.addView(detailView)
        banner.addView(
            textContainer,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val topInset = ViewCompat.getRootWindowInsets(contentRoot)
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            ?.top ?: 0

        contentRoot.addView(
            banner,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            ).apply {
                leftMargin = dp(14)
                rightMargin = dp(14)
                topMargin = topInset + dp(12)
            }
        )

        banner.alpha = 0f
        banner.translationY = -dp(24).toFloat()
        banner.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180L)
            .start()

        banner.setOnClickListener {
            FittoNotificationStore.markLocalNotificationRead(this, notification.id)
            if (notification.type == FittoNotification.TYPE_MESSAGE ||
                notification.type == FittoNotification.TYPE_GROUP_MESSAGE ||
                notification.type == FittoNotification.TYPE_GROUP_ADDED
            ) {
                notification.targetId?.let { threadId ->
                    FittoNotificationStore.removeMessageNotificationsForThread(this, threadId)
                }
            }
            openSocialNotificationTarget(notification.type, notification.targetId, notification.secondaryTargetId)
            hideInAppNotificationBanner(animate = true)
        }

        inAppBannerView = banner
        hideBannerRunnable?.let { bannerHandler.removeCallbacks(it) }
        hideBannerRunnable = Runnable { hideInAppNotificationBanner(animate = true) }
            .also { bannerHandler.postDelayed(it, 4500L) }
    }

    private fun hideInAppNotificationBanner(animate: Boolean) {
        hideBannerRunnable?.let { bannerHandler.removeCallbacks(it) }
        hideBannerRunnable = null
        val banner = inAppBannerView ?: return
        inAppBannerView = null

        if (!animate) {
            (banner.parent as? ViewGroup)?.removeView(banner)
            return
        }

        banner.animate()
            .alpha(0f)
            .translationY(-dp(24).toFloat())
            .setDuration(140L)
            .withEndAction { (banner.parent as? ViewGroup)?.removeView(banner) }
            .start()
    }

    private fun setupNotificationAccountGuard() {
        if (authStateListener != null) return

        observedUserId = auth.currentUser?.uid
        FittoNotificationStore.clearLegacyUnscopedNotifications(this)

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val newUserId = firebaseAuth.currentUser?.uid
            val oldUserId = observedUserId

            if (oldUserId != newUserId) {
                FittoNotificationStore.handleAccountChanged(this)
                observedUserId = newUserId

                if (!newUserId.isNullOrBlank()) {
                    handleActiveAccountChanged(newUserId)
                } else {
                    ReminderAlarmScheduler.cancelAll(this)
                }
            }
        }.also { listener ->
            auth.addAuthStateListener(listener)
        }
    }

    private fun ensureCurrentUserScopeOnStartup() {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return
        handleActiveAccountChanged(currentUid, rescheduleEvenIfSameUser = false)
    }

    private fun handleActiveAccountChanged(
        newUserId: String,
        rescheduleEvenIfSameUser: Boolean = true
    ) {
        lifecycleScope.launch {
            val scopeChanged = UserSessionDataGuard.ensureScopedDataForUser(
                context = this@MainActivity,
                currentUid = newUserId
            )

            if (scopeChanged || rescheduleEvenIfSameUser) {
                // Cancel alarms registered for the previously active account, then schedule
                // only the newly active user's reminders. User data itself is not deleted.
                ReminderAlarmScheduler.cancelAll(this@MainActivity)
                ReminderAlarmScheduler.scheduleAll(this@MainActivity)
                notifyHomeFragmentAccountScopeChanged()
            }
        }
    }

    private fun notifyHomeFragmentAccountScopeChanged() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mainNavHost) as? NavHostFragment

        val homeFragment = navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull { it is HomeFragment } as? HomeFragment

        homeFragment?.onAccountScopeChanged()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == FittoNotificationPermission.REQUEST_POST_NOTIFICATIONS &&
            FittoNotificationPermission.hasPermission(this) &&
            ReminderPreferenceStore.hasSavedReminders(this)
        ) {
            ReminderAlarmScheduler.scheduleAll(this)
        }
    }

    private fun clearSystemNotificationsOnAppOpen(intent: Intent?) {
        FittoNotificationStore.cancelActiveSystemNotifications(this)
        if (intent?.getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_FROM_NOTIFICATION, false) == true) {
            intent.removeExtra(FittoNotificationPublisher.EXTRA_OPENED_FROM_NOTIFICATION)
        }
    }

    private fun handleSocialNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_SOCIAL_NOTIFICATION, false) != true) {
            return
        }

        val type = intent.getStringExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TYPE)
        val targetId = intent.getStringExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TARGET_ID)
        val secondaryTargetId = intent.getStringExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID)

        if ((type == FittoNotification.TYPE_MESSAGE ||
                    type == FittoNotification.TYPE_GROUP_MESSAGE ||
                    type == FittoNotification.TYPE_GROUP_ADDED) &&
            !targetId.isNullOrBlank()
        ) {
            FittoNotificationStore.removeMessageNotificationsForThread(this, targetId)
        }

        openSocialNotificationTarget(type, targetId, secondaryTargetId)

        intent.removeExtra(FittoNotificationPublisher.EXTRA_OPENED_SOCIAL_NOTIFICATION)
        intent.removeExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TYPE)
        intent.removeExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_TARGET_ID)
        intent.removeExtra(FittoNotificationPublisher.EXTRA_SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID)
    }

    private fun handleReminderNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_REMINDER_NOTIFICATION, false) != true) {
            return
        }

        val reminderId = intent.getStringExtra(FittoNotificationPublisher.EXTRA_REMINDER_ID)
        val reminderCategory = intent.getStringExtra(FittoNotificationPublisher.EXTRA_REMINDER_CATEGORY)

        openReminderNotificationTarget(reminderCategory, reminderId)

        intent.removeExtra(FittoNotificationPublisher.EXTRA_OPENED_REMINDER_NOTIFICATION)
        intent.removeExtra(FittoNotificationPublisher.EXTRA_REMINDER_ID)
        intent.removeExtra(FittoNotificationPublisher.EXTRA_REMINDER_CATEGORY)
    }

    private fun openReminderNotificationTarget(
        categoryName: String?,
        reminderId: String?
    ) {
        dispatchToHomeFragment { homeFragment ->
            homeFragment.openReminderNotificationTarget(categoryName, reminderId)
        }
    }

    private fun openSocialNotificationTarget(
        type: String?,
        targetId: String?,
        secondaryTargetId: String?
    ) {
        dispatchToHomeFragment { homeFragment ->
            homeFragment.openSocialNotificationTarget(type, targetId, secondaryTargetId)
        }
    }

    private fun dispatchToHomeFragment(
        attempt: Int = 0,
        action: (HomeFragment) -> Unit
    ) {
        val delay = if (attempt == 0) 0L else 120L
        window.decorView.postDelayed({
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.mainNavHost) as? NavHostFragment

            val homeFragment = navHostFragment
                ?.childFragmentManager
                ?.primaryNavigationFragment as? HomeFragment

            if (homeFragment != null) {
                action(homeFragment)
            } else if (attempt < 6) {
                dispatchToHomeFragment(attempt + 1, action)
            }
        }, delay)
    }

    override fun onResume() {
        super.onResume()
        FittoInAppNotificationCenter.setAppVisible(true)
        FittoNotificationStore.cancelActiveSystemNotifications(this)
    }

    override fun onStart() {
        super.onStart()
        RealtimePresenceManager.start()
    }

    override fun onStop() {
        FittoInAppNotificationCenter.setAppVisible(false)
        RealtimePresenceManager.stop()
        hideInAppNotificationBanner(animate = false)
        super.onStop()
    }

    fun rememberCurrentHomeScreenForSettingsRecreate() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mainNavHost) as? NavHostFragment

        val homeFragment = navHostFragment
            ?.childFragmentManager
            ?.primaryNavigationFragment as? HomeFragment

        homeFragment?.rememberCurrentScreenForSettingsRecreate()
    }

    override fun onDestroy() {
        hideInAppNotificationBanner(animate = false)
        authStateListener?.let { auth.removeAuthStateListener(it) }
        authStateListener = null
        super.onDestroy()
    }

    override fun recreate() {
        super.recreate()
        overridePendingTransition(0, 0)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun getColorCompat(colorRes: Int): Int {
        return androidx.core.content.ContextCompat.getColor(this, colorRes)
    }
}
