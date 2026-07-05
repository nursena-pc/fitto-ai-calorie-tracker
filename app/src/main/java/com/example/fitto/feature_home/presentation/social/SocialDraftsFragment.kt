package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentSocialDraftsBinding
import kotlinx.coroutines.launch

class SocialDraftsFragment : DialogFragment(R.layout.fragment_social_drafts) {

    private var _binding: FragmentSocialDraftsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SocialViewModel
    private lateinit var adapter: SocialProfilePostAdapter

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

        _binding = FragmentSocialDraftsBinding.bind(view)
        viewModel = ViewModelProvider(
            this,
            requireContext().fittoAppContainer.socialFeatureDependencies.createViewModelFactory()
        )[SocialViewModel::class.java]

        adapter = SocialProfilePostAdapter(
            onPostClick = { post ->
                CreatePostFragment
                    .newDraftInstance(post.id)
                    .show(parentFragmentManager, "CreatePostFragment")
            }
        )

        binding.draftsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@SocialDraftsFragment.adapter
            setHasFixedSize(false)
        }

        binding.draftsBackButton.setOnClickListener { dismiss() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profilePageDrafts.collect { drafts ->
                    binding.draftsEmptyText.isVisible = drafts.isEmpty()
                    binding.draftsRecyclerView.isVisible = drafts.isNotEmpty()
                    adapter.submitList(drafts)
                }
            }
        }

        viewModel.loadProfilePageContent()
    }

    override fun onDestroyView() {
        binding.draftsRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
