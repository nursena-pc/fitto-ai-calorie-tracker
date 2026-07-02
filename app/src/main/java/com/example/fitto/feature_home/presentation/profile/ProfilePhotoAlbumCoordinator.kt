package com.example.fitto.feature_home.presentation.profile

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.social.SocialPost
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class ProfilePhotoAlbumCoordinator(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val scope: CoroutineScope,
    private val posts: Flow<List<SocialPost>>,
    private val contentView: View,
    private val recyclerView: RecyclerView,
    private val emptyTextView: TextView,
    private val dateFilterHintTextView: TextView,
    private val dateFilterButton: MaterialButton,
    private val dateClearButton: MaterialButton,
    initialSelectedDateKey: String,
    private val onSelectedDateKeyChanged: (String) -> Unit,
    private val requestDocumentSave: (item: ProfilePhotoAlbumItem, fileName: String) -> Unit,
    private val showMessage: (Int) -> Unit,
    private val dateFormatter: ProfilePhotoAlbumDateFormatter = ProfilePhotoAlbumDateFormatter(),
    private val stateResolver: ProfilePhotoAlbumStateResolver = ProfilePhotoAlbumStateResolver(),
    private val downloadPolicy: ProfilePhotoAlbumDownloadPolicy = ProfilePhotoAlbumDownloadPolicy(),
    private val downloadAction: ProfilePhotoAlbumDownloadAction = ProfilePhotoAlbumDownloadAction(context)
) {
    private val labels = ProfilePhotoAlbumLabels(
        profilePhotoSource = context.getString(R.string.profile_album_source_profile_photo),
        postPhotoSource = context.getString(R.string.profile_album_source_post_photo),
        undated = context.getString(R.string.profile_album_undated)
    )
    private val itemMapper = ProfilePhotoAlbumItemMapper(dateFormatter)
    private val adapter = ProfilePhotoAlbumAdapter(::showPhotoPreviewDialog)

    private var postsJob: Job? = null
    private var previewDialog: Dialog? = null
    private var latestPosts: List<SocialPost> = emptyList()
    private var profilePhotoUrl: String? = null
    private var allItems: List<ProfilePhotoAlbumItem> = emptyList()
    private var selectedDateKey: String = initialSelectedDateKey
    private var isCleared: Boolean = false

    fun start() {
        if (postsJob != null) return

        isCleared = false
        contentView.isVisible = true
        recyclerView.layoutManager = GridLayoutManager(context, PHOTO_GRID_SPAN_COUNT)
        recyclerView.adapter = adapter
        dateFilterButton.isVisible = true
        dateFilterButton.setOnClickListener { showDatePicker() }
        dateClearButton.setOnClickListener {
            selectDateKey(ProfilePhotoAlbumFilter.ALL)
        }

        remapItems()
        bind()

        postsJob = scope.launch {
            posts.collect { newPosts ->
                latestPosts = newPosts
                remapItems()
                bind()
            }
        }
    }

    fun updateProfilePhoto(photoUrl: String?) {
        if (profilePhotoUrl == photoUrl) return

        profilePhotoUrl = photoUrl
        remapItems()
        bind()
    }

    fun copyToDocument(
        item: ProfilePhotoAlbumItem,
        targetUri: Uri
    ) {
        if (isCleared) return

        scope.launch {
            try {
                downloadAction.copyToDocument(item, targetUri)
                if (!isCleared) showMessage(R.string.profile_album_download_success)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Log.e(TAG, "Photo export failed", error)
                if (!isCleared) showMessage(R.string.profile_album_download_error)
            }
        }
    }

    fun clear() {
        isCleared = true
        postsJob?.cancel()
        postsJob = null
        dateFilterButton.setOnClickListener(null)
        dateClearButton.setOnClickListener(null)
        recyclerView.adapter = null

        previewDialog?.dismiss()
        previewDialog = null

        (fragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<*>)
            ?.dismissAllowingStateLoss()
    }


    private fun selectDateKey(dateKey: String) {
        selectedDateKey = dateKey
        onSelectedDateKeyChanged(dateKey)
        bind()
    }

    private fun remapItems() {
        allItems = itemMapper.map(
            profilePhotoUrl = profilePhotoUrl,
            posts = latestPosts,
            labels = labels
        )
    }

    private fun bind() {
        if (isCleared) return

        val state = stateResolver.resolve(
            allItems = allItems,
            selectedDateKey = selectedDateKey
        )

        adapter.submitList(state.visibleItems)
        emptyTextView.isVisible = state.showEmptyState
        recyclerView.isVisible = state.showPhotoGrid
        bindDateFilter(state.hasActiveDateFilter)
    }

    private fun bindDateFilter(hasActiveDateFilter: Boolean) {
        dateFilterButton.isVisible = true
        dateFilterButton.text = ""
        dateClearButton.isVisible = hasActiveDateFilter
        dateFilterHintTextView.isVisible = hasActiveDateFilter

        dateFilterHintTextView.text = if (hasActiveDateFilter) {
            context.getString(
                R.string.profile_album_filter_date_selected,
                dateFormatter.dateTextForDateKey(
                    dateKey = selectedDateKey,
                    allText = context.getString(R.string.profile_album_filter_all),
                    undatedText = labels.undated
                )
            )
        } else {
            ""
        }
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText(context.getString(R.string.profile_album_date_picker_title))
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)

        dateFormatter.pickerUtcMillisForDateKey(selectedDateKey)?.let { selectedMillis ->
            builder.setSelection(selectedMillis)
        }

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selectedUtcMillis ->
            selectDateKey(dateFormatter.dateKeyForPickerSelection(selectedUtcMillis))
        }
        picker.show(fragmentManager, DATE_PICKER_TAG)
    }

    private fun showPhotoPreviewDialog(item: ProfilePhotoAlbumItem) {
        previewDialog?.dismiss()

        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_profile_photo_preview, null, false)

        dialogView.findViewById<ImageView>(R.id.imageProfilePhotoPreview).load(item.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_create_post_image_placeholder)
            error(R.drawable.bg_create_post_image_placeholder)
        }
        dialogView.findViewById<TextView>(R.id.textProfilePhotoPreviewTitle).text = item.sourceTitle
        dialogView.findViewById<TextView>(R.id.textProfilePhotoPreviewDate).text = item.dateText

        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogView)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnDismissListener {
                if (previewDialog === this) previewDialog = null
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.buttonProfilePhotoPreviewClose)
            .setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<MaterialButton>(R.id.buttonProfilePhotoPreviewDownload)
            .setOnClickListener {
                downloadPhoto(item)
                dialog.dismiss()
            }

        previewDialog = dialog
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * PREVIEW_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun downloadPhoto(item: ProfilePhotoAlbumItem) {
        when (
            val destination = downloadPolicy.destinationFor(
                item = item,
                sdkInt = Build.VERSION.SDK_INT,
                mediaStoreMinSdk = Build.VERSION_CODES.Q
            )
        ) {
            ProfilePhotoAlbumDownloadDestination.MediaStore -> savePhotoToGallery(item)
            is ProfilePhotoAlbumDownloadDestination.Document -> {
                requestDocumentSave(item, destination.fileName)
            }
        }
    }

    private fun savePhotoToGallery(item: ProfilePhotoAlbumItem) {
        scope.launch {
            try {
                downloadAction.saveToGallery(item)
                if (!isCleared) showMessage(R.string.profile_album_download_success)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Log.e(TAG, "Photo download failed", error)
                if (!isCleared) showMessage(R.string.profile_album_download_error)
            }
        }
    }

    private companion object {
        const val TAG = "ProfilePhotoAlbum"
        const val DATE_PICKER_TAG = "profile_album_date_picker"
        const val PHOTO_GRID_SPAN_COUNT = 2
        const val PREVIEW_WIDTH_RATIO = 0.92f
    }
}
