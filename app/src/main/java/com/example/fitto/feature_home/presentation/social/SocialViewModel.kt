package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

class SocialViewModel internal constructor(
    private val repository: SocialRepository
) : ViewModel() {
    private val _sharedContentSummary = MutableStateFlow(ThreadSharedContentSummary())
    val sharedContentSummary: StateFlow<ThreadSharedContentSummary> = _sharedContentSummary.asStateFlow()
    private val _posts = MutableStateFlow<List<SocialPost>>(emptyList())
    val posts: StateFlow<List<SocialPost>> = _posts

    private val _comments = MutableStateFlow<List<SocialComment>>(emptyList())
    val comments: StateFlow<List<SocialComment>> = _comments

    private val _selectedUser = MutableStateFlow<SocialUser?>(null)
    val selectedUser: StateFlow<SocialUser?> = _selectedUser

    private val _profilePosts = MutableStateFlow<List<SocialPost>>(emptyList())
    val profilePosts: StateFlow<List<SocialPost>> = _profilePosts

    private val _myProfile = MutableStateFlow<SocialUser?>(null)
    val myProfile: StateFlow<SocialUser?> = _myProfile
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _myPosts = MutableStateFlow<List<SocialPost>>(emptyList())
    val myPosts: StateFlow<List<SocialPost>> = _myPosts

    private val _profilePagePosts = MutableStateFlow<List<SocialPost>>(emptyList())
    val profilePagePosts: StateFlow<List<SocialPost>> = _profilePagePosts

    private val _profilePageDrafts = MutableStateFlow<List<SocialPost>>(emptyList())
    val profilePageDrafts: StateFlow<List<SocialPost>> = _profilePageDrafts

    private val _profilePageSavedPosts = MutableStateFlow<List<SocialPost>>(emptyList())
    val profilePageSavedPosts: StateFlow<List<SocialPost>> = _profilePageSavedPosts

    private val _postDetail = MutableStateFlow<SocialPost?>(null)
    val postDetail: StateFlow<SocialPost?> = _postDetail

    private val _editingDraft = MutableStateFlow<SocialPost?>(null)
    val editingDraft: StateFlow<SocialPost?> = _editingDraft

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _isCommentSending = MutableStateFlow(false)
    val isCommentSending: StateFlow<Boolean> = _isCommentSending

    private val _unreadMessagesCount = MutableStateFlow(0)
    val unreadMessagesCount: StateFlow<Int> = _unreadMessagesCount

    private val _unreadSocialNotificationsCount = MutableStateFlow(0)
    val unreadSocialNotificationsCount: StateFlow<Int> = _unreadSocialNotificationsCount

    private var commentsJob: Job? = null
    private var profilePostsJob: Job? = null
    private var myPostsJob: Job? = null
    private var profilePagePostsJob: Job? = null
    private var profilePageDraftsJob: Job? = null
    private var profilePageSavedPostsJob: Job? = null
    private var postDetailJob: Job? = null

    init {
        observeFeed()
        observeUnreadMessageCount()
        observeUnreadSocialNotificationCount()
        createCurrentUserProfileIfNeeded()
    }

    private fun observeFeed() {
        viewModelScope.launch {
            repository.observeFeed().collect { posts ->
                _posts.value = posts
            }
        }
    }
    fun refreshFeed() {
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching {
                repository.refreshFeedFromServer()
            }
            _isRefreshing.value = false
        }
    }
    private fun observeUnreadMessageCount() {
        viewModelScope.launch {
            repository.observeUnreadMessageCount().collect { count: Int ->
                _unreadMessagesCount.value = count
            }
        }
    }

    private fun observeUnreadSocialNotificationCount() {
        viewModelScope.launch {
            repository.observeUnreadSocialNotificationCount().collect { count: Int ->
                _unreadSocialNotificationsCount.value = count
            }
        }
    }

    private fun createCurrentUserProfileIfNeeded() {
        viewModelScope.launch {
            repository.createCurrentUserProfileIfNeeded()
            loadMyProfile()
        }
    }

    fun loadMyProfile() {
        viewModelScope.launch {
            _myProfile.value = repository.getCurrentUserProfile()
        }
    }
    fun observeSharedContent(threadId: String) {
        viewModelScope.launch {
            repository.observeThreadSharedContentSummary(threadId).collect { summary ->
                _sharedContentSummary.value = summary
            }
        }
    }
    fun loadProfilePageContent() {
        profilePagePostsJob?.cancel()
        profilePageDraftsJob?.cancel()
        profilePageSavedPostsJob?.cancel()

        profilePagePostsJob = viewModelScope.launch {
            repository.observeMyProfilePosts().collect { posts ->
                _profilePagePosts.value = posts
            }
        }

        profilePageDraftsJob = viewModelScope.launch {
            repository.observeMyDraftPosts().collect { drafts ->
                _profilePageDrafts.value = drafts
            }
        }

        profilePageSavedPostsJob = viewModelScope.launch {
            repository.observeSavedPosts().collect { posts ->
                _profilePageSavedPosts.value = posts
            }
        }
    }

    fun loadMyPosts() {
        myPostsJob?.cancel()

        myPostsJob = viewModelScope.launch {
            val myId = repository.getCurrentUserProfile()?.id.orEmpty()

            repository.observeFeed().collect { posts ->
                _myPosts.value = posts.filter { it.userId == myId }
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _selectedUser.value = repository.getUserProfile(userId)
        }
    }

    fun clearSelectedUser() {
        _selectedUser.value = null
        _profilePosts.value = emptyList()
        profilePostsJob?.cancel()
        profilePostsJob = null
    }

    fun loadUserPosts(userId: String) {
        profilePostsJob?.cancel()

        profilePostsJob = viewModelScope.launch {
            repository.observeFeed().collect { posts ->
                _profilePosts.value = posts.filter { it.userId == userId }
            }
        }
    }

    fun toggleFollowSelectedUser() {
        val user = _selectedUser.value ?: return

        viewModelScope.launch {
            repository.toggleFollow(user)
            _selectedUser.value = repository.getUserProfile(user.id)
            loadMyProfile()
        }
    }

    fun startDirectMessage(
        targetUserId: String,
        onSuccess: (MessageThreadUiModel) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.startDirectMessage(targetUserId)
            }.onSuccess { thread: MessageThreadUiModel ->
                onSuccess(thread)
            }.onFailure { error: Throwable ->
                onFailure(error)
            }
        }
    }

    fun observeComments(postId: String) {
        commentsJob?.cancel()

        commentsJob = viewModelScope.launch {
            repository.observeComments(postId).collect { comments ->
                _comments.value = comments
            }
        }
    }

    fun clearComments() {
        commentsJob?.cancel()
        commentsJob = null
        _comments.value = emptyList()
    }


    fun updateCurrentUserProfile(
        displayName: String,
        bio: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateCurrentUserProfile(
                    displayName = displayName,
                    bio = bio
                )
            }.onSuccess {
                loadMyProfile()
                _selectedUser.value = repository.getCurrentUserProfile()
                onSuccess()
            }.onFailure { error ->
                onFailure(error)
            }
        }
    }

    fun updateCurrentUserPhotoUrl(
        photoUrl: String?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (_isUploading.value) return

        viewModelScope.launch {
            _isUploading.value = true

            runCatching {
                repository.updateCurrentUserPhotoUrl(photoUrl)
            }.onSuccess {
                applyCurrentUserPhotoLocally(photoUrl)
                loadMyProfile()
                _selectedUser.value = repository.getCurrentUserProfile()
                onSuccess()
            }.onFailure { error ->
                onFailure(error)
            }

            _isUploading.value = false
        }
    }

    fun uploadAndSetCurrentUserPhoto(
        context: Context,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (_isUploading.value) return

        viewModelScope.launch {
            _isUploading.value = true

            runCatching {
                val uploadedUrl = repository.uploadCurrentUserProfilePhoto(
                    context = context,
                    imageUri = imageUri
                )
                repository.updateCurrentUserPhotoUrl(uploadedUrl)
                uploadedUrl
            }.onSuccess { uploadedUrl ->
                applyCurrentUserPhotoLocally(uploadedUrl)
                loadMyProfile()
                _selectedUser.value = repository.getCurrentUserProfile()
                onSuccess()
            }.onFailure { error ->
                onFailure(error)
            }

            _isUploading.value = false
        }
    }

    private fun applyCurrentUserPhotoLocally(photoUrl: String?) {
        val currentUserId = repository.currentUserId()
        if (currentUserId.isBlank()) return

        _myProfile.value = _myProfile.value?.copy(photoUrl = photoUrl)

        _selectedUser.value = _selectedUser.value?.let { user ->
            if (user.id == currentUserId) user.copy(photoUrl = photoUrl) else user
        }

        _posts.value = _posts.value.map { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _myPosts.value = _myPosts.value.map { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _profilePosts.value = _profilePosts.value.map { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _profilePagePosts.value = _profilePagePosts.value.map { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _profilePageDrafts.value = _profilePageDrafts.value.map { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _profilePageSavedPosts.value = _profilePageSavedPosts.value.map { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _postDetail.value = _postDetail.value?.let { post ->
            if (post.userId == currentUserId) post.copy(userPhotoUrl = photoUrl) else post
        }

        _comments.value = _comments.value.map { comment ->
            if (comment.userId == currentUserId) comment.copy(userPhotoUrl = photoUrl) else comment
        }
    }

    fun loadFollowers(
        userId: String,
        onSuccess: (List<SocialUser>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.getFollowers(userId)
            }.onSuccess { users ->
                onSuccess(users)
            }.onFailure { error ->
                onFailure(error)
            }
        }
    }

    fun loadFollowing(
        userId: String,
        onSuccess: (List<SocialUser>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.getFollowing(userId)
            }.onSuccess { users ->
                onSuccess(users)
            }.onFailure { error ->
                onFailure(error)
            }
        }
    }

    fun removeFollower(
        followerId: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.removeFollower(followerId)
            }.onSuccess {
                loadMyProfile()
                _selectedUser.value = repository.getCurrentUserProfile()
                onSuccess()
            }.onFailure { error ->
                onFailure(error)
            }
        }
    }

    fun stopFollowing(
        followingId: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                repository.stopFollowing(followingId)
            }.onSuccess {
                loadMyProfile()
                _selectedUser.value = repository.getCurrentUserProfile()
                onSuccess()
            }.onFailure { error ->
                onFailure(error)
            }
        }
    }


    fun observePostDetail(postId: String) {
        postDetailJob?.cancel()
        _postDetail.value = null

        postDetailJob = viewModelScope.launch {
            repository.observePost(postId).collect { post ->
                _postDetail.value = post
            }
        }
    }

    fun loadDraftForEditing(draftId: String) {
        viewModelScope.launch {
            _editingDraft.value = repository.getPostById(draftId)
        }
    }

    fun clearEditingDraft() {
        _editingDraft.value = null
    }

    fun updateDraftPost(
        draftId: String,
        content: String,
        imageUri: Uri?,
        existingImageUrl: String?,
        mealInfo: String?,
        calories: Int?,
        allowComments: Boolean,
        visibility: String,
        tags: List<String>,
        publish: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true

            runCatching {
                repository.updateDraftPost(
                    draftId = draftId,
                    content = content,
                    imageUri = imageUri,
                    existingImageUrl = existingImageUrl,
                    mealInfo = mealInfo,
                    calories = calories,
                    allowComments = allowComments,
                    visibility = visibility,
                    tags = tags,
                    publish = publish
                )
            }.onSuccess {
                onSuccess()
            }.onFailure { error ->
                error.printStackTrace()
            }

            _isUploading.value = false
        }
    }

    fun createPost(
        content: String,
        imageUri: Uri?,
        mealInfo: String?,
        calories: Int?,
        allowComments: Boolean,
        visibility: String,
        tags: List<String>,
        isDraft: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true

            runCatching {
                repository.createPost(
                    content = content,
                    imageUri = imageUri,
                    mealInfo = mealInfo,
                    calories = calories,
                    allowComments = allowComments,
                    visibility = visibility,
                    tags = tags,
                    isDraft = isDraft
                )
            }.onSuccess {
                onSuccess()
            }.onFailure { error ->
                error.printStackTrace()
            }

            _isUploading.value = false
        }
    }

    fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null
    ) {
        if (_isCommentSending.value) return

        _isCommentSending.value = true

        viewModelScope.launch {
            runCatching {
                repository.addComment(
                    postId = postId,
                    text = text,
                    parentCommentId = parentCommentId
                )
            }.onSuccess {
                _isCommentSending.value = false
            }.onFailure {
                _isCommentSending.value = false
            }
        }
    }

    fun toggleCommentLike(
        postId: String,
        comment: SocialComment
    ) {
        viewModelScope.launch {
            runCatching {
                repository.toggleCommentLike(
                    postId = postId,
                    comment = comment
                )
            }
        }
    }

    fun toggleLike(post: SocialPost) {
        viewModelScope.launch {
            runCatching {
                repository.toggleLike(post)
            }
        }
    }

    fun toggleSave(post: SocialPost) {
        viewModelScope.launch {
            runCatching {
                repository.toggleSave(post)
            }
        }
    }

    fun reportPost(
        post: SocialPost,
        reason: String
    ) {
        viewModelScope.launch {
            repository.reportPost(
                postId = post.id,
                reason = reason
            )
        }
    }

    override fun onCleared() {
        commentsJob?.cancel()
        profilePostsJob?.cancel()
        myPostsJob?.cancel()
        profilePagePostsJob?.cancel()
        profilePageDraftsJob?.cancel()
        profilePageSavedPostsJob?.cancel()
        super.onCleared()
    }
}
