package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.FragmentShoppingListBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding: FragmentShoppingListBinding
        get() = _binding!!

    private val items: MutableList<String> = mutableListOf()
    private lateinit var adapter: ShoppingListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShoppingListBinding.bind(view)

        items.clear()
        items.addAll(loadItems())

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.editButton.isVisible = false
        binding.addItemButton.setOnClickListener { addNewItem() }
        binding.exportButton.setOnClickListener { exportShoppingList() }
        binding.newItemEditText.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                event.action == android.view.KeyEvent.ACTION_UP

            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_SEND ||
                enterPressed
            ) {
                addNewItem()
                true
            } else {
                false
            }
        }

        setupRecycler()
        renderItems()
    }

    private fun setupRecycler() = with(binding) {
        adapter = ShoppingListAdapter(
            onItemClick = { item -> showItemDialog(item.sourceIndex) }
        )

        shoppingRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        shoppingRecyclerView.adapter = adapter
        shoppingRecyclerView.setHasFixedSize(false)
        shoppingRecyclerView.isNestedScrollingEnabled = true

        attachSwipeToDelete(shoppingRecyclerView)
    }

    private fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val context = requireContext()

        val touchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return

                    val removed = adapter.getItem(position)
                    if (removed == null) {
                        adapter.notifyItemChanged(position)
                        return
                    }

                    // Adapter pozisyonunu doğrudan kaynak liste index'i sanmıyoruz.
                    // Liste SharedPreferences'tan sıralı geldiği ve sonradan yeniden kurulduğu için
                    // silinecek satırı raw değer üzerinden eşliyoruz.
                    val sourceIndex = items.indexOfFirst { it == removed.raw }
                    if (sourceIndex != -1) {
                        items.removeAt(sourceIndex)
                        saveItems()
                    }

                    adapter.removeAt(position)

                    // Kalan satırların sourceIndex değerleri değiştiği için swipe animasyonu bittikten
                    // sonra adapter'i yeniden besliyoruz. Böylece tıklama/düzenleme de doğru satıra gider.
                    recyclerView.post { renderItems() }
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val itemView = viewHolder.itemView
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                    // item_shopping_list_row.xml'de kök view zaten shoppingItemCard.
                    // Önceki hesap itemView.top + cardView.top yaptığı için top değeri iki kez
                    // ekleniyor ve kırmızı swipe animasyonu satırın aşağısında kalıyordu.
                    val top = itemView.top.toFloat()
                    val bottom = itemView.bottom.toFloat()
                    val left = itemView.left.toFloat()
                    val right = itemView.right.toFloat()
                    val centerY = ((top + bottom) / 2f).toInt()
                    val cornerRadius = 18.dp.toFloat()
                    val iconSize = 30.dp

                    if (dX < 0) {
                        paint.color = ContextCompat.getColor(context, R.color.reminder_delete)

                        c.drawRoundRect(
                            right + dX,
                            top,
                            right,
                            bottom,
                            cornerRadius,
                            cornerRadius,
                            paint
                        )

                        drawSwipeDeleteIcon(
                            canvas = c,
                            centerX = itemView.right - 44.dp,
                            centerY = centerY,
                            iconSize = iconSize
                        )
                    } else if (dX > 0) {
                        paint.color = ContextCompat.getColor(context, R.color.reminder_delete)

                        c.drawRoundRect(
                            left,
                            top,
                            left + dX,
                            bottom,
                            cornerRadius,
                            cornerRadius,
                            paint
                        )

                        drawSwipeDeleteIcon(
                            canvas = c,
                            centerX = itemView.left + 44.dp,
                            centerY = centerY,
                            iconSize = iconSize
                        )
                    }

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }
        )

        touchHelper.attachToRecyclerView(recyclerView)
    }

    private fun drawSwipeDeleteIcon(
        canvas: Canvas,
        centerX: Int,
        centerY: Int,
        iconSize: Int
    ) {
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_swipe_delete)
            ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)

        icon?.setBounds(
            centerX - iconSize / 2,
            centerY - iconSize / 2,
            centerX + iconSize / 2,
            centerY + iconSize / 2
        )
        icon?.draw(canvas)
    }

    private fun addNewItem() {
        val value = binding.newItemEditText.text?.toString().orEmpty().trim()
        if (value.isBlank()) {
            Toast.makeText(requireContext(), R.string.recipes_shopping_list_add_empty, Toast.LENGTH_SHORT).show()
            return
        }
        items.add(value)
        saveItems()
        binding.newItemEditText.setText("")
        renderItems()
    }

    private fun renderItems() = with(binding) {
        val adapterItems = items.mapIndexed { index, raw ->
            val parsed = parseShoppingLine(raw)
            ShoppingListAdapter.Item(
                sourceIndex = index,
                raw = raw,
                name = parsed.name,
                amountUnit = parsed.amountUnit
            )
        }

        emptyText.isVisible = adapterItems.isEmpty()
        shoppingRecyclerView.isVisible = adapterItems.isNotEmpty()
        adapter.submitItems(adapterItems)
    }

    private fun showItemDialog(index: Int) {
        if (index !in items.indices) return

        val context = requireContext()
        val parsed = parseShoppingLine(items[index])
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp, 20.dp, 22.dp, 14.dp)
            background = ContextCompat.getDrawable(context, R.drawable.bg_dialog_fitto_green)
        }

        container.addView(TextView(context).apply {
            setText(R.string.recipes_shopping_list_edit_item)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14.dp }
        })

        val nameEdit = createDialogInput(R.string.recipes_shopping_list_item_name_hint, parsed.name)
        val amountEdit = createDialogInput(R.string.recipes_shopping_list_item_amount_hint, parsed.amount)
        val unitEdit = createDialogInput(R.string.recipes_shopping_list_item_unit_hint, parsed.unit)

        container.addView(nameEdit.first)
        container.addView(amountEdit.first)
        container.addView(unitEdit.first)

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp }
        }

        val cancelButton = createDialogTextButton(context, getString(R.string.cancel), muted = true)
        val saveButton = createDialogTextButton(context, getString(R.string.save), muted = false)
        buttonRow.addView(cancelButton)
        buttonRow.addView(saveButton)
        container.addView(buttonRow)

        val dialog = AlertDialog.Builder(context)
            .setView(container)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            val name = nameEdit.second.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                Toast.makeText(context, R.string.fridge_ingredient_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountUnit = listOf(
                amountEdit.second.text?.toString().orEmpty().trim(),
                unitEdit.second.text?.toString().orEmpty().trim()
            ).filter { it.isNotBlank() }.joinToString(" ")

            if (index in items.indices) {
                items[index] = listOf(amountUnit, name)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                saveItems()
                renderItems()
            }
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.show()
    }

    private fun createDialogTextButton(context: Context, textValue: String, muted: Boolean): MaterialButton {
        return MaterialButton(context).apply {
            text = textValue
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, if (muted) R.color.fitto_text_secondary else R.color.fitto_green))
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.transparent))
            minWidth = 72.dp
            minHeight = 44.dp
            insetTop = 0
            insetBottom = 0
            elevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                44.dp
            ).apply { marginStart = 6.dp }
        }
    }

    private fun createDialogInput(hintRes: Int, value: String): Pair<TextInputLayout, TextInputEditText> {
        val context = requireContext()
        val input = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = ContextCompat.getColor(context, R.color.profile_screen_background)
            boxStrokeColor = ContextCompat.getColor(context, R.color.fitto_green)
            setBoxStrokeColorStateList(fittoInputStrokeColors(context))
            hintTextColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.fitto_green))
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
        input.addView(
            edit,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
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

    private fun exportShoppingList() {
        val cleanItems = items.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanItems.isEmpty()) {
            Toast.makeText(requireContext(), R.string.recipes_shopping_list_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val title = getString(R.string.recipes_shopping_list_title)
        val text = buildString {
            append(title).append('\n')
            cleanItems.forEach { item -> append("• ").append(item).append('\n') }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.recipes_shopping_list_export)))
    }

    private fun parseShoppingLine(value: String): ShoppingLine {
        val tokens = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ShoppingLine(value, "", "")

        var nameStart = 0
        val amountParts = mutableListOf<String>()

        if (tokens.first().any { it.isDigit() } || tokens.first().contains('/')) {
            amountParts.add(tokens.first())
            nameStart = 1
            while (nameStart < tokens.size &&
                tokens[nameStart].lowercase() in UNIT_WORDS &&
                amountParts.size < 5
            ) {
                amountParts.add(tokens[nameStart])
                nameStart++
            }
        }

        val name = tokens.drop(nameStart).joinToString(" ").ifBlank { value.trim() }
        val amountUnit = amountParts.joinToString(" ")
        val amount = amountParts.firstOrNull().orEmpty()
        val unit = amountParts.drop(1).joinToString(" ")
        return ShoppingLine(name, amountUnit, amount, unit)
    }

    private fun loadItems(): List<String> {
        return UserSessionDataGuard.scopedSharedPreferences(requireContext(), SHOPPING_PREFS)
            .getStringSet(SHOPPING_ITEMS_KEY, emptySet())
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sorted()
    }

    private fun saveItems() {
        val cleanItems = items.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        UserSessionDataGuard.scopedSharedPreferences(requireContext(), SHOPPING_PREFS)
            .edit()
            .putStringSet(SHOPPING_ITEMS_KEY, cleanItems)
            .apply()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.shoppingRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private data class ShoppingLine(
        val name: String,
        val amountUnit: String,
        val amount: String = "",
        val unit: String = ""
    )

    companion object {
        private const val SHOPPING_PREFS = "fitto_recipe_shopping_list"
        private const val SHOPPING_ITEMS_KEY = "items"
        private val UNIT_WORDS = setOf(
            "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
            "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
            "avuç", "paket", "kase", "diş"
        )
    }
}
