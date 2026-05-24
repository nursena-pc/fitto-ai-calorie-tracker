package com.example.fitto.feature_auth.register

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.FragmentUsernameSetupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

class UsernameSetupFragment : Fragment(R.layout.fragment_username_setup) {

    private var _binding: FragmentUsernameSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileCacheRepository: ProfileCacheRepository

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var onboardingViewModel: OnboardingViewModel

    private val handler = Handler(Looper.getMainLooper())

    private var isSubmitting = false
    private var isUsernameAvailable = false
    private var lastCheckedUsername = ""

    private val usernameCheckRunnable = Runnable {
        checkUsernameAvailability()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentUsernameSetupBinding.bind(view)
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
        profileCacheRepository = ProfileCacheRepository(requireContext().applicationContext)

        setupUser()
        setupSuggestions()
        setupInput()
        setupClicks()
        updateContinueButton()
    }

    private fun setupUser() = with(binding) {
        val displayName = getDisplayName()
        usernameWelcomeText.text = getString(R.string.username_setup_welcome, displayName)
    }

    private fun setupSuggestions() = with(binding) {
        val base = createBaseUsername()

        val suggestions = listOf(
            base,
            "$base.fit",
            "${base}_fit"
        ).map { normalizeUsername(it).take(20) }
            .distinct()
            .let { list ->
                if (list.size >= 3) {
                    list
                } else {
                    list + listOf(
                        "fitto_user${Random.nextInt(10, 99)}",
                        "fitto.fit${Random.nextInt(10, 99)}"
                    )
                }
            }
            .take(3)

        bindSuggestion(usernameSuggestionOne, suggestions[0])
        bindSuggestion(usernameSuggestionTwo, suggestions[1])
        bindSuggestion(usernameSuggestionThree, suggestions[2])
    }

    private fun bindSuggestion(view: TextView, username: String) {
        view.text = "@$username"
        view.setOnClickListener {
            binding.usernameEditText.setText(username)
            binding.usernameEditText.setSelection(username.length)
        }
    }

    private fun setupInput() = with(binding) {
        usernameEditText.doAfterTextChanged { editable ->
            val current = editable?.toString().orEmpty()
            val normalized = normalizeUsername(current)

            if (current != normalized) {
                usernameEditText.setText(normalized)
                usernameEditText.setSelection(normalized.length)
                return@doAfterTextChanged
            }

            val validationError = validateUsername(normalized)
            usernameInputLayout.error = validationError.takeIf { normalized.isNotBlank() }
            usernameStatusText.isVisible = false
            isUsernameAvailable = false
            lastCheckedUsername = ""
            usernameSuggestionsContainer.isVisible = normalized.isBlank()
            updateContinueButton()

            handler.removeCallbacks(usernameCheckRunnable)

            if (validationError == null) {
                usernameStatusText.text = getString(R.string.username_checking)
                usernameStatusText.setTextColor(requireContext().getColor(R.color.fitto_green))
                usernameStatusText.isVisible = true
                handler.postDelayed(usernameCheckRunnable, USERNAME_CHECK_DELAY_MILLIS)
            }
        }
    }

    private fun setupClicks() = with(binding) {
        usernameContinueButton.setOnClickListener {
            submitUsername()
        }
    }

    private fun checkUsernameAvailability() = with(binding) {
        val username = usernameEditText.text?.toString()?.trim().orEmpty()
        val normalizedUsername = normalizeUsername(username)
        val validationError = validateUsername(normalizedUsername)

        if (validationError != null) {
            usernameInputLayout.error = validationError
            usernameStatusText.isVisible = false
            isUsernameAvailable = false
            updateContinueButton()
            return@with
        }

        firestore.collection(USERNAMES_COLLECTION)
            .document(normalizedUsername)
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener

                val currentUid = auth.currentUser?.uid
                val ownerUid = document.getString("uid")
                val available = !document.exists() || ownerUid == currentUid

                isUsernameAvailable = available
                lastCheckedUsername = normalizedUsername

                usernameStatusText.isVisible = true
                usernameStatusText.text = if (available) {
                    getString(R.string.username_available)
                } else {
                    getString(R.string.username_taken_inline)
                }
                usernameStatusText.setTextColor(
                    requireContext().getColor(
                        if (available) R.color.fitto_green else android.R.color.holo_red_dark
                    )
                )

                updateContinueButton()
            }
            .addOnFailureListener { exception ->
                if (_binding == null) return@addOnFailureListener

                Log.e(TAG, "Username availability check failed", exception)
                isUsernameAvailable = false
                usernameStatusText.isVisible = false
                usernameInputLayout.error = getString(R.string.username_error_unknown)
                updateContinueButton()
            }
    }

    private fun submitUsername() = with(binding) {
        if (isSubmitting) return@with

        val username = normalizeUsername(usernameEditText.text?.toString()?.trim().orEmpty())
        val error = validateUsername(username)

        usernameInputLayout.error = error
        if (error != null) return@with

        if (!isUsernameAvailable || lastCheckedUsername != username) {
            checkUsernameAvailability()
            return@with
        }

        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        if (uid == null) {
            usernameInputLayout.error = getString(R.string.username_error_unknown)
            return@with
        }

        setLoading(true)

        val state = onboardingViewModel.state
        val now = System.currentTimeMillis()
        val email = currentUser.email.orEmpty()
        val displayName = getDisplayName()
        val dailyNeeds = if (onboardingViewModel.canCalculateDailyNeeds()) {
            onboardingViewModel.calculateDailyNeeds()
        } else {
            null
        }

        val usernameRef = firestore.collection(USERNAMES_COLLECTION).document(username)
        val userRef = firestore.collection(USERS_COLLECTION).document(uid)

        val userDocument = buildUserDocument(
            uid = uid,
            email = email,
            username = username,
            displayName = displayName,
            state = state,
            dailyNeeds = dailyNeeds,
            now = now
        )

        firestore.runTransaction { transaction ->
            val usernameSnapshot = transaction.get(usernameRef)
            val ownerUid = usernameSnapshot.getString("uid")

            if (usernameSnapshot.exists() && ownerUid != uid) {
                throw UsernameTakenException()
            }

            transaction.set(
                usernameRef,
                buildUsernameDocument(
                    uid = uid,
                    email = email,
                    username = username,
                    now = now
                ),
                SetOptions.merge()
            )

            transaction.set(
                userRef,
                userDocument,
                SetOptions.merge()
            )

            null
        }.addOnSuccessListener {
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        profileCacheRepository.cacheProfileFromFirebaseMap(
                            userId = uid,
                            emailFallback = email,
                            data = userDocument
                        )
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Profile cached to Room failed", exception)
                }

                if (_binding == null) return@launch

                setLoading(false)

                findNavController().navigate(
                    R.id.action_usernameSetupFragment_to_homeFragment
                )
            }
        }.addOnFailureListener { exception ->
            setLoading(false)
            Log.e(TAG, "Profile creation failed", exception)

            if (exception is UsernameTakenException) {
                usernameInputLayout.error = getString(R.string.username_error_taken)
                usernameStatusText.text = getString(R.string.username_taken_inline)
                usernameStatusText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                usernameStatusText.isVisible = true
                isUsernameAvailable = false
            } else {
                usernameInputLayout.error = getString(R.string.username_error_unknown)
            }

            updateContinueButton()
        }
    }

    private fun buildUsernameDocument(
        uid: String,
        email: String,
        username: String,
        now: Long
    ): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "usernameLowercase" to username.lowercase(Locale.ROOT),
            "updatedAt" to now
        )
    }

    private fun buildUserDocument(
        uid: String,
        email: String,
        username: String,
        displayName: String,
        state: OnboardingState,
        dailyNeeds: DailyNeedsResult?,
        now: Long
    ): Map<String, Any?> {
        val proteinGrams = (state.weight * 1.6).roundToInt().coerceAtLeast(0)
        val fatGrams = dailyNeeds?.let {
            (it.appliedCalories * 0.25 / 9.0).roundToInt().coerceAtLeast(0)
        }
        val carbGrams = dailyNeeds?.let { result ->
            val caloriesAfterProteinAndFat =
                result.appliedCalories - (proteinGrams * 4) - ((fatGrams ?: 0) * 9)

            (caloriesAfterProteinAndFat / 4.0).roundToInt().coerceAtLeast(0)
        }

        return mapOf(
            "uid" to uid,
            "email" to email,
            "realName" to displayName,
            "userName" to username,
            "usernameLowercase" to username.lowercase(Locale.ROOT),
            "bio" to "",
            "photoUrl" to null,
            "followersCount" to 0,
            "followingCount" to 0,
            "profileCompleted" to true,
            "onboardingSaved" to true,

            "name" to state.name.ifBlank { displayName },
            "heightCm" to state.height,
            "weightKg" to state.weight,
            "age" to state.age,
            "gender" to state.gender?.name,

            "goalType" to state.goalType?.name,
            "barriers" to state.barriers.map { it.name },
            "otherBarrierText" to state.otherBarrierText,
            "targetWeightKg" to state.targetWeight.takeIf { it > 0 },
            "eventTimeMillis" to state.eventTimeMillis,
            "activityLevel" to state.activityLevel?.name,
            "onboardingCompleted" to true,
            "onboardingCompletedAt" to now,

            "targets" to dailyNeeds?.let { result ->
                mapOf(
                    "current" to mapOf(
                        "bmr" to result.bmr,
                        "maintenanceCalories" to result.maintenanceCalories,
                        "targetCalories" to result.targetCalories,
                        "recommendedCalories" to result.recommendedCalories,
                        "appliedCalories" to result.appliedCalories,
                        "isUserTargetApplied" to result.isUserTargetApplied,
                        "weeklyWeightChangeKg" to result.weeklyWeightChangeKg,
                        "estimatedWeeks" to result.estimatedWeeks,
                        "requiredWeeklyChangeKg" to result.requiredWeeklyChangeKg,
                        "planStatus" to result.status.name,
                        "macroTargets" to mapOf(
                            "proteinGrams" to proteinGrams,
                            "fatGrams" to fatGrams,
                            "carbGrams" to carbGrams
                        ),
                        "updatedAt" to now
                    )
                )
            },
            "updatedAt" to now
        )
    }

    private fun validateUsername(username: String): String? {
        if (username.length < MIN_USERNAME_LENGTH) {
            return getString(R.string.username_error_too_short)
        }

        if (username.length > MAX_USERNAME_LENGTH) {
            return getString(R.string.username_error_too_long)
        }

        if (!USERNAME_REGEX.matches(username)) {
            return getString(R.string.username_error_invalid_chars)
        }

        return null
    }

    private fun updateContinueButton() = with(binding) {
        val username = normalizeUsername(usernameEditText.text?.toString()?.trim().orEmpty())
        val canContinue = !isSubmitting &&
                isUsernameAvailable &&
                lastCheckedUsername == username &&
                validateUsername(username) == null

        usernameContinueButton.isEnabled = canContinue

        if (canContinue) {
            usernameContinueButton.setTextColor(requireContext().getColor(android.R.color.white))
            usernameContinueButton.backgroundTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.fitto_green))
        } else {
            usernameContinueButton.setTextColor(0xFF9FA09A.toInt())
            usernameContinueButton.backgroundTintList =
                ColorStateList.valueOf(0xFFD6D8CD.toInt())
        }
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        isSubmitting = isLoading

        usernameContinueButton.isEnabled = !isLoading
        usernameContinueButton.alpha = if (isLoading) 0.65f else 1f

        usernameEditText.isEnabled = !isLoading
        usernameSuggestionOne.isEnabled = !isLoading
        usernameSuggestionTwo.isEnabled = !isLoading
        usernameSuggestionThree.isEnabled = !isLoading
    }

    private fun getDisplayName(): String {
        val onboardingName = onboardingViewModel.state.name.trim()

        val source = onboardingName.ifBlank {
            val user = auth.currentUser

            user?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore("@")
                ?: getString(R.string.app_name)
        }

        return source.replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale("tr", "TR"))
            } else {
                char.toString()
            }
        }
    }

    private fun createBaseUsername(): String {
        val emailName = auth.currentUser?.email
            ?.substringBefore("@")
            ?.takeIf { it.isNotBlank() }

        val source = emailName
            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "fitto_user"

        return normalizeUsername(source)
            .ifBlank { "fitto_user" }
            .take(16)
    }

    private fun normalizeUsername(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("\\s+"), ".")
            .replace(Regex("[^a-z0-9._]"), "")
            .replace(Regex("[.]{2,}"), ".")
            .replace(Regex("[_]{2,}"), "_")
            .trim('.', '_')
    }

    override fun onDestroyView() {
        handler.removeCallbacks(usernameCheckRunnable)
        _binding = null
        super.onDestroyView()
    }

    private class UsernameTakenException : Exception()

    companion object {
        private const val TAG = "UsernameSetupFragment"
        private const val USERS_COLLECTION = "users"
        private const val USERNAMES_COLLECTION = "usernames"
        private const val USERNAME_CHECK_DELAY_MILLIS = 450L
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 20

        private val USERNAME_REGEX = Regex("^[a-z0-9._]+$")
    }
}