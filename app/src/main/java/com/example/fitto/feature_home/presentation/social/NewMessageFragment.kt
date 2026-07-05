package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentNewMessageBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewMessageFragment : DialogFragment(R.layout.fragment_new_message) {

    private var _binding: FragmentNewMessageBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().fittoAppContainer.socialFeatureDependencies.repository
    }
    private lateinit var userAdapter: SearchUserAdapter
    private lateinit var selectedUserAdapter: SelectedUserAdapter

    private var searchJob: Job? = null
    private var isStartingChat = false
    private var selectedMode = MessageStartMode.DIRECT
    private var latestUsers: List<SearchUserUiModel> = emptyList()
    private val selectedUsers = linkedMapOf<String, SearchUserUiModel>()

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

        _binding = FragmentNewMessageBinding.bind(view)

        setupAdapters()
        setupClicks()
        setupSearchInput()
        setupModeCards()
        updateModeUi()
        showStartState()
    }

    private fun setupAdapters() {
        userAdapter = SearchUserAdapter(
            onUserClick = { user ->
                if (selectedMode == MessageStartMode.DIRECT) {
                    startDirectChat(user)
                } else {
                    toggleGroupUser(user)
                }
            },
            selectedUserIdsProvider = { selectedUsers.keys }
        )

        selectedUserAdapter = SelectedUserAdapter(
            onRemoveClick = { user ->
                selectedUsers.remove(user.id)
                updateSelectedUsers()
                userAdapter.notifyDataSetChanged()
            }
        )

        binding.newMessageUsersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
            setHasFixedSize(false)
        }

        binding.newMessageSelectedUsersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedUserAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupClicks() = with(binding) {
        newMessageBackButton.setOnClickListener {
            dismiss()
        }

        newMessageCreateGroupButton.setOnClickListener {
            createGroupChat()
        }
    }

    private fun setupSearchInput() = with(binding) {
        newMessageSearchEditText.doAfterTextChanged { editable ->
            val query = editable
                ?.toString()
                ?.trim()
                .orEmpty()

            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                searchUsers(query)
            }
        }

        newMessageGroupNameEditText.doAfterTextChanged {
            updateCreateGroupButton()
        }
    }

    private fun setupModeCards() = with(binding) {
        newMessageDirectCard.setOnClickListener {
            selectedMode = MessageStartMode.DIRECT
            updateModeUi()
        }

        newMessageGroupCard.setOnClickListener {
            selectedMode = MessageStartMode.GROUP
            updateModeUi()
        }
    }

    private fun updateModeCards() = with(binding) {
        val isDirectSelected = selectedMode == MessageStartMode.DIRECT

        newMessageDirectCard.strokeColor = ContextCompat.getColor(
            requireContext(),
            if (isDirectSelected) R.color.fitto_green else R.color.fitto_primary_soft
        )
        newMessageDirectCard.strokeWidth = if (isDirectSelected) SELECTED_MODE_STROKE_WIDTH else DEFAULT_MODE_STROKE_WIDTH
        newMessageDirectCard.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (isDirectSelected) R.color.fitto_primary_extra_soft else R.color.fitto_surface
            )
        )

        newMessageGroupCard.strokeColor = ContextCompat.getColor(
            requireContext(),
            if (!isDirectSelected) R.color.fitto_green else R.color.fitto_primary_soft
        )
        newMessageGroupCard.strokeWidth = if (!isDirectSelected) SELECTED_MODE_STROKE_WIDTH else DEFAULT_MODE_STROKE_WIDTH
        newMessageGroupCard.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (!isDirectSelected) R.color.fitto_primary_extra_soft else R.color.fitto_surface
            )
        )
    }

    private fun updateModeUi() = with(binding) {
        updateModeCards()

        val isGroupMode = selectedMode == MessageStartMode.GROUP

        newMessageTitleText.text = getString(
            if (isGroupMode) R.string.messages_new_group else R.string.messages_new_chat
        )
        newMessageSearchEditText.hint = getString(R.string.messages_new_chat_search_hint)

        newMessageGroupNameInputLayout.isVisible = isGroupMode
        newMessageSelectedUsersRecyclerView.isVisible = isGroupMode && selectedUsers.isNotEmpty()
        newMessageCreateGroupButton.isVisible = isGroupMode
        newMessageHelperText.isVisible = isGroupMode
        newMessageGroupDescriptionInputLayout.isVisible = isGroupMode

        if (!isGroupMode) {
            selectedUsers.clear()
            updateSelectedUsers()
        }

        userAdapter.notifyDataSetChanged()
        updateCreateGroupButton()
    }

    private fun searchUsers(query: String) {
        if (query.length < 2) {
            latestUsers = emptyList()
            showStartState()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val users = runCatching {
                repository.searchUsers(query)
            }.getOrElse {
                emptyList()
            }

            if (_binding == null) return@launch

            latestUsers = users
            userAdapter.submitList(users)
            binding.newMessageStateText.text = getString(R.string.messages_new_chat_empty)
            binding.newMessageStateText.isVisible = users.isEmpty()
            binding.newMessageUsersRecyclerView.isVisible = users.isNotEmpty()
        }
    }

    private fun showStartState() = with(binding) {
        userAdapter.submitList(emptyList())
        newMessageStateText.text = getString(
            if (selectedMode == MessageStartMode.GROUP) {
                R.string.messages_new_group_start
            } else {
                R.string.messages_new_chat_start
            }
        )
        newMessageStateText.isVisible = true
        newMessageUsersRecyclerView.isVisible = false
    }

    private fun startDirectChat(user: SearchUserUiModel) {
        if (isStartingChat) return

        isStartingChat = true

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.createOrGetDirectThread(user.id)
            }.onSuccess { thread ->
                if (_binding == null) return@onSuccess

                if (thread != null) {
                    dismiss()
                    ChatDetailFragment
                        .newInstance(thread)
                        .show(parentFragmentManager, "ChatDetailFragment")
                } else {
                    showStartChatError()
                }
            }.onFailure {
                if (_binding != null) showStartChatError()
            }

            isStartingChat = false
        }
    }

    private fun toggleGroupUser(user: SearchUserUiModel) {
        if (selectedUsers.containsKey(user.id)) {
            selectedUsers.remove(user.id)
        } else {
            selectedUsers[user.id] = user
        }

        updateSelectedUsers()
        userAdapter.submitList(latestUsers.toList())
        userAdapter.notifyDataSetChanged()
    }

    private fun updateSelectedUsers() = with(binding) {
        val selectedList = selectedUsers.values.toList()
        selectedUserAdapter.submitList(selectedList)
        newMessageSelectedUsersRecyclerView.isVisible = selectedMode == MessageStartMode.GROUP && selectedList.isNotEmpty()
        newMessageHelperText.text = resources.getQuantityString(
            R.plurals.messages_selected_members_count,
            selectedList.size,
            selectedList.size
        )
        updateCreateGroupButton()
    }

    private fun createGroupChat() = with(binding) {
        if (isStartingChat) return@with

        val title = newMessageGroupNameEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (title.isBlank()) {
            newMessageGroupNameInputLayout.error = getString(R.string.messages_group_name_empty)
            return@with
        }

        if (selectedUsers.size < MIN_GROUP_SELECTED_USERS) {
            Toast.makeText(
                requireContext(),
                getString(R.string.messages_group_min_members),
                Toast.LENGTH_SHORT
            ).show()
            return@with
        }
        val description = newMessageGroupDescriptionEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        isStartingChat = true
        updateCreateGroupButton()

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.createGroupThread(
                    title = title,
                    description = description,
                    selectedUsers = selectedUsers.values.toList()
                )
            }.onSuccess { thread ->
                if (_binding == null) return@onSuccess

                if (thread != null) {
                    dismiss()
                    ChatDetailFragment
                        .newInstance(thread)
                        .show(parentFragmentManager, "ChatDetailFragment")
                } else {
                    showStartChatError()
                }
            }.onFailure {
                if (_binding != null) showStartChatError()
            }

            isStartingChat = false
            if (_binding != null) updateCreateGroupButton()
        }
    }

    private fun updateCreateGroupButton() = with(binding) {
        val title = newMessageGroupNameEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()
        val canCreate = selectedMode == MessageStartMode.GROUP &&
                title.isNotBlank() &&
                selectedUsers.size >= MIN_GROUP_SELECTED_USERS &&
                !isStartingChat

        newMessageCreateGroupButton.isEnabled = canCreate
        newMessageCreateGroupButton.alpha = if (canCreate) 1f else 0.45f

        if (title.isNotBlank()) {
            newMessageGroupNameInputLayout.error = null
        }
    }

    private fun showStartChatError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.messages_new_chat_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        binding.newMessageUsersRecyclerView.adapter = null
        binding.newMessageSelectedUsersRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private enum class MessageStartMode {
        DIRECT,
        GROUP
    }

    private companion object {
        const val MIN_GROUP_SELECTED_USERS = 2
        const val DEFAULT_MODE_STROKE_WIDTH = 2
        const val SELECTED_MODE_STROKE_WIDTH = 3
    }
}
