package com.example.fitto.feature_auth.register

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentConsentBinding
import com.example.fitto.databinding.PopupLanguageMenuBinding
import com.example.fitto.feature_home.presentation.profile.ProfileLanguage
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
class ConsentFragment : Fragment(R.layout.fragment_consent) {

    private var _binding: FragmentConsentBinding? = null
    private val binding get() = _binding!!

    private var selectedLanguage: ProfileLanguage = ProfileLanguage.TURKISH

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentConsentBinding.bind(view)
        binding.root.setOnboardingProgress(1)

        selectedLanguage = ProfilePreferenceStore.getLanguage(requireContext())
        renderLanguageButton()

        binding.consentBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.languageButton.setOnClickListener {
            showLanguagePopup()
        }

        binding.consentAcceptButton.setOnClickListener {
            findNavController().navigate(R.id.action_consentFragment_to_regionFragment)
        }

        binding.consentDeclineButton.setOnClickListener {
            showDeclineDialog()
        }
    }
    private fun renderLanguageButton() = with(binding) {
        languageText.text = when (selectedLanguage) {
            ProfileLanguage.TURKISH -> getString(R.string.language_turkish)
            ProfileLanguage.ENGLISH -> getString(R.string.language_english)
        }
    }
    private fun showLanguagePopup() {
        val popupBinding = PopupLanguageMenuBinding.inflate(layoutInflater)

        popupBinding.turkishRadio.isChecked = selectedLanguage == ProfileLanguage.TURKISH
        popupBinding.englishRadio.isChecked = selectedLanguage == ProfileLanguage.ENGLISH

        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = 8f
        }

        fun selectLanguage(language: ProfileLanguage) {
            selectedLanguage = language
            renderLanguageButton()
            popupWindow.dismiss()

            ProfilePreferenceStore.setLanguage(
                context = requireContext(),
                language = language
            )
        }

        popupBinding.turkishOption.setOnClickListener {
            selectLanguage(ProfileLanguage.TURKISH)
        }

        popupBinding.turkishRadio.setOnClickListener {
            selectLanguage(ProfileLanguage.TURKISH)
        }

        popupBinding.englishOption.setOnClickListener {
            selectLanguage(ProfileLanguage.ENGLISH)
        }

        popupBinding.englishRadio.setOnClickListener {
            selectLanguage(ProfileLanguage.ENGLISH)
        }

        popupWindow.showAsDropDown(binding.languageButton, 0, 8)
    }

    private fun showDeclineDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.consent_dialog_title))
            .setMessage(getString(R.string.consent_dialog_message))
            .setPositiveButton(getString(R.string.consent_dialog_understand)) { _, _ ->
                findNavController().popBackStack(R.id.welcomeFragment, false)
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}