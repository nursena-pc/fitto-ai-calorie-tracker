package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.databinding.DialogReportPostBinding
import com.google.android.material.card.MaterialCardView

object ReportReasonDialogHelper {

    data class ReasonItem(
        val key: String,
        val label: String
    )

    fun show(
        fragment: Fragment,
        title: String,
        subtitle: String,
        reasons: List<ReasonItem>,
        onSubmit: (ReasonItem) -> Unit
    ) {
        if (reasons.size < REQUIRED_REASON_COUNT) return

        val binding = DialogReportPostBinding.inflate(
            LayoutInflater.from(fragment.requireContext())
        )

        val dialog = Dialog(fragment.requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            setCanceledOnTouchOutside(true)
        }

        binding.reportDialogTitleText.text = title
        binding.reportDialogSubtitleText.text = subtitle

        binding.reportSpamText.text = reasons[0].label
        binding.reportHarassmentText.text = reasons[1].label
        binding.reportInappropriateText.text = reasons[2].label
        binding.reportOtherText.text = reasons[3].label

        val reasonCards = linkedMapOf(
            binding.reportSpamCard to reasons[0],
            binding.reportHarassmentCard to reasons[1],
            binding.reportInappropriateCard to reasons[2],
            binding.reportOtherCard to reasons[3]
        )

        var selectedReason: ReasonItem? = null

        fun updateSelection(selectedCard: MaterialCardView?) {
            reasonCards.keys.forEach { card ->
                val isSelected = card == selectedCard

                card.strokeColor = ContextCompat.getColor(
                    fragment.requireContext(),
                    if (isSelected) {
                        R.color.fitto_green
                    } else {
                        R.color.report_dialog_option_stroke
                    }
                )

                card.strokeWidth = if (isSelected) {
                    SELECTED_STROKE_WIDTH
                } else {
                    DEFAULT_STROKE_WIDTH
                }

                card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        fragment.requireContext(),
                        if (isSelected) {
                            R.color.report_dialog_option_selected_bg
                        } else {
                            R.color.report_dialog_option_bg
                        }
                    )
                )
            }

            binding.reportSubmitButton.isEnabled = selectedCard != null
            binding.reportSubmitButton.alpha = if (selectedCard != null) {
                1f
            } else {
                DISABLED_BUTTON_ALPHA
            }
        }

        reasonCards.forEach { (card, reason) ->
            card.setOnClickListener {
                selectedReason = reason
                updateSelection(card)
            }
        }

        binding.reportCancelButton.setOnClickListener { dialog.dismiss() }
        binding.reportSubmitButton.setOnClickListener {
            val reason = selectedReason ?: return@setOnClickListener
            onSubmit(reason)
            dialog.dismiss()
        }

        updateSelection(null)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private const val REQUIRED_REASON_COUNT = 4
    private const val DEFAULT_STROKE_WIDTH = 1
    private const val SELECTED_STROKE_WIDTH = 3
    private const val DISABLED_BUTTON_ALPHA = 0.5f
}
