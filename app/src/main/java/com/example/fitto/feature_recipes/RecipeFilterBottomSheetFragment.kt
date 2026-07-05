package com.example.fitto.feature_recipes

import com.example.fitto.core.ui.setUniformCornerRadius
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetRecipeFiltersBinding
import com.example.fitto.databinding.DialogRecipeIngredientPickerBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.fittoAppContainer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class RecipeFilterBottomSheetFragment : BottomSheetDialogFragment(R.layout.bottom_sheet_recipe_filters) {

    private var _binding: BottomSheetRecipeFiltersBinding? = null
    private val binding: BottomSheetRecipeFiltersBinding
        get() = _binding!!

    private lateinit var catalog: RecipeFilterCatalog
    private lateinit var filterController: RecipeFilterController
    private lateinit var filterCoordinator: RecipeFilterCoordinator

    override fun getTheme(): Int = R.style.ThemeOverlay_Fitto_WhiteBottomSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filterController = RecipeFilterController(
            RecipeFilterState.fromBundle(arguments?.getBundle(ARG_FILTER_STATE))
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = BottomSheetRecipeFiltersBinding.bind(view)
        catalog = RecipeFilterCatalog(resources)
        filterCoordinator = requireContext()
            .fittoAppContainer
            .createRecipeFeatureDependencies()
            .createFilterCoordinator()

        setupClicks()
        setupIncludeExcludeSectionToggles()
        renderFilters()
        updateApplyButton()

        viewLifecycleOwner.lifecycleScope.launch {
            filterController.setIngredientOptions(filterCoordinator.loadIngredientOptions())
            renderIncludeExcludeIngredientFilters()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = false
        }
    }

    private fun setupClicks(): Unit = with(binding) {
        closeButton.setOnClickListener { dismiss() }

        clearButton.setOnClickListener {
            val scrollY: Int = filterScrollView.scrollY
            filterController.clear()
            renderFilters()
            updateApplyButton()
            filterScrollView.post { filterScrollView.scrollTo(0, scrollY) }
        }

        applyButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RecipeFilterState.RESULT_REQUEST_KEY,
                bundleOf(RecipeFilterState.RESULT_BUNDLE_KEY to filterController.state.toBundle())
            )
            dismiss()
        }

        includeIngredientSelectButton.setOnClickListener { showIngredientPicker(include = true) }
        excludeIngredientSelectButton.setOnClickListener { showIngredientPicker(include = false) }
    }


    private fun setupIncludeExcludeSectionToggles(): Unit = with(binding) {
        bindSectionToggle(includeIngredientHeader, includeIngredientContent, includeIngredientArrow)
        bindSectionToggle(excludeIngredientHeader, excludeIngredientContent, excludeIngredientArrow)
    }

    private fun bindSectionToggle(header: View, content: View, arrow: ImageView): Unit {
        header.setOnClickListener {
            content.isVisible = !content.isVisible
            arrow.rotation = if (content.isVisible) 0f else -90f
        }
    }

    private fun renderFilters(): Unit = with(binding) {
        bindIconOptionCards(
            container = mainCategoryIconRow,
            options = catalog.mainCategories,
            selectedKey = filterController.state.selectedMainCategoryKey,
            emojiProvider = ::emojiForMainCategory,
            onSelected = { selectedKey: String? ->
                filterController.selectMainCategory(selectedKey)
                renderSubcategoryFilter()
                updateApplyButton()
            }
        )

        renderSubcategoryFilter()

        bindIconMultiOptionCards(
            container = mainIngredientIconRow,
            options = catalog.mainIngredients,
            selectedKeys = filterController.state.selectedMainIngredientKeys,
            emojiProvider = ::emojiForMainIngredient,
            onSelectedKeysChanged = { selectedKeys: Set<String> ->
                filterController.selectMainIngredients(selectedKeys)
                updateApplyButton()
            }
        )

        bindMultiChoiceChips(
            group = dietChipGroup,
            options = catalog.diets,
            selectedKeys = filterController.state.selectedDietKeys,
            iconProvider = ::emojiForDiet,
            onSelectedKeysChanged = { selectedKeys: Set<String> ->
                filterController.selectDiets(selectedKeys)
                updateApplyButton()
            }
        )

        bindIconMultiOptionCards(
            container = methodIconRow,
            options = catalog.methods,
            selectedKeys = filterController.state.selectedMethodKeys,
            emojiProvider = ::emojiForMethod,
            onSelectedKeysChanged = { selectedKeys: Set<String> ->
                filterController.selectMethods(selectedKeys)
                updateApplyButton()
            }
        )

        bindSingleChoiceChips(
            group = difficultyChipGroup,
            options = catalog.difficulties,
            selectedKey = filterController.state.selectedDifficultyKey,
            onSelected = { selectedKey: String? ->
                filterController.selectDifficulty(selectedKey)
                updateApplyButton()
            }
        )

        bindSingleChoiceChips(
            group = durationChipGroup,
            options = catalog.durations,
            selectedKey = filterController.state.selectedDurationKey,
            onSelected = { selectedKey: String? ->
                filterController.selectDuration(selectedKey)
                updateApplyButton()
            }
        )

        renderIncludeExcludeIngredientFilters()
    }

    private fun renderSubcategoryFilter(): Unit = with(binding) {
        val selectedMainCategoryKey: String? = filterController.state.selectedMainCategoryKey
        val subcategories: List<RecipeFilterOption> = catalog.subcategoriesFor(selectedMainCategoryKey)

        subcategoryChipGroup.removeAllViews()

        if (selectedMainCategoryKey == null || subcategories.isEmpty()) {
            subcategoryLockedRow.visibility = View.VISIBLE
            subcategoryChipGroup.visibility = View.GONE
            return
        }

        subcategoryLockedRow.visibility = View.GONE
        subcategoryChipGroup.visibility = View.VISIBLE

        bindSingleChoiceChips(
            group = subcategoryChipGroup,
            options = subcategories,
            selectedKey = filterController.state.selectedSubcategoryKey,
            onSelected = { selectedKey: String? ->
                filterController.selectSubcategory(selectedKey)
                updateApplyButton()
            }
        )
    }

    private fun showIngredientPicker(include: Boolean): Unit {
        val mode = if (include) {
            RecipeIngredientFilterMode.INCLUDE
        } else {
            RecipeIngredientFilterMode.EXCLUDE
        }
        val pickerState = filterController.ingredientPickerState(mode)
        val dialogBinding = DialogRecipeIngredientPickerBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext(), R.style.ThemeOverlay_Fitto_WhiteBottomSheet)
        dialog.setContentView(dialogBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = false

        var selectedKeys = pickerState.selectedKeys
        val adapter = RecipeIngredientPickerAdapter(selectedKeys) { updatedKeys ->
            selectedKeys = updatedKeys
        }

        dialogBinding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.ingredientsRecyclerView.adapter = adapter
        dialogBinding.ingredientsRecyclerView.itemAnimator = null

        fun submitFilteredOptions(query: String) {
            val filteredOptions = filterController.filterIngredientOptions(
                options = pickerState.availableOptions,
                query = query
            )
            adapter.submitList(filteredOptions)
            dialogBinding.emptyIngredientText.isVisible = filteredOptions.isEmpty()
            dialogBinding.ingredientsRecyclerView.isVisible = filteredOptions.isNotEmpty()
        }

        dialogBinding.ingredientPickerTitleText.text = getString(
            if (include) R.string.recipes_filter_include_picker_title else R.string.recipes_filter_exclude_picker_title
        )
        dialogBinding.ingredientPickerCloseButton.setOnClickListener { dialog.dismiss() }
        dialogBinding.ingredientPickerCancelButton.setOnClickListener { dialog.dismiss() }
        dialogBinding.ingredientPickerDoneButton.setOnClickListener {
            filterController.applyIngredientSelection(mode, selectedKeys)
            renderIncludeExcludeIngredientFilters()
            updateApplyButton()
            dialog.dismiss()
        }

        submitFilteredOptions("")
        dialogBinding.ingredientSearchEditText.doAfterTextChanged { editable ->
            submitFilteredOptions(editable?.toString().orEmpty())
        }

        dialog.show()
    }

    private fun bindIconOptionCards(
        container: LinearLayout,
        options: List<RecipeFilterOption>,
        selectedKey: String?,
        emojiProvider: (String) -> String,
        onSelected: (String?) -> Unit
    ): Unit {
        container.removeAllViews()
        container.tag = selectedKey

        options.forEach { option: RecipeFilterOption ->
            val card: MaterialCardView = createIconOptionCard(
                emoji = emojiProvider(option.key),
                label = option.label,
                checked = option.key == selectedKey
            )
            card.tag = option.key
            card.setOnClickListener {
                val currentSelectedKey: String? = container.tag as? String
                val newSelectedKey: String? = if (option.key == currentSelectedKey) null else option.key
                container.tag = newSelectedKey
                updateIconCardContainerSelection(container, newSelectedKey)
                onSelected(newSelectedKey)
            }
            container.addView(card)
        }
    }

    private fun bindIconMultiOptionCards(
        container: LinearLayout,
        options: List<RecipeFilterOption>,
        selectedKeys: Set<String>,
        emojiProvider: (String) -> String,
        onSelectedKeysChanged: (Set<String>) -> Unit
    ): Unit {
        container.removeAllViews()
        container.tag = selectedKeys

        options.forEach { option: RecipeFilterOption ->
            val card: MaterialCardView = createIconOptionCard(
                emoji = emojiProvider(option.key),
                label = option.label,
                checked = option.key in selectedKeys
            )
            card.tag = option.key
            card.setOnClickListener {
                val currentKeys: Set<String> = (container.tag as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
                val updatedKeys: Set<String> = if (option.key in currentKeys) currentKeys - option.key else currentKeys + option.key
                container.tag = updatedKeys
                updateIconCard(card, option.key in updatedKeys)
                onSelectedKeysChanged(updatedKeys)
            }
            container.addView(card)
        }
    }

    private fun createIconOptionCard(emoji: String, label: String, checked: Boolean): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            radius = 16.dp.toFloat()
            cardElevation = 0f
            strokeWidth = 1.dp
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(76.dp, 80.dp).apply { marginEnd = 10.dp }
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(6.dp, 7.dp, 6.dp, 7.dp)
        }

        val emojiText = TextView(context).apply {
            text = emoji
            gravity = Gravity.CENTER
            textSize = 23f
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                32.dp
            )
        }

        val labelText = TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            maxLines = 2
            textSize = 11f
            includeFontPadding = false
            tag = LABEL_TAG
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        content.addView(emojiText)
        content.addView(labelText)
        card.addView(content)
        updateIconCard(card, checked)
        return card
    }

    private fun updateIconCardContainerSelection(container: LinearLayout, selectedKey: String?) {
        for (index in 0 until container.childCount) {
            val card = container.getChildAt(index) as? MaterialCardView ?: continue
            updateIconCard(card, card.tag == selectedKey)
        }
    }

    private fun updateIconCard(card: MaterialCardView, checked: Boolean) {
        val context = card.context
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_surface))
        card.strokeWidth = if (checked) 2.dp else 1.dp
        card.strokeColor = ContextCompat.getColor(
            context,
            if (checked) R.color.fitto_green else R.color.fitto_primary_soft
        )

        val labelText: TextView? = findLabelText(card)
        labelText?.setTextColor(
            ContextCompat.getColor(context, if (checked) R.color.fitto_green else R.color.fitto_text_dark)
        )
        labelText?.typeface = if (checked) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }

    private fun findLabelText(card: MaterialCardView): TextView? {
        val content = card.getChildAt(0) as? LinearLayout ?: return null
        for (index in 0 until content.childCount) {
            val child = content.getChildAt(index)
            if (child is TextView && child.tag == LABEL_TAG) return child
        }
        return null
    }

    private fun bindSingleChoiceChips(
        group: ChipGroup,
        options: List<RecipeFilterOption>,
        selectedKey: String?,
        onSelected: (String?) -> Unit
    ): Unit {
        group.removeAllViews()
        group.isSingleSelection = false
        group.isSelectionRequired = false
        group.tag = selectedKey

        options.forEach { option: RecipeFilterOption ->
            val chip: Chip = createFilterChip(label = option.label, checked = option.key == selectedKey)
            chip.tag = option.key
            chip.setOnClickListener {
                val currentSelectedKey: String? = group.tag as? String
                val newSelectedKey: String? = if (option.key == currentSelectedKey) null else option.key
                group.tag = newSelectedKey
                for (index in 0 until group.childCount) {
                    val childChip = group.getChildAt(index) as? Chip ?: continue
                    val childChecked = childChip.tag == newSelectedKey
                    childChip.isChecked = childChecked
                    updateFilterChipStyle(childChip, childChecked)
                }
                onSelected(newSelectedKey)
            }
            group.addView(chip)
        }
    }

    private fun bindMultiChoiceChips(
        group: ChipGroup,
        options: List<RecipeFilterOption>,
        selectedKeys: Set<String>,
        iconProvider: (String) -> String,
        onSelectedKeysChanged: (Set<String>) -> Unit
    ): Unit {
        group.removeAllViews()
        group.isSingleSelection = false
        group.isSelectionRequired = false
        group.tag = selectedKeys

        options.forEach { option: RecipeFilterOption ->
            val label: String = "${iconProvider(option.key)}  ${option.label}"
            val chip: Chip = createFilterChip(label = label, checked = option.key in selectedKeys)
            chip.tag = option.key
            chip.setOnClickListener {
                val currentKeys: Set<String> = (group.tag as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
                val updatedKeys: Set<String> = if (option.key in currentKeys) currentKeys - option.key else currentKeys + option.key
                group.tag = updatedKeys
                val isNowChecked = option.key in updatedKeys
                chip.isChecked = isNowChecked
                updateFilterChipStyle(chip, isNowChecked)
                onSelectedKeysChanged(updatedKeys)
            }
            group.addView(chip)
        }
    }

    private fun renderIncludeExcludeIngredientFilters(): Unit {
        bindSelectedIngredientChips(
            group = binding.includeIngredientChipGroup,
            options = filterController.selectedIngredientOptions(RecipeIngredientFilterMode.INCLUDE),
            onRemove = { key ->
                filterController.removeIngredient(RecipeIngredientFilterMode.INCLUDE, key)
                renderIncludeExcludeIngredientFilters()
                updateApplyButton()
            }
        )

        bindSelectedIngredientChips(
            group = binding.excludeIngredientChipGroup,
            options = filterController.selectedIngredientOptions(RecipeIngredientFilterMode.EXCLUDE),
            onRemove = { key ->
                filterController.removeIngredient(RecipeIngredientFilterMode.EXCLUDE, key)
                renderIncludeExcludeIngredientFilters()
                updateApplyButton()
            }
        )
    }

    private fun bindSelectedIngredientChips(
        group: ChipGroup,
        options: List<RecipeFilterOption>,
        onRemove: (String) -> Unit
    ): Unit {
        group.removeAllViews()

        options.forEach { option ->
            val chip = createSelectedIngredientChip(option.label).apply {
                setOnCloseIconClickListener { onRemove(option.key) }
                setOnClickListener { onRemove(option.key) }
            }
            group.addView(chip)
        }
    }

    private fun createFilterChip(label: String, checked: Boolean): Chip {
        val context = requireContext()
        return Chip(context).apply {
            text = label
            isCheckable = true
            isChecked = checked
            isClickable = true
            isFocusable = true
            isCheckedIconVisible = false
            minHeight = 34.dp
            setUniformCornerRadius(17.dp.toFloat())
            setPadding(10.dp, 0, 10.dp, 0)
            chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_surface))
            setTextColor(ContextCompat.getColor(context, if (checked) R.color.fitto_green else R.color.fitto_text_dark))
            typeface = if (checked) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            chipStrokeWidth = if (checked) 2.dp.toFloat() else 1.dp.toFloat()
            chipStrokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, if (checked) R.color.fitto_green else R.color.fitto_primary_soft)
            )
            isCloseIconVisible = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8.dp
                bottomMargin = 8.dp
            }
        }
    }

    private fun updateFilterChipStyle(chip: Chip, checked: Boolean): Unit {
        val context = chip.context
        chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_surface))
        chip.setTextColor(ContextCompat.getColor(context, if (checked) R.color.fitto_green else R.color.fitto_text_dark))
        chip.typeface = if (checked) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        chip.chipStrokeWidth = if (checked) 2.dp.toFloat() else 1.dp.toFloat()
        chip.chipStrokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (checked) R.color.fitto_green else R.color.fitto_primary_soft)
        )
    }

    private fun createSelectedIngredientChip(label: String): Chip {
        val context = requireContext()
        return Chip(context).apply {
            text = label
            isCheckable = false
            isClickable = true
            isFocusable = true
            isCloseIconVisible = true
            closeIcon = ContextCompat.getDrawable(context, R.drawable.ic_close)
            closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_green))
            minHeight = 34.dp
            setUniformCornerRadius(17.dp.toFloat())
            setPadding(10.dp, 0, 6.dp, 0)
            chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_surface))
            setTextColor(ContextCompat.getColor(context, R.color.fitto_green))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            chipStrokeWidth = 1.dp.toFloat()
            chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_green))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8.dp
                bottomMargin = 8.dp
            }
        }
    }

    private fun updateApplyButton(): Unit {
        val hasActiveFilters: Boolean = filterController.hasActiveFilters
        binding.applyButton.isEnabled = true
        binding.applyButton.alpha = 1f
        binding.applyButton.text = getString(R.string.recipes_filter_show_results)
        binding.clearButton.isEnabled = hasActiveFilters
        binding.clearButton.alpha = if (hasActiveFilters) 1f else 0.5f
    }

    private fun emojiForMainCategory(key: String): String = when (key) {
        RecipeFilterCatalog.KEY_MAIN_MEAL -> "🍛"
        RecipeFilterCatalog.KEY_SOUP -> "🍲"
        RecipeFilterCatalog.KEY_SALAD -> "🥗"
        RecipeFilterCatalog.KEY_DESSERT -> "🍰"
        RecipeFilterCatalog.KEY_BREAKFAST -> "🍳"
        RecipeFilterCatalog.KEY_SNACK -> "🥨"
        RecipeFilterCatalog.KEY_DRINK -> "🥤"
        RecipeFilterCatalog.KEY_PICKLE -> "🥒"
        RecipeFilterCatalog.KEY_SAUCE -> "🥣"
        else -> "🍽️"
    }

    private fun emojiForMainIngredient(key: String): String = when {
        key.contains("tavuk") -> "🍗"
        key.contains("kırmızı") -> "🥩"
        key.contains("kıyma") -> "🥩"
        key.contains("balık") -> "🐟"
        key.contains("hindi") -> "🍖"
        key.contains("sebze") -> "🥦"
        key.contains("patates") -> "🥔"
        key.contains("bakliyat") -> "🫘"
        key.contains("tahıl") || key.contains("pirinç") || key.contains("bulgur") -> "🌾"
        key.contains("makarna") -> "🍝"
        key.contains("hamur") || key.contains("yufka") -> "🥟"
        key.contains("süt") -> "🥛"
        key.contains("yoğurt") -> "🥣"
        key.contains("peynir") -> "🧀"
        key.contains("yumurta") -> "🥚"
        key.contains("meyve") -> "🍎"
        key.contains("kuruyemiş") -> "🥜"
        key.contains("kakao") || key.contains("çikolata") -> "🍫"
        key.contains("kahve") || key.contains("bitki") -> "🌿"
        else -> "🍽️"
    }

    private fun emojiForDiet(key: String): String = when {
        key.contains("glutensiz") -> "🌾"
        key.contains("laktozsuz") || key.contains("sut") -> "🥛"
        key.contains("vejetaryen") -> "🌱"
        key.contains("vegan") -> "🌿"
        key.contains("protein") -> "💪"
        key.contains("karbonhidrat") -> "🥗"
        key.contains("seker") || key.contains("şeker") -> "🍯"
        key.contains("kuruyemiş") -> "🥜"
        else -> "✓"
    }

    private fun emojiForMethod(key: String): String = when {
        key.contains("fırın") -> "♨️"
        key.contains("tencere") -> "🍲"
        key.contains("tava") -> "🍳"
        key.contains("kızartma") -> "🔥"
        key.contains("haşlama") -> "💧"
        key.contains("soğuk") -> "🥗"
        key.contains("demleme") -> "☕"
        key.contains("blender") || key.contains("rondo") -> "🥤"
        key.contains("dinlendirme") || key.contains("marinasyon") -> "⏳"
        key.contains("ızgara") || key.contains("mangal") -> "🥩"
        key.contains("fermantasyon") || key.contains("turşu") -> "🥒"
        key.contains("tost") -> "🥪"
        else -> "🍳"
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView(): Unit {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_FILTER_STATE = "arg_filter_state"
        private const val LABEL_TAG = "recipe_filter_card_label"

        fun newInstance(filterState: RecipeFilterState): RecipeFilterBottomSheetFragment {
            return RecipeFilterBottomSheetFragment().apply {
                arguments = bundleOf(ARG_FILTER_STATE to filterState.toBundle())
            }
        }
    }
}
