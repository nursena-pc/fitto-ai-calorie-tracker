package com.example.fitto.feature_recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeShoppingListControllerTest {

    @Test
    fun initialItems_areTrimmedAndBlankItemsAreRemoved() {
        val controller = RecipeShoppingListController(listOf(" 2 adet Domates ", " "))

        assertEquals(listOf("2 adet Domates"), controller.items)
    }

    @Test
    fun addItem_rejectsBlankAndAddsTrimmedValue() {
        val controller = RecipeShoppingListController()

        assertFalse(controller.addItem("  "))
        assertTrue(controller.addItem(" 1 kg Patates "))
        assertEquals(listOf("1 kg Patates"), controller.items)
    }

    @Test
    fun lines_parseAmountUnitAndName() {
        val controller = RecipeShoppingListController(
            listOf("2 yemek kaşığı Zeytinyağı", "Domates")
        )

        val lines = controller.lines()

        assertEquals("2 yemek kaşığı", lines[0].amountUnit)
        assertEquals("2", lines[0].amount)
        assertEquals("yemek kaşığı", lines[0].unit)
        assertEquals("Zeytinyağı", lines[0].name)
        assertEquals("", lines[1].amountUnit)
        assertEquals("Domates", lines[1].name)
    }

    @Test
    fun updateItem_buildsNormalizedShoppingLine() {
        val controller = RecipeShoppingListController(listOf("Domates"))

        val updated = controller.updateItem(
            index = 0,
            name = " Patates ",
            amount = " 2 ",
            unit = " kg "
        )

        assertTrue(updated)
        assertEquals(listOf("2 kg Patates"), controller.items)
    }

    @Test
    fun updateItem_rejectsBlankNameAndInvalidIndex() {
        val controller = RecipeShoppingListController(listOf("Domates"))

        assertFalse(controller.updateItem(0, " ", "2", "kg"))
        assertFalse(controller.updateItem(5, "Patates", "2", "kg"))
        assertEquals(listOf("Domates"), controller.items)
    }

    @Test
    fun removeRawItem_removesMatchingSourceValue() {
        val controller = RecipeShoppingListController(listOf("Domates", "Patates"))

        assertTrue(controller.removeRawItem("Domates"))
        assertFalse(controller.removeRawItem("Biber"))
        assertEquals(listOf("Patates"), controller.items)
    }

    @Test
    fun exportText_returnsNullForEmptyList() {
        val controller = RecipeShoppingListController()

        assertNull(controller.exportText("Sepetim"))
    }

    @Test
    fun exportText_preservesCurrentItemOrder() {
        val controller = RecipeShoppingListController(listOf("Domates", "2 kg Patates"))

        assertEquals(
            "Sepetim\n• Domates\n• 2 kg Patates\n",
            controller.exportText("Sepetim")
        )
    }
}
