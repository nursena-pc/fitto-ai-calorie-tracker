package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentNotificationsBinding
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class NotificationsFragment : DialogFragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().fittoAppContainer.socialFeatureDependencies.repository
    }

    private lateinit var notificationAdapter: NotificationAdapter

    private val stateStore = NotificationsScreenStateStore()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Fitto_FullScreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotificationsBinding.bind(view)

        setupRecyclerView()
        setupClicks()
        setupTabs()

        FittoNotificationStore.cancelActiveSystemNotifications(requireContext())

        observeSocialNotifications()
        observeThreadsForReadSync()
        observeLocalNotificationChanges()

        submitNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }

        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupClicks() {
        binding.notificationsBackButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupTabs() = with(binding.notificationsTabLayout) {
        removeAllTabs()

        addTab(newTab().setText("Tümü"))
        addTab(newTab().setText("Sosyal"))

        getTabAt(
            if (stateStore.state.selectedTab == NotificationScreenTab.SOCIAL) 1 else 0
        )?.select()

        addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    stateStore.selectTab(
                        if (tab.position == 1) {
                            NotificationScreenTab.SOCIAL
                        } else {
                            NotificationScreenTab.ALL
                        }
                    )
                    submitNotifications()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
    }
    private fun observeSocialNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeNotifications(requireContext()).collect { notifications ->
                    stateStore.updateSocialNotifications(notifications)
                    submitNotifications()
                }
            }
        }
    }

    private fun observeThreadsForReadSync() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeMessageThreads().collect { threads ->
                    stateStore.updateThreads(threads)

                    threads
                        .filter { it.unreadCount == 0 }
                        .forEach { thread ->
                            FittoNotificationStore.removeMessageNotificationsForThread(
                                requireContext(),
                                thread.id
                            )
                        }

                    submitNotifications()
                }
            }
        }
    }

    private fun observeLocalNotificationChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FittoNotificationStore.changeToken.collect {
                    submitNotifications()
                }
            }
        }
    }

    private fun submitNotifications() {
        if (_binding == null) return

        val localNotifications = FittoNotificationStore.getNotifications(requireContext())
        val notifications = stateStore.allNotifications(localNotifications)
        val filtered = stateStore.visibleNotifications(localNotifications)

        updateTabTitles(notifications)
        binding.notificationsEmptyText.text = when (stateStore.state.selectedTab) {
            NotificationScreenTab.ALL -> "Henüz bildirimin yok."
            NotificationScreenTab.SOCIAL -> "Sosyal bildirimin yok."
        }

        binding.notificationsEmptyText.isVisible = filtered.isEmpty()
        binding.notificationsRecyclerView.isVisible = filtered.isNotEmpty()
        notificationAdapter.submitList(toListItems(filtered))
    }

    private fun updateTabTitles(
        notifications: List<FittoNotification>
    ) = with(binding.notificationsTabLayout) {
        val allCount = notifications.size
        val socialCount = notifications.count(stateStore::isSocialNotification)

        getTabAt(0)?.text = "Tümü ($allCount)"
        getTabAt(1)?.text = "Sosyal ($socialCount)"
    }

    private fun toListItems(
        notifications: List<FittoNotification>
    ): List<NotificationListItem> {
        return notifications.map { notification ->
            NotificationListItem.Row(notification)
        }
    }

    private fun handleNotificationClick(notification: FittoNotification) {
        FittoNotificationStore.markLocalNotificationRead(
            requireContext(),
            notification.id
        )

        if (
            stateStore.isMessageNotification(notification) &&
            !notification.targetId.isNullOrBlank()
        ) {
            FittoNotificationStore.removeMessageNotificationsForThread(
                requireContext(),
                notification.targetId
            )
        }

        if (notification.type != FittoNotification.TYPE_REMINDER) {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.markSocialNotificationRead(notification.id)
            }
        }

        submitNotifications()

        when (notification.type) {
            FittoNotification.TYPE_POST_LIKE,
            FittoNotification.TYPE_POST_COMMENT,
            FittoNotification.TYPE_FOLLOWING_POST,
            FittoNotification.TYPE_COMMENT_REPLY,
            FittoNotification.TYPE_COMMENT_LIKE -> {
                openPost(notification.targetId)
            }

            FittoNotification.TYPE_FOLLOW,
            FittoNotification.TYPE_UNFOLLOW -> {
                openProfile(notification.targetId)
            }

            FittoNotification.TYPE_MESSAGE,
            FittoNotification.TYPE_GROUP_MESSAGE,
            FittoNotification.TYPE_GROUP_ADDED -> {
                openThread(notification.targetId)
            }

            FittoNotification.TYPE_REMINDER -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notifications_reminder_open_from_profile),
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notifications_open_soon),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openPost(postId: String?) {
        if (postId.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.social_post_not_found,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        SocialPostDetailFragment
            .newInstance(postId)
            .show(childFragmentManager, "SocialPostDetailFragment")
    }

    private fun openProfile(userId: String?) {
        if (userId.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.profile_action_coming_soon,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        SocialProfileFragment
            .newInstance(userId)
            .show(childFragmentManager, "SocialProfileFragment")
    }

    private fun openThread(threadId: String?) {
        if (threadId.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.messages_empty_state,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        FittoNotificationStore.removeMessageNotificationsForThread(
            requireContext(),
            threadId
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val thread = runCatching {
                repository.getMessageThread(threadId)
            }.getOrNull()

            if (thread == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.messages_empty_state,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            ChatDetailFragment
                .newInstance(thread)
                .show(childFragmentManager, "ChatDetailFragment")
        }
    }

    override fun onResume() {
        super.onResume()

        FittoNotificationStore.cancelActiveSystemNotifications(requireContext())
        submitNotifications()
    }

    override fun onDestroyView() {
        FittoNotificationStore.markAllLocalNotificationsRead(requireContext())
        binding.notificationsRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

}
