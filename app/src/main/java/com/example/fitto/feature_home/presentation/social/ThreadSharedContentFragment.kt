package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import kotlinx.coroutines.launch

class ThreadSharedContentFragment : DialogFragment(R.layout.fragment_thread_shared_content) {

    private val repository = SocialRepository()
    private lateinit var adapter: SharedContentAdapter

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
        val backButton = view.findViewById<View>(R.id.sharedContentBackButton)
        val recyclerView = view.findViewById<RecyclerView>(R.id.sharedContentRecyclerView)
        val emptyText = view.findViewById<TextView>(R.id.sharedContentEmptyText)
        val progress = view.findViewById<View>(R.id.sharedContentProgress)

        adapter = SharedContentAdapter { item ->
            openSharedContent(item)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        backButton.setOnClickListener {
            dismiss()
        }

        progress.isVisible = true
        emptyText.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.getThreadSharedContent(threadId)
            }.onSuccess { items ->
                progress.isVisible = false
                emptyText.isVisible = items.isEmpty()
                adapter.submitList(items)
            }.onFailure {
                progress.isVisible = false
                emptyText.isVisible = true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.chat_shared_content_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openSharedContent(item: SharedContentUiModel) {
        val url = item.url

        if (url.isBlank()) return

        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.chat_shared_content_open_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val ARG_THREAD_ID = "arg_thread_id"

        fun newInstance(threadId: String): ThreadSharedContentFragment {
            return ThreadSharedContentFragment().apply {
                arguments = bundleOf(ARG_THREAD_ID to threadId)
            }
        }
    }
}