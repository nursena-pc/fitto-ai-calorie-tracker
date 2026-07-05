package com.example.fitto.feature_auth.presentation.welcome

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.BottomSheetLoginOptionsBinding
import com.example.fitto.databinding.FragmentWelcomeBinding
import com.example.fitto.feature_auth.login.GoogleSignInHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentWelcomeBinding.bind(view)

        view.doOnLayout {
            if (requireContext().fittoAppContainer.authFeatureDependencies
                    .shouldOpenHomeForCurrentSession()
            ) {
                findNavController().navigate(R.id.action_welcomeFragment_to_homeFragment)
            }
        }

        binding.welcomeLoginButton.setOnClickListener {
            showLoginOptionsSheet()
        }

        binding.welcomeRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_consentFragment)
        }
    }

    private fun renderRegisterText(sheetBinding: BottomSheetLoginOptionsBinding) {
        val fullText = getString(R.string.login_not_member_cta)
        val actionText = getString(R.string.login_create_account_action)
        val actionStart = fullText.indexOf(actionText)

        if (actionStart < 0) {
            sheetBinding.loginOptionsRegisterText.text = fullText
            return
        }

        sheetBinding.loginOptionsRegisterText.text = SpannableString(fullText).apply {
            setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.fitto_green)
                ),
                actionStart,
                actionStart + actionText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun showLoginOptionsSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetLoginOptionsBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)
        renderRegisterText(sheetBinding)

        sheetBinding.loginOptionsEmailButton.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }

        sheetBinding.loginOptionsRegisterText.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_welcomeFragment_to_consentFragment)
        }

        sheetBinding.loginOptionsGoogleButton.setOnClickListener {
            sheetBinding.loginOptionsGoogleButton.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                GoogleSignInHelper.signIn(
                    context = requireContext(),
                    onSuccess = {
                        dialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.login_button),
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigate(R.id.action_welcomeFragment_to_homeFragment)
                    },
                    onError = { message ->
                        sheetBinding.loginOptionsGoogleButton.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}