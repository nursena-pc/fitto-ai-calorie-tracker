package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.social.SocialUser

internal class ProfileDetailListRenderer(
    private val context: Context,
    private val recyclerView: RecyclerView,
    onItemClick: (ProfileDetailItem) -> Unit,
    onAchievementTarget: (ProfileMenuId) -> Unit
) {

    private val achievementAdapter = ProfileAchievementAdapter { item ->
        item.targetMenuId?.let(onAchievementTarget)
    }

    private val detailAdapter = ProfileDetailAdapter(
        items = emptyList(),
        onItemClick = onItemClick
    )

    fun render(
        menuId: ProfileMenuId,
        healthData: ProfileHealthData,
        user: SocialUser?,
        blockedUsers: List<SocialUser>
    ) {
        if (isAchievementMenu(menuId)) {
            renderAchievements(menuId, healthData)
        } else {
            renderDetails(menuId, healthData, user, blockedUsers)
        }
    }

    fun clear() {
        recyclerView.adapter = null
    }

    private fun renderDetails(
        menuId: ProfileMenuId,
        healthData: ProfileHealthData,
        user: SocialUser?,
        blockedUsers: List<SocialUser>
    ) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val firstVisiblePosition = layoutManager
            ?.findFirstVisibleItemPosition()
            ?: RecyclerView.NO_POSITION
        val firstVisibleTop = if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            layoutManager?.findViewByPosition(firstVisiblePosition)?.top ?: 0
        } else {
            0
        }

        detailAdapter.updateItems(
            ProfileDetailContent.rowsFor(
                context = context,
                menuId = menuId,
                healthData = healthData,
                user = user,
                blockedUsers = blockedUsers
            )
        )

        if (recyclerView.adapter !== detailAdapter) {
            recyclerView.adapter = detailAdapter
        }

        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            recyclerView.post {
                (recyclerView.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(firstVisiblePosition, firstVisibleTop)
            }
        }
    }

    private fun renderAchievements(
        menuId: ProfileMenuId,
        healthData: ProfileHealthData
    ) {
        if (recyclerView.adapter !== achievementAdapter) {
            recyclerView.adapter = achievementAdapter
        }

        achievementAdapter.submitList(
            ProfileAchievementContent.itemsFor(
                context = context,
                menuId = menuId,
                healthData = healthData
            )
        )
    }

    private fun isAchievementMenu(menuId: ProfileMenuId): Boolean {
        return menuId == ProfileMenuId.ACHIEVEMENTS ||
            menuId == ProfileMenuId.ACHIEVEMENT_BADGES ||
            menuId == ProfileMenuId.ACHIEVEMENT_STREAK ||
            menuId == ProfileMenuId.ACHIEVEMENT_WATER
    }
}
