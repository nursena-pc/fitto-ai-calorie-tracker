package com.example.fitto.feature_recipes

import com.example.fitto.core.ui.setUniformCornerRadius
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.FragmentRecipeAddBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.fittoAppContainer
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class RecipeAddFragment : Fragment(R.layout.fragment_recipe_add) {

    private var _binding: FragmentRecipeAddBinding? = null
    private val binding: FragmentRecipeAddBinding
        get() = _binding!!

    private lateinit var catalog: RecipeFilterCatalog
    private val formController = RecipeAddFormController()
    private lateinit var addCoordinator: RecipeAddCoordinator

    private val ingredientEdits = mutableListOf<EditText>()
    private val stepEdits = mutableListOf<EditText>()
    private val ingredientErrorTexts = mutableMapOf<EditText, TextView>()
    private val stepErrorTexts = mutableMapOf<EditText, TextView>()
    private var previousSoftInputMode: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeAddBinding.bind(view)
        catalog = RecipeFilterCatalog(resources)
        addCoordinator = requireContext().fittoAppContainer.createRecipeFeatureDependencies().createAddCoordinator()

        applyRecipeFormKeyboardMode()

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.addIngredientRowButton.setOnClickListener { addIngredientRow(requestFocus = true) }
        binding.addStepRowButton.setOnClickListener { addStepRow(requestFocus = true) }
        binding.saveRecipeButton.setOnClickListener { saveRecipe() }

        bindCategoryChips()
        bindDurationChips()
        bindDifficultyChips()
        setupTextWatchers()
        setupKeyboardInsets()
        addIngredientRow()
        addStepRow()
        updateSaveButton()
    }

    private fun setupTextWatchers() {
        binding.titleEditText.doAfterTextChanged {
            binding.titleInputLayout.error = null
            updateSaveButton()
        }

        listOf(binding.titleEditText, binding.descriptionEditText).forEach { editText ->
            editText.setOnFocusChangeListener { focusedView, hasFocus ->
                if (hasFocus) keepInputAboveKeyboard(focusedView)
            }
        }
    }

    private fun bindCategoryChips() {
        binding.categoryChipGroup.removeAllViews()
        catalog.mainCategories.forEach { option: RecipeFilterOption ->
            val chip = createChoiceChip(option.label, option.key == formController.state.selectedCategoryKey, compact = false)
            chip.setOnClickListener {
                formController.toggleCategory(option.key)
                bindCategoryChips()
                binding.categoryErrorText.visibility = View.GONE
                updateSaveButton()
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun bindDurationChips() {
        binding.durationChipGroup.removeAllViews()
        catalog.durations.forEach { option: RecipeFilterOption ->
            val chip = createChoiceChip(option.label, option.key == formController.state.selectedDurationKey, compact = true)
            chip.setOnClickListener {
                formController.toggleDuration(option.key)
                bindDurationChips()
                updateSaveButton()
            }
            binding.durationChipGroup.addView(chip)
        }
    }

    private fun bindDifficultyChips() {
        binding.difficultyChipGroup.removeAllViews()
        catalog.difficulties.forEach { option: RecipeFilterOption ->
            val chip = createChoiceChip(option.label, option.key == formController.state.selectedDifficultyKey, compact = true)
            chip.setOnClickListener {
                formController.toggleDifficulty(option.key)
                bindDifficultyChips()
                updateSaveButton()
            }
            binding.difficultyChipGroup.addView(chip)
        }
    }

    private fun createChoiceChip(label: String, checked: Boolean, compact: Boolean): Chip {
        val context = requireContext()
        return Chip(context).apply {
            text = label
            isCheckable = true
            isChecked = checked
            isCheckedIconVisible = false
            minHeight = if (compact) 30.dp else 34.dp
            setUniformCornerRadius((if (compact) 15 else 17).dp.toFloat())
            textSize = if (compact) 11f else 12f
            setChipBackgroundColorResource(R.color.chip_selector_bg)
            setTextColor(ContextCompat.getColorStateList(context, R.color.chip_selector_text))
            chipStrokeWidth = 1.dp.toFloat()
            chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_divider_soft))
            setPadding(if (compact) 6.dp else 10.dp, 0, if (compact) 6.dp else 10.dp, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 6.dp
            }
        }
    }

    private fun addIngredientRow(value: String = "", requestFocus: Boolean = false) {
        val edit = createLineEdit(
            hintText = getString(R.string.recipes_add_ingredient_row_hint),
            value = value,
            onTextChanged = { changedEdit ->
                ingredientErrorTexts[changedEdit]?.visibility = View.GONE
            }
        )
        val errorText = createInlineErrorText(getString(R.string.recipes_add_ingredient_required))
        ingredientEdits.add(edit)
        ingredientErrorTexts[edit] = errorText
        binding.ingredientsRowsContainer.addView(wrapSimpleRow(edit, null, errorText))
        if (requestFocus) focusRowInput(edit)
        updateSaveButton()
    }

    private fun addStepRow(value: String = "", requestFocus: Boolean = false) {
        val numberText = TextView(requireContext()).apply {
            this.text = (stepEdits.size + 1).toString()
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_recipe_step_number)
            layoutParams = LinearLayout.LayoutParams(28.dp, 28.dp).apply { marginEnd = 8.dp }
        }
        val edit = createLineEdit(
            hintText = getString(R.string.recipes_add_step_row_hint),
            value = value,
            onTextChanged = { changedEdit ->
                stepErrorTexts[changedEdit]?.visibility = View.GONE
            }
        )
        val errorText = createInlineErrorText(getString(R.string.recipes_add_step_required)).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = 36.dp }
        }
        stepEdits.add(edit)
        stepErrorTexts[edit] = errorText
        binding.stepsRowsContainer.addView(wrapSimpleRow(edit, numberText, errorText))
        if (requestFocus) focusRowInput(edit)
        updateSaveButton()
    }

    private fun createLineEdit(
        hintText: String,
        value: String,
        onTextChanged: (EditText) -> Unit
    ): EditText {
        return EditText(requireContext()).apply {
            hint = hintText
            setText(value)
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
            textSize = 13f
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_recipe_amount_pill)
            setPadding(12.dp, 0, 12.dp, 0)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_text_dark))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_text_secondary))
            doAfterTextChanged {
                onTextChanged(this)
                updateSaveButton()
            }
            setOnFocusChangeListener { focusedView, hasFocus ->
                if (hasFocus) keepInputAboveKeyboard(focusedView)
            }
            layoutParams = LinearLayout.LayoutParams(0, 42.dp, 1f)
        }
    }

    private fun createInlineErrorText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            textSize = 11f
            visibility = View.GONE
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 3.dp
            }
        }
    }

    private fun wrapSimpleRow(edit: EditText, leading: View?, errorText: TextView): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            leading?.let { addView(it) }
            addView(edit)
        }

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dp }
            addView(row)
            addView(errorText)
        }
    }

    private fun setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.addRecipeScrollView) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val bottomPadding = if (imeBottom > 0) 24.dp else navBottom + 24.dp
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomPadding)
            insets
        }
        ViewCompat.requestApplyInsets(binding.addRecipeScrollView)
    }

    @Suppress("DEPRECATION")
    private fun applyRecipeFormKeyboardMode() {
        val window = activity?.window ?: return
        if (previousSoftInputMode == null) {
            previousSoftInputMode = window.attributes.softInputMode
        }
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
    }

    private fun restorePreviousKeyboardMode() {
        val mode = previousSoftInputMode ?: return
        activity?.window?.setSoftInputMode(mode)
        previousSoftInputMode = null
    }

    private fun focusRowInput(edit: EditText) {
        edit.post {
            edit.requestFocus()
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
            keepInputAboveKeyboard(edit)
        }
    }

    private fun keepInputAboveKeyboard(target: View) {
        // IME açılırken yükseklik birkaç frame içinde oturuyor. Bu yüzden aynı hesabı
        // kısa aralıklarla tekrarlıyoruz; MIUI dahil cihazlarda satır klavyenin
        // altında kalmadan görünür alana taşınıyor.
        listOf(80L, 240L, 420L, 650L).forEach { delay ->
            binding.addRecipeScrollView.postDelayed({
                if (_binding == null || !target.isAttachedToWindow) return@postDelayed

                val targetLocation = IntArray(2)
                target.getLocationOnScreen(targetLocation)
                val targetBottomOnScreen = targetLocation[1] + target.height

                val visibleFrame = android.graphics.Rect()
                binding.root.getWindowVisibleDisplayFrame(visibleFrame)
                val safeBottomOnScreen = visibleFrame.bottom - 72.dp

                if (targetBottomOnScreen > safeBottomOnScreen) {
                    val delta = targetBottomOnScreen - safeBottomOnScreen
                    binding.addRecipeScrollView.smoothScrollBy(0, delta + 32.dp)
                    target.requestRectangleOnScreen(
                        android.graphics.Rect(0, 0, target.width, target.height + 120.dp),
                        true
                    )
                    return@postDelayed
                }

                val targetTopOnScreen = targetLocation[1]
                val safeTopOnScreen = visibleFrame.top + 84.dp
                if (targetTopOnScreen < safeTopOnScreen) {
                    binding.addRecipeScrollView.smoothScrollBy(0, targetTopOnScreen - safeTopOnScreen)
                } else {
                    target.requestRectangleOnScreen(
                        android.graphics.Rect(0, 0, target.width, target.height + 96.dp),
                        false
                    )
                }
            }, delay)
        }
    }

    private fun saveRecipe() {
        clearValidationErrors()

        val submission = formController.prepareSubmission(
            title = binding.titleEditText.text?.toString().orEmpty(),
            description = binding.descriptionEditText.text?.toString().orEmpty(),
            ingredientValues = ingredientEdits.map { it.text?.toString().orEmpty() },
            stepValues = stepEdits.map { it.text?.toString().orEmpty() },
            defaultDurationKey = catalog.durations.firstOrNull()?.key.orEmpty(),
            defaultDifficultyKey = catalog.difficulties.firstOrNull()?.key.orEmpty()
        )

        val readySubmission = when (submission) {
            is RecipeAddSubmission.Invalid -> {
                renderValidationErrors(submission.issues)
                return
            }

            RecipeAddSubmission.AlreadySaving -> return
            is RecipeAddSubmission.Ready -> submission
        }

        updateSaveButton()
        val draft = readySubmission.draft
        viewLifecycleOwner.lifecycleScope.launch {
            val recipeId = addCoordinator.saveRecipe(draft)
            formController.completeSave()
            if (_binding == null) return@launch

            parentFragmentManager.setFragmentResult(
                MyRecipesFragment.REQUEST_RECIPE_BOOK_CHANGED,
                bundleOf(MyRecipesFragment.RESULT_RECIPE_ID to recipeId)
            )
            Toast.makeText(requireContext(), R.string.recipes_add_saved, Toast.LENGTH_LONG).show()
            openSavedRecipeAndRemoveAddScreen(recipeId)
        }
    }

    private fun renderValidationErrors(issues: Set<RecipeAddValidationIssue>) {
        var firstInvalidView: View? = null

        if (RecipeAddValidationIssue.TITLE in issues) {
            binding.titleInputLayout.error = getString(R.string.recipes_add_title_required)
            firstInvalidView = binding.titleEditText
        }

        if (RecipeAddValidationIssue.CATEGORY in issues) {
            binding.categoryErrorText.text = getString(R.string.recipes_add_category_required)
            binding.categoryErrorText.visibility = View.VISIBLE
            if (firstInvalidView == null) firstInvalidView = binding.categoryErrorText
        }

        if (RecipeAddValidationIssue.INGREDIENTS in issues) {
            ingredientEdits.firstOrNull()?.let { edit ->
                ingredientErrorTexts[edit]?.apply {
                    text = getString(R.string.recipes_add_ingredient_required)
                    visibility = View.VISIBLE
                }
                if (firstInvalidView == null) firstInvalidView = edit
            }
        }

        if (RecipeAddValidationIssue.STEPS in issues) {
            stepEdits.firstOrNull()?.let { edit ->
                stepErrorTexts[edit]?.apply {
                    text = getString(R.string.recipes_add_step_required)
                    visibility = View.VISIBLE
                }
                if (firstInvalidView == null) firstInvalidView = edit
            }
        }

        firstInvalidView?.let { view ->
            view.requestFocus()
            keepInputAboveKeyboard(view)
        }
    }

    private fun clearValidationErrors() {
        binding.titleInputLayout.error = null
        binding.categoryErrorText.visibility = View.GONE
        ingredientErrorTexts.values.forEach { it.visibility = View.GONE }
        stepErrorTexts.values.forEach { it.visibility = View.GONE }
    }

    private fun updateSaveButton() {
        if (_binding == null) return
        // Eksik alanlar saveRecipe() içinde açıklanır; yalnızca devam eden kayıt ikinci gönderimi engeller.
        binding.saveRecipeButton.isEnabled = !formController.state.isSaving
    }

    private fun openSavedRecipeAndRemoveAddScreen(recipeId: String) {
        val fragmentManager = parentFragmentManager

        fragmentManager.popBackStackImmediate(
            TAG_RECIPE_ADD,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val visibleHost = fragmentManager.fragments
            .lastOrNull { fragment ->
                fragment.id == R.id.homeTabContainer &&
                    fragment.isVisible &&
                    fragment !is RecipeDetailFragment
            }

        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .apply { visibleHost?.let { fragment -> hide(fragment) } }
            .add(R.id.homeTabContainer, RecipeDetailFragment.newInstance(recipeId), TAG_RECIPE_DETAIL)
            .addToBackStack(TAG_RECIPE_DETAIL)
            .commit()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        restorePreviousKeyboardMode()
        ingredientEdits.clear()
        stepEdits.clear()
        ingredientErrorTexts.clear()
        stepErrorTexts.clear()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG_RECIPE_DETAIL = "recipe_detail"
        private const val TAG_RECIPE_ADD = "recipe_add"
    }
}
