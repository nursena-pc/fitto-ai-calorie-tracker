package com.example.fitto.feature_reports

internal data class ReportsScreenState(
    val selectedPageIndex: Int = 0
)

internal class ReportsScreenStateStore(
    initialState: ReportsScreenState = ReportsScreenState(),
    private val pageCount: Int = DEFAULT_PAGE_COUNT
) {
    init {
        require(pageCount > 0) { "pageCount must be positive" }
    }

    var state: ReportsScreenState = initialState.copy(
        selectedPageIndex = initialState.selectedPageIndex.coerceIn(0, pageCount - 1)
    )
        private set

    fun selectPage(index: Int): Int {
        val previous = state.selectedPageIndex
        state = state.copy(selectedPageIndex = index.coerceIn(0, pageCount - 1))
        return previous
    }

    private companion object {
        const val DEFAULT_PAGE_COUNT = 3
    }
}
