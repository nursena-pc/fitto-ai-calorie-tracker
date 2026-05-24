package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.FragmentMessagesBinding
import kotlinx.coroutines.launch

class MessagesFragment : DialogFragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val repository = SocialRepository()
    private lateinit var threadAdapter: MessageThreadAdapter

    private var selectedFilter: MessageFilter = MessageFilter.ALL
    private var searchQuery: String = ""
    private var allThreads: List<MessageThreadUiModel> = emptyList()
    private val pendingDeletedThreadIds = mutableSetOf<String>()
    private val pendingDeletedAtByThreadId = mutableMapOf<String, Long>()

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
        _binding = FragmentMessagesBinding.bind(view)
        RealtimePresenceManager.start()

        setupRecyclerView()
        setupSwipeToDelete()
        setupClicks()
        setupSearch()
        setupFilters()
        observeThreads()
        updateThreads()
    }

    private fun setupRecyclerView() {
        threadAdapter = MessageThreadAdapter(
            onThreadClick = { thread -> showChatDetailScreen(thread) }
        )

        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = threadAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupSwipeToDelete() {
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_swipe_delete)
        val deleteBackground = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.reminder_delete))
        val clearBackground = ColorDrawable(Color.TRANSPARENT)

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val thread = threadAdapter.currentList.getOrNull(position)

                if (thread == null) {
                    threadAdapter.notifyItemChanged(position)
                    return
                }

                val deletionStartedAt = System.currentTimeMillis()
                pendingDeletedThreadIds.add(thread.id)
                pendingDeletedAtByThreadId[thread.id] = deletionStartedAt
                updateThreads()

                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { repository.deleteThreadForCurrentUser(thread.id) }
                        .onSuccess {
                            Toast.makeText(requireContext(), R.string.messages_thread_deleted, Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            pendingDeletedThreadIds.remove(thread.id)
                            pendingDeletedAtByThreadId.remove(thread.id)
                            updateThreads()
                            Toast.makeText(requireContext(), R.string.messages_thread_delete_error, Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX < 0) {
                    deleteBackground.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteBackground.draw(canvas)

                    deleteIcon?.let { icon ->
                        val margin = (24 * resources.displayMetrics.density).toInt()
                        val top = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val left = itemView.right - margin - icon.intrinsicWidth
                        icon.setBounds(left, top, left + icon.intrinsicWidth, top + icon.intrinsicHeight)
                        icon.draw(canvas)
                    }
                } else {
                    clearBackground.setBounds(itemView.right, itemView.top, itemView.right, itemView.bottom)
                    clearBackground.draw(canvas)
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.messagesRecyclerView)
    }

    private fun setupClicks() = with(binding) {
        messagesBackButton.setOnClickListener { dismiss() }

        messagesNewChatButton.setOnClickListener {
            NewMessageFragment().show(childFragmentManager, "NewMessageFragment")
        }
    }

    private fun setupSearch() = with(binding) {
        messagesSearchEditText.doAfterTextChanged { editable ->
            searchQuery = editable?.toString()?.trim().orEmpty()
            updateThreads()
        }
    }

    private fun setupFilters() = with(binding) {
        messagesTabGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.friendsTabButton

            selectedFilter = when (checkedId) {
                R.id.friendsTabButton -> MessageFilter.ALL
                R.id.unreadTabButton -> MessageFilter.UNREAD
                R.id.aiAssistantTabButton -> MessageFilter.AI_ASSISTANT
                R.id.groupsTabButton -> MessageFilter.GROUPS
                else -> MessageFilter.ALL
            }

            updateThreads()
        }
    }

    private fun observeThreads() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeMessageThreads().collect { threads ->
                    val revivedThreadIds = threads
                        .filter { thread ->
                            val deletedAt = pendingDeletedAtByThreadId[thread.id]
                            deletedAt != null && thread.lastMessageCreatedAt > deletedAt
                        }
                        .map { thread -> thread.id }

                    if (revivedThreadIds.isNotEmpty()) {
                        val revivedSet = revivedThreadIds.toSet()
                        pendingDeletedThreadIds.removeAll(revivedSet)
                        revivedSet.forEach { threadId -> pendingDeletedAtByThreadId.remove(threadId) }
                    }

                    allThreads = threads
                    updateThreads()
                }
            }
        }
    }

    private fun updateThreads() {
        if (_binding == null) return

        val filteredThreads = allThreads
            .filterNot { thread -> pendingDeletedThreadIds.contains(thread.id) }
            .filter { thread ->
                when (selectedFilter) {
                    MessageFilter.ALL -> true
                    MessageFilter.UNREAD -> thread.unreadCount > 0
                    MessageFilter.AI_ASSISTANT -> thread.id.contains("ai", ignoreCase = true) ||
                            thread.title.contains("ai", ignoreCase = true) ||
                            thread.title.contains("asistan", ignoreCase = true)
                    MessageFilter.GROUPS -> thread.isGroup
                }
            }
            .filter { thread ->
                searchQuery.isBlank() ||
                        thread.title.contains(searchQuery, ignoreCase = true) ||
                        thread.lastMessage.contains(searchQuery, ignoreCase = true)
            }

        binding.messagesEmptyText.text = when (selectedFilter) {
            MessageFilter.UNREAD -> getString(R.string.messages_unread_empty_state)
            MessageFilter.AI_ASSISTANT -> getString(R.string.messages_ai_empty_state)
            MessageFilter.GROUPS -> getString(R.string.messages_groups_empty_state)
            MessageFilter.ALL -> getString(R.string.messages_empty_state)
        }



        binding.messagesEmptyText.isVisible = filteredThreads.isEmpty()
        binding.messagesRecyclerView.isVisible = filteredThreads.isNotEmpty()
        threadAdapter.submitList(filteredThreads)
    }

    private fun showChatDetailScreen(thread: MessageThreadUiModel) {
        ChatDetailFragment
            .newInstance(thread)
            .show(childFragmentManager, "ChatDetailFragment")
    }

    override fun onDestroyView() {
        binding.messagesRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private enum class MessageFilter {
        ALL,
        UNREAD,
        AI_ASSISTANT,
        GROUPS
    }
}
