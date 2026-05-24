package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentUserInfoBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar
import java.util.TimeZone

class UserInfoFragment : Fragment(R.layout.fragment_user_info) {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    private var selectedGender: Gender? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentUserInfoBinding.bind(view)
        binding.root.setOnboardingProgress(3)
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        setupInputs()
        setupClicks()
        updateGenderSelection()
    }

    private fun setupInputs() = with(binding) {
        nameEditText.doAfterTextChanged { editable ->
            showError(nameErrorText, validateName(editable?.toString().orEmpty()))
        }

        heightEditText.doAfterTextChanged { editable ->
            val filtered = editable?.toString().orEmpty().filter { it.isDigit() }
            if (filtered != editable?.toString().orEmpty()) {
                heightEditText.setText(filtered)
                heightEditText.setSelection(filtered.length)
            }

            showError(heightErrorText, validateHeight(filtered))
        }

        weightEditText.doAfterTextChanged { editable ->
            val filtered = editable?.toString().orEmpty().filter { it.isDigit() }
            if (filtered != editable?.toString().orEmpty()) {
                weightEditText.setText(filtered)
                weightEditText.setSelection(filtered.length)
            }

            showError(weightErrorText, validateWeight(filtered))
        }

        ageEditText.doAfterTextChanged { editable ->
            val current = editable?.toString().orEmpty()
            val filtered = current.filter { it.isDigit() }

            if (filtered != current) {
                ageEditText.setText(filtered)
                ageEditText.setSelection(filtered.length)
                return@doAfterTextChanged
            }

            showError(ageErrorText, validateAge(filtered))
        }
    }

    private fun setupClicks() = with(binding) {
        userInfoBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        ageInputContainer.setOnClickListener {
            ageEditText.requestFocus()
            showKeyboard(ageEditText)
        }

        ageCalendarIcon.setOnClickListener {
            showDatePicker()
        }

        womanOption.setOnClickListener {
            selectedGender = Gender.FEMALE
            genderErrorText.isVisible = false
            updateGenderSelection()
        }

        manOption.setOnClickListener {
            selectedGender = Gender.MALE
            genderErrorText.isVisible = false
            updateGenderSelection()
        }

        preferNotOption.setOnClickListener {
            selectedGender = Gender.PREFER_NOT_TO_SAY
            genderErrorText.isVisible = false
            updateGenderSelection()
        }

        userInfoNextButton.setOnClickListener {
            submitForm()
        }
    }
    private fun showKeyboard(view: View) {
        val inputMethodManager = requireContext()
            .getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

        inputMethodManager.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    private fun submitForm() = with(binding) {
        clearErrors()

        val name = nameEditText.text?.toString()?.trim().orEmpty()
        val age = ageEditText.text?.toString()?.trim().orEmpty()
        val height = heightEditText.text?.toString()?.trim().orEmpty()
        val weight = weightEditText.text?.toString()?.trim().orEmpty()

        val nameError = validateName(name)
        val ageError = validateAge(age)
        val heightError = validateHeight(height)
        val weightError = validateWeight(weight)
        val genderError = validateGender(selectedGender)

        showError(nameErrorText, nameError)
        showError(ageErrorText, ageError)
        showError(heightErrorText, heightError)
        showError(weightErrorText, weightError)
        showError(genderErrorText, genderError)

        val isValid = nameError == null &&
                ageError == null &&
                heightError == null &&
                weightError == null &&
                genderError == null

        if (!isValid) return@with

        onboardingViewModel.saveUserInfo(
            name = name,
            height = height.toInt(),
            weight = weight.toInt(),
            age = age.toInt(),
            gender = selectedGender!!
        )

        findNavController().navigate(R.id.action_userInfoFragment_to_introGreetingFragment)
    }

    private fun clearErrors() = with(binding) {
        nameErrorText.isVisible = false
        nameErrorText.text = ""

        ageErrorText.isVisible = false
        ageErrorText.text = ""

        heightErrorText.isVisible = false
        heightErrorText.text = ""

        weightErrorText.isVisible = false
        weightErrorText.text = ""

        genderErrorText.isVisible = false
        genderErrorText.text = ""
    }

    private fun showError(
        textView: TextView,
        message: String?
    ) {
        textView.text = message.orEmpty()
        textView.isVisible = message != null
    }

    private fun validateName(value: String): String? {
        val trimmed = value.trim()

        return when {
            trimmed.isEmpty() -> getString(R.string.error_name_empty)
            trimmed.length < 2 -> getString(R.string.error_name_invalid)
            else -> null
        }
    }

    private fun validateAge(value: String): String? {
        if (value.isBlank()) return getString(R.string.error_age_empty)

        val parsed = value.toIntOrNull()
            ?: return getString(R.string.error_age_invalid)

        if (parsed !in 10..100) {
            return getString(R.string.error_age_range)
        }

        return null
    }

    private fun validateHeight(value: String): String? {
        if (value.isBlank()) return getString(R.string.error_height_empty)

        val parsed = value.toIntOrNull()
            ?: return getString(R.string.error_height_invalid)

        if (parsed !in 100..250) {
            return getString(R.string.error_height_range)
        }

        return null
    }

    private fun validateWeight(value: String): String? {
        if (value.isBlank()) return getString(R.string.error_weight_empty)

        val parsed = value.toIntOrNull()
            ?: return getString(R.string.error_weight_invalid)

        if (parsed !in 25..300) {
            return getString(R.string.error_weight_range)
        }

        return null
    }

    private fun validateGender(value: Gender?): String? {
        return if (value == null) {
            getString(R.string.error_gender_empty)
        } else {
            null
        }
    }

    private fun showDatePicker() {
        val todayUtc = MaterialDatePicker.todayInUtcMilliseconds()

        val defaultBirthDateUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = todayUtc
            add(Calendar.YEAR, -20)
        }.timeInMillis

        val constraints = CalendarConstraints.Builder()
            .setEnd(todayUtc)
            .build()

        val picker: MaterialDatePicker<Long> = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)
            .setTitleText(getString(R.string.age_label))
            .setSelection(defaultBirthDateUtc)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection: Long ->
            val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }

            val calculatedAge = calculateAge(
                birthYear = selectedDate.get(Calendar.YEAR),
                birthMonth = selectedDate.get(Calendar.MONTH),
                birthDay = selectedDate.get(Calendar.DAY_OF_MONTH)
            )

            binding.ageEditText.setText(calculatedAge.toString())
            showError(binding.ageErrorText, validateAge(calculatedAge.toString()))
        }

        picker.show(parentFragmentManager, "fitto_age_picker")
    }

    private fun calculateAge(
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int
    ): Int {
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthYear

        val currentMonth = today.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        val hasBirthdayPassedThisYear =
            currentMonth > birthMonth ||
                    (currentMonth == birthMonth && currentDay >= birthDay)

        if (!hasBirthdayPassedThisYear) {
            age--
        }

        return age.coerceAtLeast(0)
    }

    private fun updateGenderSelection() = with(binding) {
        updateGenderOption(
            option = womanOption,
            textView = womanOptionText,
            iconView = womanOptionIcon,
            selected = selectedGender == Gender.FEMALE
        )

        updateGenderOption(
            option = manOption,
            textView = manOptionText,
            iconView = manOptionIcon,
            selected = selectedGender == Gender.MALE
        )

        updateGenderOption(
            option = preferNotOption,
            textView = preferNotOptionText,
            iconView = preferNotOptionIcon,
            selected = selectedGender == Gender.PREFER_NOT_TO_SAY
        )
    }

    private fun updateGenderOption(
        option: LinearLayout,
        textView: TextView,
        iconView: ImageView,
        selected: Boolean
    ) {
        option.setBackgroundResource(
            if (selected) {
                R.drawable.bg_fitto_card_selected
            } else {
                R.drawable.bg_fitto_card_normal
            }
        )

        val color = requireContext().getColor(
            if (selected) {
                R.color.fitto_green
            } else {
                R.color.fitto_text_dark
            }
        )

        textView.setTextColor(color)
        iconView.setColorFilter(color)

        textView.setTypeface(
            textView.typeface,
            if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}