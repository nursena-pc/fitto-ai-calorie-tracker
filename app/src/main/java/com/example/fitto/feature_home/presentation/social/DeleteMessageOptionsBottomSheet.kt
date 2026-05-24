package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetDeleteMessageBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DeleteMessageOptionsBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_delete_message) {

    private var _binding: BottomSheetDeleteMessageBinding? = null
    private val binding get() = _binding!!

    private val allowDeleteForEveryone: Boolean by lazy {
        requireArguments().getBoolean(ARG_ALLOW_DELETE_FOR_EVERYONE, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = BottomSheetDeleteMessageBinding.bind(view)

        setupUi()
        setupClicks()
    }

    private fun setupUi() = with(binding) {
        deleteForEveryoneContainer.isVisible = allowDeleteForEveryone
    }

    private fun setupClicks() = with(binding) {
        deleteForMeContainer.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_DELETE_MESSAGE_ACTION,
                bundleOf(BUNDLE_KEY_DELETE_FOR_EVERYONE to false)
            )
            dismiss()
        }

        deleteForEveryoneContainer.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_DELETE_MESSAGE_ACTION,
                bundleOf(BUNDLE_KEY_DELETE_FOR_EVERYONE to true)
            )
            dismiss()
        }

        deleteCancelText.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ALLOW_DELETE_FOR_EVERYONE = "arg_allow_delete_for_everyone"

        const val REQUEST_KEY_DELETE_MESSAGE_ACTION = "request_key_delete_message_action"
        const val BUNDLE_KEY_DELETE_FOR_EVERYONE = "bundle_key_delete_for_everyone"

        fun newInstance(
            allowDeleteForEveryone: Boolean
        ): DeleteMessageOptionsBottomSheet {
            return DeleteMessageOptionsBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ALLOW_DELETE_FOR_EVERYONE to allowDeleteForEveryone
                )
            }
        }
    }
}