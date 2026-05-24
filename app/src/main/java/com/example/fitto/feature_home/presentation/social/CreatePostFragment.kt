package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.FragmentCreatePostBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.io.File
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import android.Manifest
import android.content.pm.PackageManager

class CreatePostFragment : DialogFragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private var selectedPostVisibility: String = POST_VISIBILITY_COMMUNITY
    private lateinit var viewModel: SocialViewModel

    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String? = null
    private var editingDraft: SocialPost? = null
    private var didApplyDraft = false
    private var cameraImageUri: Uri? = null
    private val selectedNutritionChips = mutableSetOf<String>()

    private val editingDraftId: String by lazy {
        arguments?.getString(ARG_DRAFT_ID).orEmpty()
    }

    private val isEditingDraft: Boolean
        get() = editingDraftId.isNotBlank()

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            updateImagePreview()
            updateShareButton()
        }
    }
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.create_post_camera_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            updateImagePreview()
            updateShareButton()
        } else {
            cameraImageUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Fitto_FullScreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCreatePostBinding.bind(view)
        viewModel = ViewModelProvider(this)[SocialViewModel::class.java]

        binding.commentsSwitch.isChecked = ProfilePreferenceStore.areCommentsEnabled(requireContext())

        setupInputs()
        setupClicks()
        observeViewModel()
        setupNutritionDropdowns()
        setupAudienceDropdowns()
        setupNutritionTags()
        configureDraftModeIfNeeded()

        viewModel.loadMyProfile()

        if (isEditingDraft) {
            viewModel.loadDraftForEditing(editingDraftId)
        }
        updateAuthor(null, null)
        updateShareButton()
    }

    private fun configureDraftModeIfNeeded() = with(binding) {
        if (!isEditingDraft) return@with

        createPostTitleText.setText(R.string.create_post_edit_draft_title)
        saveDraftButton.setText(R.string.create_post_update_draft)
        sharePostButton.setText(R.string.create_post_share)
    }

    private fun setupNutritionDropdowns() = with(binding) {
        val popupWidth = (resources.displayMetrics.widthPixels * 0.72f).toInt()
        val mealOptions = listOf(
            PostDropdownOption(getString(R.string.create_post_breakfast), R.drawable.ic_nav_recipes),
            PostDropdownOption(getString(R.string.create_post_lunch), R.drawable.ic_nav_recipes),
            PostDropdownOption(getString(R.string.create_post_dinner), R.drawable.ic_nav_recipes),
            PostDropdownOption(getString(R.string.create_post_snack), R.drawable.ic_nav_recipes)
        )

        mealNameDropdown.setAdapter(
            PostDropdownOptionAdapter(requireContext(), mealOptions)
        )
        mealNameDropdown.setDropDownBackgroundResource(R.drawable.bg_popup_white)
        mealNameDropdown.setText("", false)
        mealNameDropdown.dropDownWidth = popupWidth

        mealNameDropdown.setOnItemClickListener { _, _, position, _ ->
            val selected = mealOptions[position]
            mealNameDropdown.setText(selected.title, false)
            mealNameInputLayout.setStartIconDrawable(selected.iconResId)
        }

        val timeOptions = listOf(
            PostDropdownOption(getString(R.string.create_post_morning), R.drawable.ic_schedule),
            PostDropdownOption(getString(R.string.create_post_noon), R.drawable.ic_schedule),
            PostDropdownOption(getString(R.string.create_post_evening), R.drawable.ic_schedule),
            PostDropdownOption(getString(R.string.create_post_night), R.drawable.ic_schedule)
        )

        mealTimeDropdown.setAdapter(
            PostDropdownOptionAdapter(requireContext(), timeOptions)
        )
        mealTimeDropdown.setDropDownBackgroundResource(R.drawable.bg_popup_white)
        mealTimeDropdown.setText("", false)
        mealTimeDropdown.dropDownWidth = popupWidth

        mealTimeDropdown.setOnItemClickListener { _, _, position, _ ->
            val selected = timeOptions[position]
            mealTimeDropdown.setText(selected.title, false)
            mealTimeInputLayout.setStartIconDrawable(selected.iconResId)
        }
    }
    private fun setupAudienceDropdowns() = with(binding) {
        val audienceOptions = listOf(
            PostDropdownOption(
                getString(R.string.create_post_audience_community),
                R.drawable.ic_group
            ),
            PostDropdownOption(
                getString(R.string.create_post_audience_followers),
                R.drawable.ic_group
            ),
            PostDropdownOption(
                getString(R.string.create_post_audience_private),
                R.drawable.ic_lock
            )
        )

        val adapter = PostDropdownOptionAdapter(requireContext(), audienceOptions)

        shareAudienceDropdown.setAdapter(adapter)
        visibilityDropdown.setAdapter(adapter)

        shareAudienceDropdown.setDropDownBackgroundResource(R.drawable.bg_popup_white)
        visibilityDropdown.setDropDownBackgroundResource(R.drawable.bg_popup_white)

        applyAudienceSelection(audienceOptions.first())

        shareAudienceDropdown.setOnItemClickListener { _, _, position, _ ->
            applyAudienceSelection(audienceOptions[position])
        }

        visibilityDropdown.setOnItemClickListener { _, _, position, _ ->
            applyAudienceSelection(audienceOptions[position])
        }
    }

    private fun applyAudienceSelection(option: PostDropdownOption) = with(binding) {
        selectedPostVisibility = when (option.title) {
            getString(R.string.create_post_audience_followers) -> POST_VISIBILITY_FOLLOWERS
            getString(R.string.create_post_audience_private) -> POST_VISIBILITY_PRIVATE
            else -> POST_VISIBILITY_COMMUNITY
        }

        shareAudienceDropdown.setText(option.title, false)
        visibilityDropdown.setText(option.title, false)

        shareAudienceInputLayout.setStartIconDrawable(option.iconResId)
        visibilityInputLayout.setStartIconDrawable(option.iconResId)
    }
    private fun applyAudienceSelectionByVisibility(visibility: String) = with(binding) {
        val option = when (visibility) {
            POST_VISIBILITY_FOLLOWERS -> PostDropdownOption(
                getString(R.string.create_post_audience_followers),
                R.drawable.ic_group
            )
            POST_VISIBILITY_PRIVATE -> PostDropdownOption(
                getString(R.string.create_post_audience_private),
                R.drawable.ic_lock
            )
            else -> PostDropdownOption(
                getString(R.string.create_post_audience_community),
                R.drawable.ic_group
            )
        }

        applyAudienceSelection(option)
    }

    private fun getSelectedTags(): List<String> {
        return selectedNutritionChips.toList()
    }

    private fun setupNutritionTags() = with(binding) {
        nutritionChipGroup.removeAllViews()

        val defaultTags = listOf(
            Triple("Ev yapımı", R.color.tag_chip_green_bg, R.color.tag_chip_green_text),
            Triple("Akşam öğünü", R.color.tag_chip_yellow_bg, R.color.tag_chip_yellow_text),
            Triple("Dengeli", R.color.tag_chip_blue_bg, R.color.tag_chip_blue_text),
            Triple("Hafif", R.color.tag_chip_purple_bg, R.color.tag_chip_purple_text),
            Triple("Proteinli", R.color.tag_chip_orange_bg, R.color.tag_chip_orange_text),
            Triple("Fit tarif", R.color.tag_chip_green_bg, R.color.tag_chip_green_text)
        )

        defaultTags.forEach { tag ->
            nutritionChipGroup.addView(
                createTagChip(
                    text = tag.first,
                    backgroundColorRes = tag.second,
                    textColorRes = tag.third,
                    removable = false
                )
            )
        }

        nutritionChipGroup.addView(createAddTagChip())
    }
    private fun createTagChip(
        text: String,
        backgroundColorRes: Int,
        textColorRes: Int,
        removable: Boolean
    ): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            isCheckedIconVisible = false
            isCloseIconVisible = removable
            chipMinHeight = 42f
            chipCornerRadius = 18f
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)

            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), backgroundColorRes)
            )
            setTextColor(ContextCompat.getColor(requireContext(), textColorRes))

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedNutritionChips.add(text)
                } else {
                    selectedNutritionChips.remove(text)
                }
            }
            setOnCloseIconClickListener {
                selectedNutritionChips.remove(text)
                binding.nutritionChipGroup.removeView(this)
            }
            if (removable) {
                closeIconTint = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), textColorRes)
                )

                setOnCloseIconClickListener {
                    selectedNutritionChips.remove(text)
                    binding.nutritionChipGroup.removeView(this)
                }
            }
        }
    }

    private fun createAddTagChip(): Chip {
        return Chip(requireContext()).apply {
            text = getString(R.string.create_post_add_tag)
            isCheckable = false
            isCloseIconVisible = false
            chipMinHeight = 42f
            chipCornerRadius = 18f
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)

            chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add)
            isChipIconVisible = true
            chipIconTint = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.fitto_green)
            )

            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.fitto_primary_soft)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_green))

            setOnClickListener {
                showAddTagDialog()
            }
        }
    }
    private fun showAddTagDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_post_tag, null)

        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.addTagInputLayout)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.addTagEditText)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.FittoAlertDialogTheme)
            .setView(dialogView)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.common_add, null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val tagText = editText.text?.toString()?.trim().orEmpty()

                    when {
                        tagText.isBlank() -> {
                            inputLayout.error = getString(R.string.create_post_tag_empty_error)
                        }

                        tagText.length < 2 -> {
                            inputLayout.error = getString(R.string.create_post_tag_short_error)
                        }

                        else -> {
                            inputLayout.error = null
                            addCustomTag(tagText)
                            dialog.dismiss()
                        }
                    }
                }
        }

        dialog.show()
    }
    private fun addCustomTag(tagText: String) {
        val chipGroup = binding.nutritionChipGroup

        val addChipIndex = (0 until chipGroup.childCount).firstOrNull { index ->
            (chipGroup.getChildAt(index) as? Chip)?.text == getString(R.string.create_post_add_tag)
        } ?: chipGroup.childCount

        chipGroup.addView(
            createTagChip(
                text = tagText,
                backgroundColorRes = R.color.tag_chip_blue_bg,
                textColorRes = R.color.tag_chip_blue_text,
                removable = true
            ),
            addChipIndex
        )
    }


    private fun applyDraftToForm(post: SocialPost) = with(binding) {
        if (didApplyDraft) return@with
        didApplyDraft = true
        editingDraft = post
        existingImageUrl = post.imageUrl

        descriptionEditText.setText(post.content)
        noteEditText.setText("")
        caloriesEditText.setText(post.calories?.toString().orEmpty())
        commentsSwitch.isChecked = post.allowComments
        applyAudienceSelectionByVisibility(
            post.visibility.takeIf { it != POST_VISIBILITY_PRIVATE } ?: POST_VISIBILITY_COMMUNITY
        )
        applyMealInfo(post.mealInfo)
        applySelectedTags(post.tags)
        updateImagePreview()
        updateShareButton()
    }

    private fun applyMealInfo(mealInfo: String?) = with(binding) {
        val parts = mealInfo
            ?.split("•")
            ?.map { value -> value.trim() }
            ?.filter { value -> value.isNotBlank() }
            .orEmpty()

        parts.getOrNull(0)?.let { mealName ->
            mealNameDropdown.setText(mealName, false)
        }

        parts.getOrNull(1)?.let { mealTime ->
            mealTimeDropdown.setText(mealTime, false)
        }
    }

    private fun applySelectedTags(tags: List<String>) {
        selectedNutritionChips.clear()

        tags.forEach { tag ->
            ensureTagChipExists(tag)
            selectedNutritionChips.add(tag)
        }

        for (index in 0 until binding.nutritionChipGroup.childCount) {
            val chip = binding.nutritionChipGroup.getChildAt(index) as? Chip ?: continue
            val tagText = chip.text?.toString().orEmpty()
            if (tagText != getString(R.string.create_post_add_tag)) {
                chip.isChecked = selectedNutritionChips.contains(tagText)
            }
        }
    }

    private fun ensureTagChipExists(tag: String) {
        val chipGroup = binding.nutritionChipGroup
        val alreadyExists = (0 until chipGroup.childCount).any { index ->
            (chipGroup.getChildAt(index) as? Chip)?.text?.toString() == tag
        }

        if (!alreadyExists) {
            val addChipIndex = (0 until chipGroup.childCount).firstOrNull { index ->
                (chipGroup.getChildAt(index) as? Chip)?.text == getString(R.string.create_post_add_tag)
            } ?: chipGroup.childCount

            chipGroup.addView(
                createTagChip(
                    text = tag,
                    backgroundColorRes = R.color.tag_chip_blue_bg,
                    textColorRes = R.color.tag_chip_blue_text,
                    removable = true
                ),
                addChipIndex
            )
        }
    }

    private fun setupInputs() {
        binding.descriptionEditText.doAfterTextChanged {
            val length = it?.length ?: 0
            binding.descriptionCounterText.text = "$length/500"
            updateShareButton()
        }

        binding.noteEditText.doAfterTextChanged {
            val length = it?.length ?: 0
            binding.noteCounterText.text = "$length/200"
            updateShareButton()
        }

        binding.commentsSwitch.setOnCheckedChangeListener { _, isChecked ->
            ProfilePreferenceStore.setCommentsEnabled(requireContext(), isChecked)
        }

        binding.caloriesEditText.doAfterTextChanged { editable ->
            val current = editable?.toString().orEmpty()
            val filtered = current.filter { it.isDigit() }.take(4)

            if (filtered != current) {
                binding.caloriesEditText.setText(filtered)
                binding.caloriesEditText.setSelection(filtered.length)
            }
        }
    }

    private fun setupClicks() {
        binding.backButton.setOnClickListener {
            dismiss()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.galleryButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.saveDraftButton.setOnClickListener {
            saveDraft()
        }

        binding.sharePostButton.setOnClickListener {
            sharePost()
        }
    }
    private fun saveDraft() {
        val calories = binding.caloriesEditText.text
            ?.toString()
            ?.trim()
            ?.toIntOrNull()

        ProfilePreferenceStore.setCommentsEnabled(
            context = requireContext(),
            enabled = binding.commentsSwitch.isChecked
        )

        if (isEditingDraft) {
            viewModel.updateDraftPost(
                draftId = editingDraftId,
                content = buildFinalContent(),
                imageUri = selectedImageUri,
                existingImageUrl = existingImageUrl,
                mealInfo = buildMealInfo(),
                calories = calories,
                allowComments = binding.commentsSwitch.isChecked,
                visibility = POST_VISIBILITY_PRIVATE,
                tags = getSelectedTags(),
                publish = false,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.create_post_draft_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
            )
            return
        }

        viewModel.createPost(
            content = buildFinalContent(),
            imageUri = selectedImageUri,
            mealInfo = buildMealInfo(),
            calories = calories,
            allowComments = binding.commentsSwitch.isChecked,
            visibility = POST_VISIBILITY_PRIVATE,
            tags = getSelectedTags(),
            isDraft = true,
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.create_post_draft_saved),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        )
    }
    private fun openCamera() {
        runCatching {
            val uri = createCameraImageUri()
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.create_post_camera_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myProfile.collect { profile ->
                    updateAuthor(
                        name = profile?.userName,
                        photoUrl = profile?.photoUrl
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editingDraft.collect { draft ->
                    if (draft != null && isEditingDraft) {
                        applyDraftToForm(draft)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isUploading.collect { isUploading ->
                    binding.shareProgress.isVisible = isUploading
                    binding.sharePostButton.text = if (isUploading) {
                        ""
                    } else {
                        getString(R.string.create_post_share)
                    }

                    binding.sharePostButton.isEnabled = !isUploading && canShare()
                    binding.saveDraftButton.isEnabled = !isUploading
                }
            }
        }
    }

    private fun updateAuthor(name: String?, photoUrl: String?) {
        val displayName = name
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.create_post_user_placeholder)

        binding.authorNameText.text = displayName
        binding.authorInitialText.text = displayName.firstOrNull()?.uppercase() ?: "F"

        val hasPhoto = !photoUrl.isNullOrBlank()
        binding.authorAvatarImage.isVisible = hasPhoto
        binding.authorInitialText.isVisible = !hasPhoto

        if (hasPhoto) {
            ProfilePhotoLoader.load(
                imageView = binding.authorAvatarImage,
                photoUrl = photoUrl,
                placeholderResId = R.drawable.ic_profile_person,
                errorResId = R.drawable.ic_profile_person
            )
        } else {
            binding.authorAvatarImage.setImageDrawable(null)
        }
    }

    private fun updateImagePreview() {
        val uri = selectedImageUri
        val existingUrl = existingImageUrl
        val hasImage = uri != null || !existingUrl.isNullOrBlank()

        binding.selectedPostImage.isVisible = hasImage
        binding.imagePlaceholder.isVisible = !hasImage

        when {
            uri != null -> binding.selectedPostImage.load(uri)
            !existingUrl.isNullOrBlank() -> binding.selectedPostImage.load(existingUrl)
            else -> binding.selectedPostImage.setImageDrawable(null)
        }
    }

    private fun updateShareButton() {
        binding.sharePostButton.isEnabled = canShare() && !viewModel.isUploading.value
    }

    private fun canShare(): Boolean {
        val content = binding.descriptionEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val note = binding.noteEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        return content.isNotBlank() || note.isNotBlank() || selectedImageUri != null || !existingImageUrl.isNullOrBlank()
    }
    private fun buildFinalContent(): String {
        val content = binding.descriptionEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val note = binding.noteEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()

        return buildString {
            if (content.isNotBlank()) {
                append(content)
            }

            if (note.isNotBlank()) {
                if (isNotBlank()) append("\n\n")
                append(getString(R.string.create_post_note_prefix))
                append(" ")
                append(note)
            }
        }
    }
    private fun sharePost() {
        if (!canShare()) return

        val calories = binding.caloriesEditText.text
            ?.toString()
            ?.trim()
            ?.toIntOrNull()

        ProfilePreferenceStore.setCommentsEnabled(
            context = requireContext(),
            enabled = binding.commentsSwitch.isChecked
        )

        if (isEditingDraft) {
            viewModel.updateDraftPost(
                draftId = editingDraftId,
                content = buildFinalContent(),
                imageUri = selectedImageUri,
                existingImageUrl = existingImageUrl,
                mealInfo = buildMealInfo(),
                calories = calories,
                allowComments = binding.commentsSwitch.isChecked,
                visibility = selectedPostVisibility,
                tags = getSelectedTags(),
                publish = true,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.create_post_draft_published),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
            )
            return
        }

        viewModel.createPost(
            content = buildFinalContent(),
            imageUri = selectedImageUri,
            mealInfo = buildMealInfo(),
            calories = calories,
            allowComments = binding.commentsSwitch.isChecked,
            visibility = selectedPostVisibility,
            tags = getSelectedTags(),
            isDraft = false,
            onSuccess = {
                dismiss()
            }
        )
    }

    private fun buildMealInfo(): String? {
        val mealName = binding.mealNameDropdown.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val mealTime = binding.mealTimeDropdown.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val values = mutableListOf<String>()

        val selectMealText = getString(R.string.create_post_select_meal)
        val selectTimeText = getString(R.string.create_post_select_time)

        if (mealName.isNotBlank() && mealName != selectMealText) {
            values.add(mealName)
        }

        if (mealTime.isNotBlank() && mealTime != selectTimeText) {
            values.add(mealTime)
        }

        return values.joinToString(" • ").takeIf { it.isNotBlank() }
    }

    private fun createCameraImageUri(): Uri {
        val imagesDir = File(requireContext().cacheDir, "images").apply {
            mkdirs()
        }

        val imageFile = File.createTempFile(
            "fitto_post_${System.currentTimeMillis()}_",
            ".jpg",
            imagesDir
        )

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }


    override fun onDestroyView() {
        binding.createPostScroll.clearFocus()
        viewModel.clearEditingDraft()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_DRAFT_ID = "draft_id"

        fun newDraftInstance(draftId: String): CreatePostFragment {
            return CreatePostFragment().apply {
                arguments = bundleOf(ARG_DRAFT_ID to draftId)
            }
        }
    }
}
data class PostDropdownOption(
    val title: String,
    val iconResId: Int
)
class PostDropdownOptionAdapter(
    context: Context,
    private val items: List<PostDropdownOption>
) : ArrayAdapter<PostDropdownOption>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_post_dropdown_option, parent, false)

        val item = items[position]

        view.findViewById<ImageView>(R.id.dropdownOptionIcon)
            .setImageResource(item.iconResId)

        view.findViewById<TextView>(R.id.dropdownOptionText)
            .text = item.title

        return view
    }

    override fun getItem(position: Int): PostDropdownOption? = items[position]
}
