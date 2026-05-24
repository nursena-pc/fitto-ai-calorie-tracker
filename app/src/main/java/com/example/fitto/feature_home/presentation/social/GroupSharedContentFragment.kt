package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.fitto.databinding.FragmentGroupSharedContentBinding
import kotlinx.coroutines.launch

class GroupSharedContentFragment : DialogFragment(R.layout.fragment_group_shared_content) {

    private var _binding: FragmentGroupSharedContentBinding? = null
    private val binding get() = _binding!!

    private val repository = SocialRepository()
    private val adapter = SharedContentAdapter(::onSharedContentClick)

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
    }

    private var allItems: List<SharedContentUiModel> = emptyList()
    private var selectedType: SharedContentType = SharedContentType.LINK

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
        _binding = FragmentGroupSharedContentBinding.bind(view)

        setupRecyclerView()
        setupClicks()
        loadSharedContent()
    }

    private fun setupRecyclerView() = with(binding) {
        sharedContentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        sharedContentRecyclerView.adapter = adapter
    }

    private fun setupClicks() = with(binding) {
        sharedContentBackButton.setOnClickListener {
            dismiss()
        }

        sharedContentChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = when (checkedIds.firstOrNull()) {
                R.id.mediaChip -> SharedContentType.MEDIA
                R.id.linksChip -> SharedContentType.LINK
                R.id.filesChip -> SharedContentType.FILE
                else -> SharedContentType.LINK
            }

            renderSelectedItems()
        }
    }

    private fun loadSharedContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.getThreadSharedContent(threadId)
            }.onSuccess { items ->
                allItems = items
                renderSelectedItems()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_shared_content_load_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun renderSelectedItems() = with(binding) {
        val filteredItems = allItems.filter { it.type == selectedType }

        adapter.submitList(filteredItems)

        sharedContentEmptyText.isVisible = filteredItems.isEmpty()
        sharedContentRecyclerView.isVisible = filteredItems.isNotEmpty()

        sharedContentEmptyText.text = when (selectedType) {
            SharedContentType.MEDIA -> getString(R.string.group_shared_media_empty)
            SharedContentType.LINK -> getString(R.string.group_shared_links_empty)
            SharedContentType.FILE -> getString(R.string.group_shared_files_empty)
        }
    }

    private fun onSharedContentClick(item: SharedContentUiModel) {
        if (item.url.isBlank()) return

        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            startActivity(intent)
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.group_shared_content_open_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"

        fun newInstance(threadId: String): GroupSharedContentFragment {
            return GroupSharedContentFragment().apply {
                arguments = bundleOf(ARG_THREAD_ID to threadId)
            }
        }
    }
}