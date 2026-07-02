package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialPost

internal data class ProfilePhotoAlbumLabels(
    val profilePhotoSource: String,
    val postPhotoSource: String,
    val undated: String
)

internal class ProfilePhotoAlbumItemMapper(
    private val dateFormatter: ProfilePhotoAlbumDateFormatter
) {

    fun map(
        profilePhotoUrl: String?,
        posts: List<SocialPost>,
        labels: ProfilePhotoAlbumLabels
    ): List<ProfilePhotoAlbumItem> {
        val items = buildList {
            profilePhotoUrl
                ?.takeIf(String::isNotBlank)
                ?.let { photoUrl ->
                    add(
                        ProfilePhotoAlbumItem(
                            id = PROFILE_PHOTO_ID,
                            imageUrl = photoUrl,
                            sourceTitle = labels.profilePhotoSource,
                            dateKey = ProfilePhotoAlbumFilter.UNDATED,
                            dateText = labels.undated,
                            createdAt = 0L
                        )
                    )
                }

            posts
                .asSequence()
                .filter { post -> !post.imageUrl.isNullOrBlank() }
                .forEach { post ->
                    val createdAt = post.createdAt.takeIf { it > 0L } ?: 0L
                    add(
                        ProfilePhotoAlbumItem(
                            id = post.id,
                            imageUrl = post.imageUrl.orEmpty(),
                            sourceTitle = post.mealInfo?.takeIf(String::isNotBlank)
                                ?: labels.postPhotoSource,
                            dateKey = dateFormatter.dateKeyFor(createdAt),
                            dateText = dateFormatter.dateTextFor(createdAt, labels.undated),
                            createdAt = createdAt
                        )
                    )
                }
        }

        return items.sortedWith(
            compareByDescending<ProfilePhotoAlbumItem> { item -> item.createdAt }
                .thenBy { item -> item.sourceTitle }
        )
    }

    private companion object {
        const val PROFILE_PHOTO_ID = "profile_photo"
    }
}
