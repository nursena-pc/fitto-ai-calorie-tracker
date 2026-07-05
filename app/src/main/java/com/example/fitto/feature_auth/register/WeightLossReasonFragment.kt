package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentWeightLossReasonBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.datepicker.DateValidatorPointForward

class WeightLossReasonFragment : Fragment(R.layout.fragment_weight_loss_reason) {

    private var _binding: FragmentWeightLossReasonBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    private var selectedReason: ReasonType? = null
    private var selectedEventDateMillis: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentWeightLossReasonBinding.bind(view)
        onboardingViewModel = obtainOnboardingViewModel()

        binding.root.setOnboardingProgress(8)

        setupInitialState()
        setupClicks()
        updateReasonSelection()
    }

    private fun setupInitialState() = with(binding) {
        reasonNextButton.isEnabled = false
        reasonNextButton.alpha = 0.55f
        reasonEventDateContainer.isVisible = false
        reasonEventDateErrorText.isVisible = false
    }

    private fun setupClicks() = with(binding) {
        reasonBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        reasonWeddingCard.setOnClickListener {
            selectReason(ReasonType.WEDDING)
        }

        reasonGraduationCard.setOnClickListener {
            selectReason(ReasonType.GRADUATION)
        }

        reasonHolidayCard.setOnClickListener {
            selectReason(ReasonType.HOLIDAY)
        }

        reasonLookFitterCard.setOnClickListener {
            selectReason(ReasonType.LOOK_FITTER)
        }

        reasonImproveHealthCard.setOnClickListener {
            selectReason(ReasonType.IMPROVE_HEALTH)
        }

        reasonMoreEnergyCard.setOnClickListener {
            selectReason(ReasonType.MORE_ENERGY)
        }

        reasonClothesCard.setOnClickListener {
            selectReason(ReasonType.CLOTHES)
        }

        reasonDoctorCard.setOnClickListener {
            selectReason(ReasonType.DOCTOR)
        }

        reasonFeelBetterCard.setOnClickListener {
            selectReason(ReasonType.FEEL_BETTER)
        }

        reasonOtherCard.setOnClickListener {
            selectReason(ReasonType.OTHER)
        }

        reasonEventDateButton.setOnClickListener {
            showEventDatePicker()
        }

        reasonNextButton.setOnClickListener {
            submitReason()
        }
    }

    private fun selectReason(reason: ReasonType) {
        val oldReasonNeedsDate = selectedReason.requiresEventDate()
        val newReasonNeedsDate = reason.requiresEventDate()

        selectedReason = reason

        if (oldReasonNeedsDate && !newReasonNeedsDate) {
            selectedEventDateMillis = null
            binding.reasonEventDateText.text = getString(R.string.select_date)
            binding.reasonEventDateText.setTextColor(requireContext().getColor(R.color.fitto_text_dark))
        }

        binding.reasonEventDateErrorText.isVisible = false
        updateReasonSelection()
    }

    private fun updateReasonSelection() = with(binding) {
        updateReasonCard(reasonWeddingCard, reasonWeddingCheck, selectedReason == ReasonType.WEDDING)
        updateReasonCard(reasonGraduationCard, reasonGraduationCheck, selectedReason == ReasonType.GRADUATION)
        updateReasonCard(reasonHolidayCard, reasonHolidayCheck, selectedReason == ReasonType.HOLIDAY)
        updateReasonCard(reasonLookFitterCard, reasonLookFitterCheck, selectedReason == ReasonType.LOOK_FITTER)
        updateReasonCard(reasonImproveHealthCard, reasonImproveHealthCheck, selectedReason == ReasonType.IMPROVE_HEALTH)
        updateReasonCard(reasonMoreEnergyCard, reasonMoreEnergyCheck, selectedReason == ReasonType.MORE_ENERGY)
        updateReasonCard(reasonClothesCard, reasonClothesCheck, selectedReason == ReasonType.CLOTHES)
        updateReasonCard(reasonDoctorCard, reasonDoctorCheck, selectedReason == ReasonType.DOCTOR)
        updateReasonCard(reasonFeelBetterCard, reasonFeelBetterCheck, selectedReason == ReasonType.FEEL_BETTER)
        updateReasonCard(reasonOtherCard, reasonOtherCheck, selectedReason == ReasonType.OTHER)

        reasonEventDateContainer.isVisible = selectedReason.requiresEventDate()

        val hasSelection = selectedReason != null
        reasonNextButton.isEnabled = hasSelection
        reasonNextButton.alpha = if (hasSelection) 1f else 0.55f
    }

    private fun updateReasonCard(
        card: LinearLayout,
        checkView: FrameLayout,
        isSelected: Boolean
    ) {
        card.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_fitto_card_selected
            } else {
                R.drawable.bg_fitto_card_normal
            }
        )

        checkView.isVisible = isSelected
    }

    private fun showEventDatePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()

        val constraints = CalendarConstraints.Builder()
            .setStart(today)
            .setOpenAt(selectedEventDateMillis ?: today)
            .setValidator(DateValidatorPointForward.from(today))
            .build()

        val picker: MaterialDatePicker<Long> = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)
            .setTitleText(getString(R.string.event_date))
            .setSelection(selectedEventDateMillis ?: today)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection: Long ->
            selectedEventDateMillis = selection

            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("tr", "TR"))

            binding.reasonEventDateText.text = formatter.format(Date(selection))
            binding.reasonEventDateText.setTextColor(
                requireContext().getColor(R.color.fitto_text_dark)
            )

            binding.reasonEventDateErrorText.isVisible = false
        }

        picker.show(parentFragmentManager, "weight_loss_reason_event_date_picker")
    }

    private fun submitReason() {
        val reason = selectedReason ?: return

        if (reason.requiresEventDate() && selectedEventDateMillis == null) {
            binding.reasonEventDateErrorText.isVisible = true
            return
        }

        onboardingViewModel.saveWeightLossReason(
            reason = reason,
            eventTimeMillis = selectedEventDateMillis
        )
        findNavController().navigate(R.id.action_weightLossReasonFragment_to_activityLevelFragment)
    }

    private fun ReasonType?.requiresEventDate(): Boolean {
        return this == ReasonType.WEDDING ||
                this == ReasonType.GRADUATION ||
                this == ReasonType.HOLIDAY
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}