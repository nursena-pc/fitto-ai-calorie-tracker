package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import kotlinx.coroutines.launch

class SocialPostDetailFragment : DialogFragment() {

    private lateinit var viewModel: SocialViewModel
    private lateinit var postAdapter: SocialPostAdapter

    private var recyclerView: RecyclerView? = null
    private var emptyTextView: TextView? = null

    private val postId: String by lazy {
        requireArguments().getString(ARG_POST_ID).orEmpty()
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createContentView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            requireContext().fittoAppContainer.socialFeatureDependencies.createViewModelFactory()
        )[SocialViewModel::class.java]
        setupAdapter()
        observePost()
        viewModel.observePostDetail(postId)
    }

    private fun createContentView(): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.profile_screen_background))
        }

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(8))
            background = ContextCompat.getDrawable(context, R.drawable.bg_profile_card)
        }

        val backButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_arrow_back)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.profile_text))
            background = ColorDrawable(Color.TRANSPARENT)
            setOnClickListener { dismiss() }
        }

        val titleText = TextView(context).apply {
            setText(R.string.social_post_detail_title)
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            includeFontPadding = false
        }

        topBar.addView(
            backButton,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        topBar.addView(
            titleText,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
            }
        )

        val list = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val emptyText = TextView(context).apply {
            setText(R.string.social_post_not_found)
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            textSize = 14f
            gravity = Gravity.CENTER
            isVisible = false
        }

        recyclerView = list
        emptyTextView = emptyText

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            list,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            emptyText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        return root
    }

    private fun setupAdapter() {
        postAdapter = SocialPostAdapter(
            onLikeClick = { post -> viewModel.toggleLike(post) },
            onCommentClick = { post -> showCommentsBottomSheet(post.id) },
            onSaveClick = { post -> viewModel.toggleSave(post) },
            onShareClick = { post ->
                PostShareHelper.sharePost(
                    context = requireContext(),
                    post = post
                )
            },
            onReportClick = { post -> showReportPostDialog(post) },
            onUserClick = { post ->
                if (post.userId.isNotBlank()) {
                    SocialProfileFragment
                        .newInstance(post.userId)
                        .show(childFragmentManager, "SocialProfileFragment")
                }
            }
        )

        recyclerView?.adapter = postAdapter
    }

    private fun observePost() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.postDetail.collect { post ->
                    val visiblePost = post?.takeUnless { it.isDraft }

                    recyclerView?.isVisible = visiblePost != null
                    emptyTextView?.isVisible = visiblePost == null
                    postAdapter.submitList(visiblePost?.let { listOf(it) }.orEmpty())
                }
            }
        }
    }

    private fun showCommentsBottomSheet(postId: String) {
        CommentsBottomSheetFragment
            .newInstance(postId)
            .show(childFragmentManager, "CommentsBottomSheet")
    }

    private fun showReportPostDialog(post: SocialPost) {
        ReportReasonDialogHelper.show(
            fragment = this,
            title = getString(R.string.report_title),
            subtitle = getString(R.string.report_post_subtitle),
            reasons = listOf(
                ReportReasonDialogHelper.ReasonItem(
                    key = "spam",
                    label = getString(R.string.report_reason_spam)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "harassment",
                    label = getString(R.string.report_reason_harassment)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "inappropriate_content",
                    label = getString(R.string.report_reason_inappropriate)
                ),
                ReportReasonDialogHelper.ReasonItem(
                    key = "other",
                    label = getString(R.string.report_reason_other)
                )
            ),
            onSubmit = { reason ->
                viewModel.reportPost(
                    post = post,
                    reason = reason.key
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.report_post_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        recyclerView?.adapter = null
        recyclerView = null
        emptyTextView = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: String): SocialPostDetailFragment {
            return SocialPostDetailFragment().apply {
                arguments = bundleOf(ARG_POST_ID to postId)
            }
        }
    }
}
