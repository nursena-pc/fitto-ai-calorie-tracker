package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentGroupMembersBinding
import kotlinx.coroutines.launch
import android.graphics.Color
import androidx.core.view.isVisible
import coil.load
import com.example.fitto.databinding.BottomSheetGroupMemberActionsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale
class GroupMembersFragment : DialogFragment(R.layout.fragment_group_members) {

    private var _binding: FragmentGroupMembersBinding? = null
    private val binding get() = _binding!!

    private val repository = SocialRepository()

    private val adapter = GroupMembersAdapter { member ->
        showMemberActions(member)

    }

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
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
        _binding = FragmentGroupMembersBinding.bind(view)

        setupUi()
        loadMembers()
    }

    private fun setupUi() = with(binding) {
        groupMembersBackButton.setOnClickListener {
            dismiss()
        }

        groupMembersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        groupMembersRecyclerView.adapter = adapter
    }
    private fun showMemberActions(member: GroupMemberUiModel) {
        if (member.isMe) {
            Toast.makeText(
                requireContext(),
                getString(R.string.group_this_is_you),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (member.isCreator) {
            Toast.makeText(
                requireContext(),
                getString(R.string.group_creator_cannot_be_removed),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetGroupMemberActionsBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)

            bottomSheet?.let {
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        with(sheetBinding) {
            sheetMemberNameText.text = member.userName
            sheetMemberUsernameText.text = getString(
                R.string.group_username_format,
                member.userName
            )

            val hasPhoto = !member.photoUrl.isNullOrBlank()
            sheetMemberAvatarImage.isVisible = hasPhoto
            sheetMemberInitialsText.isVisible = !hasPhoto

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = sheetMemberAvatarImage,
                    photoUrl = member.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                sheetMemberAvatarImage.setImageDrawable(null)
                sheetMemberInitialsText.text = createInitials(member.userName)
            }

            removeMemberActionCard.setOnClickListener {
                dialog.dismiss()
                showRemoveMemberDialog(member)
            }
        }

        dialog.show()
    }
    private fun createInitials(value: String): String {
        return value
            .replace("@", "")
            .split(" ", ".", "_", "-")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase(Locale("tr", "TR")) }
            .joinToString("")
            .ifBlank {
                value.firstOrNull()?.uppercase(Locale("tr", "TR")) ?: "F"
            }
    }

    private fun showRemoveMemberDialog(member: GroupMemberUiModel) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.group_remove_member))
            .setMessage(getString(R.string.group_remove_member_confirm, member.userName))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.group_remove_member)) { _, _ ->
                removeMember(member)
            }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }

    private fun removeMember(member: GroupMemberUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.removeMemberFromGroup(threadId, member.uid)
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_remove_member_success),
                    Toast.LENGTH_SHORT
                ).show()

                loadMembers()

                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY_MEMBER_REMOVED,
                    bundleOf(BUNDLE_KEY_REMOVED_MEMBER_ID to member.uid)
                )
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_remove_member_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun loadMembers() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.getGroupMembers(threadId)
            }.onSuccess { members ->
                adapter.submitList(members)
                binding.groupMembersCountText.text = resources.getQuantityString(
                    R.plurals.group_members_count,
                    members.size,
                    members.size
                )
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_members_load_error),
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
        const val REQUEST_KEY_MEMBER_REMOVED = "request_key_member_removed"
        const val BUNDLE_KEY_REMOVED_MEMBER_ID = "bundle_key_removed_member_id"
        fun newInstance(threadId: String): GroupMembersFragment {
            return GroupMembersFragment().apply {
                arguments = bundleOf(ARG_THREAD_ID to threadId)
            }
        }
    }
}