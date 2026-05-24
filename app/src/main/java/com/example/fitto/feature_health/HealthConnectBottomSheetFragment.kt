package com.example.fitto.feature_health

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetHealthConnectBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class HealthConnectBottomSheetFragment : BottomSheetDialogFragment(
    R.layout.bottom_sheet_health_connect
) {

    private var _binding: BottomSheetHealthConnectBinding? = null
    private val binding get() = _binding!!
    private lateinit var healthConnectManager: HealthConnectManager

    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetHealthConnectBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        healthConnectManager = HealthConnectManager(requireContext().applicationContext)

        binding.laterButton.setOnClickListener {
            dismiss()
        }

        binding.connectHealthButton.setOnClickListener {
            handlePrimaryAction()
        }

        renderState()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            HealthConnectManager(requireContext().applicationContext).permissionContract()
        ) { granted ->
            if (::healthConnectManager.isInitialized &&
                granted.containsAll(healthConnectManager.permissions)
            ) {
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
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.health_connect_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                renderState()
            }
        }
    }
    private fun renderState() {
        viewLifecycleOwner.lifecycleScope.launch {
            val isAvailable = healthConnectManager.isAvailable()
            val needsInstallOrUpdate = healthConnectManager.needsInstallOrUpdate()
            val hasPermission = if (isAvailable) {
                healthConnectManager.hasAllPermissions()
            } else {
                false
            }

            when {
                hasPermission -> {
                    binding.healthConnectStatusText.text =
                        getString(R.string.health_connect_connected)
                    binding.healthConnectSubtitleText.text =
                        getString(R.string.health_connect_connected_desc)
                    binding.connectHealthButton.text =
                        getString(R.string.health_connect_connected)
                    binding.connectHealthButton.isEnabled = false
                }

                isAvailable -> {
                    binding.healthConnectStatusText.text =
                        getString(R.string.health_connect_not_connected)
                    binding.healthConnectSubtitleText.text =
                        getString(R.string.health_connect_not_connected_desc)
                    binding.connectHealthButton.text =
                        getString(R.string.health_connect_connect)
                    binding.connectHealthButton.isEnabled = true
                }

                needsInstallOrUpdate -> {
                    binding.healthConnectStatusText.text =
                        getString(R.string.health_connect_unavailable)
                    binding.healthConnectSubtitleText.text =
                        getString(R.string.health_connect_unavailable_desc)
                    binding.connectHealthButton.text =
                        getString(R.string.health_connect_update_or_install)
                    binding.connectHealthButton.isEnabled = true
                }

                else -> {
                    binding.healthConnectStatusText.text =
                        getString(R.string.health_connect_unavailable)
                    binding.healthConnectSubtitleText.text =
                        getString(R.string.health_connect_unavailable_desc)
                    binding.connectHealthButton.text =
                        getString(R.string.health_connect_update_or_install)
                    binding.connectHealthButton.isEnabled = false
                }
            }
        }
    }

    private fun handlePrimaryAction() {
        when {
            healthConnectManager.isAvailable() -> {
                permissionLauncher.launch(healthConnectManager.permissions)
            }

            healthConnectManager.needsInstallOrUpdate() -> {
                healthConnectManager.openInstallOrUpdateScreen()
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