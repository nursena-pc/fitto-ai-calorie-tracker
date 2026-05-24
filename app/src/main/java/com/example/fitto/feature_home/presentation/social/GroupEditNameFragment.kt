package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.FragmentEditGroupNameBinding
import kotlinx.coroutines.launch

class GroupEditNameFragment : DialogFragment(R.layout.fragment_edit_group_name) {

    private var _binding: FragmentEditGroupNameBinding? = null
    private val binding get() = _binding!!

    private val repository = SocialRepository()

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
    }

    private val currentName: String by lazy {
        requireArguments().getString(ARG_CURRENT_NAME).orEmpty()
    }

    private val initials: String by lazy {
        requireArguments().getString(ARG_INITIALS).orEmpty()
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
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditGroupNameBinding.bind(view)

        setupUi()
        setupClicks()
    }

    private fun setupUi() = with(binding) {
        val safeInitials = initials.ifBlank { "G" }

        currentInitialsText.text = safeInitials
        previewInitialsText.text = safeInitials

        currentGroupNameText.text = currentName
        previewGroupNameText.text = currentName

        val membersText = resources.getQuantityString(
            R.plurals.group_members_count,
            memberCount,
            memberCount
        )

        currentGroupMemberCountText.text = membersText
        previewGroupMemberCountText.text = membersText

        groupNameEditText.setText(currentName)
        groupNameEditText.setSelection(currentName.length.coerceAtMost(50))

        updateSaveButtonState(currentName)

        groupNameEditText.doAfterTextChanged { text ->
            val value = text?.toString().orEmpty()

            previewGroupNameText.text = value.ifBlank { currentName }
            updateSaveButtonState(value)
            groupNameInputLayout.error = null
        }
    }

    private fun setupClicks() = with(binding) {
        editNameBackButton.setOnClickListener {
            dismiss()
        }

        editNameSaveText.setOnClickListener {
            saveGroupName()
        }
    }

    private fun updateSaveButtonState(value: String) = with(binding) {
        val trimmed = value.trim()

        val enabled = trimmed.isNotBlank() &&
                trimmed.length <= GROUP_NAME_MAX_LENGTH &&
                trimmed != currentName.trim()

        editNameSaveText.isEnabled = enabled
        editNameSaveText.alpha = if (enabled) 1f else 0.45f
    }

    private fun saveGroupName() {
        val newName = binding.groupNameEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        when {
            newName.isBlank() -> {
                binding.groupNameInputLayout.error =
                    getString(R.string.group_name_empty_error)
                return
            }

            newName.length > GROUP_NAME_MAX_LENGTH -> {
                binding.groupNameInputLayout.error =
                    getString(R.string.group_name_too_long_error)
                return
            }

            newName == currentName.trim() -> {
                dismiss()
                return
            }
        }

        binding.groupNameInputLayout.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.updateGroupName(threadId, newName)
            }.onSuccess {
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY_GROUP_NAME_UPDATED,
                    bundleOf(BUNDLE_KEY_GROUP_NAME to newName)
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_name_update_success),
                    Toast.LENGTH_SHORT
                ).show()

                dismiss()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_name_update_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_CURRENT_NAME = "current_name"
        private const val ARG_INITIALS = "initials"
        private const val ARG_MEMBER_COUNT = "member_count"

        private const val GROUP_NAME_MAX_LENGTH = 50

        const val REQUEST_KEY_GROUP_NAME_UPDATED = "request_key_group_name_updated"
        const val BUNDLE_KEY_GROUP_NAME = "bundle_key_group_name"

        fun newInstance(
            threadId: String,
            currentName: String,
            initials: String,
            memberCount: Int
        ): GroupEditNameFragment {
            return GroupEditNameFragment().apply {
                arguments = bundleOf(
                    ARG_THREAD_ID to threadId,
                    ARG_CURRENT_NAME to currentName,
                    ARG_INITIALS to initials,
                    ARG_MEMBER_COUNT to memberCount
                )
            }
        }
    }
}