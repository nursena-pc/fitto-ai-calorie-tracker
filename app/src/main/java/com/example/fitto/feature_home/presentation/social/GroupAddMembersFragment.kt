package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentGroupAddMembersBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GroupAddMembersFragment : DialogFragment(R.layout.fragment_group_add_members) {

    private var _binding: FragmentGroupAddMembersBinding? = null
    private val binding get() = _binding!!

    private val repository = SocialRepository()
    private var searchJob: Job? = null

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
    }

    private val adapter: GroupAddMembersAdapter by lazy {
        GroupAddMembersAdapter(
            onUserClick = { user: SearchUserUiModel ->
                adapter.toggleSelection(user)
                updateDoneButton()
            }
        )
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
        _binding = FragmentGroupAddMembersBinding.bind(view)

        setupUi()
        setupSearch()
    }

    private fun setupUi() = with(binding) {
        addMembersBackButton.setOnClickListener {
            dismiss()
        }

        addMembersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        addMembersRecyclerView.adapter = adapter

        addMembersDoneButton.isEnabled = false
        addMembersDoneButton.alpha = 0.5f

        addMembersDoneButton.setOnClickListener {
            addSelectedMembers()
        }
    }

    private fun setupSearch() = with(binding) {
        addMembersSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()

                val query = s?.toString().orEmpty()

                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(350)
                    searchUsers(query)
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun searchUsers(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (query.trim().length < 2) {
                adapter.submitList(emptyList())
                binding.addMembersEmptyText.isVisible = true
                binding.addMembersRecyclerView.isVisible = false
                binding.addMembersEmptyText.text = getString(R.string.group_add_member_empty)
                return@launch
            }

            runCatching {
                repository.searchUsersForGroupInvite(threadId, query)
            }.onSuccess { users ->
                adapter.submitList(users)
                binding.addMembersEmptyText.isVisible = users.isEmpty()
                binding.addMembersRecyclerView.isVisible = users.isNotEmpty()
                binding.addMembersEmptyText.text = getString(R.string.group_add_member_no_result)
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_add_member_search_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateDoneButton() = with(binding) {
        val hasSelection = adapter.getSelectedUsers().isNotEmpty()
        addMembersDoneButton.isEnabled = hasSelection
        addMembersDoneButton.alpha = if (hasSelection) 1f else 0.5f
    }

    private fun addSelectedMembers() {
        val selectedUsers = adapter.getSelectedUsers()
        if (selectedUsers.isEmpty()) return

        binding.addMembersDoneButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.addMembersToGroup(threadId, selectedUsers)
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_add_member_success),
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY_MEMBERS_ADDED,
                    bundleOf(BUNDLE_KEY_ADDED_COUNT to selectedUsers.size)
                )

                dismiss()
            }.onFailure {
                binding.addMembersDoneButton.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_add_member_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"

        const val REQUEST_KEY_MEMBERS_ADDED = "request_key_members_added"
        const val BUNDLE_KEY_ADDED_COUNT = "bundle_key_added_count"

        fun newInstance(threadId: String): GroupAddMembersFragment {
            return GroupAddMembersFragment().apply {
                arguments = bundleOf(ARG_THREAD_ID to threadId)
            }
        }
    }
}