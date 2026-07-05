package com.example.fitto.feature_recipes

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.databinding.FragmentFridgeInventoryBinding
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_recipes.util.RecipeImageLoader
import com.example.fitto.fittoAppContainer
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Locale

class FridgeInventoryFragment : Fragment(R.layout.fragment_fridge_inventory) {

    private var _binding: FragmentFridgeInventoryBinding? = null
    private val binding: FragmentFridgeInventoryBinding
        get() = _binding!!

    private lateinit var inventoryController: FridgeInventoryController
    private lateinit var inventoryStore: FridgeInventoryStore
    private lateinit var inventoryCoordinator: FridgeInventoryCoordinator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFridgeInventoryBinding.bind(view)
        val dependencies = requireContext().fittoAppContainer.createRecipeFeatureDependencies()
        inventoryStore = dependencies.fridgeInventoryStore
        val storedIngredients = inventoryStore.ingredients()
        inventoryController = FridgeInventoryController(storedIngredients)
        inventoryCoordinator = dependencies.createFridgeCoordinator()
        if (storedIngredients.isEmpty()) {
            persistIngredients()
        }

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.fridgeAddIngredientButton.setOnClickListener { showIngredientDialog(null) }
        binding.fridgeFindRecipesButton.setOnClickListener { renderSuggestions(searchNow = true) }
        binding.ingredientSearchEditText.doAfterTextChanged { editable ->
            inventoryController.updateSearchQuery(editable?.toString().orEmpty())
            renderIngredients()
        }

        renderIngredients()
        renderSuggestions(searchNow = false)
    }

    private fun showIngredientDialog(existing: FridgeIngredient?) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp, 14.dp, 22.dp, 2.dp)
        }

        val headerCard = MaterialCardView(context).apply {
            radius = 22.dp.toFloat()
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_primary_soft))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14.dp }
        }

        headerCard.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp, 12.dp, 14.dp, 12.dp)

            addView(TextView(context).apply {
                text = "🥗"
                gravity = Gravity.CENTER
                textSize = 24f
                background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_circle_white)
                layoutParams = LinearLayout.LayoutParams(44.dp, 44.dp).apply { marginEnd = 12.dp }
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text = if (existing == null) getString(R.string.fridge_add_ingredient) else getString(R.string.fridge_edit_ingredient)
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
                })

                addView(TextView(context).apply {
                    text = getString(R.string.fridge_dialog_subtitle)
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 3.dp }
                })
            })
        })

        val nameInput = createIngredientInput(R.string.fridge_ingredient_name_hint, existing?.name.orEmpty())
        val amountInput = createIngredientInput(R.string.fridge_ingredient_amount_hint, existing?.amount.orEmpty())
        val unitInput = createIngredientInput(R.string.fridge_ingredient_unit_hint, existing?.unit.orEmpty())

        container.addView(headerCard)
        container.addView(nameInput.first)
        container.addView(amountInput.first)
        container.addView(unitInput.first)

        val dialog = MaterialAlertDialogBuilder(context, R.style.FittoAlertDialogTheme)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.second.text?.toString().orEmpty().trim()
                if (name.isBlank()) {
                    Toast.makeText(context, R.string.fridge_ingredient_name_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                inventoryController.upsertIngredient(
                    existingId = existing?.id,
                    input = FridgeIngredientInput(
                        name = name,
                        amount = amountInput.second.text?.toString().orEmpty(),
                        unit = unitInput.second.text?.toString().orEmpty()
                    )
                ) ?: return@setPositiveButton
                persistIngredients()
                renderIngredients()
                renderSuggestions(searchNow = false)
            }
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
            val green = ContextCompat.getColor(context, R.color.fitto_green)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(green)
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(green)
        }
        dialog.show()
    }

    private fun createIngredientInput(hintRes: Int, value: String): Pair<TextInputLayout, TextInputEditText> {
        val context = requireContext()
        val input = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = ContextCompat.getColor(context, R.color.fitto_surface)
            boxStrokeColor = ContextCompat.getColor(context, R.color.fitto_primary_soft)
            setBoxStrokeColorStateList(fittoInputStrokeColors(context))
            hintTextColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            hint = getString(hintRes)
            setBoxCornerRadii(16.dp.toFloat(), 16.dp.toFloat(), 16.dp.toFloat(), 16.dp.toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10.dp }
        }
        val edit = TextInputEditText(context).apply {
            setText(value)
            setSingleLine(true)
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            setHintTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
        }
        input.addView(edit, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        return input to edit
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

    private fun renderIngredients() = with(binding) {
        fridgeIngredientGrid.removeAllViews()
        val visibleIngredients = inventoryController.visibleIngredients()
        fridgeEmptyText.isVisible = visibleIngredients.isEmpty()
        visibleIngredients.forEach { ingredient ->
            fridgeIngredientGrid.addView(createIngredientCard(ingredient))
        }
    }

    private fun createIngredientCard(ingredient: FridgeIngredient): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            radius = 18.dp.toFloat()
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_surface))
            strokeColor = ContextCompat.getColor(context, R.color.fitto_divider_soft)
            strokeWidth = 1.dp
            isClickable = true
            isFocusable = true
            setOnClickListener { showIngredientDialog(ingredient) }
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4.dp, 4.dp, 4.dp, 8.dp)
            }
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8.dp, 10.dp, 8.dp, 8.dp)
        }
        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = TextView(context).apply {
            text = ingredient.name.ingredientEmoji()
            textSize = 28f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val remove = ImageButton(context).apply {
            background = null
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            layoutParams = LinearLayout.LayoutParams(34.dp, 34.dp)
            setOnClickListener {
                inventoryController.removeIngredient(ingredient.id)
                persistIngredients()
                renderIngredients()
            }
        }
        top.addView(icon)
        top.addView(remove)
        content.addView(top)
        content.addView(TextView(context).apply {
            text = ingredient.name
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
        })
        content.addView(TextView(context).apply {
            text = listOf(ingredient.amount, ingredient.unit).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "—" }
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
            background = ContextCompat.getDrawable(context, R.drawable.bg_recipe_amount_pill)
            setPadding(8.dp, 6.dp, 8.dp, 6.dp)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8.dp
            }
        })
        card.addView(content)
        return card
    }

    private fun renderSuggestions(searchNow: Boolean) = with(binding) {
        fridgeSuggestionsContainer.removeAllViews()
        fridgeSuggestionsScroll.isVisible = false
        fridgeSuggestionEmptyText.isVisible = true
        fridgeSuggestionEmptyText.text = getString(if (searchNow) R.string.fridge_suggestions_loading else R.string.fridge_suggestions_empty)
        if (!searchNow) return@with

        viewLifecycleOwner.lifecycleScope.launch {
            val matches = inventoryCoordinator.findRecipeSuggestions(inventoryController.ingredients)
            if (_binding == null) return@launch
            fridgeSuggestionsContainer.removeAllViews()
            if (matches.isEmpty()) {
                fridgeSuggestionsScroll.isVisible = false
                fridgeSuggestionEmptyText.isVisible = true
                fridgeSuggestionEmptyText.text = getString(R.string.fridge_suggestions_no_match)
            } else {
                fridgeSuggestionEmptyText.isVisible = false
                fridgeSuggestionsScroll.isVisible = true
                matches.forEach { recipe -> fridgeSuggestionsContainer.addView(createSuggestionCard(recipe)) }
            }
        }
    }

    private fun createSuggestionCard(recipe: RecipeUiModel): View {
        val context = requireContext()
        return MaterialCardView(context).apply {
            radius = 18.dp.toFloat()
            cardElevation = 0f
            strokeWidth = 1.dp
            strokeColor = ContextCompat.getColor(context, R.color.fitto_divider_soft)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_surface))
            isClickable = true
            isFocusable = true
            setOnClickListener { openRecipeDetail(recipe) }
            layoutParams = LinearLayout.LayoutParams(168.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 10.dp }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(10.dp, 10.dp, 10.dp, 12.dp)
                addView(ImageView(context).apply {
                    RecipeImageLoader.load(this, recipe)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 86.dp)
                })
                addView(TextView(context).apply {
                    text = recipe.title
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    maxLines = 2
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8.dp }
                })
                addView(TextView(context).apply {
                    text = listOf(recipe.durationLabel, recipe.difficultyKey).joinToString(" • ")
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 3.dp }
                })
            })
        }
    }

    private fun openRecipeDetail(recipe: RecipeUiModel) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this@FridgeInventoryFragment)
            .add(R.id.homeTabContainer, RecipeDetailFragment.newInstance(recipe.id), TAG_RECIPE_DETAIL)
            .addToBackStack(TAG_RECIPE_DETAIL)
            .commit()
    }

    private fun persistIngredients() {
        inventoryStore.replaceIngredients(inventoryController.ingredients)
    }

    private fun String.ingredientEmoji(): String {
        val normalized = lowercase(TURKISH_LOCALE)
        return when {
            "domates" in normalized -> "🍅"
            "yumurta" in normalized -> "🥚"
            "süt" in normalized -> "🥛"
            "havuç" in normalized -> "🥕"
            "yoğurt" in normalized -> "🥣"
            "patates" in normalized -> "🥔"
            "peynir" in normalized -> "🧀"
            "yağ" in normalized -> "🫒"
            else -> "🧺"
        }
    }


    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


    companion object {
        private const val TAG_RECIPE_DETAIL = "recipe_detail"
        private val TURKISH_LOCALE = Locale("tr", "TR")
    }
}
