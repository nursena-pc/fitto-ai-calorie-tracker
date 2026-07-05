package com.example.fitto.feature_health

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetHealthConnectBinding
import com.example.fitto.fittoAppContainer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class HealthConnectBottomSheetFragment : BottomSheetDialogFragment(
    R.layout.bottom_sheet_health_connect
) {

    private var _binding: BottomSheetHealthConnectBinding? = null
    private val binding get() = _binding!!

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var connectionCoordinator: HealthConnectConnectionCoordinator
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>
    private var connectionState = HealthConnectConnectionState(
        HealthConnectConnectionStatus.UNAVAILABLE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dependencies = requireContext()
            .fittoAppContainer
            .createHealthConnectConnectionDependencies()
        healthConnectManager = dependencies.manager
        connectionCoordinator = dependencies.coordinator
        permissionLauncher = registerForActivityResult(
            healthConnectManager.permissionContract()
        ) { grantedPermissions ->
            handlePermissionOutcome(
                connectionCoordinator.resolvePermissionOutcome(grantedPermissions)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetHealthConnectBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.laterButton.setOnClickListener {
            dismiss()
        }
        binding.connectHealthButton.setOnClickListener {
            handlePrimaryAction()
        }

        loadAndRenderState()
    }

    private fun loadAndRenderState() {
        viewLifecycleOwner.lifecycleScope.launch {
            connectionState = connectionCoordinator.loadState()
            renderState(connectionState)
        }
    }

    private fun renderState(state: HealthConnectConnectionState) = with(binding) {
        when (state.status) {
            HealthConnectConnectionStatus.CONNECTED -> {
                healthConnectStatusText.text = getString(R.string.health_connect_connected)
                healthConnectSubtitleText.text = getString(
                    R.string.health_connect_connected_desc
                )
                connectHealthButton.text = getString(R.string.health_connect_connected)
            }

            HealthConnectConnectionStatus.READY_TO_CONNECT -> {
                healthConnectStatusText.text = getString(
                    R.string.health_connect_not_connected
                )
                healthConnectSubtitleText.text = getString(
                    R.string.health_connect_not_connected_desc
                )
                connectHealthButton.text = getString(R.string.health_connect_connect)
            }

            HealthConnectConnectionStatus.INSTALL_OR_UPDATE_REQUIRED,
            HealthConnectConnectionStatus.UNAVAILABLE -> {
                healthConnectStatusText.text = getString(R.string.health_connect_unavailable)
                healthConnectSubtitleText.text = getString(
                    R.string.health_connect_unavailable_desc
                )
                connectHealthButton.text = getString(
                    R.string.health_connect_update_or_install
                )
            }
        }
        connectHealthButton.isEnabled = state.isPrimaryActionEnabled
    }

    private fun handlePrimaryAction() {
        when (connectionCoordinator.resolvePrimaryAction(connectionState)) {
            HealthConnectPrimaryAction.REQUEST_PERMISSIONS -> {
                permissionLauncher.launch(connectionCoordinator.requestedPermissions)
            }

            HealthConnectPrimaryAction.OPEN_INSTALL_OR_UPDATE -> {
                healthConnectManager.openInstallOrUpdateScreen()
            }

            HealthConnectPrimaryAction.NONE -> Unit
        }
    }

    private fun handlePermissionOutcome(outcome: HealthConnectPermissionOutcome) {
        when (outcome) {
            HealthConnectPermissionOutcome.CONNECTED -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.health_connect_permission_granted),
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.setFragmentResult(
                    REQUEST_HEALTH_CONNECT_CHANGED,
                    bundleOf(BUNDLE_CONNECTED to true)
                )
                dismiss()
            }

            HealthConnectPermissionOutcome.DENIED -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.health_connect_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                loadAndRenderState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_HEALTH_CONNECT_CHANGED = "request_health_connect_changed"
        const val BUNDLE_CONNECTED = "bundle_connected"
    }
}
