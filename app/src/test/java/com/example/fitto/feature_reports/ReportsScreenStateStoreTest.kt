package com.example.fitto.feature_reports

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsScreenStateStoreTest {

    @Test
    fun selectPage_returnsPreviousPageAndStoresNewPage() {
        val store = ReportsScreenStateStore(
            initialState = ReportsScreenState(selectedPageIndex = 1)
        )

        val previous = store.selectPage(2)

        assertEquals(1, previous)
        assertEquals(2, store.state.selectedPageIndex)
    }

    @Test
    fun pageIndex_isClampedToAvailablePages() {
        val store = ReportsScreenStateStore(
            initialState = ReportsScreenState(selectedPageIndex = 9),
            pageCount = 3
        )

        assertEquals(2, store.state.selectedPageIndex)
        store.selectPage(-5)
        assertEquals(0, store.state.selectedPageIndex)
    }
}
