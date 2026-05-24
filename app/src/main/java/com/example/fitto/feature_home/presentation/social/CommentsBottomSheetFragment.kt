package com.example.fitto.feature_home.presentation.social

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetCommentsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CommentsBottomSheetFragment : BottomSheetDialogFragment(R.layout.bottom_sheet_comments) {

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SocialViewModel
    private lateinit var commentAdapter: SocialCommentAdapter

    private var replyingToComment: SocialComment? = null

    private val postId: String by lazy {
        requireArguments().getString(ARG_POST_ID).orEmpty()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = BottomSheetCommentsBinding.bind(view)
        viewModel = ViewModelProvider(requireParentFragment())[SocialViewModel::class.java]

        setupRecyclerView()
        setupInput()
        observeComments()

        viewModel.observeComments(postId)
    }

    private fun setupRecyclerView() {
        commentAdapter = SocialCommentAdapter(
            onReplyClick = { comment ->
                replyingToComment = comment
                updateReplyingView()
            },
            onLikeClick = { comment ->
                viewModel.toggleCommentLike(
                    postId = postId,
                    comment = comment
                )
            }
        )

        binding.commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupInput() {
        binding.cancelReplyText.setOnClickListener {
            replyingToComment = null
            updateReplyingView()
        }

        binding.commentEditText.doAfterTextChanged {
            updateSendButton()
        }

        binding.commentEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment()
                true
            } else {
                false
            }
        }

        binding.sendCommentButton.setOnClickListener {
            sendComment()
        }

        updateReplyingView()
        updateSendButton()
    }

    private fun observeComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.comments.collect { comments ->
                    commentAdapter.submitComments(comments)

                    binding.noCommentsText.isVisible = comments.isEmpty()
                    binding.commentsRecyclerView.isVisible = comments.isNotEmpty()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isCommentSending.collect { isSending ->
                    binding.sendCommentProgress.isVisible = isSending
                    binding.sendCommentButton.isVisible = !isSending
                    binding.commentEditText.isEnabled = !isSending
                    updateSendButton()
                }
            }
        }
    }

    private fun sendComment() {
        val text = binding.commentEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (text.isBlank()) return

        viewModel.addComment(
            postId = postId,
            text = text,
            parentCommentId = replyingToComment?.id
        )

        binding.commentEditText.setText("")
        replyingToComment = null
        updateReplyingView()
    }

    private fun updateReplyingView() {
        val comment = replyingToComment

        binding.replyingContainer.isVisible = comment != null

        if (comment != null) {
            binding.replyingText.text = getString(
                R.string.social_replying_to,
                comment.userName.ifBlank {
                    getString(R.string.social_default_user)
                }
            )

            binding.commentEditText.hint = getString(R.string.social_write_reply)
        } else {
            binding.commentEditText.hint = getString(R.string.social_write_comment)
        }
    }

    private fun updateSendButton() {
        val hasText = binding.commentEditText.text
            ?.toString()
            ?.trim()
            ?.isNotBlank() == true

        val isSending = viewModel.isCommentSending.value

        binding.sendCommentButton.isEnabled = hasText && !isSending
        binding.sendCommentButton.alpha = if (hasText && !isSending) 1f else 0.45f
    }

    override fun onDestroyView() {
        binding.commentsRecyclerView.adapter = null
        viewModel.clearComments()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: String): CommentsBottomSheetFragment {
            return CommentsBottomSheetFragment().apply {
                arguments = bundleOf(ARG_POST_ID to postId)
            }
        }
    }
}