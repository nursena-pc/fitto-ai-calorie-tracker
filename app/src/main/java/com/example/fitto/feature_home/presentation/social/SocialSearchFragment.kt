package com.example.fitto.feature_home.presentation.social

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentSocialSearchBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Locale

class SocialSearchFragment : DialogFragment(R.layout.fragment_social_search) {

    private var _binding: FragmentSocialSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SocialViewModel
    private lateinit var userAdapter: SearchUserAdapter

    private var query = ""
    private var latestPosts: List<SocialPost> = emptyList()
    private var latestUsers: List<SearchUserUiModel> = emptyList()
    private var filters = SearchFilters()
    private var recentSearches: List<String> = emptyList()

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

        _binding = FragmentSocialSearchBinding.bind(view)
        viewModel = ViewModelProvider(requireParentFragment())[SocialViewModel::class.java]

        recentSearches = loadRecentSearches()

        setupAdapter()
        setupClicks()
        setupSearchInput()
        renderRecentSearches()
        observePosts()
        updateResults()
    }

    private fun setupAdapter() {
        userAdapter = SearchUserAdapter(
            onUserClick = { user ->
                saveRecentSearch(user.userName)
                SocialProfileFragment
                    .newInstance(user.id)
                    .show(childFragmentManager, "SocialProfileFragment")
            }
        )

        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupClicks() {
        binding.searchBackButton.setOnClickListener { dismiss() }
        binding.searchFilterButton.setOnClickListener { showFiltersDialog() }
        binding.recentSearchesClearText.setOnClickListener {
            clearRecentSearches()
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.doAfterTextChanged { editable ->
            query = editable?.toString()?.trim().orEmpty()
            updateResults()
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                saveRecentSearch(query)
                true
            } else {
                false
            }
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.posts.collect { posts ->
                    latestPosts = posts
                    latestUsers = buildUsersFromPosts(posts)
                    updateResults()
                }
            }
        }
    }

    private fun buildUsersFromPosts(posts: List<SocialPost>): List<SearchUserUiModel> {
        return posts
            .groupBy { post -> post.userId.ifBlank { post.userName.lowercase(Locale.getDefault()) } }
            .mapNotNull { (key, userPosts) ->
                val latestPost = userPosts.maxByOrNull { it.createdAt } ?: return@mapNotNull null
                val userName = latestPost.userName.ifBlank { "fitto_user" }
                val allSearchText = buildString {
                    append(userName)
                    append(' ')
                    userPosts.forEach { post ->
                        append(post.content)
                        append(' ')
                        append(post.mealInfo.orEmpty())
                        append(' ')
                    }
                }

                val category = inferUserCategory(userName, allSearchText)
                val engagement = userPosts.sumOf { it.likesCount + it.commentsCount }
                val verified = inferVerified(userName, category, engagement)
                val bio = when (category) {
                    UserCategory.DIETITIAN -> getString(R.string.search_bio_dietitian)
                    UserCategory.TRAINER -> getString(R.string.search_bio_trainer)
                    UserCategory.NORMAL -> getString(R.string.search_bio_member)
                }

                SearchUserUiModel(
                    id = latestPost.userId.ifBlank { key },
                    userName = userName,
                    bio = bio,
                    photoUrl = latestPost.userPhotoUrl,
                    isVerified = verified,
                    isDietitian = category == UserCategory.DIETITIAN,
                    isTrainer = category == UserCategory.TRAINER,
                    isNewMember = isNewMember(userPosts),
                    isFollowed = userPosts.any { it.likedByMe || it.savedByMe },
                    engagementScore = engagement,
                    postCount = userPosts.size,
                    lastActiveAt = latestPost.createdAt,
                    searchKeywords = allSearchText.lowercase(Locale.getDefault())
                )
            }
            .sortedByDescending { it.engagementScore }
    }

    private fun inferUserCategory(userName: String, searchText: String): UserCategory {
        val normalized = (userName + " " + searchText).lowercase(Locale.getDefault())

        return when {
            normalized.contains("diyet") ||
                    normalized.contains("beslenme") ||
                    normalized.contains("nutrition") -> UserCategory.DIETITIAN

            normalized.contains("fitness") ||
                    normalized.contains("trainer") ||
                    normalized.contains("antren") ||
                    normalized.contains("koç") ||
                    normalized.contains("egitmen") ||
                    normalized.contains("eğitmen") -> UserCategory.TRAINER

            else -> UserCategory.NORMAL
        }
    }

    private fun inferVerified(
        userName: String,
        category: UserCategory,
        engagementScore: Int
    ): Boolean {
        val normalizedUserName = userName.lowercase(Locale.getDefault())
        return category != UserCategory.NORMAL ||
                engagementScore >= 12 ||
                normalizedUserName.contains("fit") ||
                normalizedUserName.contains("official")
    }

    private fun isNewMember(userPosts: List<SocialPost>): Boolean {
        val latestActivity = userPosts.maxOfOrNull { it.createdAt } ?: return false
        val twentyOneDays = 21L * 24L * 60L * 60L * 1000L
        return System.currentTimeMillis() - latestActivity <= twentyOneDays
    }

    private fun updateResults() {
        if (_binding == null) return

        if (query.isBlank()) {
            binding.searchResultsTitleText.isVisible = false
            binding.searchRecyclerView.isVisible = false
            binding.searchStateText.isVisible = true
            binding.searchStateText.text = getString(R.string.search_start)
            userAdapter.submitList(emptyList())
            return
        }

        val normalizedQuery = query.lowercase(Locale.getDefault())

        var results = latestUsers.filter { user ->
            user.userName.contains(query, ignoreCase = true) ||
                    user.searchKeywords.contains(normalizedQuery)
        }

        if (filters.onlyFollowing) {
            results = results.filter { it.isFollowed }
        }
        if (filters.onlyNewMembers) {
            results = results.filter { it.isNewMember }
        }
        if (filters.onlyVerified) {
            results = results.filter { it.isVerified }
        }
        if (filters.onlyDietitians) {
            results = results.filter { it.isDietitian }
        }
        if (filters.onlyTrainers) {
            results = results.filter { it.isTrainer }
        }
        if (filters.onlyPopular) {
            results = results.filter { it.engagementScore > 0 }
                .sortedByDescending { it.engagementScore }
        }
        if (filters.onlyMostInteraction) {
            results = results.sortedWith(
                compareByDescending<SearchUserUiModel> { it.engagementScore }
                    .thenByDescending { it.postCount }
            )
        }

        userAdapter.submitList(results)
        binding.searchResultsTitleText.isVisible = true
        binding.searchRecyclerView.isVisible = results.isNotEmpty()
        binding.searchStateText.isVisible = results.isEmpty()
        if (results.isEmpty()) {
            binding.searchStateText.text = getString(R.string.search_empty)
        }
    }

    private fun renderRecentSearches() {
        if (_binding == null) return

        val hasRecent = recentSearches.isNotEmpty()
        binding.recentSearchesSection.isVisible = hasRecent
        binding.recentSearchesChipGroup.removeAllViews()
        binding.recentSearchesClearText.isVisible = hasRecent

        recentSearches.forEach { searchText ->
            val chip = Chip(requireContext()).apply {
                text = searchText
                isClickable = true
                isCheckable = false
                chipIcon = requireContext().getDrawable(R.drawable.ic_history)
                isChipIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(requireContext().getColor(R.color.fitto_surface))
                setTextColor(requireContext().getColor(R.color.fitto_text_dark))
                setOnClickListener {
                    binding.searchEditText.setText(searchText)
                    binding.searchEditText.setSelection(searchText.length)
                    saveRecentSearch(searchText)
                }
            }
            binding.recentSearchesChipGroup.addView(chip)
        }
    }

    private fun showFiltersDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_search_filters)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        var tempFilters = filters.copy()

        val followingCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterFollowingCheckBox)
        val newMembersCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterNewMembersCheckBox)
        val popularCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterPopularCheckBox)
        val verifiedCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterVerifiedCheckBox)
        val dietitiansCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterDietitiansCheckBox)
        val trainersCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterTrainersCheckBox)
        val interactionCheckBox = dialog.findViewById<MaterialCheckBox>(R.id.filterInteractionCheckBox)

        followingCheckBox.isChecked = tempFilters.onlyFollowing
        newMembersCheckBox.isChecked = tempFilters.onlyNewMembers
        popularCheckBox.isChecked = tempFilters.onlyPopular
        verifiedCheckBox.isChecked = tempFilters.onlyVerified
        dietitiansCheckBox.isChecked = tempFilters.onlyDietitians
        trainersCheckBox.isChecked = tempFilters.onlyTrainers
        interactionCheckBox.isChecked = tempFilters.onlyMostInteraction

        fun syncTempFilters() {
            tempFilters = SearchFilters(
                onlyFollowing = followingCheckBox.isChecked,
                onlyNewMembers = newMembersCheckBox.isChecked,
                onlyPopular = popularCheckBox.isChecked,
                onlyVerified = verifiedCheckBox.isChecked,
                onlyDietitians = dietitiansCheckBox.isChecked,
                onlyTrainers = trainersCheckBox.isChecked,
                onlyMostInteraction = interactionCheckBox.isChecked
            )
        }

        setRowToggle(dialog, R.id.filterFollowingRow, followingCheckBox)
        setRowToggle(dialog, R.id.filterNewMembersRow, newMembersCheckBox)
        setRowToggle(dialog, R.id.filterPopularRow, popularCheckBox)
        setRowToggle(dialog, R.id.filterVerifiedRow, verifiedCheckBox)
        setRowToggle(dialog, R.id.filterDietitiansRow, dietitiansCheckBox)
        setRowToggle(dialog, R.id.filterTrainersRow, trainersCheckBox)
        setRowToggle(dialog, R.id.filterInteractionRow, interactionCheckBox)

        val allCheckBoxes = listOf(
            followingCheckBox,
            newMembersCheckBox,
            popularCheckBox,
            verifiedCheckBox,
            dietitiansCheckBox,
            trainersCheckBox,
            interactionCheckBox
        )

        allCheckBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> syncTempFilters() }
        }

        dialog.findViewById<ImageButton>(R.id.filterDialogCloseButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.filterDialogResetText).setOnClickListener {
            allCheckBoxes.forEach { it.isChecked = false }
            tempFilters = SearchFilters()
        }

        dialog.findViewById<View>(R.id.filterDialogApplyButton).setOnClickListener {
            filters = tempFilters
            updateResults()
            if (query.isNotBlank()) {
                Toast.makeText(requireContext(), getString(R.string.search_filter_applied), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
    }

    private fun setRowToggle(dialog: Dialog, rowId: Int, checkBox: MaterialCheckBox) {
        dialog.findViewById<View>(rowId).setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
        }
    }

    private fun saveRecentSearch(searchText: String) {
        val normalized = searchText.trim()
        if (normalized.isBlank()) return

        val updated = mutableListOf<String>()
        updated.add(normalized)
        recentSearches.forEach { existing ->
            if (!existing.equals(normalized, ignoreCase = true)) {
                updated.add(existing)
            }
        }

        recentSearches = updated.take(MAX_RECENT_SEARCHES)
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENT_SEARCHES, recentSearches.joinToString(SEPARATOR)).apply()
        renderRecentSearches()
    }

    private fun loadRecentSearches(): List<String> {
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_RECENT_SEARCHES, null).orEmpty()
        if (saved.isBlank()) return emptyList()

        return saved.split(SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_RECENT_SEARCHES)
    }

    private fun clearRecentSearches() {
        recentSearches = emptyList()
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
        renderRecentSearches()
    }

    override fun onDestroyView() {
        binding.searchRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private data class SearchFilters(
        val onlyFollowing: Boolean = false,
        val onlyNewMembers: Boolean = false,
        val onlyPopular: Boolean = false,
        val onlyVerified: Boolean = false,
        val onlyDietitians: Boolean = false,
        val onlyTrainers: Boolean = false,
        val onlyMostInteraction: Boolean = false
    )

    private enum class UserCategory {
        NORMAL,
        DIETITIAN,
        TRAINER
    }

    companion object {
        private const val PREFS_SEARCH = "social_search_prefs"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val MAX_RECENT_SEARCHES = 6
        private const val SEPARATOR = "||"
    }
}
