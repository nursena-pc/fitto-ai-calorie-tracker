package com.example.fitto.feature_home.presentation.social

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentSocialBinding
import com.example.fitto.feature_recipes.RecipeDetailFragment
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import android.view.GestureDetector
class SocialFragment : Fragment(R.layout.fragment_social) {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var viewModel: SocialViewModel
    private val socialDependencies by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().fittoAppContainer.socialFeatureDependencies
    }
    private val recipeRepository: RecipeRepository
        get() = socialDependencies.recipeRepository
    private lateinit var headerAdapter: SocialCreateHeaderAdapter
    private lateinit var postAdapter: SocialPostAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSocialBinding.bind(view)
        viewModel = ViewModelProvider(
            this,
            socialDependencies.createViewModelFactory()
        )[SocialViewModel::class.java]
        socialDependencies.startRealtimePresence()


        setupRecyclerView()
        setupClicks()
        observePosts()
        observeUnreadMessagesCount()
        observeUnreadSocialNotificationsCount()
        observeMyProfileForHeader()
        setupSwipeToMyProfile()
        setupSwipeRefresh()
        observeRefreshing()

    }
    private fun setupSwipeToMyProfile() {
        gestureDetector = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y

                    val isHorizontalSwipe = abs(diffX) > abs(diffY)
                    val isStrongEnough = abs(diffX) > 180 && abs(velocityX) > 250
                    val isRightToLeft = diffX < 0

                    if (isHorizontalSwipe && isStrongEnough && isRightToLeft) {
                        showMySocialProfile()
                        return true
                    }

                    return false
                }
            }
        )

        binding.socialPostsRecyclerView.addOnItemTouchListener(
            object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(
                    rv: RecyclerView,
                    event: MotionEvent
                ): Boolean {
                    gestureDetector.onTouchEvent(event)
                    return false
                }
            }
        )
    }
    private fun setupSwipeRefresh() {
        binding.socialSwipeRefreshLayout.setColorSchemeResources(R.color.fitto_green)

        binding.socialSwipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshFeed()
        }
    }
    private fun observeRefreshing() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isRefreshing.collect { isRefreshing ->
                    binding.socialSwipeRefreshLayout.isRefreshing = isRefreshing
                }
            }
        }
    }
    private fun setupRecyclerView() {
        headerAdapter = SocialCreateHeaderAdapter(
            onCreatePostClick = { showCreatePostScreen() },
            onProfileClick = { showMySocialProfile() }
        )

        postAdapter = SocialPostAdapter(
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                showCommentsBottomSheet(post.id)
            },
            onSaveClick = { post ->
                savePostOrRecipe(post)
            },
            onShareClick = { post ->
                PostShareHelper.sharePost(
                    context = requireContext(),
                    post = post
                )
            },
            onReportClick = { post ->
                showReportPostDialog(post)
            },
            onUserClick = { post ->
                showProfileScreen(post.userId)
            },
            onRecipeClick = { post ->
                openRecipeFromPost(post)
            }
        )

        layoutManager = LinearLayoutManager(requireContext())

        binding.socialPostsRecyclerView.apply {
            layoutManager = this@SocialFragment.layoutManager
            adapter = ConcatAdapter(headerAdapter, postAdapter)
            setHasFixedSize(false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateFabVisibility()
                }
            })
        }

        updateFabVisibility()
    }


    private fun updateFabVisibility() {
        if (_binding == null || !::layoutManager.isInitialized) return

        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val shouldShowFab = firstVisiblePosition > 0
        binding.socialFab.isVisible = shouldShowFab
    }



    private fun savePostOrRecipe(post: SocialPost) {
        viewLifecycleOwner.lifecycleScope.launch {
            val isRecipePost = post.postType == "recipe" || post.recipeId?.isNotBlank() == true || post.recipeTitle?.isNotBlank() == true

            if (!isRecipePost) {
                viewModel.toggleSave(post)
                return@launch
            }

            if (post.savedByMe) {
                val alreadyInBook = withContext(socialDependencies.ioDispatcher) {
                    recipeRepository.hasCommunityRecipeFromPost(post.id)
                }

                if (!alreadyInBook && post.userId != currentUserIdOrLocal()) {
                    val repairedRecipeId = saveRecipeFromPost(post)
                    if (_binding != null) {
                        Toast.makeText(
                            requireContext(),
                            if (repairedRecipeId != null) R.string.social_recipe_saved_to_book else R.string.social_recipe_save_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                viewModel.toggleSave(post)
                return@launch
            }

            val savedRecipeId = saveRecipeFromPost(post)
            if (_binding == null) return@launch

            if (savedRecipeId == null) {
                Toast.makeText(requireContext(), R.string.social_recipe_save_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }

            val isCommunityRecipe = post.recipeSourceTag == RecipeRepository.RECIPE_SOURCE_TAG_COMMUNITY || post.userId != currentUserIdOrLocal()
            Toast.makeText(
                requireContext(),
                if (isCommunityRecipe) R.string.social_recipe_saved_to_book else R.string.social_recipe_saved_normally,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.toggleSave(post)
        }
    }

    private suspend fun saveRecipeFromPost(post: SocialPost): String? {
        return withContext(socialDependencies.ioDispatcher) {
            recipeRepository.saveRecipeFromCommunityPost(
                postId = post.id,
                postUserId = post.userId,
                recipeId = post.recipeId,
                recipeTitle = post.recipeTitle ?: post.mealInfo,
                recipeImageResId = post.recipeImageResId,
                recipeDuration = post.recipeDuration,
                recipeServing = post.recipeServing,
                recipeCalories = post.recipeCalories,
                recipeCategory = post.recipeCategory,
                recipeSourceTag = post.recipeSourceTag,
                recipeDescription = post.recipeDescription ?: post.content,
                recipeIngredients = post.recipeIngredients,
                recipeSteps = post.recipeSteps
            )
        }
    }

    private fun currentUserIdOrLocal(): String {
        return socialDependencies.repository.currentUserId()
            .takeIf { uid -> uid.isNotBlank() }
            ?: "local_user"
    }

    private fun openRecipeFromPost(post: SocialPost) {
        if (post.recipeId.isNullOrBlank() && post.recipeTitle.isNullOrBlank() && post.mealInfo.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val localRecipeId = withContext(socialDependencies.ioDispatcher) {
                recipeRepository.ensureRecipeAvailableFromCommunityPost(
                    postId = post.id,
                    postUserId = post.userId,
                    recipeId = post.recipeId,
                    recipeTitle = post.recipeTitle ?: post.mealInfo,
                    recipeImageResId = post.recipeImageResId,
                    recipeDuration = post.recipeDuration,
                    recipeServing = post.recipeServing,
                    recipeCalories = post.recipeCalories,
                    recipeCategory = post.recipeCategory,
                    recipeSourceTag = post.recipeSourceTag,
                    recipeDescription = post.recipeDescription ?: post.content,
                    recipeIngredients = post.recipeIngredients,
                    recipeSteps = post.recipeSteps
                )
            }

            if (_binding == null) return@launch

            if (localRecipeId.isNullOrBlank()) {
                Toast.makeText(requireContext(), R.string.recipes_detail_not_found, Toast.LENGTH_SHORT).show()
                return@launch
            }

            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .hide(this@SocialFragment)
                .add(
                    R.id.homeTabContainer,
                    RecipeDetailFragment.newInstance(
                        recipeId = localRecipeId,
                        fromCommunity = true,
                        communityPostId = post.id,
                        communityPostOwnerId = post.userId,
                        communityRecipeSourceTag = post.recipeSourceTag
                    ),
                    "social_recipe_detail"
                )
                .addToBackStack("social_recipe_detail")
                .commit()
        }
    }

    private fun showCommentsBottomSheet(postId: String) {
        CommentsBottomSheetFragment
            .newInstance(postId)
            .show(childFragmentManager, "CommentsBottomSheet")
    }
    private fun showReportPostDialog(post: SocialPost) {
        ReportReasonDialogHelper.show(
            fragment = this,
            title = getString(R.string.report_title),
            subtitle = getString(R.string.report_post_subtitle),
            reasons = listOf(
                ReportReasonDialogHelper.ReasonItem(
                    key = "spam",
                    label = getString(R.string.report_reason_spam)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "harassment",
                    label = getString(R.string.report_reason_harassment)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "inappropriate_content",
                    label = getString(R.string.report_reason_inappropriate)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "other",
                    label = getString(R.string.report_reason_other)
                )
            )
        ) { selectedReason ->
            viewModel.reportPost(
                post = post,
                reason = selectedReason.key
            )

            Toast.makeText(
                requireContext(),
                getString(R.string.report_post_success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showProfileScreen(userId: String) {
        if (userId.isBlank()) return

        SocialProfileFragment
            .newInstance(userId)
            .show(childFragmentManager, "SocialProfileFragment")
    }

    private fun showMySocialProfile() {
        val myUserId = socialDependencies.repository.currentUserId()
        if (myUserId.isBlank()) return

        SocialProfileFragment
            .newInstance(myUserId)
            .show(childFragmentManager, "MySocialProfileFragment")
    }

    private fun setupClicks() {
        binding.socialFab.setOnClickListener {
            showCreatePostScreen()
        }

        binding.socialSearchButton.setOnClickListener {
            showSearchScreen()
        }

        binding.socialNotificationsButton.setOnClickListener {
            showNotificationsScreen()
        }

        binding.socialMessagesButton.setOnClickListener {
            showMessagesScreen()
        }
    }

    private fun showSearchScreen() {
        SocialSearchFragment()
            .show(childFragmentManager, "SocialSearchFragment")
    }

    private fun showMessagesScreen() {
        MessagesFragment()
            .show(childFragmentManager, "MessagesFragment")
    }

    private fun showNotificationsScreen() {
        NotificationsFragment()
            .show(childFragmentManager, "NotificationsFragment")
    }

    private fun showCreatePostScreen() {
        CreatePostFragment()
            .show(childFragmentManager, "CreatePostFragment")
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.posts.collect { posts ->
                    postAdapter.submitList(posts)
                }
            }
        }
    }

    fun openNotificationTarget(
        type: String?,
        targetId: String?,
        secondaryTargetId: String? = null
    ) {
        if (_binding == null) {
            view?.post { openNotificationTarget(type, targetId, secondaryTargetId) }
            return
        }

        when (type) {
            FittoNotification.TYPE_MESSAGE,
            FittoNotification.TYPE_GROUP_MESSAGE,
            FittoNotification.TYPE_GROUP_ADDED -> openThreadFromNotification(targetId)

            FittoNotification.TYPE_POST_LIKE,
            FittoNotification.TYPE_POST_COMMENT,
            FittoNotification.TYPE_COMMENT_REPLY,
            FittoNotification.TYPE_COMMENT_LIKE,
            FittoNotification.TYPE_FOLLOWING_POST -> if (!targetId.isNullOrBlank()) {
                SocialPostDetailFragment
                    .newInstance(targetId)
                    .show(childFragmentManager, "SocialPostDetailFragment")
            }

            FittoNotification.TYPE_FOLLOW,
            FittoNotification.TYPE_UNFOLLOW -> if (!targetId.isNullOrBlank()) {
                showProfileScreen(targetId)
            }

            else -> showNotificationsScreen()
        }
    }

    private fun openThreadFromNotification(threadId: String?) {
        if (threadId.isNullOrBlank()) {
            showMessagesScreen()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val thread = runCatching {
                socialDependencies.repository.getMessageThread(threadId)
            }.getOrNull()
            if (thread == null || _binding == null) {
                showMessagesScreen()
                return@launch
            }

            ChatDetailFragment
                .newInstance(thread)
                .show(childFragmentManager, "ChatDetailFragment")
        }
    }

    private fun observeMyProfileForHeader() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myProfile.collect { profile ->
                    headerAdapter.updateProfilePhoto(profile?.photoUrl)
                }
            }
        }
    }

    private fun observeUnreadMessagesCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unreadMessagesCount.collect { count ->
                    binding.socialMessagesBadgeText.isVisible = count > 0
                    binding.socialMessagesBadgeText.text = if (count > 99) "99+" else count.toString()
                }
            }
        }
    }

    private fun observeUnreadSocialNotificationsCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FittoNotificationStore.changeToken.collect {
                    updateNotificationsBadge()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::viewModel.isInitialized) {
            viewModel.loadMyProfile()
            viewModel.refreshFeed()
        }

        updateNotificationsBadge()
    }

    private fun updateNotificationsBadge() {
        if (_binding == null) return
        val count = FittoNotificationStore.getUnreadCount(requireContext())
        binding.socialNotificationsBadgeText.isVisible = count > 0
        binding.socialNotificationsBadgeText.text = if (count > 99) "99+" else count.toString()
    }

    override fun onDestroyView() {
        binding.socialPostsRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
