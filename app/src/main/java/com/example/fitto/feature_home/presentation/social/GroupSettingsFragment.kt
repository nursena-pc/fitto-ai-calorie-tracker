package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.FragmentGroupSettingsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GroupSettingsFragment : DialogFragment(R.layout.fragment_group_settings) {

    private var _binding: FragmentGroupSettingsBinding? = null
    private val binding get() = _binding!!

    private val repository = SocialRepository()

    private val threadId: String by lazy {
        requireArguments().getString(ARG_THREAD_ID).orEmpty()
    }

    private var currentSettings = GroupSettingsUiModel()
    private var saveJob: Job? = null
    private var isBindingSettings = false

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
        _binding = FragmentGroupSettingsBinding.bind(view)

        setupClicks()
        loadSettings()
    }

    private fun setupClicks() = with(binding) {
        groupSettingsToolbar.setNavigationOnClickListener {
            dismiss()
        }

        joinApprovalSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isBindingSettings) {
                updateSettings(currentSettings.copy(joinApprovalRequired = isChecked))
            }
        }

        membersCanSendMessagesSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isBindingSettings) {
                updateSettings(currentSettings.copy(membersCanSendMessages = isChecked))
            }
        }

        membersCanAddMembersSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isBindingSettings) {
                updateSettings(currentSettings.copy(membersCanAddMembers = isChecked))
            }
        }

        membersCanEditGroupInfoSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isBindingSettings) {
                updateSettings(currentSettings.copy(membersCanEditGroupInfo = isChecked))
            }
        }
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.getGroupSettings(threadId)
            }.onSuccess { settings ->
                currentSettings = settings
                renderSettings(settings)
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_settings_load_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun renderSettings(settings: GroupSettingsUiModel) = with(binding) {
        isBindingSettings = true

        joinApprovalSwitch.isChecked = settings.joinApprovalRequired
        membersCanSendMessagesSwitch.isChecked = settings.membersCanSendMessages
        membersCanAddMembersSwitch.isChecked = settings.membersCanAddMembers
        membersCanEditGroupInfoSwitch.isChecked = settings.membersCanEditGroupInfo

        isBindingSettings = false
    }

    private fun updateSettings(newSettings: GroupSettingsUiModel) {
        currentSettings = newSettings
        renderSettings(newSettings)

        saveJob?.cancel()
        saveJob = viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repository.updateGroupSettings(threadId, newSettings)
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_settings_saved),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.group_settings_save_error),
                    Toast.LENGTH_SHORT
                ).show()

                loadSettings()
            }
        }
    }

    override fun onDestroyView() {
        saveJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_THREAD_ID = "thread_id"

        fun newInstance(threadId: String): GroupSettingsFragment {
            return GroupSettingsFragment().apply {
                arguments = bundleOf(ARG_THREAD_ID to threadId)
            }
        }
    }
}