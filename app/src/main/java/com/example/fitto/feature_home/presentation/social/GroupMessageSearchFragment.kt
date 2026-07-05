package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentGroupMessageSearchBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

class GroupMessageSearchFragment : DialogFragment(R.layout.fragment_group_message_search) {

    private var _binding: FragmentGroupMessageSearchBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().fittoAppContainer.socialFeatureDependencies.repository
    }
    private val queryFlow = MutableStateFlow("")

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
    }

    private val threadTitle: String by lazy {
        requireArguments().getString(ARG_THREAD_TITLE).orEmpty()
    }

    private val threadInitials: String by lazy {
        requireArguments().getString(ARG_THREAD_INITIALS).orEmpty()
    }

    private val threadPhotoUrl: String? by lazy {
        requireArguments().getString(ARG_THREAD_PHOTO_URL)
    }

    private val memberCount: Int by lazy {
        requireArguments().getInt(ARG_MEMBER_COUNT, 0)
    }

    private val adapter by lazy {
        GroupMessageSearchAdapter { message ->
            openMessageInChat(message)
        }
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
        _binding = FragmentGroupMessageSearchBinding.bind(view)

        setupUi()
        observeSearch()
    }

    private fun setupUi() = with(binding) {
        searchToolbar.setNavigationOnClickListener {
            dismiss()
        }

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecyclerView.adapter = adapter

        searchEditText.doAfterTextChanged {
            queryFlow.value = it?.toString().orEmpty()
        }

        searchEditText.requestFocus()
        searchEditText.postDelayed({
            val manager = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 250L)
    }

    private fun observeSearch() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    repository.observeMessages(threadId),
                    queryFlow
                ) { messages, query ->
                    filterMessages(messages, query)
                }.collect { results ->
                    adapter.submitList(results)
                    renderSearchState(results, queryFlow.value)
                }
            }
        }
    }

    private fun filterMessages(
        messages: List<ChatMessageUiModel>,
        query: String
    ): List<ChatMessageUiModel> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        val lowerQuery = normalizedQuery.lowercase(Locale("tr", "TR"))

        return messages
            .filter { message ->
                !message.isDeleted &&
                        (
                                message.text.lowercase(Locale("tr", "TR")).contains(lowerQuery) ||
                                        message.senderName.lowercase(Locale("tr", "TR")).contains(lowerQuery)
                                )
            }
            .sortedWith(
                compareByDescending<ChatMessageUiModel> { message ->
                    message.text.lowercase(Locale("tr", "TR")).startsWith(lowerQuery)
                }.thenByDescending { message ->
                    message.createdAt
                }
            )
    }

    private fun renderSearchState(
        results: List<ChatMessageUiModel>,
        query: String
    ) = with(binding) {
        val hasQuery = query.isNotBlank()
        val hasResults = results.isNotEmpty()

        searchResultCountText.text = if (!hasQuery) {
            getString(R.string.group_search_start_typing)
        } else {
            getString(R.string.group_search_result_count, results.size)
        }

        searchResultsRecyclerView.isVisible = hasResults
        emptyStateLayout.isVisible = !hasResults

        if (!hasQuery) {
            emptyStateTitleText.text = getString(R.string.group_search_empty_title)
            emptyStateSubtitleText.text = getString(R.string.group_search_empty_subtitle)
        } else {
            emptyStateTitleText.text = getString(R.string.group_search_no_result_title)
            emptyStateSubtitleText.text = getString(
                R.string.group_search_no_result_subtitle,
                query
            )
        }
    }

    private fun openMessageInChat(message: ChatMessageUiModel) {
        val thread = MessageThreadUiModel(
            id = threadId,
            title = threadTitle,
            lastMessage = "",
            timeText = "",
            isGroup = true,
            initials = threadInitials,
            unreadCount = 0,
            participantIds = emptyList(),
            otherParticipantId = null,
            photoUrl = threadPhotoUrl,
            memberCount = memberCount
        )

        ChatDetailFragment
            .newInstance(
                thread = thread,
                highlightMessageId = message.id
            )
            .show(parentFragmentManager, "ChatDetailFromSearch_${message.id}")
    }

    override fun onDestroyView() {
        binding.searchResultsRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_THREAD_TITLE = "thread_title"
        private const val ARG_THREAD_INITIALS = "thread_initials"
        private const val ARG_THREAD_PHOTO_URL = "thread_photo_url"
        private const val ARG_MEMBER_COUNT = "member_count"

        fun newInstance(thread: MessageThreadUiModel): GroupMessageSearchFragment {
            return GroupMessageSearchFragment().apply {
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