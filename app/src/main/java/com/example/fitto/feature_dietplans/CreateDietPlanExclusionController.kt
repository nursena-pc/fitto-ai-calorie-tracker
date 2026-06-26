package com.example.fitto.feature_dietplans

import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.Window
import android.widget.EditText
import com.example.fitto.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CreateDietPlanExclusionController(
    private val context: Context,
    private val chipGroup: ChipGroup,
    private val onChanged: () -> Unit
) {
    private val exclusions = linkedSetOf<String>()

    fun values(): List<String> {
        return exclusions.toList()
    }

    fun setValues(values: Collection<String>) {
        chipGroup.removeAllViews()
        exclusions.clear()
        values.forEach { value ->
            addChip(value = value, notifyChange = false)
        }
    }

    fun showAddDialog() {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = context.getString(R.string.create_diet_plan_add_exclusion_hint)
            setSingleLine(true)
            setPadding(32, 20, 32, 20)
        }

        val dialog = MaterialAlertDialogBuilder(
            context,
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setTitle(context.getString(R.string.create_diet_plan_add_exclusion_title))
            .setView(input)
            .setPositiveButton(
                context.getString(R.string.create_diet_plan_add_exclusion_positive),
                null
            )
            .setNegativeButton(
                context.getString(R.string.create_diet_plan_add_exclusion_negative),
                null
            )
            .create()

        dialog.setOnShowListener {
            forceWhiteDialog(dialog.window)

            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isNotBlank()) {
                    addChip(value = value, notifyChange = true)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun forceWhiteDialog(window: Window?) {
        window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }

    private fun addChip(value: String, notifyChange: Boolean) {
        if (value.isBlank()) return

        exclusions.add(value)

        val chip = Chip(context)
        chip.text = value
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            exclusions.remove(value)
            chipGroup.removeView(chip)
            onChanged()
        }

        chipGroup.addView(chip)

        if (notifyChange) {
            onChanged()
        }
    }
}
