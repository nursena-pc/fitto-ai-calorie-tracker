package com.example.fitto.feature_recipes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitto.R
import com.example.fitto.databinding.ItemRecipeHeaderBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeHeaderState
import com.example.fitto.feature_recipes.model.RecipeListMode
import com.google.android.material.chip.Chip

class RecipeHeaderAdapter(
    private val filterCatalog: RecipeFilterCatalog,
    private val onSearchChanged: (String) -> Unit,
    private val onFilterClick: () -> Unit,
    private val onNotificationClick: () -> Unit,
    private val onViewModeClick: () -> Unit,
    private val onCategoryClick: (RecipeFilterOption) -> Unit,
    private val onClearFiltersClick: () -> Unit,
    private val onPantrySearchClick: () -> Unit,
    private val onShoppingListClick: () -> Unit,
    private val onMyRecipesClick: () -> Unit,
    private val onVoiceSearchClick: () -> Unit
) : RecyclerView.Adapter<RecipeHeaderAdapter.HeaderViewHolder>() {

    private var headerState: RecipeHeaderState = RecipeHeaderState()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = HEADER_STABLE_ID

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        return HeaderViewHolder(
            ItemRecipeHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(headerState)
    }

    fun submitState(nextState: RecipeHeaderState) {
        if (headerState == nextState) return
        headerState = nextState
        notifyItemChanged(HEADER_POSITION)
    }

    fun currentState(): RecipeHeaderState = headerState

    inner class HeaderViewHolder(
        private val binding: ItemRecipeHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var suppressSearchCallback: Boolean = false

        init {
            Glide.with(binding.heroMascot)
                .asGif()
                .load(R.drawable.fitto_mascot_anim_bye)
                .into(binding.heroMascot)

            binding.searchEditText.doAfterTextChanged { editable ->
                if (!suppressSearchCallback) {
                    onSearchChanged(editable?.toString().orEmpty())
                }
            }

            binding.filterButton.setOnClickListener { onFilterClick() }
            binding.notificationButton.setOnClickListener { onNotificationClick() }
            binding.viewModeButton.setOnClickListener { onViewModeClick() }
            binding.clearFiltersButton.setOnClickListener { onClearFiltersClick() }
            binding.pantrySearchButton.setOnClickListener { onPantrySearchClick() }
            binding.shoppingListButton.setOnClickListener { onShoppingListClick() }
            binding.myRecipesButton.setOnClickListener { onMyRecipesClick() }
            binding.allCategoryChip.isCheckable = true
            binding.allCategoryChip.isCheckedIconVisible = false
            binding.allCategoryChip.text = decoratedCategoryLabel(
                RecipeFilterOption(
                    key = KEY_ALL_CATEGORIES,
                    label = binding.root.context.getString(R.string.recipes_all_chip)
                )
            )
            binding.allCategoryChip.minHeight = (34 * binding.root.resources.displayMetrics.density).toInt()
            binding.allCategoryChip.chipCornerRadius = 17 * binding.root.resources.displayMetrics.density
            binding.allCategoryChip.setChipBackgroundColorResource(R.color.chip_selector_bg)
            binding.allCategoryChip.setTextColor(
                ContextCompat.getColorStateList(binding.root.context, R.color.chip_selector_text)
            )
            binding.allCategoryChip.chipIcon = null
            binding.allCategoryChip.setOnClickListener {
                onCategoryClick(
                    RecipeFilterOption(
                        key = KEY_ALL_CATEGORIES,
                        label = binding.root.context.getString(R.string.recipes_all_chip)
                    )
                )
            }
        }

        fun bind(state: RecipeHeaderState): Unit = with(binding) {
            val currentText = searchEditText.text?.toString().orEmpty()
            if (currentText != state.searchQuery) {
                suppressSearchCallback = true
                searchEditText.setText(state.searchQuery)
                searchEditText.setSelection(searchEditText.text?.length ?: 0)
                suppressSearchCallback = false
            }

            resultCountText.text = root.context.getString(
                R.string.recipes_result_count,
                state.resultCount
            )

            clearFiltersButton.isVisible = state.filterState.hasActiveFilters()

            notificationBadgeText.isVisible = state.notificationBadgeCount > 0
            notificationBadgeText.text = when {
                state.notificationBadgeCount > 99 -> "99+"
                state.notificationBadgeCount > 0 -> state.notificationBadgeCount.toString()
                else -> ""
            }

            val iconRes: Int
            val descriptionRes: Int
            when (state.listMode) {
                RecipeListMode.GRID -> {
                    iconRes = R.drawable.ic_view_list_24
                    descriptionRes = R.string.recipes_view_list
                }

                RecipeListMode.LIST -> {
                    iconRes = R.drawable.ic_grid_2x2
                    descriptionRes = R.string.recipes_view_grid
                }
            }
            viewModeButton.setIconResource(iconRes)
            viewModeButton.contentDescription = root.context.getString(descriptionRes)

            renderCategoryChips(state.filterState)
        }

        private fun renderCategoryChips(filterState: RecipeFilterState) {
            binding.allCategoryChip.isChecked = filterState.selectedMainCategoryKey == null

            binding.categoryChipGroup.removeAllViews()
            filterCatalog.mainCategories.forEach { option: RecipeFilterOption ->
                val isChecked: Boolean = filterState.selectedMainCategoryKey == option.key
                val chip = createCategoryChip(option = option, checked = isChecked)
                chip.setOnClickListener { onCategoryClick(option) }
                binding.categoryChipGroup.addView(chip)
            }
        }

        private fun createCategoryChip(option: RecipeFilterOption, checked: Boolean): Chip {
            val context = binding.root.context
            val density = context.resources.displayMetrics.density

            return Chip(context).apply {
                text = decoratedCategoryLabel(option)
                isCheckable = true
                isChecked = checked
                isClickable = true
                isFocusable = true
                isCheckedIconVisible = false
                minHeight = (34 * density).toInt()
                chipCornerRadius = 17 * density
                setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
                setChipBackgroundColorResource(R.color.chip_selector_bg)
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_selector_text))
                chipStrokeWidth = 1 * density
                chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.fitto_divider_soft)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * density).toInt() }
            }
        }

        private fun decoratedCategoryLabel(option: RecipeFilterOption): String {
            val icon = when (option.key) {
                KEY_ALL_CATEGORIES -> ""
                RecipeFilterCatalog.KEY_MAIN_MEAL -> "🍽️"
                RecipeFilterCatalog.KEY_SOUP -> "🥣"
                RecipeFilterCatalog.KEY_SALAD -> "🥗"
                RecipeFilterCatalog.KEY_DESSERT -> "🍰"
                RecipeFilterCatalog.KEY_BREAKFAST -> "🍳"
                RecipeFilterCatalog.KEY_SNACK -> "🥨"
                RecipeFilterCatalog.KEY_DRINK -> "🥤"
                RecipeFilterCatalog.KEY_PICKLE -> "🥒"
                RecipeFilterCatalog.KEY_SAUCE -> "🧂"
                else -> ""
            }
            return listOf(icon, option.label).filter { value -> value.isNotBlank() }.joinToString(" ")
        }
    }

    companion object {
        const val KEY_ALL_CATEGORIES = RecipeCategorySelectionKeys.ALL
        private const val HEADER_POSITION = 0
        private const val HEADER_STABLE_ID = Long.MIN_VALUE
    }
}
