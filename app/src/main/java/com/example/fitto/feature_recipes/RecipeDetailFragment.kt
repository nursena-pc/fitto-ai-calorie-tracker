package com.example.fitto.feature_recipes

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.FragmentRecipeDetailBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_recipes.util.RecipeImageLoader
import com.example.fitto.fittoAppContainer
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding: FragmentRecipeDetailBinding
        get() = _binding!!

    private lateinit var detailCoordinator: RecipeDetailCoordinator
    private lateinit var shoppingListStore: RecipeShoppingListStore
    private lateinit var catalog: RecipeFilterCatalog
    private val detailController = RecipeDetailController()
    private val detailState: RecipeDetailState
        get() = detailController.state

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeDetailBinding.bind(view)
        val dependencies = requireContext().fittoAppContainer.createRecipeFeatureDependencies()
        detailCoordinator = dependencies.createDetailCoordinator()
        shoppingListStore = dependencies.shoppingListStore
        catalog = RecipeFilterCatalog(resources)

        setupStaticClicks()

        val args = arguments
        val recipeId: String? = args?.getString(ARG_RECIPE_ID)
        detailController.updateOrigin(
            RecipeDetailOrigin(
                fromCommunity = args?.getBoolean(ARG_FROM_COMMUNITY, false) ?: false,
                communityPostId = args?.getString(ARG_COMMUNITY_POST_ID),
                communityPostOwnerId = args?.getString(ARG_COMMUNITY_POST_OWNER_ID),
                communityRecipeSourceTag = args?.getString(ARG_COMMUNITY_RECIPE_SOURCE_TAG)
            )
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val recipe: RecipeUiModel? = detailCoordinator.loadRecipe(recipeId)

            if (_binding == null) return@launch

            if (recipe == null) {
                Toast.makeText(requireContext(), R.string.recipes_detail_not_found, Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
                return@launch
            }

            detailController.loadRecipe(recipe)
            bindRecipe(recipe)
            showTab(RecipeDetailTab.OVERVIEW)
        }
    }

    private fun setupStaticClicks(): Unit = with(binding) {
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        topShareButton.setOnClickListener { showRecipeOptionsMenu() }

        overviewTabButton.setOnClickListener { showTab(RecipeDetailTab.OVERVIEW) }
        ingredientsTabButton.setOnClickListener { showTab(RecipeDetailTab.INGREDIENTS) }
        stepsTabButton.setOnClickListener { showTab(RecipeDetailTab.STEPS) }

        portionMinusButton.setOnClickListener {
            detailController.decreaseServingMultiplier()
            renderPortionAndNutrition()
        }
        portionPlusButton.setOnClickListener {
            detailController.increaseServingMultiplier()
            renderPortionAndNutrition()
        }

        ingredientEditToggleButton.setOnClickListener { toggleEditMode() }
        addIngredientButton.setOnClickListener { addBlankIngredient() }

        bottomShoppingButton.setOnClickListener {
            if (detailState.shoppingSelectMode) addSelectedIngredientsToShoppingList() else startShoppingSelection()
        }

        saveEditedRecipeButton.setOnClickListener { saveRecipeToBook(useEditedVersion = true) }

        topCookModeCard.setOnClickListener {
            detailController.toggleCookMode()
            renderCookMode(showToast = true)
        }
        setCookModeEnabled(false, showToast = false)

        startCookingButton.setOnClickListener { openCookingMode() }
    }

    private fun setCookModeEnabled(enabled: Boolean, showToast: Boolean) {
        detailController.setCookModeEnabled(enabled)
        renderCookMode(showToast)
    }

    private fun renderCookMode(showToast: Boolean) = with(binding) {
        val enabled = detailState.cookModeEnabled
        root.keepScreenOn = enabled
        topCookModeCard.strokeColor = ContextCompat.getColor(
            requireContext(),
            if (enabled) R.color.fitto_green else R.color.fitto_primary_soft
        )
        topCookModeCard.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), if (enabled) R.color.fitto_primary_soft else R.color.fitto_surface)
        )
        topCookModeTitleText.setTextColor(
            ContextCompat.getColor(requireContext(), if (enabled) R.color.fitto_green else R.color.fitto_text_dark)
        )
        topCookModeStatusText.setTextColor(
            ContextCompat.getColor(requireContext(), if (enabled) R.color.fitto_green else R.color.fitto_text_secondary)
        )
        topCookModeStatusText.text = getString(
            if (enabled) R.string.recipes_detail_cook_mode_active else R.string.recipes_detail_cook_mode_subtitle
        )
        if (showToast) {
            val messageRes = if (enabled) R.string.recipes_detail_cook_mode_on else R.string.recipes_detail_cook_mode_off
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindRecipe(recipe: RecipeUiModel): Unit = with(binding) {
        RecipeImageLoader.load(recipeImage, recipe)
        titleText.text = recipe.title
        subcategoryText.text = catalog.subcategoryLabel(
            recipe.mainCategoryKey,
            recipe.subcategoryKey.ifBlank { recipe.subcategoryKeys.firstOrNull().orEmpty() }
        )
        categoryBadgeText.text = catalog.mainCategoryLabel(recipe.mainCategoryKey)
        infoServingText.text = getString(
            R.string.recipes_detail_info_serving,
            recipe.servingDisplay.ifBlank { getString(R.string.recipes_detail_metric_serving, recipe.servings) }
        )
        infoDurationText.text = getString(R.string.recipes_detail_info_duration, recipe.durationLabel)
        infoDifficultyText.text = getString(
            R.string.recipes_detail_info_difficulty,
            catalog.difficulties.firstOrNull { option -> option.key == recipe.difficultyKey }?.label ?: recipe.difficultyKey
        )

        overviewDescriptionText.isVisible = false
        tipText.isVisible = false

        renderIngredients()
        renderPortionAndNutrition()
        renderSteps(recipe)
    }

    private fun shouldUseCommunityBookAction(): Boolean {
        return detailController.shouldUseCommunityBookAction()
    }

    private fun saveOriginalRecipe() {
        val recipe = detailState.recipe ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            detailCoordinator.saveOriginalRecipe(recipe.id)
        }
        Toast.makeText(requireContext(), getString(R.string.recipes_detail_saved_original, recipe.title), Toast.LENGTH_SHORT).show()
    }

    private fun saveCommunityRecipeToBook() {
        val recipe = detailState.recipe ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val savedId = detailCoordinator.saveCommunityRecipe(
                RecipeCommunityBookRequest(
                    postId = detailState.origin.communityPostId.orEmpty(),
                    postUserId = detailState.origin.communityPostOwnerId.orEmpty(),
                    recipeId = recipe.id,
                    recipeTitle = recipe.title,
                    recipeImageResId = recipe.imageResId,
                    recipeDuration = recipe.durationLabel,
                    recipeServing = recipe.servingDisplay,
                    recipeCalories = recipe.calories.takeIf { it > 0 },
                    recipeCategory = recipe.mainCategoryKey,
                    recipeSourceTag = RecipeRepository.RECIPE_SOURCE_TAG_COMMUNITY,
                    recipeDescription = recipe.description,
                    recipeIngredients = detailState.ingredientLines,
                    recipeSteps = recipe.steps
                )
            )

            if (_binding == null) return@launch
            Toast.makeText(
                requireContext(),
                if (savedId != null) R.string.recipes_detail_added_to_my_shared_book else R.string.recipes_detail_community_book_add_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showRecipeOptionsMenu() {
        val recipe = detailState.recipe ?: return
        val context = requireContext()

        val card = MaterialCardView(context).apply {
            radius = 18.dp.toFloat()
            cardElevation = 8.dp.toFloat()
            strokeWidth = 1.dp
            strokeColor = ContextCompat.getColor(context, R.color.fitto_divider_soft)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_surface))
        }

        val menuContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6.dp, 6.dp, 6.dp, 6.dp)
        }
        card.addView(menuContainer)

        val popupWidth = 218.dp
        val popup = PopupWindow(
            card,
            popupWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 8.dp.toFloat()
        }

        fun addMenuRow(iconRes: Int, titleRes: Int, onClick: () -> Unit) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                foreground = android.util.TypedValue().let { value ->
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
                    ContextCompat.getDrawable(context, value.resourceId)
                }
                setPadding(12.dp, 10.dp, 12.dp, 10.dp)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    46.dp
                )
                setOnClickListener {
                    popup.dismiss()
                    onClick()
                }
            }

            row.addView(ImageView(context).apply {
                setImageResource(iconRes)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_green))
                layoutParams = LinearLayout.LayoutParams(20.dp, 20.dp).apply { marginEnd = 10.dp }
            })

            row.addView(TextView(context).apply {
                text = getString(titleRes)
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            menuContainer.addView(row)
        }

        if (shouldUseCommunityBookAction()) {
            addMenuRow(R.drawable.ic_bookmark, R.string.recipes_detail_add_to_my_book) {
                saveCommunityRecipeToBook()
            }
        } else {
            addMenuRow(R.drawable.ic_bookmark_border, R.string.recipes_detail_save) {
                saveOriginalRecipe()
            }
        }
        addMenuRow(R.drawable.ic_share, R.string.recipes_detail_share_device) {
            shareRecipeToDevice(recipe)
        }
        addMenuRow(R.drawable.ic_group, R.string.recipes_detail_share_post) {
            showCommunityPostDialog(recipe)
        }

        popup.showAsDropDown(binding.topShareButton, -(popupWidth - 46.dp), 8.dp)
    }

    private fun showCommunityPostDialog(recipe: RecipeUiModel) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp, 10.dp, 20.dp, 0)
        }

        val previewCard = MaterialCardView(context).apply {
            radius = 18.dp.toFloat()
            cardElevation = 0f
            strokeColor = ContextCompat.getColor(context, R.color.fitto_primary_soft)
            strokeWidth = 1.dp
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_surface_soft))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14.dp }
        }

        val previewRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        }
        previewRow.addView(ImageView(context).apply {
            RecipeImageLoader.load(this, recipe)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(72.dp, 72.dp).apply { marginEnd = 12.dp }
        })
        previewRow.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(context).apply {
                text = getString(R.string.recipes_detail_post_preview_recipe)
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.fitto_green))
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = recipe.title
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 4.dp }
            })
            addView(TextView(context).apply {
                text = listOf(recipe.durationLabel, recipe.servingDisplay, getString(R.string.recipes_detail_info_kcal, calculatedCaloriesPerServing()))
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 4.dp }
            })
        })
        previewCard.addView(previewRow)
        container.addView(previewCard)

        val commentInput = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = ContextCompat.getColor(context, R.color.fitto_surface)
            boxStrokeColor = ContextCompat.getColor(context, R.color.fitto_primary_soft)
            hint = getString(R.string.recipes_detail_post_comment_hint)
            setBoxCornerRadii(16.dp.toFloat(), 16.dp.toFloat(), 16.dp.toFloat(), 16.dp.toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        commentInput.setBoxStrokeColorStateList(fittoInputStrokeColors(context))
        commentInput.hintTextColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_text_secondary))

        val commentEdit = TextInputEditText(context).apply {
            minLines = 3
            maxLines = 5
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            setHintTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
        }
        commentInput.addView(
            commentEdit,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        container.addView(commentInput)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.recipes_detail_post_preview_title)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.recipes_detail_post_publish) { _, _ ->
                shareRecipeToSocial(recipe, commentEdit.text?.toString().orEmpty())
            }
            .create()

        dialog.setOnShowListener {
            val green = ContextCompat.getColor(context, R.color.fitto_green)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(green)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(green)
        }
        dialog.show()
    }

    private fun fittoInputStrokeColors(context: Context): ColorStateList {
        val active = ContextCompat.getColor(context, R.color.fitto_green)
        val normal = ContextCompat.getColor(context, R.color.fitto_primary_soft)
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_hovered),
                intArrayOf()
            ),
            intArrayOf(active, active, normal)
        )
    }

    private fun shareRecipeToDevice(recipe: RecipeUiModel) {
        val shareText = buildString {
            append(recipe.title).append('\n')
            append(recipe.description).append('\n')
            if (detailState.ingredients.isNotEmpty()) {
                append('\n').append(getString(R.string.recipes_detail_ingredients)).append(':').append('\n')
                append(
                    detailState.ingredients.joinToString(separator = "\n") { ingredient ->
                        "• ${RecipeDetailIngredientMapper.toDisplayLine(ingredient)}"
                    }
                )
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipe.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.recipes_detail_share)))
    }

    private fun shareRecipeToSocial(recipe: RecipeUiModel, comment: String = "") {
        viewLifecycleOwner.lifecycleScope.launch {
            val postId = detailCoordinator.shareRecipeToCommunity(
                RecipeCommunityShareRequest(
                    recipeId = recipe.id,
                    ingredients = detailState.ingredientLines,
                    comment = comment
                )
            )
            if (_binding == null) return@launch
            val message = if (postId != null) {
                getString(R.string.recipes_detail_shared_to_community_with_comment)
            } else {
                getString(R.string.recipes_detail_community_share_failed)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showTab(tab: RecipeDetailTab): Unit = with(binding) {
        val previousShoppingMode = detailState.shoppingSelectMode
        val state = detailController.selectTab(tab)
        if (previousShoppingMode && !state.shoppingSelectMode) {
            renderIngredients()
        }
        renderIngredientModeControls()

        overviewContent.isVisible = tab == RecipeDetailTab.OVERVIEW
        ingredientsContent.isVisible = tab == RecipeDetailTab.INGREDIENTS
        stepsContent.isVisible = tab == RecipeDetailTab.STEPS

        val showBottomActions = tab == RecipeDetailTab.INGREDIENTS
        bottomShoppingButton.isVisible = showBottomActions
        bottomActionRow.isVisible = showBottomActions

        styleTabButton(overviewTabButton, tab == RecipeDetailTab.OVERVIEW)
        styleTabButton(ingredientsTabButton, tab == RecipeDetailTab.INGREDIENTS)
        styleTabButton(stepsTabButton, tab == RecipeDetailTab.STEPS)
    }

    private fun styleTabButton(button: MaterialButton, selected: Boolean) {
        button.background = ContextCompat.getDrawable(
            requireContext(),
            if (selected) R.drawable.bg_recipe_tab_selected else R.drawable.bg_recipe_tab_unselected
        )
        button.backgroundTintList = null
        button.strokeWidth = 0
        button.setTextColor(ContextCompat.getColor(requireContext(), if (selected) R.color.fitto_green else R.color.fitto_text_dark))
        button.iconTint = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), if (selected) R.color.fitto_green else R.color.fitto_text_secondary)
        )
        button.setTypeface(button.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun toggleEditMode() {
        val change = detailController.toggleEditMode()
        if (change.editModeStarted) {
            Toast.makeText(requireContext(), R.string.recipes_detail_edit_mode_enabled, Toast.LENGTH_LONG).show()
        }
        renderIngredientModeControls()
        renderIngredients()
        renderPortionAndNutrition()
    }

    private fun renderIngredientModeControls(): Unit = with(binding) {
        val state = detailState
        ingredientEditToggleButton.text = getString(
            if (state.editMode) R.string.cancel else R.string.recipes_detail_edit_for_me_short
        )
        addIngredientButton.isVisible = state.editMode
        saveEditedRecipeButton.isVisible = state.editMode
        bottomShoppingButton.text = getString(
            if (state.shoppingSelectMode) {
                R.string.recipes_detail_add_selected_to_shopping
            } else {
                R.string.recipes_detail_add_to_shopping
            }
        )
    }

    private fun renderIngredients(): Unit = with(binding) {
        overviewIngredientListContainer.removeAllViews()
        ingredientListContainer.removeAllViews()
        addIngredientButton.isVisible = detailState.editMode
        saveEditedRecipeButton.isVisible = detailState.editMode

        overviewIngredientListContainer.addView(createQuickIngredientSummary())
        detailState.ingredients.forEachIndexed { index, ingredient ->
            ingredientListContainer.addView(createIngredientRow(index, ingredient, compact = false))
        }
    }

    private fun createQuickIngredientSummary(): View {
        val context = requireContext()
        val names = detailState.ingredients
            .map { ingredient -> ingredient.name.trim() }
            .filter { name -> name.isNotBlank() }
            .distinct()
        return TextView(context).apply {
            text = if (names.isEmpty()) "—" else names.joinToString(separator = " • ")
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            textSize = 12f
            maxLines = 4
            setLineSpacing(2f, 1.0f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createIngredientRow(
        index: Int,
        ingredient: RecipeDetailIngredient,
        compact: Boolean
    ): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_ingredient_row)
            setPadding(10.dp, 7.dp, 6.dp, 7.dp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 6.dp }
        }

        val selectionText = TextView(context).apply {
            isVisible = detailState.shoppingSelectMode
            text = if (index in detailState.selectedShoppingIndexes) "✓" else "○"
            gravity = Gravity.CENTER
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, if (index in detailState.selectedShoppingIndexes) R.color.fitto_green else R.color.fitto_text_secondary))
            background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_ingredient_icon_circle)
            layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp).apply { marginEnd = 8.dp }
            setOnClickListener { toggleIngredientSelection(index) }
        }

        val kcalText = TextView(context).apply {
            text = getString(R.string.recipes_detail_ingredient_kcal, ingredient.estimatedCalories)
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.fitto_green))
            layoutParams = LinearLayout.LayoutParams(62.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 8.dp
            }
        }

        val nameView: View = TextView(context).apply {
            text = ingredient.name
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8.dp
            }
        }

        val (amountOnly, unitOnly) = RecipeDetailIngredientMapper.splitAmountAndUnit(
            ingredient.amountUnit
        )
        val amountView: View = if (detailState.editMode && !compact) {
            EditText(context).apply {
                setText(amountOnly)
                setSingleLine(true)
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.fitto_green))
                setHintTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
                hint = getString(R.string.recipes_detail_amount_short_hint)
                background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_amount_pill)
                setPadding(6.dp, 0, 6.dp, 0)
                layoutParams = LinearLayout.LayoutParams(58.dp, 40.dp)
                doAfterTextChanged { editable ->
                    val updatedIngredient = detailController.updateIngredientAmount(
                        index = index,
                        amount = editable?.toString().orEmpty()
                    ) ?: return@doAfterTextChanged
                    kcalText.text = getString(
                        R.string.recipes_detail_ingredient_kcal,
                        updatedIngredient.estimatedCalories
                    )
                    renderPortionAndNutrition()
                }
            }
        } else {
            TextView(context).apply {
                text = ingredient.amountUnit.ifBlank { "—" }
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
                background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_amount_pill)
                setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                layoutParams = LinearLayout.LayoutParams(104.dp, 38.dp)
            }
        }

        val unitText = TextView(context).apply {
            isVisible = detailState.editMode && !compact
            text = unitOnly.ifBlank { getString(R.string.recipes_detail_unit_empty) }
            gravity = Gravity.CENTER_VERTICAL
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            layoutParams = LinearLayout.LayoutParams(52.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 6.dp
            }
        }

        val removeButton = ImageButton(context).apply {
            isVisible = detailState.editMode && !compact
            background = null
            setImageResource(R.drawable.ic_close)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            contentDescription = getString(R.string.recipes_detail_remove_ingredient)
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
            layoutParams = LinearLayout.LayoutParams(36.dp, 36.dp)
            setOnClickListener { removeIngredientAt(index) }
        }


        row.setOnClickListener { if (detailState.shoppingSelectMode) toggleIngredientSelection(index) }

        row.addView(selectionText)
        row.addView(nameView)
        row.addView(amountView)
        row.addView(unitText)
        row.addView(kcalText)
        row.addView(removeButton)
        return row
    }

    private fun toggleIngredientSelection(index: Int) {
        detailController.toggleIngredientSelection(index)
        renderIngredients()
    }

    private fun removeIngredientAt(index: Int) {
        detailController.removeIngredient(index)
        renderIngredients()
        renderPortionAndNutrition()
    }

    private fun renderPortionAndNutrition(): Unit = with(binding) {
        val nutritionState = resolvePortionNutritionState()
        val activeServings = nutritionState.activeServings
        val perServingCalories = nutritionState.perServingCalories
        val activeTotalCalories = nutritionState.activeTotalCalories
        val macros = nutritionState.macros

        portionValueText.text = activeServings.toString()
        perServingKcalText.text = getString(R.string.recipes_detail_metric_kcal, perServingCalories)
        infoKcalText.text = getString(R.string.recipes_detail_info_kcal, perServingCalories)

        val totalLabel = getString(R.string.recipes_detail_total_for_servings, activeServings)
        overviewTotalLabelText.text = totalLabel
        overviewTotalKcalText.text = getString(R.string.recipes_detail_total_kcal, activeTotalCalories)

        val macroPercent = nutritionState.macroPercent
        overviewCarbText.text = getString(R.string.recipes_detail_macro_carb_percent, macros.carbs, macroPercent.carbs)
        overviewProteinText.text = getString(R.string.recipes_detail_macro_protein_percent, macros.protein, macroPercent.protein)
        overviewFatText.text = getString(R.string.recipes_detail_macro_fat_percent, macros.fat, macroPercent.fat)
        macroDonutView.setNutrition(activeTotalCalories, macros.carbs, macros.protein, macros.fat)
    }

    private fun calculatedCaloriesPerServing(): Int {
        return resolvePortionNutritionState().perServingCalories
    }

    private fun resolvePortionNutritionState(): RecipePortionNutritionResolver.State {
        return detailController.nutritionState()
    }

    private fun openCookingMode() {
        val recipe = detailState.recipe ?: return
        setCookModeEnabled(true, showToast = false)
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this@RecipeDetailFragment)
            .add(R.id.homeTabContainer, RecipeCookingModeFragment.newInstance(recipe.id), TAG_RECIPE_COOKING)
            .addToBackStack(TAG_RECIPE_COOKING)
            .commit()
    }

    private fun renderSteps(recipe: RecipeUiModel): Unit = with(binding) {
        stepsContainer.removeAllViews()
        recipe.steps.forEachIndexed { index, step ->
            val number = index + 1
            stepsContainer.addView(createStepRow(number, step, compact = false))
        }
    }

    private fun createStepRow(number: Int, step: String, compact: Boolean): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_step_row)
            setPadding(2.dp, if (compact) 6.dp else 10.dp, 2.dp, if (compact) 6.dp else 10.dp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = if (compact) 2.dp else 6.dp }
        }

        val numberText = TextView(context).apply {
            text = number.toString()
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_step_number)
            layoutParams = LinearLayout.LayoutParams(28.dp, 28.dp).apply { marginEnd = 10.dp }
        }

        val stepText = TextView(context).apply {
            text = step
            textSize = if (compact) 13f else 14f
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(numberText)
        row.addView(stepText)
        return row
    }

    private fun addBlankIngredient() {
        val change = detailController.addIngredient(
            RecipeDetailIngredient(
                id = "ingredient_added_${System.currentTimeMillis()}",
                icon = "🥄",
                name = getString(R.string.recipes_detail_new_ingredient),
                amountUnit = "",
                estimatedCalories = 40
            )
        )
        if (change.editModeStarted) {
            Toast.makeText(requireContext(), R.string.recipes_detail_edit_mode_enabled, Toast.LENGTH_LONG).show()
        }
        renderIngredientModeControls()
        renderIngredients()
        renderPortionAndNutrition()
    }

    private fun startShoppingSelection() {
        detailController.startShoppingSelection()
        renderIngredientModeControls()
        renderIngredients()
        renderPortionAndNutrition()
        Toast.makeText(requireContext(), R.string.recipes_detail_select_ingredients_hint, Toast.LENGTH_SHORT).show()
    }

    private fun addSelectedIngredientsToShoppingList() {
        val selectedLines = detailState.selectedShoppingLines
        if (selectedLines.isEmpty()) {
            Toast.makeText(requireContext(), R.string.recipes_detail_no_ingredient_selected, Toast.LENGTH_SHORT).show()
            return
        }
        shoppingListStore.addItems(selectedLines)

        detailController.completeShoppingSelection()
        renderIngredientModeControls()
        renderIngredients()
        Toast.makeText(requireContext(), getString(R.string.recipes_detail_added_to_shopping, selectedLines.size), Toast.LENGTH_LONG).show()
    }

    private fun saveRecipeToBook(useEditedVersion: Boolean) {
        val recipe = detailState.recipe ?: return
        val editedLines: List<String>? = if (useEditedVersion) detailState.ingredientLines else null

        viewLifecycleOwner.lifecycleScope.launch {
            val copiedRecipeId = detailCoordinator.copyRecipeToUserBook(
                RecipeBookCopyRequest(
                    recipeId = recipe.id,
                    ingredientsOverride = editedLines
                )
            )
            if (_binding == null) return@launch

            val saved = copiedRecipeId != null
            val messageRes = if (saved) {
                R.string.recipes_detail_saved_to_book
            } else {
                R.string.recipes_detail_save_failed
            }
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            detailController.completeEditedRecipeSave(
                success = saved,
                useEditedVersion = useEditedVersion
            )
            renderIngredientModeControls()
            renderIngredients()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.root.keepScreenOn = false
        _binding = null
        super.onDestroyView()
    }


    companion object {
        private const val ARG_RECIPE_ID = "arg_recipe_id"
        private const val ARG_FROM_COMMUNITY = "arg_from_community"
        private const val ARG_COMMUNITY_POST_ID = "arg_community_post_id"
        private const val ARG_COMMUNITY_POST_OWNER_ID = "arg_community_post_owner_id"
        private const val ARG_COMMUNITY_RECIPE_SOURCE_TAG = "arg_community_recipe_source_tag"
        private const val TAG_RECIPE_COOKING = "recipe_cooking_mode"
        fun newInstance(
            recipeId: String,
            fromCommunity: Boolean = false,
            communityPostId: String? = null,
            communityPostOwnerId: String? = null,
            communityRecipeSourceTag: String? = null
        ): RecipeDetailFragment {
            return RecipeDetailFragment().apply {
                arguments = bundleOf(
                    ARG_RECIPE_ID to recipeId,
                    ARG_FROM_COMMUNITY to fromCommunity,
                    ARG_COMMUNITY_POST_ID to communityPostId,
                    ARG_COMMUNITY_POST_OWNER_ID to communityPostOwnerId,
                    ARG_COMMUNITY_RECIPE_SOURCE_TAG to communityRecipeSourceTag
                )
            }
        }
    }
}
