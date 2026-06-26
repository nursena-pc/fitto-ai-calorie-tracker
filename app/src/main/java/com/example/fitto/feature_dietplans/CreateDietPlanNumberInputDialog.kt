package com.example.fitto.feature_dietplans

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.databinding.DialogPlanNumberPickerBinding

data class CreateDietPlanNumberInputConfig(
    val icon: String,
    @StringRes val titleRes: Int,
    @StringRes val helperTextRes: Int,
    @StringRes val unitTextRes: Int,
    val initialValue: Int,
    val validRange: IntRange,
    val step: Int,
    @StringRes val errorTextRes: Int
)

class CreateDietPlanNumberInputDialog(
    private val context: Context
) {

    fun show(
        config: CreateDietPlanNumberInputConfig,
        onValueSaved: (Int) -> Unit
    ) {
        val dialogBinding = DialogPlanNumberPickerBinding.inflate(
            LayoutInflater.from(context)
        )
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCanceledOnTouchOutside(true)
        }

        bindDialogContent(
            binding = dialogBinding,
            dialog = dialog,
            config = config,
            onValueSaved = onValueSaved
        )

        dialog.setOnShowListener {
            dialog.window?.let { window ->
                window.setBackgroundDrawableResource(android.R.color.transparent)
                window.setLayout(
                    (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialog.show()
    }

    private fun bindDialogContent(
        binding: DialogPlanNumberPickerBinding,
        dialog: Dialog,
        config: CreateDietPlanNumberInputConfig,
        onValueSaved: (Int) -> Unit
    ) = with(binding) {
        val unitText = context.getString(config.unitTextRes)

        numberDialogIconText.text = config.icon
        numberDialogTitleText.setText(config.titleRes)
        numberDialogHelperText.setText(config.helperTextRes)
        numberDialogUnitText.text = unitText

        setValue(binding, config, config.initialValue)

        numberDialogMinusButton.setOnClickListener {
            setValue(
                binding = binding,
                config = config,
                value = currentValue(binding, config.initialValue) - config.step
            )
        }

        numberDialogPlusButton.setOnClickListener {
            setValue(
                binding = binding,
                config = config,
                value = currentValue(binding, config.initialValue) + config.step
            )
        }

        numberDialogCancelButton.setOnClickListener {
            dialog.dismiss()
        }

        numberDialogSaveButton.setOnClickListener {
            val value = numberDialogInput.text
                ?.toString()
                ?.trim()
                ?.toIntOrNull()

            if (value == null || value !in config.validRange) {
                numberDialogRangeText.setText(config.errorTextRes)
                numberDialogRangeText.setTextColor(
                    ContextCompat.getColor(context, R.color.fitto_error)
                )
                return@setOnClickListener
            }

            onValueSaved(value)
            dialog.dismiss()
        }
    }

    private fun currentValue(
        binding: DialogPlanNumberPickerBinding,
        initialValue: Int
    ): Int {
        return binding.numberDialogInput.text
            ?.toString()
            ?.trim()
            ?.toIntOrNull()
            ?: initialValue
    }

    private fun setValue(
        binding: DialogPlanNumberPickerBinding,
        config: CreateDietPlanNumberInputConfig,
        value: Int
    ) = with(binding) {
        val safeValue = value.coerceIn(config.validRange)
        val unitText = context.getString(config.unitTextRes)

        numberDialogInput.setText(safeValue.toString())
        numberDialogInput.setSelection(numberDialogInput.text?.length ?: 0)
        numberDialogRangeText.text = context.getString(
            R.string.create_diet_plan_number_dialog_range,
            config.validRange.first,
            config.validRange.last,
            unitText
        )
        numberDialogRangeText.setTextColor(
            ContextCompat.getColor(context, R.color.fitto_text_secondary)
        )
    }

    private companion object {
        const val DIALOG_WIDTH_RATIO = 0.90f
    }
}
