package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding: FragmentRecipeDetailBinding
        get() = _binding!!

    private lateinit var repository: RecipeRepository
    private lateinit var catalog: RecipeFilterCatalog
    private var currentRecipe: RecipeUiModel? = null
    private var fromCommunity: Boolean = false
    private var communityPostId: String? = null
    private var communityPostOwnerId: String? = null
    private var communityRecipeSourceTag: String? = null
    private var activeTab: DetailTab = DetailTab.OVERVIEW
    private var editMode: Boolean = false
    private var shoppingSelectMode: Boolean = false
    private var cookModeEnabled: Boolean = false
    private var servingMultiplier: Int = 1
    private val selectedShoppingIndexes: MutableSet<Int> = linkedSetOf()
    private val detailIngredients: MutableList<DetailIngredient> = mutableListOf()
    private var editModeSnapshot: List<DetailIngredient> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipeDetailBinding.bind(view)
        repository = RecipeRepository(requireContext())
        catalog = RecipeFilterCatalog(resources)

        setupStaticClicks()

        val args = arguments
        val recipeId: String? = args?.getString(ARG_RECIPE_ID)
        fromCommunity = args?.getBoolean(ARG_FROM_COMMUNITY, false) ?: false
        communityPostId = args?.getString(ARG_COMMUNITY_POST_ID)
        communityPostOwnerId = args?.getString(ARG_COMMUNITY_POST_OWNER_ID)
        communityRecipeSourceTag = args?.getString(ARG_COMMUNITY_RECIPE_SOURCE_TAG)

        viewLifecycleOwner.lifecycleScope.launch {
            val recipe: RecipeUiModel? = withContext(Dispatchers.IO) {
                recipeId?.let { id -> repository.findById(id) }
            }

            if (_binding == null) return@launch

            if (recipe == null) {
                Toast.makeText(requireContext(), R.string.recipes_detail_not_found, Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
                return@launch
            }

            currentRecipe = recipe
            detailIngredients.clear()
            detailIngredients.addAll(recipe.ingredients.mapIndexed { index, line -> line.toDetailIngredient(index) })
            servingMultiplier = 1
            bindRecipe(recipe)
            showTab(DetailTab.OVERVIEW)
        }
    }

    private fun setupStaticClicks(): Unit = with(binding) {
        backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        topShareButton.setOnClickListener { showRecipeOptionsMenu() }

        overviewTabButton.setOnClickListener { showTab(DetailTab.OVERVIEW) }
        ingredientsTabButton.setOnClickListener { showTab(DetailTab.INGREDIENTS) }
        stepsTabButton.setOnClickListener { showTab(DetailTab.STEPS) }

        portionMinusButton.setOnClickListener {
            if (servingMultiplier > 1) {
                servingMultiplier--
                renderPortionAndNutrition()
            }
        }
        portionPlusButton.setOnClickListener {
            servingMultiplier++
            renderPortionAndNutrition()
        }

        ingredientEditToggleButton.setOnClickListener { toggleEditMode() }
        addIngredientButton.setOnClickListener { addBlankIngredient() }

        bottomShoppingButton.setOnClickListener {
            if (shoppingSelectMode) addSelectedIngredientsToShoppingList() else startShoppingSelection()
        }

        saveEditedRecipeButton.setOnClickListener { saveRecipeToBook(useEditedVersion = true) }

        topCookModeCard.setOnClickListener { setCookModeEnabled(!cookModeEnabled, showToast = true) }
        setCookModeEnabled(false, showToast = false)

        startCookingButton.setOnClickListener { openCookingMode() }
    }

    private fun setCookModeEnabled(enabled: Boolean, showToast: Boolean) = with(binding) {
        cookModeEnabled = enabled
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
        if (!fromCommunity) return false
        val sourceTag = communityRecipeSourceTag.orEmpty()
        return sourceTag == RecipeRepository.RECIPE_SOURCE_TAG_COMMUNITY ||
            currentRecipe?.id.orEmpty().startsWith("community_preview_") ||
            currentRecipe?.id.orEmpty().startsWith("shared_")
    }

    private fun saveOriginalRecipe() {
        val recipe = currentRecipe ?: return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repository.saveFavorite(recipe.id)
        }
        Toast.makeText(requireContext(), getString(R.string.recipes_detail_saved_original, recipe.title), Toast.LENGTH_SHORT).show()
    }

    private fun saveCommunityRecipeToBook() {
        val recipe = currentRecipe ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val savedId = withContext(Dispatchers.IO) {
                repository.saveRecipeFromCommunityPost(
                    postId = communityPostId.orEmpty(),
                    postUserId = communityPostOwnerId.orEmpty(),
                    recipeId = recipe.id,
                    recipeTitle = recipe.title,
                    recipeImageResId = recipe.imageResId,
                    recipeDuration = recipe.durationLabel,
                    recipeServing = recipe.servingDisplay,
                    recipeCalories = recipe.calories.takeIf { it > 0 },
                    recipeCategory = recipe.mainCategoryKey,
                    recipeSourceTag = RecipeRepository.RECIPE_SOURCE_TAG_COMMUNITY,
                    recipeDescription = recipe.description,
                    recipeIngredients = detailIngredients.map { ingredient -> ingredient.toDisplayLine() },
                    recipeSteps = recipe.steps
                )
            }

            if (_binding == null) return@launch
            Toast.makeText(
                requireContext(),
                if (savedId != null) R.string.recipes_detail_added_to_my_shared_book else R.string.recipes_detail_community_book_add_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showRecipeOptionsMenu() {
        val recipe = currentRecipe ?: return
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
            if (detailIngredients.isNotEmpty()) {
                append('\n').append(getString(R.string.recipes_detail_ingredients)).append(':').append('\n')
                append(detailIngredients.joinToString(separator = "\n") { ingredient -> "• ${ingredient.toDisplayLine()}" })
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
            val postId = withContext(Dispatchers.IO) {
                repository.shareRecipeToCommunity(recipe.id, detailIngredients.map { ingredient -> ingredient.toDisplayLine() }, comment)
            }
            if (_binding == null) return@launch
            val message = if (postId != null) {
                getString(R.string.recipes_detail_shared_to_community_with_comment)
            } else {
                getString(R.string.recipes_detail_community_share_failed)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showTab(tab: DetailTab): Unit = with(binding) {
        activeTab = tab
        if (tab != DetailTab.INGREDIENTS && shoppingSelectMode) {
            shoppingSelectMode = false
            selectedShoppingIndexes.clear()
            bottomShoppingButton.text = getString(R.string.recipes_detail_add_to_shopping)
            renderIngredients()
        }
        overviewContent.isVisible = tab == DetailTab.OVERVIEW
        ingredientsContent.isVisible = tab == DetailTab.INGREDIENTS
        stepsContent.isVisible = tab == DetailTab.STEPS

        val showBottomActions = tab == DetailTab.INGREDIENTS
        bottomShoppingButton.isVisible = showBottomActions
        bottomActionRow.isVisible = showBottomActions

        styleTabButton(overviewTabButton, tab == DetailTab.OVERVIEW)
        styleTabButton(ingredientsTabButton, tab == DetailTab.INGREDIENTS)
        styleTabButton(stepsTabButton, tab == DetailTab.STEPS)
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
        if (editMode) {
            detailIngredients.clear()
            detailIngredients.addAll(editModeSnapshot)
            editModeSnapshot = emptyList()
            editMode = false
            shoppingSelectMode = false
            selectedShoppingIndexes.clear()
        } else {
            editModeSnapshot = detailIngredients.map { ingredient -> ingredient.copy() }
            editMode = true
            shoppingSelectMode = false
            selectedShoppingIndexes.clear()
            binding.bottomShoppingButton.text = getString(R.string.recipes_detail_add_to_shopping)
            Toast.makeText(requireContext(), R.string.recipes_detail_edit_mode_enabled, Toast.LENGTH_LONG).show()
        }
        binding.ingredientEditToggleButton.text = getString(
            if (editMode) R.string.cancel else R.string.recipes_detail_edit_for_me_short
        )
        binding.addIngredientButton.isVisible = editMode
        binding.saveEditedRecipeButton.isVisible = editMode
        renderIngredients()
        renderPortionAndNutrition()
    }

    private fun enableEditMode() {
        if (!editMode) {
            editModeSnapshot = detailIngredients.map { ingredient -> ingredient.copy() }
            editMode = true
            binding.ingredientEditToggleButton.text = getString(R.string.cancel)
            binding.addIngredientButton.isVisible = true
            binding.saveEditedRecipeButton.isVisible = true
            Toast.makeText(requireContext(), R.string.recipes_detail_edit_mode_enabled, Toast.LENGTH_LONG).show()
        }
    }

    private fun renderIngredients(): Unit = with(binding) {
        overviewIngredientListContainer.removeAllViews()
        ingredientListContainer.removeAllViews()
        addIngredientButton.isVisible = editMode
        saveEditedRecipeButton.isVisible = editMode

        overviewIngredientListContainer.addView(createQuickIngredientSummary())
        detailIngredients.forEachIndexed { index, ingredient ->
            ingredientListContainer.addView(createIngredientRow(index, ingredient, compact = false))
        }
    }

    private fun createQuickIngredientSummary(): View {
        val context = requireContext()
        val names = detailIngredients
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

    private fun createIngredientRow(index: Int, ingredient: DetailIngredient, compact: Boolean): View {
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
            isVisible = shoppingSelectMode
            text = if (index in selectedShoppingIndexes) "✓" else "○"
            gravity = Gravity.CENTER
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, if (index in selectedShoppingIndexes) R.color.fitto_green else R.color.fitto_text_secondary))
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

        val (amountOnly, unitOnly) = ingredient.amountUnit.splitAmountAndUnit()
        val amountView: View = if (editMode && !compact) {
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
                    if (index in detailIngredients.indices) {
                        val newAmount = editable?.toString().orEmpty().trim()
                        val newAmountUnit = listOf(newAmount, unitOnly).filter { it.isNotBlank() }.joinToString(" ")
                        val current = detailIngredients[index]
                        val updatedCalories = estimateCalories(current.name, newAmountUnit)
                        detailIngredients[index] = current.copy(
                            amountUnit = newAmountUnit,
                            estimatedCalories = updatedCalories
                        )
                        kcalText.text = getString(R.string.recipes_detail_ingredient_kcal, updatedCalories)
                        renderPortionAndNutrition()
                    }
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
            isVisible = editMode && !compact
            text = unitOnly.ifBlank { getString(R.string.recipes_detail_unit_empty) }
            gravity = Gravity.CENTER_VERTICAL
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            layoutParams = LinearLayout.LayoutParams(52.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = 6.dp
            }
        }

        val removeButton = ImageButton(context).apply {
            isVisible = editMode && !compact
            background = null
            setImageResource(R.drawable.ic_close)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            contentDescription = getString(R.string.recipes_detail_remove_ingredient)
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
            layoutParams = LinearLayout.LayoutParams(36.dp, 36.dp)
            setOnClickListener { removeIngredientAt(index) }
        }


        row.setOnClickListener { if (shoppingSelectMode) toggleIngredientSelection(index) }

        row.addView(selectionText)
        row.addView(nameView)
        row.addView(amountView)
        row.addView(unitText)
        row.addView(kcalText)
        row.addView(removeButton)
        return row
    }

    private fun toggleIngredientSelection(index: Int) {
        if (!shoppingSelectMode) return
        if (index in selectedShoppingIndexes) selectedShoppingIndexes.remove(index) else selectedShoppingIndexes.add(index)
        renderIngredients()
    }

    private fun removeIngredientAt(index: Int) {
        if (index !in detailIngredients.indices) return
        detailIngredients.removeAt(index)
        selectedShoppingIndexes.clear()
        if (shoppingSelectMode) selectedShoppingIndexes.addAll(detailIngredients.indices)
        renderIngredients()
        renderPortionAndNutrition()
    }

    private fun renderPortionAndNutrition(): Unit = with(binding) {
        val baseServings = baseServingCount()
        val activeServings = activeServingCount()
        val perServingCalories = calculatedCaloriesPerServing()
        val activeTotalCalories = perServingCalories * activeServings
        val macros = calculatedMacros(activeTotalCalories)

        portionValueText.text = activeServings.toString()
        perServingKcalText.text = getString(R.string.recipes_detail_metric_kcal, perServingCalories)
        infoKcalText.text = getString(R.string.recipes_detail_info_kcal, perServingCalories)

        val totalLabel = getString(R.string.recipes_detail_total_for_servings, activeServings)
        overviewTotalLabelText.text = totalLabel
        overviewTotalKcalText.text = getString(R.string.recipes_detail_total_kcal, activeTotalCalories)

        val macroPercent = macroPercentages(macros)
        overviewCarbText.text = getString(R.string.recipes_detail_macro_carb_percent, macros.carbs, macroPercent.carbs)
        overviewProteinText.text = getString(R.string.recipes_detail_macro_protein_percent, macros.protein, macroPercent.protein)
        overviewFatText.text = getString(R.string.recipes_detail_macro_fat_percent, macros.fat, macroPercent.fat)
        macroDonutView.setNutrition(activeTotalCalories, macros.carbs, macros.protein, macros.fat)
    }

    private fun baseServingCount(): Int {
        return currentRecipe?.servings?.coerceAtLeast(1) ?: 1
    }

    private fun activeServingCount(): Int {
        return baseServingCount() * servingMultiplier
    }

    private fun calculatedCaloriesPerServing(): Int {
        val recipe = currentRecipe
        val fromRecipe = recipe?.calories ?: 0
        val estimatedTotal = detailIngredients.sumOf { ingredient -> ingredient.estimatedCalories }
        val estimatedPerServing = (estimatedTotal / baseServingCount().toDouble()).roundToInt()
        return maxOf(fromRecipe, estimatedPerServing, 0)
    }

    private fun calculatedMacros(totalCalories: Int): MacroSummary {
        val recipe = currentRecipe
        val scale = activeServingCount().toDouble() / baseServingCount().toDouble()
        val sourceProtein = recipe?.proteinGrams ?: 0
        val sourceCarbs = recipe?.carbGrams ?: 0
        val sourceFat = recipe?.fatGrams ?: 0

        if (sourceProtein > 0 || sourceCarbs > 0 || sourceFat > 0) {
            return MacroSummary(
                protein = (sourceProtein * scale).roundToInt().coerceAtLeast(0),
                carbs = (sourceCarbs * scale).roundToInt().coerceAtLeast(0),
                fat = (sourceFat * scale).roundToInt().coerceAtLeast(0)
            )
        }

        return MacroSummary(
            protein = (totalCalories * 0.15 / 4).roundToInt().coerceAtLeast(0),
            carbs = (totalCalories * 0.55 / 4).roundToInt().coerceAtLeast(0),
            fat = (totalCalories * 0.30 / 9).roundToInt().coerceAtLeast(0)
        )
    }

    private fun macroPercentages(macros: MacroSummary): MacroPercentSummary {
        val proteinCalories = macros.protein * 4
        val carbCalories = macros.carbs * 4
        val fatCalories = macros.fat * 9
        val total = (proteinCalories + carbCalories + fatCalories).coerceAtLeast(1)
        return MacroPercentSummary(
            protein = ((proteinCalories * 100.0) / total).roundToInt().coerceIn(0, 100),
            carbs = ((carbCalories * 100.0) / total).roundToInt().coerceIn(0, 100),
            fat = ((fatCalories * 100.0) / total).roundToInt().coerceIn(0, 100)
        )
    }

    private fun openCookingMode() {
        val recipe = currentRecipe ?: return
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
        enableEditMode()
        val nextIndex = detailIngredients.size
        val ingredient = DetailIngredient(
            id = "ingredient_added_${System.currentTimeMillis()}",
            icon = "🥄",
            name = getString(R.string.recipes_detail_new_ingredient),
            amountUnit = "",
            estimatedCalories = 40
        )
        detailIngredients.add(ingredient)
        if (shoppingSelectMode) {
            selectedShoppingIndexes.add(nextIndex)
        }
        renderIngredients()
        renderPortionAndNutrition()
    }

    private fun startShoppingSelection() {
        if (editMode) toggleEditMode()
        shoppingSelectMode = true
        selectedShoppingIndexes.clear()
        selectedShoppingIndexes.addAll(detailIngredients.indices)
        binding.bottomShoppingButton.text = getString(R.string.recipes_detail_add_selected_to_shopping)
        renderIngredients()
        Toast.makeText(requireContext(), R.string.recipes_detail_select_ingredients_hint, Toast.LENGTH_SHORT).show()
    }

    private fun addSelectedIngredientsToShoppingList() {
        if (selectedShoppingIndexes.isEmpty()) {
            Toast.makeText(requireContext(), R.string.recipes_detail_no_ingredient_selected, Toast.LENGTH_SHORT).show()
            return
        }
        val selectedLines = selectedShoppingIndexes.sorted().mapNotNull { index ->
            detailIngredients.getOrNull(index)?.toDisplayLine()
        }
        val prefs = UserSessionDataGuard.scopedSharedPreferences(requireContext(), SHOPPING_PREFS)
        val current = prefs.getStringSet(SHOPPING_ITEMS_KEY, emptySet()).orEmpty().toMutableSet()
        current.addAll(selectedLines)
        prefs.edit().putStringSet(SHOPPING_ITEMS_KEY, current).apply()

        shoppingSelectMode = false
        selectedShoppingIndexes.clear()
        binding.bottomShoppingButton.text = getString(R.string.recipes_detail_add_to_shopping)
        renderIngredients()
        Toast.makeText(requireContext(), getString(R.string.recipes_detail_added_to_shopping, selectedLines.size), Toast.LENGTH_LONG).show()
    }

    private fun saveRecipeToBook(useEditedVersion: Boolean) {
        val recipe = currentRecipe ?: return
        val editedLines: List<String>? = if (useEditedVersion) detailIngredients.map { it.toDisplayLine() } else null

        viewLifecycleOwner.lifecycleScope.launch {
            val copiedRecipeId = withContext(Dispatchers.IO) {
                repository.copyRecipeToUserBook(recipe.id, editedLines)
            }
            if (_binding == null) return@launch

            val messageRes = if (copiedRecipeId != null) {
                R.string.recipes_detail_saved_to_book
            } else {
                R.string.recipes_detail_save_failed
            }
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            if (copiedRecipeId != null && useEditedVersion && editMode) {
                editMode = false
                editModeSnapshot = emptyList()
                binding.ingredientEditToggleButton.text = getString(R.string.recipes_detail_edit_for_me_short)
                binding.addIngredientButton.isVisible = false
                binding.saveEditedRecipeButton.isVisible = false
                renderIngredients()
            }
        }
    }

    private fun String.splitAmountAndUnit(): Pair<String, String> {
        val parts = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return "" to ""
        return if (parts.first().looksLikeAmount()) {
            parts.first() to parts.drop(1).joinToString(" ")
        } else {
            "" to parts.joinToString(" ")
        }
    }

    private fun DetailIngredient.toDisplayLine(): String {
        return listOf(amountUnit, name)
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() && value != "—" }
            .joinToString(" ")
            .ifBlank { name }
    }

    private fun String.toDetailIngredient(index: Int): DetailIngredient {
        val raw = trim()
        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        val amountParts = mutableListOf<String>()
        var nameStart = 0
        if (tokens.isNotEmpty() && tokens.first().looksLikeAmount()) {
            amountParts.add(tokens.first())
            nameStart = 1
            while (nameStart < tokens.size && tokens[nameStart].lowercase(TURKISH_LOCALE) in UNIT_WORDS && amountParts.size < 5) {
                amountParts.add(tokens[nameStart])
                nameStart++
            }
        }
        val name = tokens.drop(nameStart).joinToString(" ").ifBlank { raw }
        val amount = amountParts.joinToString(" ")
        val displayName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(TURKISH_LOCALE) else it.toString() }
        return DetailIngredient(
            id = "ingredient_${index}_${raw.hashCode()}",
            icon = iconForIngredient(displayName),
            name = displayName,
            amountUnit = amount,
            estimatedCalories = estimateCalories(displayName, amount)
        )
    }

    private fun String.looksLikeAmount(): Boolean {
        return any { it.isDigit() } || contains("/")
    }

    private fun iconForIngredient(name: String): String {
        val normalized = name.lowercase(TURKISH_LOCALE)
        return when {
            "lahana" in normalized || "marul" in normalized || "ıspanak" in normalized -> "🥬"
            "havuç" in normalized -> "🥕"
            "patates" in normalized -> "🥔"
            "soğan" in normalized -> "🧅"
            "domates" in normalized -> "🍅"
            "biber" in normalized -> "🫑"
            "tavuk" in normalized -> "🍗"
            "et" in normalized || "kıyma" in normalized -> "🥩"
            "balık" in normalized -> "🐟"
            "yumurta" in normalized -> "🥚"
            "süt" in normalized || "yoğurt" in normalized || "peynir" in normalized -> "🥛"
            "zeytinya" in normalized || "yağ" in normalized -> "🫒"
            "muz" in normalized -> "🍌"
            "elma" in normalized -> "🍎"
            "mercimek" in normalized || "nohut" in normalized || "fasulye" in normalized -> "🫘"
            "un" in normalized || "yulaf" in normalized || "pirinç" in normalized || "bulgur" in normalized -> "🌾"
            else -> "🥄"
        }
    }

    private fun estimateCalories(name: String, amountUnit: String): Int {
        val normalizedName = name.lowercase(TURKISH_LOCALE)
        val normalizedAmount = amountUnit.lowercase(TURKISH_LOCALE)
        val amount = normalizedAmount.extractAmountValue()
        val kcalPer100 = kcalPer100Gram(normalizedName)
        val grams = approximateGrams(normalizedName, normalizedAmount, amount)

        if (grams != null) {
            return ((grams / 100.0) * kcalPer100).roundToInt().coerceAtLeast(0)
        }

        return fallbackIngredientCalories(normalizedName)
    }

    private fun String.extractAmountValue(): Double? {
        val token = split(Regex("\\s+")).firstOrNull { part -> part.any { char -> char.isDigit() } } ?: return null
        val normalized = token.replace(',', '.')
        if ("/" in normalized) {
            val parts = normalized.split("/")
            val numerator = parts.getOrNull(0)?.toDoubleOrNull()
            val denominator = parts.getOrNull(1)?.toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) return numerator / denominator
        }
        return normalized.toDoubleOrNull()
    }

    private fun approximateGrams(name: String, amountUnit: String, amount: Double?): Double? {
        if (amount == null) return null
        return when {
            "kg" in amountUnit -> amount * 1000.0
            "g" in amountUnit || "gr" in amountUnit || "gram" in amountUnit -> amount
            "lt" in amountUnit || "litre" in amountUnit -> amount * 1000.0
            "ml" in amountUnit -> amount
            "su barda" in amountUnit -> amount * 200.0
            "çay barda" in amountUnit -> amount * 100.0
            "yemek kaşı" in amountUnit -> amount * 15.0
            "tatlı kaşı" in amountUnit -> amount * 10.0
            "çay kaşı" in amountUnit -> amount * 5.0
            "dilim" in amountUnit -> amount * 30.0
            "demet" in amountUnit -> amount * 50.0
            "diş" in amountUnit -> amount * 3.0
            "adet" in amountUnit -> amount * approximateItemGrams(name)
            else -> null
        }
    }

    private fun approximateItemGrams(name: String): Double {
        return when {
            "yumurta" in name -> 55.0
            "soğan" in name -> 90.0
            "patates" in name -> 150.0
            "havuç" in name -> 70.0
            "domates" in name -> 110.0
            "biber" in name -> 60.0
            "muz" in name -> 120.0
            "elma" in name -> 160.0
            "tavuk but" in name -> 150.0
            "tavuk" in name -> 120.0
            else -> 80.0
        }
    }

    private fun kcalPer100Gram(name: String): Int {
        return when {
            "zeytinya" in name || "sıvı yağ" in name || "tereya" in name || "yağ" in name -> 884
            "şeker" in name || "bal" in name || "pekmez" in name -> 310
            "fıstık" in name || "ceviz" in name || "badem" in name || "fındık" in name -> 600
            "kıyma" in name || "dana" in name || "kuzu" in name || "et" in name -> 250
            "tavuk" in name || "hindi" in name -> 165
            "balık" in name -> 160
            "yumurta" in name -> 143
            "peynir" in name -> 260
            "yoğurt" in name -> 60
            "süt" in name -> 50
            "pirinç" in name || "bulgur" in name || "makarna" in name || "erişte" in name -> 350
            "un" in name || "galeta" in name || "yulaf" in name -> 360
            "nohut" in name || "fasulye" in name || "mercimek" in name -> 165
            "patates" in name -> 77
            "muz" in name -> 89
            "elma" in name -> 52
            "havuç" in name -> 41
            "soğan" in name -> 40
            "domates" in name -> 18
            "biber" in name -> 25
            "lahana" in name || "kabak" in name || "salatalık" in name -> 24
            "tuz" in name || "karabiber" in name || "kimyon" in name || "nane" in name || "pul biber" in name -> 0
            else -> 60
        }
    }

    private fun fallbackIngredientCalories(name: String): Int {
        return when {
            "yağ" in name -> 120
            "fıstık" in name || "ceviz" in name || "badem" in name -> 110
            "tavuk" in name || "et" in name || "kıyma" in name -> 95
            "patates" in name -> 80
            "yumurta" in name -> 70
            "peynir" in name -> 65
            "süt" in name || "yoğurt" in name -> 55
            "pirinç" in name || "bulgur" in name || "makarna" in name || "yulaf" in name -> 90
            "muz" in name -> 90
            "havuç" in name || "soğan" in name || "domates" in name || "biber" in name -> 35
            else -> 40
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.root.keepScreenOn = false
        _binding = null
        super.onDestroyView()
    }

    private enum class DetailTab { OVERVIEW, INGREDIENTS, STEPS }

    private data class DetailIngredient(
        val id: String,
        val icon: String,
        val name: String,
        val amountUnit: String,
        val estimatedCalories: Int
    )

    private data class MacroSummary(
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )

    private data class MacroPercentSummary(
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )

    companion object {
        private const val ARG_RECIPE_ID = "arg_recipe_id"
        private const val ARG_FROM_COMMUNITY = "arg_from_community"
        private const val ARG_COMMUNITY_POST_ID = "arg_community_post_id"
        private const val ARG_COMMUNITY_POST_OWNER_ID = "arg_community_post_owner_id"
        private const val ARG_COMMUNITY_RECIPE_SOURCE_TAG = "arg_community_recipe_source_tag"
        private const val SHOPPING_PREFS = "fitto_recipe_shopping_list"
        private const val SHOPPING_ITEMS_KEY = "items"
        private const val TAG_RECIPE_COOKING = "recipe_cooking_mode"
        private val TURKISH_LOCALE = Locale("tr", "TR")
        private val UNIT_WORDS = setOf(
            "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
            "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
            "avuç", "paket", "kase", "diş"
        )

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
