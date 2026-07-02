package com.example.fitto.feature_home.presentation.profile

internal data class ProfilePhotoAlbumListState(
    val visibleItems: List<ProfilePhotoAlbumItem>,
    val hasActiveDateFilter: Boolean,
    val showEmptyState: Boolean
) {
    val showPhotoGrid: Boolean = !showEmptyState
}

internal class ProfilePhotoAlbumStateResolver {

    fun resolve(
        allItems: List<ProfilePhotoAlbumItem>,
        selectedDateKey: String
    ): ProfilePhotoAlbumListState {
        val hasActiveDateFilter = selectedDateKey != ProfilePhotoAlbumFilter.ALL
        val visibleItems = if (hasActiveDateFilter) {
            allItems.filter { item -> item.dateKey == selectedDateKey }
        } else {
            allItems
        }

        return ProfilePhotoAlbumListState(
            visibleItems = visibleItems,
            hasActiveDateFilter = hasActiveDateFilter,
            showEmptyState = visibleItems.isEmpty()
        )
    }
}
