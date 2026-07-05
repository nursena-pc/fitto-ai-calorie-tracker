package com.example.fitto.feature_home.presentation.social

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import coil.load
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentGroupInfoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.PopupWindow
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.Manifest
class GroupInfoFragment : DialogFragment(R.layout.fragment_group_info) {

    private var _binding: FragmentGroupInfoBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().fittoAppContainer.socialFeatureDependencies.repository
    }

    private val threadId: String by lazy { requireArguments().getString(ARG_THREAD_ID).orEmpty() }
    private val threadTitle: String by lazy { requireArguments().getString(ARG_THREAD_TITLE).orEmpty() }
    private val threadInitials: String by lazy { requireArguments().getString(ARG_THREAD_INITIALS).orEmpty() }
    private val threadPhotoUrl: String? by lazy { requireArguments().getString(ARG_THREAD_PHOTO_URL) }

    private val groupPhotoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                updateGroupPhoto(uri)
            }
        }

    private val memberCount: Int by lazy { requireArguments().getInt(ARG_MEMBER_COUNT, 0) }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGroupInfoBinding.bind(view)

        setupUi()
        setupClicks()
        observeRenameResult()
        loadGroupDetails()
        observeSharedContentSummary()
        observeMembersAddedResult()
        observeMemberRemovedResult()
    }
    private fun observeMemberRemovedResult() {
        parentFragmentManager.setFragmentResultListener(
            GroupMembersFragment.REQUEST_KEY_MEMBER_REMOVED,
            viewLifecycleOwner
        ) { _, _ ->
            loadGroupDetails()
        }
    }
    private fun observeMembersAddedResult() {
        parentFragmentManager.setFragmentResultListener(
            GroupAddMembersFragment.REQUEST_KEY_MEMBERS_ADDED,
            viewLifecycleOwner
        ) { _, _ ->
            loadGroupDetails()
        }
    }

    private fun loadGroupDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.getMessageThreadRawData(threadId)
            }.onSuccess { data ->
                val description = data["description"] as? String
                val photoUrl = data["photoUrl"] as? String

                val participantIds = (data["participantIds"] as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()

                val latestMemberCount = (data["memberCount"] as? Number)?.toInt()
                    ?: participantIds.size

                binding.groupDescriptionText.text = description
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.group_description_empty)

                binding.groupMembersCountText.text = getString(
                    R.string.chat_group_members,
                    latestMemberCount
                )

                binding.groupMembersPreviewText.text = resources.getQuantityString(
                    R.plurals.group_members_count,
                    latestMemberCount,
                    latestMemberCount
                )

                val hasPhoto = !photoUrl.isNullOrBlank()
                binding.groupAvatarImage.isVisible = hasPhoto
                binding.groupInitialsText.isVisible = !hasPhoto

                if (hasPhoto) {
                    binding.groupAvatarImage.load(photoUrl)
                } else {
                    binding.groupInitialsText.text = threadInitials.ifBlank { "G" }
                }
            }
        }
    }
    private fun setupUi() = with(binding) {
        groupNameText.text = threadTitle
        groupMembersCountText.text = getString(R.string.chat_group_members, memberCount)

        groupMembersPreviewText.text = resources.getQuantityString(
            R.plurals.group_members_count,
            memberCount,
            memberCount
        )

        groupDescriptionText.text = "Sağlıklı yaşam, dengeli beslenme ve motivasyon! 💚"
        groupMediaFilesCountText.text = getString(
            R.string.group_media_links_files_format,
            0,
            0,
            0
        )


        val hasPhoto = !threadPhotoUrl.isNullOrBlank()
        groupAvatarImage.isVisible = hasPhoto
        groupInitialsText.isVisible = !hasPhoto

        if (hasPhoto) {
            groupAvatarImage.load(threadPhotoUrl)
        } else {
            groupInitialsText.text = threadInitials.ifBlank { "G" }
        }
    }
    private fun observeSharedContentSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.observeThreadSharedContentSummary(threadId).collect { summary ->
                binding.groupMediaFilesCountText.text = getString(
                    R.string.group_media_links_files_format,
                    summary.mediaCount,
                    summary.linkCount,
                    summary.fileCount
                )
            }
        }
    }
    private fun setupClicks() = with(binding) {
        groupInfoBackButton.setOnClickListener { dismiss() }

        groupInfoMoreButton.setOnClickListener { showMoreMenu(it) }

        groupPhotoEditButton.setOnClickListener {
            openGroupPhotoPicker()
        }

        groupAvatarImage.setOnClickListener {
            openGroupPhotoPicker()
        }

        groupInitialsText.setOnClickListener {
            openGroupPhotoPicker()
        }

        groupNameContainer.setOnClickListener {
            GroupEditNameFragment
                .newInstance(
                    threadId = threadId,
                    currentName = groupNameText.text.toString(),
                    initials = threadInitials.ifBlank { "G" },
                    memberCount = memberCount
                )
                .show(parentFragmentManager, "GroupEditNameFragment")
        }

        groupDescriptionCard.setOnClickListener {
            showEditDescriptionDialog()
        }



        groupActionAddMember.setOnClickListener {
            GroupAddMembersFragment
                .newInstance(threadId)
                .show(parentFragmentManager, "GroupAddMembersFragment")
        }

        groupActionSearch.setOnClickListener {
            GroupMessageSearchFragment
                .newInstance(createCurrentThreadUiModel())
                .show(parentFragmentManager, "GroupMessageSearchFragment")
        }

        groupMediaFilesCard.setOnClickListener {
            GroupSharedContentFragment
                .newInstance(threadId)
                .show(parentFragmentManager, "GroupSharedContentFragment")
        }

        groupMembersCard.setOnClickListener {
            GroupMembersFragment
                .newInstance(threadId)
                .show(parentFragmentManager, "GroupMembersFragment")
        }

        groupSettingsCard.setOnClickListener {
            GroupSettingsFragment
                .newInstance(threadId)
                .show(parentFragmentManager, "GroupSettingsFragment")
        }
        groupLeaveText.setOnClickListener {
            showLeaveGroupDialog()
        }
    }

    private fun createCurrentThreadUiModel(): MessageThreadUiModel {
        return MessageThreadUiModel(
            id = threadId,
            title = binding.groupNameText.text?.toString().orEmpty().ifBlank { threadTitle },
            lastMessage = "",
            timeText = "",
            isGroup = true,
            initials = threadInitials.ifBlank { "G" },
            unreadCount = 0,
            participantIds = emptyList(),
            otherParticipantId = null,
            photoUrl = threadPhotoUrl,
            memberCount = memberCount
        )
    }
    private fun showMoreMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_group_info_more_menu, null, false)

        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 12f
        }

        popupView.findViewById<View>(R.id.clearChatMenuItem).setOnClickListener {
            popupWindow.dismiss()
            showClearChatDialog()
        }

        popupView.findViewById<View>(R.id.leaveGroupMenuItem).setOnClickListener {
            popupWindow.dismiss()
            showLeaveGroupDialog()
        }

        popupWindow.showAsDropDown(
            anchor,
            -230,
            8,
            Gravity.NO_GRAVITY
        )
    }
    private fun openGroupPhotoPicker() {
        groupPhotoPickerLauncher.launch("image/*")
    }

    private fun updateGroupPhoto(imageUri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.updateGroupPhoto(threadId, imageUri)
            }.onSuccess { photoUrl ->
                binding.groupAvatarImage.isVisible = true
                binding.groupInitialsText.isVisible = false
                binding.groupAvatarImage.load(photoUrl)

                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY_GROUP_PHOTO_UPDATED,
                    bundleOf(
                        BUNDLE_KEY_PHOTO_THREAD_ID to threadId,
                        BUNDLE_KEY_GROUP_PHOTO_URL to photoUrl
                    )
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_photo_update_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_photo_update_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun showClearChatDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("🗑️ ${getString(R.string.group_clear_chat)}")
            .setMessage(getString(R.string.group_clear_chat_confirm))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.group_clear_chat)) { _, _ ->
                clearChatForMe()
            }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }

    private fun clearChatForMe() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.clearThreadForCurrentUser(threadId)
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_clear_chat_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_clear_chat_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLeaveGroupDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("🚪 ${getString(R.string.group_leave)}")
            .setMessage(getString(R.string.group_leave_confirm))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.group_leave)) { _, _ ->
                leaveGroup()
            }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }

    private fun leaveGroup() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.leaveGroup(threadId)
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_leave_success),
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY_GROUP_LEFT,
                    bundleOf(BUNDLE_KEY_LEFT_THREAD_ID to threadId)
                )

                dismiss()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_leave_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun showEditDescriptionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_group_description, null)

        val inputLayout =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(
                R.id.groupDescriptionInputLayout
            )

        val editText =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                R.id.groupDescriptionEditText
            )

        val closeButton =
            dialogView.findViewById<android.widget.ImageButton>(
                R.id.dialogCloseButton
            )

        val cancelButton =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(
                R.id.dialogCancelButton
            )

        val saveButton =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(
                R.id.dialogSaveButton
            )

        val currentDescription = binding.groupDescriptionText.text?.toString().orEmpty()

        editText.setText(
            if (currentDescription == getString(R.string.group_description_empty)) {
                ""
            } else {
                currentDescription
            }
        )
        editText.setSelection(editText.text?.length ?: 0)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val newDescription = editText.text?.toString()?.trim().orEmpty()

            if (newDescription.length > 180) {
                inputLayout.error = getString(R.string.group_description_too_long)
                return@setOnClickListener
            }

            inputLayout.error = null
            dialog.dismiss()
            updateGroupDescription(newDescription)
        }

        dialog.show()
    }
    private fun updateGroupDescription(newDescription: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.updateGroupDescription(threadId, newDescription)
            }.onSuccess {
                binding.groupDescriptionText.text = newDescription.ifBlank {
                    getString(R.string.group_description_empty)
                }

                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_description_update_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_description_update_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun observeRenameResult() {
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_GROUP_NAME_UPDATED,
            viewLifecycleOwner
        ) { _, bundle ->
            val newTitle = bundle.getString(BUNDLE_KEY_GROUP_NAME).orEmpty()
            if (newTitle.isNotBlank()) {
                binding.groupNameText.text = newTitle
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_THREAD_TITLE = "thread_title"
        private const val ARG_THREAD_INITIALS = "thread_initials"
        private const val ARG_THREAD_PHOTO_URL = "thread_photo_url"
        private const val ARG_MEMBER_COUNT = "member_count"

        const val REQUEST_KEY_GROUP_NAME_UPDATED = "request_key_group_name_updated"
        const val BUNDLE_KEY_GROUP_NAME = "bundle_key_group_name"
        const val REQUEST_KEY_GROUP_LEFT = "request_key_group_left"
        const val BUNDLE_KEY_LEFT_THREAD_ID = "bundle_key_left_thread_id"
        const val REQUEST_KEY_GROUP_PHOTO_UPDATED = "request_key_group_photo_updated"
        const val BUNDLE_KEY_PHOTO_THREAD_ID = "bundle_key_photo_thread_id"
        const val BUNDLE_KEY_GROUP_PHOTO_URL = "bundle_key_group_photo_url"
        private const val MENU_CLEAR_CHAT = 101
        private const val MENU_LEAVE_GROUP = 102

        fun newInstance(thread: MessageThreadUiModel): GroupInfoFragment {
            return GroupInfoFragment().apply {
                arguments = bundleOf(
                    ARG_THREAD_ID to thread.id,
                    ARG_THREAD_TITLE to thread.title,
                    ARG_THREAD_INITIALS to thread.initials,
                    ARG_THREAD_PHOTO_URL to thread.photoUrl,
                    ARG_MEMBER_COUNT to thread.memberCount
                )
            }
        }
    }
}