package com.example.fitto.feature_home.presentation.profile

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetCountryPickerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

internal class ProfilePreferenceDialogCoordinator(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val refreshRows: () -> Unit,
    private val countryCatalog: ProfileCountryCatalog = ProfileCountryCatalog()
) {

    private val openDialogs = mutableSetOf<Dialog>()

    fun showRegion() {
        val currentRegion = ProfilePreferenceStore.getRegion(context)
        val turkey = context.getString(R.string.region_suggested_turkey)
        val english = context.getString(R.string.region_suggested_english)
        val other = context.getString(R.string.region_other)

        showChoiceDialog(
            titleResId = R.string.profile_detail_settings_region,
            options = listOf(
                ChoiceOption(turkey, turkey, currentRegion == turkey),
                ChoiceOption(english, english, currentRegion == english),
                ChoiceOption(
                    title = other,
                    value = other,
                    isSelected = currentRegion != turkey && currentRegion != english
                )
            ),
            onSelected = { selectedRegion ->
                if (selectedRegion == other) {
                    showCountryPicker()
                } else {
                    ProfilePreferenceStore.setRegion(context, selectedRegion)
                    refreshRows()
                }
            }
        )
    }

    fun showLanguage() {
        val currentLanguage = ProfilePreferenceStore.getLanguage(context)
        showChoiceDialog(
            titleResId = R.string.profile_detail_settings_language,
            options = listOf(
                ChoiceOption(
                    context.getString(R.string.language_turkish),
                    ProfileLanguage.TURKISH,
                    currentLanguage == ProfileLanguage.TURKISH
                ),
                ChoiceOption(
                    context.getString(R.string.language_english),
                    ProfileLanguage.ENGLISH,
                    currentLanguage == ProfileLanguage.ENGLISH
                )
            ),
            onSelected = { language ->
                ProfilePreferenceStore.setLanguage(context, language)
                refreshRows()
            }
        )
    }

    fun showTheme() {
        val currentTheme = ProfilePreferenceStore.getTheme(context)
        showChoiceDialog(
            titleResId = R.string.profile_detail_settings_theme,
            options = listOf(
                ChoiceOption(
                    context.getString(R.string.profile_theme_system),
                    FittoThemeMode.SYSTEM,
                    currentTheme == FittoThemeMode.SYSTEM
                ),
                ChoiceOption(
                    context.getString(R.string.profile_theme_light),
                    FittoThemeMode.LIGHT,
                    currentTheme == FittoThemeMode.LIGHT
                ),
                ChoiceOption(
                    context.getString(R.string.profile_theme_dark),
                    FittoThemeMode.DARK,
                    currentTheme == FittoThemeMode.DARK
                )
            ),
            onSelected = { theme ->
                ProfilePreferenceStore.setTheme(context, theme)
                refreshRows()
            }
        )
    }

    fun showAccountVisibility() {
        val currentVisibility = ProfilePreferenceStore.getAccountVisibility(context)
        showChoiceDialog(
            titleResId = R.string.profile_detail_privacy_visibility,
            options = listOf(
                ChoiceOption(
                    context.getString(R.string.profile_visibility_public),
                    ProfileAccountVisibility.PUBLIC,
                    currentVisibility == ProfileAccountVisibility.PUBLIC
                ),
                ChoiceOption(
                    context.getString(R.string.profile_visibility_followers),
                    ProfileAccountVisibility.FOLLOWERS,
                    currentVisibility == ProfileAccountVisibility.FOLLOWERS
                ),
                ChoiceOption(
                    context.getString(R.string.profile_visibility_private),
                    ProfileAccountVisibility.PRIVATE,
                    currentVisibility == ProfileAccountVisibility.PRIVATE
                )
            ),
            onSelected = { visibility ->
                ProfilePreferenceStore.setAccountVisibility(context, visibility)
                refreshRows()
            }
        )
    }

    fun showNutritionDietTypes() {
        var current = ProfilePreferenceStore
            .getNutritionDietPreferences(context)
            .toSet()
        val options = NutritionDietPreference.entries
        val dialog = trackedDialog()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = inflater.inflate(R.layout.dialog_profile_choice, null, false)
        val titleTextView = dialogView.findViewById<TextView>(R.id.textChoiceDialogTitle)
        val optionsLayout = dialogView.findViewById<LinearLayout>(R.id.layoutChoiceOptions)
        val saveButton = dialogView.findViewById<TextView>(R.id.buttonChoiceDialogCancel)

        titleTextView.setText(R.string.nutrition_pref_diet_type_title)
        saveButton.setText(R.string.common_save)
        saveButton.setTextColor(context.getColor(R.color.fitto_green))

        fun refreshOptions() {
            optionsLayout.removeAllViews()
            options.forEach { option ->
                val optionView = inflater.inflate(
                    R.layout.item_profile_choice,
                    optionsLayout,
                    false
                )
                optionView.findViewById<TextView>(R.id.textChoiceTitle).text =
                    ProfilePreferenceStore.nutritionDietPreferenceText(context, option)
                optionView.findViewById<TextView>(R.id.textChoiceCheck).visibility =
                    if (option in current) View.VISIBLE else View.INVISIBLE
                optionView.setOnClickListener {
                    current = ProfileNutritionDietSelectionResolver.toggle(current, option)
                    refreshOptions()
                }
                optionsLayout.addView(optionView)
            }
        }

        saveButton.setOnClickListener {
            ProfilePreferenceStore.setNutritionDietPreferences(context, current)
            dialog.dismiss()
            refreshRows()
        }

        refreshOptions()
        showDialog(dialog, dialogView)
    }

    fun showNutritionMealRoutine() {
        val currentRoutine = ProfilePreferenceStore.getNutritionMealRoutine(context)
        showChoiceDialog(
            titleResId = R.string.nutrition_pref_meal_routine_title,
            options = listOf(
                ChoiceOption(
                    context.getString(R.string.nutrition_pref_routine_three_meals),
                    NutritionMealRoutine.THREE_MEALS,
                    currentRoutine == NutritionMealRoutine.THREE_MEALS
                ),
                ChoiceOption(
                    context.getString(R.string.nutrition_pref_routine_two_meals_snacks),
                    NutritionMealRoutine.TWO_MEALS_SNACKS,
                    currentRoutine == NutritionMealRoutine.TWO_MEALS_SNACKS
                ),
                ChoiceOption(
                    context.getString(R.string.nutrition_pref_routine_flexible),
                    NutritionMealRoutine.FLEXIBLE,
                    currentRoutine == NutritionMealRoutine.FLEXIBLE
                )
            ),
            onSelected = { routine ->
                ProfilePreferenceStore.setNutritionMealRoutine(context, routine)
                refreshRows()
            }
        )
    }

    fun showNutritionAllergies() {
        showNutritionText(
            titleResId = R.string.nutrition_pref_allergies_title,
            messageResId = R.string.nutrition_pref_allergies_desc,
            labelResId = R.string.nutrition_pref_allergies_title,
            hintResId = R.string.nutrition_pref_allergies_hint,
            initialValue = ProfilePreferenceStore.getNutritionAllergies(context),
            onConfirmed = { value ->
                ProfilePreferenceStore.setNutritionAllergies(context, value)
                refreshRows()
            }
        )
    }

    fun showNutritionDislikedFoods() {
        showNutritionText(
            titleResId = R.string.nutrition_pref_disliked_title,
            messageResId = R.string.nutrition_pref_disliked_desc,
            labelResId = R.string.nutrition_pref_disliked_title,
            hintResId = R.string.nutrition_pref_disliked_hint,
            initialValue = ProfilePreferenceStore.getNutritionDislikedFoods(context),
            onConfirmed = { value ->
                ProfilePreferenceStore.setNutritionDislikedFoods(context, value)
                refreshRows()
            }
        )
    }

    fun clear() {
        openDialogs.toList().forEach(Dialog::dismiss)
        openDialogs.clear()
    }

    private fun showCountryPicker() {
        val dialog = BottomSheetDialog(context)
        track(dialog)
        val binding = BottomSheetCountryPickerBinding.inflate(inflater)
        val countries = countryCatalog.countries(ProfilePreferenceStore.getLanguage(context))
        var selectedCountry = countries.firstOrNull {
            it.name == ProfilePreferenceStore.getRegion(context)
        } ?: countries.firstOrNull { it.isRecommended }
        ?: countries.first()

        val adapter = ProfileCountryAdapter(
            selectedCountryCode = selectedCountry.code,
            onCountryClick = { country -> selectedCountry = country }
        )

        binding.countryRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
            setHasFixedSize(false)
        }

        fun submitCountries(query: String = "") {
            adapter.submitList(countryCatalog.filter(countries, query))
        }

        binding.countrySearchEditText.doAfterTextChanged { editable ->
            submitCountries(editable?.toString().orEmpty())
        }
        binding.countryPickerDoneText.setOnClickListener {
            ProfilePreferenceStore.setRegion(context, selectedCountry.name)
            refreshRows()
            dialog.dismiss()
        }

        submitCountries()
        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            (binding.root.parent as? View)?.let { sheet ->
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }
        dialog.show()
    }

    private fun showNutritionText(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes labelResId: Int,
        @StringRes hintResId: Int,
        initialValue: String,
        onConfirmed: (String) -> Unit
    ) {
        val dialog = trackedDialog()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = inflater.inflate(R.layout.dialog_fitto_action, null, false)
        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val inputArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val inputLabel = dialogView.findViewById<TextView>(R.id.textPasswordLabel)
        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutPasswordInput)
        val inputEditText = dialogView.findViewById<EditText>(R.id.editActionPassword)
        val clearButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        iconImage.setImageResource(R.drawable.ic_nutrition_fork)
        iconImage.setColorFilter(context.getColor(R.color.fitto_green))
        titleText.setText(titleResId)
        messageText.setText(messageResId)
        warningText.isVisible = false
        inputArea.isVisible = true
        inputLabel.setText(labelResId)
        inputLayout.hint = context.getString(hintResId)
        inputLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        inputEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        inputEditText.isSingleLine = false
        inputEditText.minLines = 1
        inputEditText.maxLines = 3
        applySoftInputHintStyle(inputLayout, inputEditText)
        inputEditText.setText(initialValue)
        inputEditText.setSelection(inputEditText.text?.length ?: 0)

        clearButton.setText(R.string.common_clear)
        saveButton.setText(R.string.common_save)
        clearButton.setTextColor(context.getColor(R.color.profile_subtext))
        saveButton.setTextColor(context.getColor(R.color.fitto_green))
        clearButton.setOnClickListener { inputEditText.setText("") }
        saveButton.setOnClickListener {
            dialog.dismiss()
            onConfirmed(inputEditText.text?.toString().orEmpty())
        }

        showDialog(dialog, dialogView)
    }

    private fun <T> showChoiceDialog(
        @StringRes titleResId: Int,
        options: List<ChoiceOption<T>>,
        onSelected: (T) -> Unit
    ) {
        val dialog = trackedDialog()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = inflater.inflate(R.layout.dialog_profile_choice, null, false)
        val titleText = dialogView.findViewById<TextView>(R.id.textChoiceDialogTitle)
        val optionsLayout = dialogView.findViewById<LinearLayout>(R.id.layoutChoiceOptions)
        val cancelButton = dialogView.findViewById<TextView>(R.id.buttonChoiceDialogCancel)

        titleText.setText(titleResId)
        options.forEach { option ->
            val optionView = inflater.inflate(R.layout.item_profile_choice, optionsLayout, false)
            optionView.findViewById<TextView>(R.id.textChoiceTitle).text = option.title
            optionView.findViewById<TextView>(R.id.textChoiceCheck).visibility =
                if (option.isSelected) View.VISIBLE else View.INVISIBLE
            optionView.setOnClickListener {
                onSelected(option.value)
                dialog.dismiss()
            }
            optionsLayout.addView(optionView)
        }
        cancelButton.setOnClickListener { dialog.dismiss() }
        showDialog(dialog, dialogView)
    }

    private fun showDialog(dialog: Dialog, contentView: View) {
        dialog.setContentView(contentView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun trackedDialog(): Dialog {
        return Dialog(context).also(::track)
    }

    private fun track(dialog: Dialog) {
        openDialogs += dialog
        dialog.setOnDismissListener { openDialogs -= dialog }
    }

    private fun applySoftInputHintStyle(
        inputLayout: TextInputLayout,
        editText: EditText
    ) {
        val baseColor = context.getColor(R.color.profile_subtext)
        val hintColor = Color.argb(
            (Color.alpha(baseColor) * HINT_ALPHA).toInt().coerceIn(0, 255),
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
        inputLayout.defaultHintTextColor = ColorStateList.valueOf(hintColor)
        editText.setHintTextColor(hintColor)
    }

    private data class ChoiceOption<T>(
        val title: String,
        val value: T,
        val isSelected: Boolean
    )

    private companion object {
        const val DIALOG_WIDTH_RATIO = 0.90f
        const val HINT_ALPHA = 0.58f
    }
}
