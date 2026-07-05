package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentSocialProfileBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlin.math.abs

class SocialProfileFragment : DialogFragment(R.layout.fragment_social_profile) {

    private var _binding: FragmentSocialProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SocialViewModel
    private lateinit var postsAdapter: SocialProfilePostAdapter

    private val profilePhotoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadProfilePhoto(uri)
        }
    }

    private val userId: String by lazy {
        requireArguments().getString(ARG_USER_ID).orEmpty()
    }

    private var selectedTab: SocialProfileContentTab = SocialProfileContentTab.POSTS
    private var latestPosts: List<SocialPost> = emptyList()
    private var latestDrafts: List<SocialPost> = emptyList()
    private var latestSavedPosts: List<SocialPost> = emptyList()

    private val isMyProfile: Boolean
        get() = requireContext()
            .fittoAppContainer
            .socialFeatureDependencies
            .repository
            .currentUserId() == userId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTab = savedInstanceState
            ?.getString(KEY_SELECTED_TAB)
            ?.let { value -> runCatching { SocialProfileContentTab.valueOf(value) }.getOrNull() }
            ?: SocialProfileContentTab.POSTS

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSocialProfileBinding.bind(view)
        viewModel = ViewModelProvider(
            this,
            requireContext().fittoAppContainer.socialFeatureDependencies.createViewModelFactory()
        )[SocialViewModel::class.java]

        binding.profileMessageButton.isVisible = false
        binding.profilePhotoEditButton.isVisible = isMyProfile
        binding.profileTopTitleText.setText(
            if (isMyProfile) R.string.social_my_profile else R.string.social_profile
        )

        setupRecyclerView()
        setupClicks()
        observeProfile()

        viewModel.loadMyProfile()
        viewModel.loadUserProfile(userId)
        viewModel.loadUserPosts(userId)

        if (isMyProfile) {
            viewModel.loadProfilePageContent()
        }

        updateContentTabs()
    }

    private fun setupRecyclerView() {
        postsAdapter = SocialProfilePostAdapter(
            onPostClick = { post ->
                openPostDetail(post)
            }
        )

        binding.profilePostsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = postsAdapter
            setHasFixedSize(false)
            setupSwipeTabs(this)
        }
    }

    private fun openPostDetail(post: SocialPost) {
        if (post.id.isBlank()) {
            Toast.makeText(
                requireContext(),
                R.string.social_post_not_found,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        SocialPostDetailFragment
            .newInstance(post.id)
            .show(childFragmentManager, "SocialPostDetailFragment")
    }

    private fun setupSwipeTabs(target: View) {
        var downX = 0f
        var downY = 0f

        target.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (isMyProfile && abs(dx) > SWIPE_THRESHOLD && abs(dx) > abs(dy) * 1.35f) {
                        if (dx < 0) {
                            selectTab(SocialProfileContentTab.SAVED)
                        } else {
                            selectTab(SocialProfileContentTab.POSTS)
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun setupClicks() = with(binding) {
        profileBackButton.setOnClickListener { dismiss() }

        profileActionButton.setOnClickListener {
            val selectedUser = viewModel.selectedUser.value ?: return@setOnClickListener
            val myProfile = viewModel.myProfile.value

            if (selectedUser.id == myProfile?.id) {
                showEditProfileDialog(selectedUser)
            } else {
                viewModel.toggleFollowSelectedUser()
            }
        }

        profileMessageButton.setOnClickListener {
            val selectedUser = viewModel.selectedUser.value ?: return@setOnClickListener
            val myProfile = viewModel.myProfile.value

            if (selectedUser.id == myProfile?.id) return@setOnClickListener

            profileMessageButton.isEnabled = false

            viewModel.startDirectMessage(
                targetUserId = selectedUser.id,
                onSuccess = onSuccess@ { thread: MessageThreadUiModel ->
                    if (_binding == null) return@onSuccess

                    profileMessageButton.isEnabled = true
                    ChatDetailFragment
                        .newInstance(thread)
                        .show(childFragmentManager, "ChatDetailFragment")
                },
                onFailure = onFailure@ { _: Throwable ->
                    if (_binding == null) return@onFailure

                    profileMessageButton.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.messages_new_chat_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        profileAvatarContainer.setOnClickListener {
            if (isMyProfile) showProfilePhotoDialog()
        }

        profilePhotoEditButton.setOnClickListener {
            showProfilePhotoDialog()
        }

        profileFollowersContainer.setOnClickListener {
            showFollowers()
        }

        profileFollowingContainer.setOnClickListener {
            showFollowing()
        }

        buttonSocialProfilePosts.setOnClickListener {
            selectTab(SocialProfileContentTab.POSTS)
        }

        buttonSocialProfileSaved.setOnClickListener {
            selectTab(SocialProfileContentTab.SAVED)
        }

        profileDraftsLinkText.setOnClickListener {
            SocialDraftsFragment()
                .show(parentFragmentManager, "SocialDraftsFragment")
        }
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedUser.collect { user ->
                    updateProfile(user)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profilePosts.collect { posts ->
                    latestPosts = posts
                    updateSelectedContent()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profilePagePosts.collect { posts ->
                    if (isMyProfile) {
                        latestPosts = posts
                        updateSelectedContent()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profilePageDrafts.collect { drafts ->
                    latestDrafts = drafts
                    updateSelectedContent()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profilePageSavedPosts.collect { posts ->
                    latestSavedPosts = posts
                    updateSelectedContent()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myProfile.collect {
                    updateActionButton()
                    updateContentTabs()
                }
            }
        }
    }

    private fun updateProfile(user: SocialUser?) = with(binding) {
        profileProgress.isVisible = user == null
        profileContentScroll.isVisible = user != null

        if (user == null) return@with

        val publicUserName = user.userName
            .takeIf { it.isNotBlank() }
            ?: user.email
                ?.substringBefore("@")
                ?.takeIf { it.isNotBlank() }
            ?: "fitto_user"

        profileUserNameText.text = publicUserName
        profileBioText.text = user.bio
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.social_no_bio)

        profileFollowersCountText.text = user.followersCount.toString()
        profileFollowingCountText.text = user.followingCount.toString()

        val initial = publicUserName.firstOrNull()?.uppercase() ?: "F"
        profileInitialText.text = initial

        val hasPhoto = !user.photoUrl.isNullOrBlank()
        profileAvatarImage.isVisible = hasPhoto
        profileInitialText.isVisible = !hasPhoto

        if (hasPhoto) {
            ProfilePhotoLoader.load(
                imageView = profileAvatarImage,
                photoUrl = user.photoUrl,
                placeholderResId = R.drawable.ic_profile_person,
                errorResId = R.drawable.ic_profile_person
            )
        } else {
            profileAvatarImage.setImageDrawable(null)
        }

        updateActionButton()
        updateContentTabs()
    }

    private fun selectTab(tab: SocialProfileContentTab) {
        if (!isMyProfile && tab != SocialProfileContentTab.POSTS) return
        if (selectedTab == tab) return

        selectedTab = tab
        updateSelectedContent()
    }

    private fun updateContentTabs() = with(binding) {
        val showSavedTab = isMyProfile
        profileSavedTabContainer.isVisible = showSavedTab

        if (!showSavedTab && selectedTab != SocialProfileContentTab.POSTS) {
            selectedTab = SocialProfileContentTab.POSTS
        }

        updateSelectedContent()
    }

    private fun updateSelectedContent() = with(binding) {
        profilePostsCountText.text = latestPosts.size.toString()

        updateTabButton(
            button = buttonSocialProfilePosts,
            indicator = profilePostsTabIndicator,
            selected = selectedTab == SocialProfileContentTab.POSTS
        )
        updateTabButton(
            button = buttonSocialProfileSaved,
            indicator = profileSavedTabIndicator,
            selected = selectedTab == SocialProfileContentTab.SAVED
        )

        profileDraftsLinkText.isVisible = isMyProfile && selectedTab == SocialProfileContentTab.POSTS
        if (profileDraftsLinkText.isVisible) {
            profileDraftsLinkText.text = getString(
                R.string.profile_drafts_link_count,
                latestDrafts.size
            )
        }

        val currentItems = when (selectedTab) {
            SocialProfileContentTab.POSTS -> latestPosts
            SocialProfileContentTab.SAVED -> latestSavedPosts
        }

        profileEmptyPostsText.isVisible = currentItems.isEmpty()
        profilePostsRecyclerView.isVisible = currentItems.isNotEmpty()

        profileEmptyPostsText.setText(
            when (selectedTab) {
                SocialProfileContentTab.POSTS -> R.string.profile_content_posts_empty
                SocialProfileContentTab.SAVED -> R.string.profile_content_saved_empty
            }
        )

        postsAdapter.submitList(currentItems)
    }

    private fun updateTabButton(
        button: ImageButton,
        indicator: View,
        selected: Boolean
    ) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (selected) R.color.fitto_green else R.color.fitto_text_secondary
        )
        button.imageTintList = ColorStateList.valueOf(color)
        indicator.isVisible = selected
    }

    private fun updateActionButton() = with(binding) {
        val selectedUser = viewModel.selectedUser.value
        val myProfile = viewModel.myProfile.value

        if (selectedUser == null) {
            profileMessageButton.isVisible = false
            return@with
        }

        val mine = selectedUser.id == myProfile?.id

        if (mine) {
            profileActionButton.text = getString(R.string.social_edit_profile)
            profileActionButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)
            profileActionButton.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            profileActionButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_green))
            profileActionButton.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_green))
            profileActionButton.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_green))
        } else {
            profileActionButton.text = if (selectedUser.isFollowedByMe) {
                getString(R.string.social_unfollow)
            } else {
                getString(R.string.social_follow)
            }
            profileActionButton.icon = null
            profileActionButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_green))
            profileActionButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            profileActionButton.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_green))
        }

        profileMessageButton.isVisible = !mine
        profileMessageButton.isEnabled = true
    }

    private fun showEditProfileDialog(user: SocialUser) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(14))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_card)
        }

        val title = TextView(requireContext()).apply {
            setText(R.string.social_edit_profile)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        container.addView(title)

        val nameInput = createInputBlock(
            labelResId = R.string.profile_edit_display_name,
            hintResId = R.string.profile_edit_display_name_hint,
            maxLines = 1
        )
        nameInput.editText.setText(
            user.userName.ifBlank { user.displayName.orEmpty() }
        )

        val bioInput = createInputBlock(
            labelResId = R.string.profile_edit_bio,
            hintResId = R.string.profile_edit_bio_hint,
            maxLines = 4
        )
        bioInput.editText.setText(user.bio.orEmpty())

        container.addView(nameInput.root, marginTop = 18)
        container.addView(bioInput.root, marginTop = 14)

        val buttons = LinearLayout(requireContext()).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        val cancelButton = MaterialButton(requireContext()).apply {
            setText(R.string.profile_delete_account_cancel)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_subtext))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            elevation = 0f
            isAllCaps = false
        }

        val saveButton = MaterialButton(requireContext()).apply {
            setText(R.string.profile_account_save_changes)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_green))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            elevation = 0f
            isAllCaps = false
        }

        buttons.addView(cancelButton)
        buttons.addView(saveButton)
        container.addView(buttons, marginTop = 18)

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val displayName = nameInput.editText.text?.toString().orEmpty().trim()
            val bio = bioInput.editText.text?.toString().orEmpty().trim()

            if (displayName.length < 3) {
                nameInput.inputLayout.error = getString(R.string.profile_edit_display_name_error)
                return@setOnClickListener
            }

            nameInput.inputLayout.error = null
            saveButton.isEnabled = false

            viewModel.updateCurrentUserProfile(
                displayName = displayName,
                bio = bio,
                onSuccess = {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), R.string.profile_edit_saved, Toast.LENGTH_SHORT).show()
                        notifyProfileUpdated()
                        dialog.dismiss()
                    }
                },
                onFailure = {
                    if (_binding != null) {
                        saveButton.isEnabled = true
                        Toast.makeText(requireContext(), R.string.profile_edit_error, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createInputBlock(
        labelResId: Int,
        hintResId: Int,
        maxLines: Int
    ): InputBlock {
        val wrapper = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val label = TextView(requireContext()).apply {
            setText(labelResId)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            includeFontPadding = false
        }

        val inputLayout = TextInputLayout(requireContext()).apply {
            isHintEnabled = false
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(16f, 16f, 16f, 16f)
        }

        val editText = TextInputEditText(requireContext()).apply {
            setHint(hintResId)
            inputType = InputType.TYPE_CLASS_TEXT or if (maxLines > 1) {
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else {
                InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            this.maxLines = maxLines
            minLines = if (maxLines > 1) 3 else 1
            isSingleLine = maxLines == 1
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
            setHintTextColor(withAlpha(ContextCompat.getColor(requireContext(), R.color.profile_subtext), 0.55f))
        }

        inputLayout.addView(editText)
        wrapper.addView(label)
        wrapper.addView(
            inputLayout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(7) }
        )

        return InputBlock(
            root = wrapper,
            inputLayout = inputLayout,
            editText = editText
        )
    }

    private fun showProfilePhotoDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(14))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_card)
        }

        val title = TextView(requireContext()).apply {
            setText(R.string.profile_photo_title)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        container.addView(title)

        val avatarGrid = GridLayout(requireContext()).apply {
            columnCount = 4
            rowCount = 2
        }

        avatarOptions().forEach { option ->
            val image = ImageView(requireContext()).apply {
                if (option.isEmpty) {
                    setImageResource(R.drawable.ic_profile_person)
                    alpha = 0.72f
                    setPadding(dp(13), dp(13), dp(13), dp(13))
                    contentDescription = getString(R.string.profile_photo_remove)
                } else {
                    setImageResource(option.resId)
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    contentDescription = getString(R.string.profile_photo_change)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_primary_soft)
                clipToOutline = true
                setOnClickListener {
                    dialog.dismiss()
                    if (option.isEmpty) {
                        removeProfilePhoto()
                    } else {
                        setAvatarPhoto(option)
                    }
                }
            }

            avatarGrid.addView(
                image,
                GridLayout.LayoutParams().apply {
                    width = dp(62)
                    height = dp(62)
                    setMargins(dp(5), dp(8), dp(5), dp(4))
                }
            )
        }

        container.addView(avatarGrid, marginTop = 10)

        val galleryButton = createPhotoActionButton(
            textResId = R.string.profile_photo_from_gallery,
            iconResId = R.drawable.ic_image
        )
        galleryButton.setOnClickListener {
            dialog.dismiss()
            profilePhotoPicker.launch("image/*")
        }

        container.addView(galleryButton, marginTop = 14)

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createPhotoActionButton(
        textResId: Int,
        iconResId: Int
    ): MaterialButton {
        return MaterialButton(requireContext()).apply {
            setText(textResId)
            icon = ContextCompat.getDrawable(requireContext(), iconResId)
            iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_green))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_green))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_divider_soft))
            strokeWidth = dp(1)
            cornerRadius = dp(16)
            isAllCaps = false
        }
    }

    private fun setAvatarPhoto(option: AvatarOption) {
        if (option.isEmpty) {
            removeProfilePhoto()
            return
        }

        viewModel.updateCurrentUserPhotoUrl(
            photoUrl = "android.resource://${requireContext().packageName}/drawable/${option.name}",
            onSuccess = {
                if (_binding != null) {
                    notifyProfileUpdated()
                    Toast.makeText(requireContext(), R.string.profile_photo_updated, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = {
                if (_binding != null) {
                    Toast.makeText(requireContext(), R.string.profile_photo_error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uploadProfilePhoto(uri: Uri) {
        Toast.makeText(requireContext(), R.string.profile_photo_uploading, Toast.LENGTH_SHORT).show()
        viewModel.uploadAndSetCurrentUserPhoto(
            context = requireContext().applicationContext,
            imageUri = uri,
            onSuccess = {
                if (_binding != null) {
                    notifyProfileUpdated()
                    Toast.makeText(requireContext(), R.string.profile_photo_updated, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = {
                if (_binding != null) {
                    Toast.makeText(requireContext(), R.string.profile_photo_error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun removeProfilePhoto() {
        viewModel.updateCurrentUserPhotoUrl(
            photoUrl = null,
            onSuccess = {
                if (_binding != null) {
                    notifyProfileUpdated()
                    Toast.makeText(requireContext(), R.string.profile_photo_removed, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = {
                if (_binding != null) {
                    Toast.makeText(requireContext(), R.string.profile_photo_error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun notifyProfileUpdated() {
        parentFragmentManager.setFragmentResult(
            PROFILE_UPDATED_RESULT_KEY,
            bundleOf(PROFILE_UPDATED_PHOTO_CHANGED_KEY to true)
        )
    }

    private fun showFollowers() {
        val user = viewModel.selectedUser.value ?: return
        viewModel.loadFollowers(
            userId = user.id,
            onSuccess = { users ->
                if (_binding != null) {
                    showUserListDialog(
                        title = getString(R.string.social_followers),
                        users = users,
                        emptyText = getString(R.string.profile_followers_empty),
                        mode = FollowListMode.FOLLOWERS
                    )
                }
            },
            onFailure = {
                if (_binding != null) Toast.makeText(requireContext(), R.string.profile_follow_list_error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showFollowing() {
        val user = viewModel.selectedUser.value ?: return
        viewModel.loadFollowing(
            userId = user.id,
            onSuccess = { users ->
                if (_binding != null) {
                    showUserListDialog(
                        title = getString(R.string.social_following),
                        users = users,
                        emptyText = getString(R.string.profile_following_empty),
                        mode = FollowListMode.FOLLOWING
                    )
                }
            },
            onFailure = {
                if (_binding != null) Toast.makeText(requireContext(), R.string.profile_follow_list_error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showUserListDialog(
        title: String,
        users: List<SocialUser>,
        emptyText: String,
        mode: FollowListMode
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val currentUsers = users.toMutableList()

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(18))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_card)
        }

        container.addView(TextView(requireContext()).apply {
            text = title
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })

        val listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun renderList() {
            listContainer.removeAllViews()

            if (currentUsers.isEmpty()) {
                listContainer.addView(TextView(requireContext()).apply {
                    text = emptyText
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_subtext))
                    textSize = 14f
                    setPadding(0, dp(30), 0, dp(30))
                })
            } else {
                currentUsers.forEach { user ->
                    listContainer.addView(
                        createUserRow(
                            dialog = dialog,
                            user = user,
                            mode = mode,
                            onRemoved = { removedUser ->
                                currentUsers.removeAll { it.id == removedUser.id }
                                renderList()
                            }
                        )
                    )
                }
            }
        }

        renderList()

        val scroll = ScrollView(requireContext()).apply {
            addView(listContainer)
        }
        container.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(380)
            ).apply { topMargin = dp(12) }
        )

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createUserRow(
        dialog: Dialog,
        user: SocialUser,
        mode: FollowListMode,
        onRemoved: (SocialUser) -> Unit
    ): View {
        val row = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(9), dp(8), dp(9))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_menu_item)
            isClickable = true
        }

        val avatar = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            ProfilePhotoLoader.load(
                imageView = this,
                photoUrl = user.photoUrl,
                placeholderResId = R.drawable.ic_profile_person,
                errorResId = R.drawable.ic_profile_person
            )
        }

        row.addView(avatar, LinearLayout.LayoutParams(dp(44), dp(44)).apply {
            marginStart = dp(10)
            marginEnd = dp(12)
        })

        val name = TextView(requireContext()).apply {
            text = user.userName.ifBlank { user.displayName.orEmpty().ifBlank { "fitto_user" } }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        row.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        if (isMyProfile && user.id.isNotBlank()) {
            val removeButton = MaterialButton(requireContext()).apply {
                text = when (mode) {
                    FollowListMode.FOLLOWERS -> getString(R.string.common_remove)
                    FollowListMode.FOLLOWING -> getString(R.string.social_unfollow)
                }
                setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_green))
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.fitto_divider_soft))
                strokeWidth = dp(1)
                cornerRadius = dp(14)
                minHeight = 0
                minimumHeight = 0
                insetTop = 0
                insetBottom = 0
                isAllCaps = false
                textSize = 12f
                setPadding(dp(10), 0, dp(10), 0)
            }

            removeButton.setOnClickListener {
                removeButton.isEnabled = false
                val onSuccess = {
                    if (_binding != null) {
                        onRemoved(user)
                        viewModel.loadMyProfile()
                        viewModel.loadUserProfile(userId)
                        Toast.makeText(
                            requireContext(),
                            when (mode) {
                                FollowListMode.FOLLOWERS -> R.string.profile_follow_removed
                                FollowListMode.FOLLOWING -> R.string.profile_following_removed
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                val onFailure = { _: Throwable ->
                    if (_binding != null) {
                        removeButton.isEnabled = true
                        Toast.makeText(requireContext(), R.string.profile_follow_list_error, Toast.LENGTH_SHORT).show()
                    }
                }

                when (mode) {
                    FollowListMode.FOLLOWERS -> viewModel.removeFollower(user.id, onSuccess, onFailure)
                    FollowListMode.FOLLOWING -> viewModel.stopFollowing(user.id, onSuccess, onFailure)
                }
            }

            row.addView(
                removeButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(34)
                ).apply { marginStart = dp(8) }
            )
        }

        row.setOnClickListener {
            dialog.dismiss()
            if (user.id.isNotBlank()) {
                SocialProfileFragment
                    .newInstance(user.id)
                    .show(parentFragmentManager, "social_profile_${user.id}")
            }
        }

        return row
    }

    private fun avatarOptions(): List<AvatarOption> {
        return listOf(
            AvatarOption(name = null, resId = 0, isEmpty = true),
            AvatarOption("profile_avatar_01", R.drawable.profile_avatar_01),
            AvatarOption("profile_avatar_02", R.drawable.profile_avatar_02),
            AvatarOption("profile_avatar_03", R.drawable.profile_avatar_03),
            AvatarOption("profile_avatar_04", R.drawable.profile_avatar_04),
            AvatarOption("profile_avatar_05", R.drawable.profile_avatar_05),
            AvatarOption("profile_avatar_06", R.drawable.profile_avatar_06),
            AvatarOption("profile_avatar_07", R.drawable.profile_avatar_07)
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        return Color.argb(
            (Color.alpha(color) * alpha.coerceIn(0f, 1f)).toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun LinearLayout.addView(view: View, marginTop: Int) {
        addView(
            view,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(marginTop) }
        )
    }

    override fun onDestroyView() {
        binding.profilePostsRecyclerView.adapter = null
        viewModel.clearSelectedUser()
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SELECTED_TAB, selectedTab.name)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val KEY_SELECTED_TAB = "social_profile_selected_tab"
        private const val SWIPE_THRESHOLD = 90
        private const val PROFILE_UPDATED_RESULT_KEY = "social_profile_updated"
        private const val PROFILE_UPDATED_PHOTO_CHANGED_KEY = "photo_changed"

        fun newInstance(userId: String): SocialProfileFragment {
            return SocialProfileFragment().apply {
                arguments = bundleOf(ARG_USER_ID to userId)
            }
        }
    }

    private data class AvatarOption(
        val name: String?,
        val resId: Int,
        val isEmpty: Boolean = false
    )

    private data class InputBlock(
        val root: View,
        val inputLayout: TextInputLayout,
        val editText: EditText
    )

    private enum class FollowListMode {
        FOLLOWERS,
        FOLLOWING
    }

    private enum class SocialProfileContentTab {
        POSTS,
        SAVED
    }
}
