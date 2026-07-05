package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentChatDetailBinding
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import androidx.appcompat.widget.PopupMenu
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
class ChatDetailFragment : DialogFragment(R.layout.fragment_chat_detail) {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().fittoAppContainer.socialFeatureDependencies.repository
    }
    private lateinit var messageAdapter: ChatMessageAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val messageComposerController = ChatMessageComposerController()
    private val conversationAccessController by lazy(LazyThreadSafetyMode.NONE) {
        ChatConversationAccessController(isGroup = isCurrentGroupChat)
    }
    private val headerPresentationController by lazy(LazyThreadSafetyMode.NONE) {
        ChatHeaderPresentationController(
            isGroup = isGroup,
            memberCount = memberCount,
            initialTitle = threadTitle,
            initialInitials = threadInitials,
            initialPhotoUrl = threadPhotoUrl
        )
    }
    private lateinit var mediaController: ChatMediaController
    private var _headerRenderer: ChatHeaderRenderer? = null
    private val headerRenderer get() = _headerRenderer!!
    private var _conversationAccessRenderer: ChatConversationAccessRenderer? = null
    private val conversationAccessRenderer get() = _conversationAccessRenderer!!
    private val messagePresentationController by lazy(LazyThreadSafetyMode.NONE) {
        ChatMessagePresentationController(highlightMessageId)
    }
    private val conversationStateStore = ChatConversationStateStore()
    private val stopTypingRunnable = Runnable {
        repository.setTyping(threadId, false)
    }
    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                toggleVoiceRecording()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_voice_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                sendFileMessage(uri)
            }
        }
    private val typingAnimationRunnable = object : Runnable {
        override fun run() {
            val presentation = headerPresentationController.onTypingAnimationTick()
            if (_binding != null) {
                headerRenderer.renderSubtitle(presentation.subtitle)
            }
            if (presentation.shouldAnimateTyping) {
                handler.postDelayed(this, TYPING_ANIMATION_DELAY_MILLIS)
            }
        }
    }

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
    }

    private val threadTitle: String by lazy {
        requireArguments().getString(ARG_THREAD_TITLE).orEmpty()
    }
    private val highlightMessageId: String? by lazy {
        requireArguments().getString(ARG_HIGHLIGHT_MESSAGE_ID)
    }
    private val threadInitials: String by lazy {
        requireArguments().getString(ARG_THREAD_INITIALS).orEmpty()
    }

    private val isGroup: Boolean by lazy {
        requireArguments().getBoolean(ARG_IS_GROUP, false)
    }

    private val isCurrentGroupChat: Boolean by lazy {
        isGroup || threadId.startsWith("group_")
    }

    private val otherParticipantId: String? by lazy {
        requireArguments().getString(ARG_OTHER_PARTICIPANT_ID)
    }

    private val threadPhotoUrl: String? by lazy {
        requireArguments().getString(ARG_THREAD_PHOTO_URL)
    }

    private val memberCount: Int by lazy {
        requireArguments().getInt(ARG_MEMBER_COUNT, 0)
    }

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
        requireContext().fittoAppContainer.socialFeatureDependencies.startRealtimePresence()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onStop() {
        repository.setTyping(threadId, false)
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentChatDetailBinding.bind(view)
        _headerRenderer = ChatHeaderRenderer(binding)
        _conversationAccessRenderer = ChatConversationAccessRenderer(binding)
        mediaController = createMediaController()
        ensureInitialConversationState()

        setupHeader()
        setupRecyclerView()
        setupInput()
        setupClicks()
        observeMessages()
        observePresence()
        observeTypingUsers()
        markThreadAsRead()
        renderComposerUiState()
        observeBlockState()
        checkDeletedDirectUserState()
        setupDeleteMessageResultListener()
        parentFragmentManager.setFragmentResultListener(
            GroupEditNameFragment.REQUEST_KEY_GROUP_NAME_UPDATED,
            viewLifecycleOwner
        ) { _, bundle ->
            val updatedName = bundle.getString(GroupEditNameFragment.BUNDLE_KEY_GROUP_NAME).orEmpty()

            if (updatedName.isNotBlank()) {
                val presentation = headerPresentationController.updateTitle(updatedName)
                if (selectedMessage == null) {
                    headerRenderer.renderTitle(presentation)
                }
                conversationStateStore.updateThreadTitle(updatedName)
            }
        }
        parentFragmentManager.setFragmentResultListener(
            GroupInfoFragment.REQUEST_KEY_GROUP_LEFT,
            viewLifecycleOwner
        ) { _, bundle ->
            val leftThreadId = bundle.getString(GroupInfoFragment.BUNDLE_KEY_LEFT_THREAD_ID).orEmpty()
            if (leftThreadId == threadId) {
                dismiss()
            }
        }
        parentFragmentManager.setFragmentResultListener(
            GroupInfoFragment.REQUEST_KEY_GROUP_PHOTO_UPDATED,
            viewLifecycleOwner
        ) { _, bundle ->
            val updatedThreadId = bundle.getString(GroupInfoFragment.BUNDLE_KEY_PHOTO_THREAD_ID).orEmpty()
            val updatedPhotoUrl = bundle.getString(GroupInfoFragment.BUNDLE_KEY_GROUP_PHOTO_URL)

            if (updatedThreadId == threadId) {
                val presentation = headerPresentationController.updatePhoto(updatedPhotoUrl)
                conversationStateStore.updateThreadPhoto(updatedPhotoUrl)
                headerRenderer.renderAvatar(presentation)
            }
        }
    }

    private fun ensureInitialConversationState() {
        if (conversationStateStore.state.thread != null) return

        conversationStateStore.updateThread(
            MessageThreadUiModel(
                id = threadId,
                title = threadTitle,
                lastMessage = "",
                timeText = "",
                isGroup = isCurrentGroupChat,
                initials = threadInitials,
                unreadCount = 0,
                participantIds = emptyList(),
                otherParticipantId = otherParticipantId,
                photoUrl = threadPhotoUrl,
                memberCount = memberCount
            )
        )
    }

    private fun setupDeleteMessageResultListener() {
        parentFragmentManager.setFragmentResultListener(
            DeleteMessageOptionsBottomSheet.REQUEST_KEY_DELETE_MESSAGE_ACTION,
            viewLifecycleOwner
        ) { _, bundle ->
            val deleteForEveryone = bundle.getBoolean(
                DeleteMessageOptionsBottomSheet.BUNDLE_KEY_DELETE_FOR_EVERYONE,
                false
            )

            deleteSelectedMessage(deleteForEveryone)
        }
    }
    private fun setupHeader() = with(binding) {
        val presentation = headerPresentationController.currentPresentation
        headerRenderer.renderIdentity(presentation)
        headerRenderer.renderStatus(presentation)

        chatSubtitleText.isVisible = true
        chatInfoButton.isVisible = false
        chatDeleteButton.isVisible = false
        updateMoreButtonVisibility()
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter(
            onMessageLongClick = { message ->
                selectMessage(message)
            },
            onMessageClick = { message ->
                toggleMessageSelection(message)
            },
            onAttachmentClick = { message ->
                openAttachment(message)
            },
            onAudioClick = { url ->
                playOrStopAudio(url)
            }
        )

        binding.chatMessagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupInput() = with(binding) {
        chatMessageEditText.doAfterTextChanged { editable ->
            val textChange = messageComposerController.onTextChanged(editable)
            renderComposerUiState(textChange.uiState)

            repository.setTyping(threadId, textChange.isTyping)
            handler.removeCallbacks(stopTypingRunnable)
            if (textChange.isTyping) {
                handler.postDelayed(stopTypingRunnable, STOP_TYPING_DELAY_MILLIS)
            }
        }

        chatMessageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() = with(binding) {
        chatBackButton.setOnClickListener {
            if (selectedMessage != null) {
                clearSelectedMessage()
            } else {
                dismiss()
            }
        }
        chatMoreButton.setOnClickListener { view ->
            if (!isCurrentGroupChat) {
                showChatMoreMenu(view)
            }
        }
        chatVoiceButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                toggleVoiceRecording()
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        chatInfoButton.setOnClickListener {
            showSelectedMessageInfo()
        }

        chatDeleteButton.setOnClickListener {
            showDeleteMessageDialog()
        }

        chatAttachButton.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        chatTitleContainer.setOnClickListener {
            if (selectedMessage != null) return@setOnClickListener

            if (isGroup) {
                openGroupInfoScreen()
            } else {
                openDirectUserSocialProfile()
            }
        }

        chatAvatarContainer.setOnClickListener {
            if (selectedMessage != null) return@setOnClickListener

            if (isGroup) {
                openGroupInfoScreen()
            } else {
                openDirectUserSocialProfile()
            }
        }

        chatSendButton.setOnClickListener {
            sendMessage()
        }
    }
    private fun openMediaLinksFilesScreen() {
        ThreadSharedContentFragment
            .newInstance(threadId)
            .show(parentFragmentManager, "ThreadSharedContentFragment")
    }
    private fun openOtherUserProfile() {
        val userId = otherParticipantId ?: return

        Toast.makeText(
            requireContext(),
            "Profil açılacak: $userId",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showClearChatDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.FittoAlertDialogTheme)
            .setTitle(R.string.chat_clear_chat)
            .setMessage(R.string.chat_clear_chat_confirm)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.chat_clear_chat) { _, _ ->
                clearChatForMe()
            }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }
    private fun showBlockUserDialog() {
        if (isGroup) return

        val targetUserId = otherParticipantId ?: return
        val userName = binding.chatTitleText.text?.toString().orEmpty()
            .ifBlank { getString(R.string.chat_user_placeholder) }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.FittoAlertDialogTheme)
            .setTitle(getString(R.string.chat_block_user_title))
            .setMessage(getString(R.string.chat_block_user_message, userName))
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.chat_block_user_confirm) { _, _ ->
                blockUser(targetUserId)
            }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }
    private fun showUnblockUserDialog() {
        if (isGroup) return

        val targetUserId = otherParticipantId ?: return
        val userName = binding.chatTitleText.text?.toString().orEmpty()
            .ifBlank { getString(R.string.chat_user_placeholder) }

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.FittoAlertDialogTheme)
            .setTitle(getString(R.string.chat_unblock_user_title))
            .setMessage(getString(R.string.chat_unblock_user_message, userName))
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.chat_unblock_user_confirm) { _, _ ->
                unblockUser(targetUserId)
            }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }

    private fun showReportUserDialog() {
        if (isGroup) return

        ReportReasonDialogHelper.show(
            fragment = this,
            title = getString(R.string.chat_report_user_title),
            subtitle = getString(R.string.chat_report_user_subtitle),
            reasons = listOf(
                ReportReasonDialogHelper.ReasonItem(
                    key = "spam",
                    label = getString(R.string.chat_report_reason_spam)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "harassment",
                    label = getString(R.string.chat_report_reason_harassment)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "inappropriate_content",
                    label = getString(R.string.chat_report_reason_inappropriate)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "other",
                    label = getString(R.string.chat_report_reason_other)
                )
            )
        ) { selectedReason ->
            reportUser(selectedReason.key)
        }
    }

    private fun reportUser(reason: String) {
        val targetUserId = otherParticipantId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.reportUser(
                    targetUserId = targetUserId,
                    threadId = threadId,
                    reason = reason
                )
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_report_user_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_report_user_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun blockUser(targetUserId: String) {
        val preview = conversationAccessController.resolveBlockPreview(
            messages = conversationStateStore.state.messages,
            emptyPreviewText = "Henüz mesaj yok.",
            fallbackCreatedAt = System.currentTimeMillis()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.blockUser(
                    targetUserId = targetUserId,
                    threadId = threadId,
                    previewText = preview.text,
                    previewCreatedAt = preview.createdAt
                )
            }.onSuccess {
                checkBlockState()

                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_block_user_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_block_user_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun unblockUser(targetUserId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.unblockUser(
                    targetUserId = targetUserId,
                    threadId = threadId
                )
            }.onSuccess {
                checkBlockState()

                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_unblock_user_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_unblock_user_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun clearChatForMe() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.clearChatForMe(threadId)
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_clear_chat_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_clear_chat_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun observeBlockState() {
        if (isGroup) return

        val targetUserId = otherParticipantId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeDirectThreadBlockState(
                    threadId = threadId,
                    targetUserId = targetUserId
                ).collect { blockState ->
                    val accessState = conversationAccessController.onBlockStateChanged(blockState)

                    if (_binding != null) {
                        renderConversationAccessState(accessState)
                        if (accessState.isBlocked) {
                            repository.setTyping(threadId, false)
                        }
                        submitMessages(conversationStateStore.state.messages)
                    }
                }
            }
        }
    }

    private fun checkBlockState() {
        if (isGroup) return

        val targetUserId = otherParticipantId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val blockState = repository.getDirectThreadBlockState(
                threadId = threadId,
                targetUserId = targetUserId
            )

            val accessState = conversationAccessController.onBlockStateChanged(blockState)

            if (_binding != null) {
                renderConversationAccessState(accessState)
                submitMessages(conversationStateStore.state.messages)
            }
        }
    }

    private fun checkDeletedDirectUserState() {
        if (isGroup) return

        val targetUserId = otherParticipantId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = runCatching {
                repository.isUserDeletedOrMissing(targetUserId)
            }.getOrDefault(false)

            if (_binding == null) return@launch

            val accessState = conversationAccessController.onDeletedUserChanged(deleted)

            if (deleted) {
                renderConversationAccessState(accessState)
            }
        }
    }

    private fun renderConversationAccessState(state: ChatConversationAccessState) {
        if (_binding == null) return

        conversationAccessRenderer.render(state)
        if (state.isInteractionEnabled) {
            renderComposerUiState()
        }
        updateMoreButtonVisibility()
    }

    private fun showChatMoreMenu(anchor: View) {
        val themedContext = ContextThemeWrapper(
            requireContext(),
            R.style.FittoWhitePopupMenuOverlay
        )

        val popupMenu = PopupMenu(
            themedContext,
            anchor,
            Gravity.END
        )


        popupMenu.menuInflater.inflate(R.menu.menu_chat_more, popupMenu.menu)
        popupMenu.setForceShowIcon(true)

        popupMenu.menu.findItem(R.id.action_block_user)?.title =
            if (conversationAccessController.currentState.isBlocked) {
                getString(R.string.chat_unblock_user)
            } else {
                getString(R.string.chat_block_user)
            }

        popupMenu.menu.findItem(R.id.action_block_user)?.isVisible = !isGroup
        popupMenu.menu.findItem(R.id.action_report_user)?.isVisible = !isGroup

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_media_links_files -> {
                    openMediaLinksFilesScreen()
                    true
                }

                R.id.action_clear_chat -> {
                    showClearChatDialog()
                    true
                }

                R.id.action_block_user -> {
                    if (conversationAccessController.currentState.isBlocked) {
                        showUnblockUserDialog()
                    } else {
                        showBlockUserDialog()
                    }
                    true
                }

                R.id.action_report_user -> {
                    showReportUserDialog()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }
    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeMessages(threadId).collect { messages: List<ChatMessageUiModel> ->
                    conversationStateStore.updateMessages(messages)
                    submitMessages(messages)
                    markThreadAsRead()
                }
            }
        }
    }
    private fun openDirectUserProfile() {
        openDirectUserSocialProfile()
    }
    private fun openGroupInfoScreen() {
        val thread = conversationStateStore.state.thread ?: MessageThreadUiModel(
            id = threadId,
            title = binding.chatTitleText.text?.toString().orEmpty(),
            lastMessage = "",
            timeText = "",
            isGroup = true,
            initials = binding.chatInitialsText.text?.toString().orEmpty(),
            unreadCount = 0,
            participantIds = emptyList(),
            otherParticipantId = null,
            photoUrl = headerPresentationController.currentPresentation.photoUrl,
            memberCount = memberCount
        )

        GroupInfoFragment
            .newInstance(thread)
            .show(parentFragmentManager, "GroupInfoFragment")
    }
    private fun openDirectUserSocialProfile() {
        val targetUserId = when (
            val decision = conversationAccessController.resolveDirectProfileOpen(otherParticipantId)
        ) {
            ChatDirectProfileOpenDecision.MissingParticipant -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_user_profile_open_error),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            ChatDirectProfileOpenDecision.DeletedUser -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.social_deleted_user_unavailable),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            is ChatDirectProfileOpenDecision.Open -> decision.userId
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = runCatching {
                repository.isUserDeletedOrMissing(targetUserId)
            }.getOrDefault(true)

            if (_binding == null) return@launch

            if (deleted) {
                val accessState = conversationAccessController.onDeletedUserChanged(true)
                renderConversationAccessState(accessState)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.social_deleted_user_unavailable),
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            SocialProfileFragment
                .newInstance(targetUserId)
                .show(childFragmentManager, "SocialProfileFragment")
        }
    }
    private fun observePresence() {
        if (isGroup || otherParticipantId.isNullOrBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeUserPresence(otherParticipantId.orEmpty()).collect { presence ->
                    val presentation = headerPresentationController.onPresenceChanged(presence)
                    if (_binding != null) {
                        headerRenderer.renderStatus(presentation)
                    }
                }
            }
        }
    }

    private fun observeTypingUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeTypingUsers(threadId).collect { users ->
                    val presentation = headerPresentationController.onTypingUsersChanged(users)
                    if (_binding == null) return@collect

                    handler.removeCallbacks(typingAnimationRunnable)
                    headerRenderer.renderStatus(presentation)

                    if (presentation.shouldAnimateTyping) {
                        handler.postDelayed(
                            typingAnimationRunnable,
                            TYPING_ANIMATION_DELAY_MILLIS
                        )
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val decision = messageComposerController.beginTextSend(
            text = binding.chatMessageEditText.text,
            isChatBlocked = conversationAccessController.currentState.isBlocked
        )

        when (decision) {
            is ChatTextSendDecision.Send -> {
                repository.setTyping(threadId, false)
                handler.removeCallbacks(stopTypingRunnable)

                launchComposerDelivery(
                    delivery = {
                        repository.sendMessage(
                            threadId = threadId,
                            text = decision.text
                        )
                    },
                    failureMessageRes = R.string.messages_new_chat_error,
                    onSuccess = {
                        binding.chatMessageEditText.setText("")
                    }
                )
            }

            is ChatTextSendDecision.Rejected -> {
                handleComposerSendRejection(decision.reason)
            }
        }
    }

    private fun sendFileMessage(fileUri: Uri) {
        when (val decision = messageComposerController.beginAttachmentSend(
            conversationAccessController.currentState.isBlocked
        )) {
            ChatMediaSendDecision.Send -> {
                launchComposerDelivery(
                    delivery = {
                        repository.sendFileMessage(
                            context = requireContext(),
                            threadId = threadId,
                            fileUri = fileUri
                        )
                    },
                    successMessageRes = R.string.chat_file_sent,
                    failureMessageRes = R.string.chat_file_send_error
                )
            }

            is ChatMediaSendDecision.Rejected -> {
                handleComposerSendRejection(decision.reason)
            }
        }
    }

    private fun createMediaController(): ChatMediaController {
        return ChatMediaController(
            voiceFileFactory = {
                File(
                    requireContext().cacheDir,
                    "voice_${System.currentTimeMillis()}.m4a"
                )
            },
            voiceRecorderFactory = AndroidChatVoiceRecorderFactory(
                context = requireContext(),
                maximumDurationMillis = MAX_VOICE_DURATION_MILLIS
            ),
            audioPlayerFactory = AndroidChatAudioPlayerFactory(),
            elapsedRealtime = SystemClock::elapsedRealtime,
            minimumVoiceDurationMillis = MIN_VOICE_DURATION_MILLIS,
            onVoiceRecordingStateChanged = { isRecording ->
                if (_binding != null) {
                    renderVoiceRecordingState(isRecording)
                }
            },
            onAudioPlaybackStateChanged = { audioUrl ->
                if (::messageAdapter.isInitialized) {
                    messageAdapter.setPlayingAudioUrl(audioUrl)
                }
            },
            onAudioPlaybackError = { error ->
                Log.e("FittoAudio", "Audio play failed: ${error.message}", error)

                if (_binding != null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.chat_voice_play_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun toggleVoiceRecording() {
        when (
            messageComposerController.resolveVoiceToggle(
                isChatBlocked = conversationAccessController.currentState.isBlocked,
                isVoiceRecording = mediaController.isVoiceRecording
            )
        ) {
            ChatVoiceToggleAction.Blocked -> {
                showBlockedSendError()
                return
            }
            ChatVoiceToggleAction.Busy -> return
            ChatVoiceToggleAction.Toggle -> Unit
        }

        when (val result = mediaController.toggleVoiceRecording()) {
            ChatVoiceRecordingActionResult.Started -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_voice_recording_started),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatVoiceRecordingActionResult.ReadyToSend -> {
                sendAudioMessage(
                    audioFile = result.audioFile,
                    durationMillis = result.durationMillis
                )
            }

            ChatVoiceRecordingActionResult.TooShort -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_voice_too_short),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ChatVoiceRecordingActionResult.Failed -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_voice_recording_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun renderVoiceRecordingState(isRecording: Boolean) = with(binding) {
        chatVoiceButton.isSelected = isRecording

        if (isRecording) {
            chatVoiceButton.alpha = 1f
            chatMessageEditText.hint = getString(R.string.chat_voice_recording_hint)
        } else {
            chatVoiceButton.alpha = 1f
            chatMessageEditText.hint = getString(R.string.chat_message_hint)
        }
    }

    private fun sendAudioMessage(
        audioFile: File,
        durationMillis: Long
    ) {
        if (messageComposerController.beginAudioSend() is ChatMediaSendDecision.Rejected) {
            return
        }

        launchComposerDelivery(
            delivery = {
                repository.sendAudioMessage(
                    context = requireContext(),
                    threadId = threadId,
                    audioFile = audioFile,
                    durationMillis = durationMillis
                )
            },
            successMessageRes = R.string.chat_voice_sent,
            failureMessageRes = R.string.chat_voice_send_error,
            onFailure = audioFile::delete
        )
    }

    private fun openAttachment(message: ChatMessageUiModel) {
        val url = message.fileUrl.orEmpty()
        if (url.isBlank()) return

        if (message.fileMimeType?.startsWith("image/") == true || message.type == "image") {
            showImagePreview(url)
            return
        }

        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.chat_file_open_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showImagePreview(url: String) {
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        val container = FrameLayout(requireContext()).apply {
            setBackgroundColor(Color.BLACK)
        }

        val imageView = ImageView(requireContext()).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            load(url) {
                crossfade(true)
                placeholder(R.drawable.bg_circle_primary_soft)
                error(R.drawable.bg_circle_primary_soft)
            }
        }

        val closeButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_close)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            contentDescription = getString(R.string.common_close)
            setOnClickListener { dialog.dismiss() }
        }

        container.addView(
            imageView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        container.addView(
            closeButton,
            FrameLayout.LayoutParams(
                (48 * resources.displayMetrics.density).toInt(),
                (48 * resources.displayMetrics.density).toInt(),
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = (20 * resources.displayMetrics.density).toInt()
                marginEnd = (16 * resources.displayMetrics.density).toInt()
            }
        )
        container.setOnClickListener { dialog.dismiss() }
        imageView.setOnClickListener { }

        dialog.setContentView(container)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.show()
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }
    private fun playOrStopAudio(url: String) {
        mediaController.playOrStopAudio(url)
    }

    private fun submitMessages(messages: List<ChatMessageUiModel>) {
        val presentation = messagePresentationController.present(
            ChatMessagePresentationInput(
                messages = messages,
                isGroup = isGroup,
                hiddenSenderId = otherParticipantId,
                isCurrentChatBlocked = conversationAccessController.currentState.isBlocked,
                hiddenMessageRanges = conversationAccessController.currentState.hiddenMessageRanges
            )
        )

        if (presentation.selectionWasCleared) {
            messageAdapter.setSelectedMessage(null)
            updateSelectionUi()
        }

        messageAdapter.submitList(presentation.visibleMessages) {
            applyMessagePostSubmitAction(presentation.postSubmitAction)
        }
    }

    private fun applyMessagePostSubmitAction(action: ChatMessagePostSubmitAction) {
        if (!messagePresentationController.shouldApplyPostSubmitAction(action)) return

        when (action) {
            ChatMessagePostSubmitAction.None -> Unit

            is ChatMessagePostSubmitAction.ScrollToPosition -> {
                binding.chatMessagesRecyclerView.scrollToPosition(action.position)
            }

            is ChatMessagePostSubmitAction.HighlightMessage -> {
                binding.chatMessagesRecyclerView.scrollToPosition(action.position)
                messageAdapter.setHighlightedMessage(action.messageId)

                handler.postDelayed({
                    if (_binding != null) {
                        messageAdapter.setHighlightedMessage(null)
                    }
                }, action.clearAfterMillis)
            }
        }
    }

    private fun markThreadAsRead() {
        FittoNotificationStore.removeMessageNotificationsForThread(requireContext(), threadId)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.markThreadRead(threadId)
            }.onSuccess {
                if (_binding != null) {
                    FittoNotificationStore.removeMessageNotificationsForThread(requireContext(), threadId)
                }
            }
        }
    }


    private val selectedMessage: ChatMessageUiModel?
        get() = messagePresentationController.selectedMessage

    private fun selectMessage(message: ChatMessageUiModel) {
        messagePresentationController.selectMessage(message)
        messageAdapter.setSelectedMessage(message.id)
        updateSelectionUi()
    }

    private fun toggleMessageSelection(message: ChatMessageUiModel) {
        messagePresentationController.toggleMessageSelection(message)
        messageAdapter.setSelectedMessage(selectedMessage?.id)
        updateSelectionUi()
    }

    private fun clearSelectedMessage() {
        messagePresentationController.clearSelection()
        messageAdapter.setSelectedMessage(null)
        updateSelectionUi()
    }

    private fun updateMoreButtonVisibility() {
        if (_binding == null) return

        binding.chatMoreButton.isVisible = conversationAccessController.shouldShowMoreButton(
            hasSelectedMessage = selectedMessage != null
        )
    }
    private fun updateSelectionUi() = with(binding) {
        val hasSelection = selectedMessage != null

        chatDeleteButton.isVisible = hasSelection
        chatInfoButton.isVisible = false

        chatAvatarContainer.isVisible = !hasSelection
        chatTitleContainer.isVisible = true
        chatSubtitleText.isVisible = !hasSelection

        chatTitleText.text = if (hasSelection) {
            getString(R.string.chat_message_selected)
        } else {
            conversationStateStore.state.thread?.title ?: threadTitle
        }

        updateMoreButtonVisibility()
    }

    private fun showSelectedMessageInfo() {
        val message = selectedMessage ?: return
        val deliveredText = if (message.deliveredAt > 0L) {
            formatFullTime(message.deliveredAt)
        } else {
            getString(R.string.chat_message_info_unknown)
        }
        val readers = message.readBy
            .filterKeys { uid -> uid != message.senderId }
            .entries
            .sortedBy { it.value }
            .joinToString("\n") { entry ->
                val name = message.participantNames[entry.key] ?: entry.key.take(6)
                "$name · ${formatFullTime(entry.value)}"
            }
            .ifBlank { getString(R.string.chat_message_info_no_readers) }
        val infoText = getString(
            R.string.chat_message_info_body,
            message.senderName,
            formatFullTime(message.createdAt),
            deliveredText,
            readers
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_message_info)
            .setMessage(infoText)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showDeleteMessageDialog() {
        val message = selectedMessage ?: return

        DeleteMessageOptionsBottomSheet
            .newInstance(
                allowDeleteForEveryone = message.isFromMe && !message.isDeleted
            )
            .show(parentFragmentManager, "DeleteMessageOptionsBottomSheet")
    }

    private fun deleteSelectedMessage(deleteForEveryone: Boolean) {
        val message = selectedMessage ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                if (deleteForEveryone) {
                    repository.deleteMessageForEveryone(threadId, message.id)
                } else {
                    repository.deleteMessageForMe(threadId, message.id)
                }
            }.onSuccess {
                if (_binding == null) return@onSuccess

                clearSelectedMessage()

                Toast.makeText(
                    requireContext(),
                    if (deleteForEveryone) {
                        getString(R.string.chat_delete_for_everyone_success)
                    } else {
                        getString(R.string.chat_delete_for_me_success)
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                if (_binding == null) return@onFailure

                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_delete_message_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun formatFullTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return getString(R.string.chat_message_info_unknown)
        return SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr", "TR")).format(Date(timeMillis))
    }

    private fun launchComposerDelivery(
        delivery: suspend () -> Unit,
        failureMessageRes: Int,
        successMessageRes: Int? = null,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        renderComposerUiState()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { delivery() }

            if (_binding != null) {
                result.onSuccess {
                    onSuccess()
                    successMessageRes?.let(::showShortToast)
                }.onFailure {
                    onFailure()
                    showShortToast(failureMessageRes)
                }
            }

            finishComposerSend()
        }
    }

    private fun showShortToast(messageRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(messageRes),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleComposerSendRejection(rejection: ChatComposerSendRejection) {
        if (rejection == ChatComposerSendRejection.BLOCKED) {
            showBlockedSendError()
        }
    }

    private fun showBlockedSendError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.chat_blocked_send_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun finishComposerSend() {
        if (_binding != null) {
            val uiState = messageComposerController.finishSend(binding.chatMessageEditText.text)
            renderComposerUiState(uiState)
        } else {
            messageComposerController.finishSend(text = null)
        }
    }

    private fun renderComposerUiState(uiState: ChatComposerUiState? = null) {
        if (_binding == null) return

        val resolvedUiState = uiState
            ?: messageComposerController.uiState(binding.chatMessageEditText.text)

        binding.chatSendButton.isEnabled = resolvedUiState.isSendButtonEnabled
        binding.chatSendButton.alpha = if (resolvedUiState.isSendButtonEnabled) 1f else 0.45f
    }

    override fun onDestroyView() {
        if (::mediaController.isInitialized) {
            mediaController.release()
        }
        handler.removeCallbacks(stopTypingRunnable)
        handler.removeCallbacks(typingAnimationRunnable)
        repository.setTyping(threadId, false)
        binding.chatMessagesRecyclerView.adapter = null
        _headerRenderer = null
        _conversationAccessRenderer = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"
        private const val MIN_VOICE_DURATION_MILLIS = 800L
        private const val MAX_VOICE_DURATION_MILLIS = 180_000L
        private const val ARG_THREAD_TITLE = "thread_title"
        private const val ARG_THREAD_INITIALS = "thread_initials"
        private const val ARG_IS_GROUP = "is_group"
        private const val ARG_OTHER_PARTICIPANT_ID = "other_participant_id"
        private const val ARG_THREAD_PHOTO_URL = "thread_photo_url"
        private const val ARG_MEMBER_COUNT = "member_count"
        private const val STOP_TYPING_DELAY_MILLIS = 1_600L
        private const val TYPING_ANIMATION_DELAY_MILLIS = 360L
        private const val ARG_HIGHLIGHT_MESSAGE_ID = "highlight_message_id"

        fun newInstance(
            thread: MessageThreadUiModel,
            highlightMessageId: String? = null
        ): ChatDetailFragment {
            return ChatDetailFragment().apply {
                arguments = bundleOf(
                    ARG_THREAD_ID to thread.id,
                    ARG_THREAD_TITLE to thread.title,
                    ARG_THREAD_INITIALS to thread.initials,
                    ARG_IS_GROUP to thread.isGroup,
                    ARG_OTHER_PARTICIPANT_ID to thread.otherParticipantId,
                    ARG_THREAD_PHOTO_URL to thread.photoUrl,
                    ARG_MEMBER_COUNT to thread.memberCount,
                    ARG_HIGHLIGHT_MESSAGE_ID to highlightMessageId
                )
            }
        }
    }
}
