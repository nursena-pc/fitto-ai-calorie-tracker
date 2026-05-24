package com.example.fitto.feature_daily

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.DietPlanSeedRepository
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_dietplans.DietPlanDetailFragment
import com.example.fitto.feature_dietplans.DietPlansFragment
import com.example.fitto.feature_dietplans.IntermittentFastingDetailFragment
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.DietMealPlanCatalog
import com.example.fitto.feature_meal.AddMealFragment
import com.example.fitto.feature_meal.MealDetailFragment
import com.example.fitto.feature_meal.model.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.example.fitto.feature_daily.data.FastingSessionStore
import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.AiDailyExerciseInfo
import com.example.fitto.feature_daily.ai.AiDailyMealInfo
import com.example.fitto.feature_daily.ai.AiDailyPromptBuilder
import com.example.fitto.feature_daily.ai.AiDailyPromptData
import com.example.fitto.feature_daily.ai.AiDailyTriggerType
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_daily.ai.GeminiDailyAiRepository
import com.example.fitto.feature_auth.register.GoalType
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.fitto.feature_exercise.AddExerciseFragment
import com.example.fitto.feature_exercise.data.ExerciseLogStore
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_health.HealthConnectDailySummary
import com.example.fitto.feature_health.HealthConnectManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fitto.feature_health.FittoStepCounterManager
import com.example.fitto.feature_daily.data.WaterTrackingStore
import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import android.app.Dialog
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.example.fitto.feature_sleep.data.SleepLogLocalModel
import com.example.fitto.feature_sleep.data.SleepLogStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import com.example.fitto.feature_meal.CameraFoodEntryFragment
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
class DailyFragment : Fragment(R.layout.fragment_daily) {

    private lateinit var healthConnectManager: HealthConnectManager
    private var healthConnectSummary: HealthConnectDailySummary = HealthConnectDailySummary()
    private var _binding: FragmentDailyBinding? = null
    private val binding get() = _binding!!
    private lateinit var fastingSessionStore: FastingSessionStore
    private lateinit var exerciseLogStore: ExerciseLogStore
    private var selectedDate: LocalDate = LocalDate.now()
    private var fastingTimer: android.os.CountDownTimer? = null
    private var hourglassAnimator: ObjectAnimator? = null
    private var selectedDateIso: String = LocalDate.now().toString()
    private var currentProfileWeightKg: Double? = null
    private lateinit var sleepLogStore: SleepLogStore
    private lateinit var dailyAiRepository: GeminiDailyAiRepository
    private lateinit var dailyAiCache: AiDailyAnalysisCache

    private var currentWeightKg: Double = 70.0
    private var currentWaterMl: Int = 0
    private var currentDailyAiSnapshot: DailyAiSnapshot? = null
    private var dailyAiLoading: Boolean = false
    private var dailyAiRequestInFlightKey: String? = null
    private var pendingDailyAiManualRequest: Boolean = false
    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var dietPlanStore: DietPlanPreferenceStore
    private lateinit var nutritionTargetStore: UserNutritionTargetStore
    private lateinit var mealRepository: MealRepository
    private lateinit var dietPlanSeedRepository: DietPlanSeedRepository
    private lateinit var profileCacheRepository: ProfileCacheRepository
    private lateinit var stepCounterManager: FittoStepCounterManager
    private var localStepCount: Long = 0L
    private lateinit var waterTrackingStore: WaterTrackingStore
    private var waterGoalMl: Int = 2500
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(
                requireContext(),
                getString(R.string.step_counter_permission_granted),
                Toast.LENGTH_SHORT
            ).show()

            startLocalStepCounterIfNeeded()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.step_counter_permission_denied),
                Toast.LENGTH_SHORT
            ).show()

            renderActivityStepsOnly()
        }
    }
    private var currentDailyTargetCalories: Int = DEFAULT_DAILY_CALORIE_TARGET
    private var currentProteinTargetGrams: Int = DEFAULT_PROTEIN_TARGET_GRAMS
    private var currentCarbTargetGrams: Int = DEFAULT_CARB_TARGET_GRAMS
    private var currentFatTargetGrams: Int = DEFAULT_FAT_TARGET_GRAMS

    private var currentActivePlan: ActiveDietPlanUiModel? = null
    private var currentDailyMealPlan: DailyPlannedMealsUiModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDailyBinding.bind(view)

        val appContext = requireContext().applicationContext

        dietPlanStore = DietPlanPreferenceStore(requireContext())
        nutritionTargetStore = UserNutritionTargetStore(requireContext())
        mealRepository = MealRepository(appContext)
        dietPlanSeedRepository = DietPlanSeedRepository(appContext)
        profileCacheRepository = ProfileCacheRepository(appContext)
        customDietPlanStore = CustomDietPlanStore(appContext)
        fastingSessionStore = FastingSessionStore(requireContext())
        exerciseLogStore = ExerciseLogStore(requireContext())
        waterTrackingStore = WaterTrackingStore(appContext)
        healthConnectManager = HealthConnectManager(appContext)
        stepCounterManager = FittoStepCounterManager(appContext)
        sleepLogStore = SleepLogStore(requireContext())
        dailyAiRepository = GeminiDailyAiRepository()
        dailyAiCache = AiDailyAnalysisCache(appContext)

        currentDailyTargetCalories = nutritionTargetStore
            .getDailyTargetCalories()
            .takeIf { it > 0 }
            ?: DEFAULT_DAILY_CALORIE_TARGET

        binding.dateSelectorContainer.setOnClickListener {
            openDatePicker()
        }

        binding.dailySelectedDateText.setOnClickListener {
            openDatePicker()
        }

        setupMockUi()
        setupHealthConnectResultListener()
        setupDietPlanResultListener()
        setupMealAddedResultListener()
        setupMealDetailChangedResultListener()
        setupExerciseAddedResultListener()
        setupClicks()
        setupSleepCard()
        setupDailyAiCard()

        renderSelectedDate()
        renderActivePlan()
        renderWaterTracking()
        renderSleepCard()
        renderSleepWeightSummary()
        startLocalStepCounterIfNeeded()
        loadWaterGoalAndRender()
        loadSleepWeightSummary()
        loadHealthConnectSummary()
        renderWeeklyCalorieProgress()
    }
    private fun currentEntryDate(): String {
        return selectedDate.toString()
    }
    private fun loadSleepWeightSummary() {
        if (!::profileCacheRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            val resolvedWeight = resolveProfileWeightKg(profile)

            currentProfileWeightKg = resolvedWeight
            currentWeightKg = resolvedWeight ?: 70.0

            renderSleepWeightSummary()
            renderSleepCard()
            renderMealTotals()
        }
    }

    private fun setupSleepCard() {
        binding.sleepCard.setOnClickListener {
            showSleepTimeDialog()
        }
    }
    private fun setupDailyAiCard() {
        binding.dailyAiCard.setOnClickListener {
            val snapshot = currentDailyAiSnapshot
            Log.d(
                FITTO_AI_LOG_TAG,
                "Daily AI card clicked. snapshotReady=${snapshot != null}, loading=$dailyAiLoading"
            )

            if (snapshot == null) {
                pendingDailyAiManualRequest = true
                renderDailyAiPreparingState()
                renderMealTotals()
            } else {
                // Kullanıcı dokunduğunda kart sessiz kalmasın; cache/cooldown varsa hemen sonra güncellenir.
                renderDailyAiLoading()
                requestDailyAiAnalysis(snapshot, isAuto = false)
            }
        }
    }

    private fun renderDailyAiDefaultState() = with(binding) {
        applyDailyAiStatusStyle(AiDailyStatusLevel.GREEN)
        dailyAiProgressIndicator.isVisible = false
        dailyAiTitleText.text = getString(R.string.daily_ai_title)
        dailyAiMessageText.text = getString(R.string.daily_ai_default_message)
        dailyAiSubtitleText.text = getString(R.string.daily_ai_default_subtitle)
    }

    private fun renderDailyAiPreparingState() = with(binding) {
        dailyAiProgressIndicator.isVisible = true
        dailyAiTitleText.text = getString(R.string.daily_ai_title)
        dailyAiMessageText.text = getString(R.string.daily_ai_preparing)
        dailyAiSubtitleText.text = getString(R.string.daily_ai_default_subtitle)
    }

    private fun renderDailyAiState(snapshot: DailyAiSnapshot?) {
        if (_binding == null || dailyAiLoading) return

        if (snapshot == null) {
            renderDailyAiDefaultState()
            return
        }

        currentDailyAiSnapshot = snapshot

        if (pendingDailyAiManualRequest) {
            pendingDailyAiManualRequest = false
            requestDailyAiAnalysis(snapshot, isAuto = false)
            return
        }

        val cachedMessage = dailyAiCache.get(snapshot.dateIso, snapshot.triggerType)
        if (cachedMessage != null) {
            val cachedAiMessage = cachedMessage.aiMessage.trim()
            if (isUsableDailyAiCachedMessage(cachedMessage, snapshot)) {
                Log.d(
                    FITTO_AI_LOG_TAG,
                    "Daily AI render cache hit. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, message=${cachedAiMessage.take(80)}"
                )
                renderDailyAiMessage(
                    message = cachedAiMessage,
                    subtitle = getString(R.string.daily_ai_result_subtitle)
                )
                return
            } else {
                Log.w(
                    FITTO_AI_LOG_TAG,
                    "Daily AI stale cache ignored in render. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, source=${cachedMessage.source}, message=${cachedAiMessage.take(80)}"
                )
                dailyAiCache.clear(snapshot.dateIso, snapshot.triggerType)
            }
        }

        val localMessage = localDailyAiMessage(snapshot)
        renderDailyAiMessage(
            message = localMessage,
            subtitle = getString(R.string.daily_ai_default_subtitle)
        )

        if (snapshot.shouldAutoRequestAi() && !dailyAiCache.hasAutoAttempted(snapshot.dateIso, snapshot.triggerType)) {
            requestDailyAiAnalysis(snapshot, isAuto = true)
        }
    }

    private fun renderDailyAiLoading() = with(binding) {
        val status = currentDailyAiSnapshot?.localStatus() ?: AiDailyStatusLevel.GREEN
        applyDailyAiStatusStyle(status)
        dailyAiProgressIndicator.isVisible = true
        dailyAiTitleText.text = getString(R.string.daily_ai_title)
        dailyAiMessageText.text = getString(R.string.daily_ai_loading)
        dailyAiSubtitleText.text = getString(R.string.daily_ai_default_subtitle)
    }

    private fun renderDailyAiMessage(
        message: String,
        subtitle: String = getString(R.string.daily_ai_default_subtitle),
        status: AiDailyStatusLevel = currentDailyAiSnapshot?.localStatus() ?: AiDailyStatusLevel.GREEN
    ) = with(binding) {
        applyDailyAiStatusStyle(status)
        dailyAiProgressIndicator.isVisible = false
        dailyAiTitleText.text = getString(R.string.daily_ai_title)
        dailyAiMessageText.text = message
        dailyAiSubtitleText.text = subtitle
    }

    private fun renderDailyAiStructuredResult(
        analysis: com.example.fitto.feature_daily.ai.AiDailyAnalysisResult,
        snapshot: DailyAiSnapshot,
        subtitle: String = getString(R.string.daily_ai_result_subtitle)
    ) {
        val status = snapshot.localStatus()
        val message = buildDailyAiStructuredMessage(
            mealComments = analysis.mealComments,
            waterComment = analysis.waterComment,
            exerciseComment = analysis.exerciseComment,
            generalComment = analysis.generalComment,
            suggestion = analysis.suggestion,
            plainText = analysis.plainText
        ).ifBlank { localDailyAiMessage(snapshot) }

        renderDailyAiMessage(
            message = message,
            subtitle = subtitle,
            status = status
        )
    }

    private fun buildDailyAiStructuredMessage(
        mealComments: List<com.example.fitto.feature_daily.ai.AiDailyMealComment>,
        waterComment: String,
        exerciseComment: String,
        generalComment: String,
        suggestion: String,
        plainText: String? = null
    ): String {
        plainText
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val builder = StringBuilder()

        val usefulMealComments = mealComments.filter { it.comment.isNotBlank() }
        if (usefulMealComments.isNotEmpty()) {
            builder.append(getString(R.string.daily_ai_meal_comments_label)).append('\n')
            usefulMealComments.forEach { meal ->
                val name = meal.mealName.trim().ifBlank { getString(R.string.daily_ai_entry_unknown) }
                builder.append("• ")
                    .append(name)
                    .append(": ")
                    .append(meal.comment.trim())
                    .append('\n')
            }
        }

        waterComment.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(getString(R.string.daily_ai_water_label)).append('\n')
            builder.append(comment).append('\n')
        }

        exerciseComment.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(getString(R.string.daily_ai_exercise_label)).append('\n')
            builder.append(comment).append('\n')
        }

        generalComment.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(getString(R.string.daily_ai_general_comment_label)).append('\n')
            builder.append(comment).append('\n')
        }

        suggestion.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(getString(R.string.daily_ai_suggestion_label)).append('\n')
            builder.append(comment)
        }

        return builder.toString().trim()
    }

    private fun applyDailyAiStatusStyle(status: AiDailyStatusLevel) = with(binding) {
        val bgColorRes: Int
        val textColorRes: Int
        when (status) {
            AiDailyStatusLevel.GREEN -> {
                bgColorRes = R.color.ai_status_green_bg
                textColorRes = R.color.ai_status_green_text
            }
            AiDailyStatusLevel.YELLOW -> {
                bgColorRes = R.color.ai_status_yellow_bg
                textColorRes = R.color.ai_status_yellow_text
            }
            AiDailyStatusLevel.RED -> {
                bgColorRes = R.color.ai_status_red_bg
                textColorRes = R.color.ai_status_red_text
            }
        }

        val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)
        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
        dailyAiCard.setCardBackgroundColor(bgColor)
        dailyAiCard.strokeColor = textColor
        dailyAiTitleText.setTextColor(textColor)
        dailyAiMessageText.setTextColor(textColor)
        dailyAiSubtitleText.setTextColor(textColor)
        dailyAiProgressIndicator.setIndicatorColor(textColor)
    }

    private fun requestDailyAiAnalysis(
        snapshot: DailyAiSnapshot,
        isAuto: Boolean
    ) {
        if (_binding == null) {
            Log.w(FITTO_AI_LOG_TAG, "Daily AI request ignored: binding is null.")
            return
        }

        if (!::dailyAiRepository.isInitialized || !::dailyAiCache.isInitialized) {
            Log.w(FITTO_AI_LOG_TAG, "Daily AI request ignored: repository/cache is not ready.")
            renderDailyAiMessage(
                message = localDailyAiMessage(snapshot),
                subtitle = getString(R.string.daily_ai_error_general)
            )
            return
        }

        dailyAiCache.get(snapshot.dateIso, snapshot.triggerType)?.let { cached ->
            val cachedAiMessage = cached.aiMessage.trim()
            if (isUsableDailyAiCachedMessage(cached, snapshot)) {
                Log.d(
                    FITTO_AI_LOG_TAG,
                    "Daily AI cache hit. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, message=${cachedAiMessage.take(80)}"
                )
                renderDailyAiMessage(
                    message = cachedAiMessage,
                    subtitle = getString(R.string.daily_ai_result_subtitle)
                )
                return
            } else {
                Log.w(
                    FITTO_AI_LOG_TAG,
                    "Daily AI stale cache cleared. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, source=${cached.source}, message=${cachedAiMessage.take(80)}"
                )
                dailyAiCache.clear(snapshot.dateIso, snapshot.triggerType)
            }
        }

        val now = System.currentTimeMillis()
        val cooldownUntil = dailyAiCache.getCooldownUntil()
        if (cooldownUntil > now) {
            val remainingSeconds = ((cooldownUntil - now) / 1000L).coerceAtLeast(1L)
            Log.d(
                FITTO_AI_LOG_TAG,
                "Daily AI request blocked by cooldown. remaining=${remainingSeconds}s"
            )
            renderDailyAiMessage(
                message = getString(R.string.daily_ai_error_rate_limited),
                subtitle = localDailyAiMessage(snapshot)
            )
            return
        }

        val requestKey = snapshot.requestKey()
        if (dailyAiLoading) {
            Log.d(FITTO_AI_LOG_TAG, "Daily AI request ignored: already loading.")
            renderDailyAiLoading()
            return
        }

        if (dailyAiRequestInFlightKey == requestKey) {
            Log.d(FITTO_AI_LOG_TAG, "Daily AI request ignored: same request is already in flight.")
            renderDailyAiLoading()
            return
        }

        if (isAuto) {
            dailyAiCache.markAutoAttempted(snapshot.dateIso, snapshot.triggerType)
        }

        Log.d(
            FITTO_AI_LOG_TAG,
            "Daily AI request started. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, isAuto=$isAuto"
        )

        dailyAiLoading = true
        dailyAiRequestInFlightKey = requestKey
        renderDailyAiLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                val language = currentAppLanguage()
                val prompt = AiDailyPromptBuilder.build(snapshot.toPromptData(language))
                dailyAiRepository.requestDailyAnalysis(prompt, language)
            }

            if (_binding == null) {
                dailyAiLoading = false
                dailyAiRequestInFlightKey = null
                return@launch
            }

            result.onSuccess { analysis ->
                Log.d(FITTO_AI_LOG_TAG, "Daily AI request succeeded. localStatus=${snapshot.localStatus().name}, aiStatus=${analysis.status.name}")

                val message = buildDailyAiStructuredMessage(
                    mealComments = analysis.mealComments,
                    waterComment = analysis.waterComment,
                    exerciseComment = analysis.exerciseComment,
                    generalComment = analysis.generalComment,
                    suggestion = analysis.suggestion,
                    plainText = analysis.plainText
                ).ifBlank { localDailyAiMessage(snapshot) }

                dailyAiCache.save(
                    dateIso = snapshot.dateIso,
                    triggerType = snapshot.triggerType,
                    aiMessage = message,
                    totalCalories = snapshot.totalCalories,
                    mealCount = snapshot.meals.size,
                    waterMl = snapshot.waterMl,
                    proteinGrams = snapshot.proteinGrams,
                    exerciseSignature = snapshot.exerciseSignature(),
                    nutritionPreferenceSignature = snapshot.nutritionPreferenceSignature()
                )

                renderDailyAiStructuredResult(
                    analysis = analysis,
                    snapshot = snapshot,
                    subtitle = getString(R.string.daily_ai_result_subtitle)
                )
            }.onFailure { throwable ->
                Log.w(FITTO_AI_LOG_TAG, "Daily AI request failed: ${throwable.message}")

                if (throwable.message.orEmpty().contains(GeminiDailyAiRepository.ERROR_RATE_LIMIT)) {
                    dailyAiCache.setCooldownFor(DAILY_AI_RATE_LIMIT_COOLDOWN_MS)
                }

                renderDailyAiMessage(
                    message = localDailyAiMessage(snapshot),
                    subtitle = getString(resolveDailyAiErrorStringRes(throwable))
                )
            }

            dailyAiLoading = false
            dailyAiRequestInFlightKey = null
        }
    }

    private fun resolveDailyAiErrorStringRes(throwable: Throwable): Int {
        val message = throwable.message.orEmpty()
        return when {
            message.contains(GeminiDailyAiRepository.ERROR_NETWORK) -> R.string.daily_ai_error_network
            message.contains(GeminiDailyAiRepository.ERROR_RATE_LIMIT) || message.contains("429") -> R.string.daily_ai_error_rate_limited
            else -> R.string.daily_ai_error_general
        }
    }

    private fun localDailyAiMessage(snapshot: DailyAiSnapshot): String {
        return when (snapshot.triggerType) {
            AiDailyTriggerType.NO_MEAL -> getString(R.string.daily_ai_no_meal_local)
            AiDailyTriggerType.CALORIE_EXCEEDED -> getString(R.string.daily_ai_calorie_exceeded_local)
            AiDailyTriggerType.CALORIE_REACHED -> getString(R.string.daily_ai_calorie_reached_local)
            AiDailyTriggerType.LOW_WATER -> getString(R.string.daily_ai_low_water_local)
            AiDailyTriggerType.LOW_PROTEIN,
            AiDailyTriggerType.LOW_DAILY_PROTEIN -> getString(R.string.daily_ai_low_protein_local)
            AiDailyTriggerType.LOW_MEAL_PROTEIN -> getString(R.string.daily_ai_low_meal_protein_local)
            AiDailyTriggerType.INSUFFICIENT_MEAL -> getString(R.string.daily_ai_insufficient_meal_local)
            AiDailyTriggerType.NO_EXERCISE -> getString(R.string.daily_ai_no_exercise_local)
            AiDailyTriggerType.NORMAL -> getString(R.string.daily_ai_default_message)
        }
    }

    private fun isUsableDailyAiCachedMessage(
        cachedMessage: com.example.fitto.feature_daily.ai.CachedDailyAiMessage,
        snapshot: DailyAiSnapshot
    ): Boolean {
        if (cachedMessage.source != AiDailyAnalysisCache.CACHE_SOURCE) return false

        if (!cachedMessage.matchesSnapshot(snapshot)) {
            Log.d(
                FITTO_AI_LOG_TAG,
                "Daily AI cache data mismatch. cached=${cachedMessage.dataDebugText()}, current=${snapshot.dataDebugText()}"
            )
            return false
        }

        val normalizedMessage = cachedMessage.aiMessage.normalizedDailyAiCacheText()
        if (normalizedMessage.isBlank()) return false

        val localTexts = listOf(
            getString(R.string.daily_ai_default_message),
            getString(R.string.daily_ai_default_subtitle),
            getString(R.string.daily_ai_loading),
            getString(R.string.daily_ai_preparing),
            getString(R.string.daily_ai_error_general),
            getString(R.string.daily_ai_error_network),
            getString(R.string.daily_ai_error_rate_limited),
            localDailyAiMessage(snapshot)
        ).map { it.normalizedDailyAiCacheText() }

        return localTexts.none { localText ->
            localText.isNotBlank() && normalizedMessage == localText
        }
    }

    private fun String.normalizedDailyAiCacheText(): String {
        return trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    private fun currentAppLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]?.language ?: Locale.getDefault().language
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.language ?: Locale.getDefault().language
        }
    }

    private fun DailyAiSnapshot.localStatus(): AiDailyStatusLevel {
        return when (triggerType) {
            AiDailyTriggerType.CALORIE_EXCEEDED -> AiDailyStatusLevel.RED
            AiDailyTriggerType.CALORIE_REACHED,
            AiDailyTriggerType.LOW_WATER,
            AiDailyTriggerType.LOW_PROTEIN,
            AiDailyTriggerType.LOW_DAILY_PROTEIN,
            AiDailyTriggerType.LOW_MEAL_PROTEIN,
            AiDailyTriggerType.INSUFFICIENT_MEAL,
            AiDailyTriggerType.NO_EXERCISE,
            AiDailyTriggerType.NO_MEAL -> AiDailyStatusLevel.YELLOW
            AiDailyTriggerType.NORMAL -> AiDailyStatusLevel.GREEN
        }
    }

    private fun DailyAiSnapshot.shouldAutoRequestAi(): Boolean {
        return selectedDate == LocalDate.now() && triggerType == AiDailyTriggerType.CALORIE_EXCEEDED
    }

    private fun DailyAiSnapshot.requestKey(): String {
        return "$dateIso-${triggerType.name}-${dataSignature()}"
    }

    private fun DailyAiSnapshot.dataSignature(): String {
        return "cal=$totalCalories|meals=${meals.size}|water=$waterMl|protein=${proteinGrams.roundToInt()}|exercise=${exerciseSignature()}|prefs=${nutritionPreferenceSignature()}"
    }

    private fun DailyAiSnapshot.exerciseSignature(): String {
        return "count=$exerciseLogCount|min=$exerciseDurationMinutes|cal=$exerciseCalories|summary=${exerciseSummaryText.normalizedDailyAiCacheText()}"
    }

    private fun DailyAiSnapshot.nutritionPreferenceSignature(): String {
        return "diet=${dietPreferences.normalizedDailyAiCacheText()}|allergy=${allergies.normalizedDailyAiCacheText()}|dislike=${dislikedFoods.normalizedDailyAiCacheText()}|routine=${mealRoutine.normalizedDailyAiCacheText()}"
    }

    private fun DailyAiSnapshot.dataDebugText(): String {
        return dataSignature()
    }

    private fun com.example.fitto.feature_daily.ai.CachedDailyAiMessage.matchesSnapshot(
        snapshot: DailyAiSnapshot
    ): Boolean {
        return totalCalories == snapshot.totalCalories &&
            mealCount == snapshot.meals.size &&
            waterMl == snapshot.waterMl &&
            proteinGrams.roundToInt() == snapshot.proteinGrams.roundToInt() &&
            exerciseSignature == snapshot.exerciseSignature() &&
            nutritionPreferenceSignature == snapshot.nutritionPreferenceSignature()
    }

    private fun com.example.fitto.feature_daily.ai.CachedDailyAiMessage.dataDebugText(): String {
        return "cal=$totalCalories|meals=$mealCount|water=$waterMl|protein=${proteinGrams.roundToInt()}|exercise=$exerciseSignature|prefs=$nutritionPreferenceSignature"
    }

    private fun DailyAiSnapshot.toPromptData(appLanguage: String): AiDailyPromptData {
        return AiDailyPromptData(
            appLanguage = appLanguage,
            userName = userName,
            dateIso = dateIso,
            goalLabel = resolveDailyAiGoalLabel(goalType, appLanguage),
            goalGuidance = resolveDailyAiGoalGuidance(goalType, appLanguage),
            dailyCalorieTarget = dailyCalorieTarget,
            totalCalories = totalCalories,
            proteinGrams = proteinGrams,
            proteinTargetGrams = proteinTargetGrams,
            carbGrams = carbGrams,
            fatGrams = fatGrams,
            waterMl = waterMl,
            waterGoalMl = waterGoalMl,
            triggerType = triggerType,
            localStatus = localStatus(),
            meals = meals.map { meal ->
                AiDailyMealInfo(
                    mealName = meal.mealTypeLabel,
                    totalCalories = meal.totalCalories,
                    proteinGrams = meal.proteinGrams,
                    carbGrams = meal.carbGrams,
                    fatGrams = meal.fatGrams,
                    entryCount = meal.entryCount,
                    foodSummary = meal.foodSummary
                )
            },
            exerciseSummary = AiDailyExerciseInfo(
                hasExercise = exerciseLogCount > 0,
                summary = exerciseSummaryText,
                totalDurationMinutes = exerciseDurationMinutes,
                totalCalories = exerciseCalories
            ),
            dietPreferences = dietPreferences,
            allergies = allergies,
            dislikedFoods = dislikedFoods,
            mealRoutine = mealRoutine
        )
    }

    private fun resolveDailyAiGoalLabel(goalTypeValue: String, appLanguage: String): String {
        val normalized = normalizeGoalTypeName(goalTypeValue)
        return when (normalized) {
            GoalType.LOSE_WEIGHT.name -> getString(R.string.daily_ai_goal_label_lose_weight)
            GoalType.GAIN_WEIGHT.name -> getString(R.string.daily_ai_goal_label_gain_weight)
            GoalType.MAINTAIN_WEIGHT.name -> getString(R.string.daily_ai_goal_label_maintain_weight)
            GoalType.LEARN_NUTRITION.name -> getString(R.string.daily_ai_goal_label_learn_nutrition)
            GoalType.EAT_HEALTHY.name -> getString(R.string.daily_ai_goal_label_eat_healthy)
            else -> getString(R.string.daily_ai_goal_label_not_specified)
        }
    }

    private fun resolveDailyAiGoalGuidance(goalTypeValue: String, appLanguage: String): String {
        val normalized = normalizeGoalTypeName(goalTypeValue)
        val isEnglish = appLanguage.startsWith("en", ignoreCase = true)
        return when (normalized) {
            GoalType.LOSE_WEIGHT.name -> if (isEnglish) {
                "Gently mention calorie excess, also notice overly low or insufficient meals, and suggest filling balanced choices."
            } else {
                "Kalori aşımını nazikçe belirt, çok düşük veya yetersiz öğünleri de fark et, tok tutan ve dengeli seçimler öner."
            }
            GoalType.GAIN_WEIGHT.name -> if (isEnglish) {
                "Notice low-energy meals more clearly and suggest supportive energy and protein sources without strict dieting language."
            } else {
                "Düşük enerjili öğünleri daha net fark et, enerji ve protein desteği öner, kesin diyet dili kullanma."
            }
            GoalType.MAINTAIN_WEIGHT.name -> if (isEnglish) {
                "Emphasize balance and sustainable habits; gently mention very low or very high days."
            } else {
                "Denge ve sürdürülebilir alışkanlıkları vurgula; aşırı düşük veya aşırı yüksek günleri nazikçe belirt."
            }
            GoalType.LEARN_NUTRITION.name -> if (isEnglish) {
                "Use a more educational tone and briefly explain the stronger and weaker sides of meals to build nutrition awareness."
            } else {
                "Daha öğretici konuş; öğünlerin güçlü ve zayıf tarafını kısa açıklayarak beslenme farkındalığı kazandır."
            }
            GoalType.EAT_HEALTHY.name -> if (isEnglish) {
                "Focus on variety, balance, water, movement, and sustainable choices without over-focusing on calories."
            } else {
                "Kaloriye fazla takılmadan çeşitlilik, denge, su, hareket ve sürdürülebilir seçimleri vurgula."
            }
            else -> if (isEnglish) {
                "Do not assume a special goal; comment on the visible daily balance safely and briefly."
            } else {
                "Özel hedef varsayma; görünen günlük dengeyi güvenli ve kısa yorumla."
            }
        }
    }

    private fun normalizeGoalTypeName(goalTypeValue: String): String {
        val raw = goalTypeValue.trim()
        GoalType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }?.let { return it.name }
        return raw
            .uppercase(Locale.ROOT)
            .replace(" ", "_")
            .replace("-", "_")
    }

    private fun calculateStepCalories(
        steps: Long,
        weightKg: Double
    ): Int {
        val kcalPerStep = when {
            weightKg < 55 -> 0.032
            weightKg < 70 -> 0.038
            weightKg < 85 -> 0.044
            else -> 0.050
        }

        return (steps * kcalPerStep).roundToInt().coerceAtLeast(0)
    }

        private fun showSleepTimeDialog() {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_sleep_time, null, false)

        dialog.setContentView(view)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val bedtimeCard = view.findViewById<MaterialCardView>(R.id.bedtimeCard)
        val wakeTimeCard = view.findViewById<MaterialCardView>(R.id.wakeTimeCard)
        val bedtimeValueText = view.findViewById<TextView>(R.id.bedtimeValueText)
        val wakeTimeValueText = view.findViewById<TextView>(R.id.wakeTimeValueText)
        val durationResultText = view.findViewById<TextView>(R.id.sleepDurationResultText)
        val caloriesResultText = view.findViewById<TextView>(R.id.sleepCaloriesResultText)
        val cancelButton = view.findViewById<MaterialButton>(R.id.sleepCancelButton)
        val saveButton = view.findViewById<MaterialButton>(R.id.sleepSaveButton)

        val currentDate = currentEntryDate()
        val currentLog = sleepLogStore.getLogForDate(currentDate)

        var bedtimeHour = currentLog?.bedtimeHour ?: 23
        var bedtimeMinute = currentLog?.bedtimeMinute ?: 0
        var wakeHour = currentLog?.wakeHour ?: 7
        var wakeMinute = currentLog?.wakeMinute ?: 0

        fun refreshPreview() {
            bedtimeValueText.text = formatHourMinute(bedtimeHour, bedtimeMinute)
            wakeTimeValueText.text = formatHourMinute(wakeHour, wakeMinute)

            val durationMinutes = calculateSleepDurationMinutes(
                bedtimeHour = bedtimeHour,
                bedtimeMinute = bedtimeMinute,
                wakeHour = wakeHour,
                wakeMinute = wakeMinute
            )

            val calories = calculateSleepCalories(
                durationMinutes = durationMinutes,
                weightKg = currentWeightKg
            )

            durationResultText.text = getString(
                R.string.sleep_duration_result,
                formatDuration(durationMinutes)
            )

            caloriesResultText.text = getString(
                R.string.sleep_calories_result,
                calories
            )
        }

        bedtimeCard.setOnClickListener {
            showTimePicker(
                title = getString(R.string.sleep_time_picker_title_bed),
                initialHour = bedtimeHour,
                initialMinute = bedtimeMinute
            ) { hour, minute ->
                bedtimeHour = hour
                bedtimeMinute = minute
                refreshPreview()
            }
        }

        wakeTimeCard.setOnClickListener {
            showTimePicker(
                title = getString(R.string.sleep_time_picker_title_wake),
                initialHour = wakeHour,
                initialMinute = wakeMinute
            ) { hour, minute ->
                wakeHour = hour
                wakeMinute = minute
                refreshPreview()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val durationMinutes = calculateSleepDurationMinutes(
                bedtimeHour = bedtimeHour,
                bedtimeMinute = bedtimeMinute,
                wakeHour = wakeHour,
                wakeMinute = wakeMinute
            )

            val calories = calculateSleepCalories(
                durationMinutes = durationMinutes,
                weightKg = currentWeightKg
            )

            val log = SleepLogLocalModel(
                date = currentDate,
                bedtimeHour = bedtimeHour,
                bedtimeMinute = bedtimeMinute,
                wakeHour = wakeHour,
                wakeMinute = wakeMinute,
                durationMinutes = durationMinutes,
                caloriesBurned = calories
            )

            sleepLogStore.saveLog(log)
            renderSleepCard()
            dialog.dismiss()

            Toast.makeText(
                requireContext(),
                getString(R.string.sleep_saved_message),
                Toast.LENGTH_SHORT
            ).show()
        }

        refreshPreview()
        dialog.show()
    }


    private fun showTimePicker(
        initialHour: Int,
        initialMinute: Int,
        title: String,
        onTimeSelected: (hour: Int, minute: Int) -> Unit
    ) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText(title)
            .setTheme(R.style.ThemeOverlay_Fitto_WhiteTimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(childFragmentManager, "fitto_time_picker")
    }

    private fun renderSleepCard() {
        if (_binding == null || !::sleepLogStore.isInitialized) return

        val log = sleepLogStore.getLogForDate(currentEntryDate())

        if (log == null) {
            binding.sleepValueText.text = getString(R.string.daily_no_sleep_data_short)
            binding.sleepStatusText.text = getString(R.string.daily_no_sleep_data_desc)
            return
        }

        binding.sleepValueText.text = formatDuration(log.durationMinutes)
        binding.sleepStatusText.text = getString(
            R.string.sleep_time_range_format,
            formatHourMinute(log.bedtimeHour, log.bedtimeMinute),
            formatHourMinute(log.wakeHour, log.wakeMinute)
        )
    }

    private fun renderWeeklyCalorieProgress() {
        if (_binding == null || !::mealRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            val targetCalories = resolveDailyCalorieTarget(profile).coerceAtLeast(1)

            val days = (6 downTo 0).map { offset ->
                selectedDate.minusDays(offset.toLong())
            }

            val dailyCalories = withContext(Dispatchers.IO) {
                days.map { date ->
                    val summary = mealRepository.getNutritionSummaryForDate(date.toString())
                    summary.totalCalories.coerceAtLeast(0)
                }
            }

            if (_binding == null) return@launch

            val barViews: List<View> = listOf(
                binding.weekBar1,
                binding.weekBar2,
                binding.weekBar3,
                binding.weekBar4,
                binding.weekBar5,
                binding.weekBar6,
                binding.weekBar7
            )

            val dayTexts: List<TextView> = listOf(
                binding.weekDay1,
                binding.weekDay2,
                binding.weekDay3,
                binding.weekDay4,
                binding.weekDay5,
                binding.weekDay6,
                binding.weekDay7
            )

            val valueTexts: List<TextView> = listOf(
                binding.weekValue1,
                binding.weekValue2,
                binding.weekValue3,
                binding.weekValue4,
                binding.weekValue5,
                binding.weekValue6,
                binding.weekValue7
            )

            val daysWithFood = dailyCalories.count { it > 0 }

            val averageProgress = if (daysWithFood > 0) {
                dailyCalories
                    .filter { it > 0 }
                    .map { calories ->
                        ((calories.toDouble() / targetCalories.toDouble()) * 100.0)
                            .coerceIn(0.0, 150.0)
                    }
                    .average()
                    .roundToInt()
            } else {
                0
            }

            binding.weeklyProgressDescriptionText.text = if (daysWithFood == 0) {
                getString(R.string.daily_no_weekly_data)
            } else {
                getString(
                    R.string.daily_weekly_calorie_summary,
                    daysWithFood,
                    averageProgress
                )
            }

            binding.weeklyWaterProgress.progress = averageProgress.coerceIn(0, 100)

            days.forEachIndexed { index, date ->
                val calories = dailyCalories[index]
                val progressPercent = ((calories.toDouble() / targetCalories.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 150)

                val visualProgress = progressPercent.coerceIn(0, 100)

                val minHeight = 18.dp
                val maxHeight = 90.dp

                val targetHeight = if (calories > 0) {
                    minHeight + (((maxHeight - minHeight) * visualProgress) / 100)
                } else {
                    minHeight
                }

                val params = barViews[index].layoutParams
                params.height = targetHeight
                barViews[index].layoutParams = params

                dayTexts[index].text = date.dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    Locale.getDefault()
                )

                valueTexts[index].text = if (calories > 0) {
                    getString(R.string.daily_weekly_calorie_percent, progressPercent)
                } else {
                    "—"
                }

                barViews[index].alpha = if (calories > 0) 1f else 0.25f
            }
        }
    }
    private fun calculateSleepDurationMinutes(
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeHour: Int,
        wakeMinute: Int
    ): Int {
        val today = LocalDate.now()
        val bedtime = LocalDateTime.of(
            today,
            LocalTime.of(bedtimeHour, bedtimeMinute)
        )

        var wake = LocalDateTime.of(
            today,
            LocalTime.of(wakeHour, wakeMinute)
        )

        if (!wake.isAfter(bedtime)) {
            wake = wake.plusDays(1)
        }

        return Duration.between(bedtime, wake).toMinutes().toInt().coerceAtLeast(0)
    }

    private fun calculateSleepCalories(
        durationMinutes: Int,
        weightKg: Double
    ): Int {
        val hours = durationMinutes / 60.0
        val sleepMet = 0.95
        return (sleepMet * weightKg * hours).roundToInt().coerceAtLeast(0)
    }

    private fun formatHourMinute(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        return if (Locale.getDefault().language.equals("tr", ignoreCase = true)) {
            "${hours}sa ${minutes}dk"
        } else {
            "${hours}h ${minutes}m"
        }
    }

    private fun renderSleepWeightSummary() {
        if (_binding == null) return

        renderSleepCard()

        val weightKg = currentProfileWeightKg

        if (weightKg == null) {
            binding.weightValueText.text = getString(R.string.daily_weight_unknown_short)
            binding.weightStatusText.text = getString(R.string.daily_weight_not_saved)
        } else {
            binding.weightValueText.text = getString(
                R.string.daily_weight_value_format,
                formatWeightValue(weightKg)
            )
            binding.weightStatusText.text = getString(R.string.daily_weight_from_profile)
        }
    }

    private fun formatWeightValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.getDefault(), "%.1f", value)
        }
    }
    private fun loadWaterGoalAndRender() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            waterGoalMl = resolveWaterGoalMl(profile)
            renderWaterTracking()
        }
    }

    private fun resolveProfileWeightKg(profile: UserProfileEntity?): Double? {
        if (profile == null) return null

        val candidateNames = listOf(
            "weightKg",
            "currentWeightKg",
            "weight",
            "currentWeight",
            "initialWeightKg",
            "initialWeight"
        )

        candidateNames.forEach { name ->
            profile.readNumberProperty(name)
                ?.takeIf { it in 25.0..300.0 }
                ?.let { return it }
        }

        return null
    }

    private fun Any.readNumberProperty(name: String): Double? {
        val fieldValue = runCatching {
            javaClass.getDeclaredField(name).apply {
                isAccessible = true
            }.get(this)
        }.getOrNull()

        return when (fieldValue) {
            is Number -> fieldValue.toDouble()
            is String -> fieldValue.replace(",", ".").toDoubleOrNull()
            else -> null
        }
    }

    private fun Int.roundUpToBottle(): Int {
        val size = WaterTrackingStore.BOTTLE_SIZE_ML
        return ((this + size - 1) / size) * size
    }
    private fun resolveWaterGoalMl(profile: UserProfileEntity?): Int {
        val explicitGoal = listOf(
            "waterGoalMl",
            "dailyWaterGoalMl",
            "waterTargetMl",
            "waterIntakeGoalMl"
        ).firstNotNullOfOrNull { fieldName ->
            profile?.readNumberProperty(fieldName)?.toInt()
        }

        if (explicitGoal != null && explicitGoal > 0) {
            return explicitGoal.roundUpToBottle()
        }

        val weightKg = resolveProfileWeightKg(profile) ?: 70.0
        val calculated = (weightKg * 35.0).toInt()
            .coerceIn(1500, 4500)

        return calculated.roundUpToBottle()
    }

    private fun renderWaterTracking(
        scrollToEnd: Boolean = false,
        scrollToStart: Boolean = false
    ) {
        if (_binding == null || !::waterTrackingStore.isInitialized) return

        val bottleSizeMl = WaterTrackingStore.BOTTLE_SIZE_ML

        val rawWaterMl = waterTrackingStore.getWaterMl(selectedDate)
        val currentWaterMl = normalizeWaterToBottleSize(rawWaterMl)
        this.currentWaterMl = currentWaterMl

        if (currentWaterMl != rawWaterMl) {
            waterTrackingStore.setWaterMl(selectedDate, currentWaterMl)
        }

        val filledBottleCount = currentWaterMl / bottleSizeMl

        val defaultBottleCount = (waterGoalMl / bottleSizeMl)
            .coerceAtLeast(WaterTrackingStore.MIN_BOTTLE_COUNT)
            .coerceAtMost(WaterTrackingStore.MAX_BOTTLE_COUNT)

        val visibleBottleCount = waterTrackingStore.getVisibleBottleCount(
            date = selectedDate,
            defaultCount = defaultBottleCount
        )

        val totalBottleCount = maxOf(
            visibleBottleCount,
            filledBottleCount.coerceAtMost(WaterTrackingStore.MAX_BOTTLE_COUNT)
        ).coerceIn(
            WaterTrackingStore.MIN_BOTTLE_COUNT,
            WaterTrackingStore.MAX_BOTTLE_COUNT
        )

        binding.waterAmountText.text = getString(
            R.string.daily_water_value_format,
            currentWaterMl / 1000.0,
            waterGoalMl / 1000.0
        )

        binding.waterBottleContainer.removeAllViews()

        binding.waterBottleContainer.addView(
            createRemoveBottleView(
                defaultBottleCount = defaultBottleCount,
                visibleBottleCount = totalBottleCount
            )
        )

        repeat(totalBottleCount) { index ->
            val isFilled = index < filledBottleCount

            binding.waterBottleContainer.addView(
                createBottleView(
                    index = index,
                    isFilled = isFilled
                )
            )
        }

        if (scrollToEnd) {
            binding.waterBottleScrollView.post {
                binding.waterBottleScrollView.fullScroll(View.FOCUS_RIGHT)
            }
        }

        if (scrollToStart) {
            binding.waterBottleScrollView.post {
                binding.waterBottleScrollView.fullScroll(View.FOCUS_LEFT)
            }
        }

        refreshDailyAiSnapshotForWater()
    }

    private fun createRemoveBottleView(
        defaultBottleCount: Int,
        visibleBottleCount: Int
    ): ImageView {
        val context = requireContext()

        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp).apply {
                marginEnd = 10.dp
                topMargin = 8.dp
                bottomMargin = 8.dp
            }

            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
            setPadding(9.dp, 9.dp, 9.dp, 9.dp)

            setImageResource(R.drawable.ic_remove)

            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (visibleBottleCount > WaterTrackingStore.MIN_BOTTLE_COUNT) {
                        R.color.fitto_orange
                    } else {
                        R.color.fitto_text_secondary
                    }
                )
            )

            alpha = if (visibleBottleCount > WaterTrackingStore.MIN_BOTTLE_COUNT) 1f else 0.35f

            contentDescription = getString(R.string.daily_remove_bottle)

            setOnClickListener {
                if (visibleBottleCount <= WaterTrackingStore.MIN_BOTTLE_COUNT) return@setOnClickListener

                val filledBottleCount = waterTrackingStore
                    .getWaterMl(selectedDate) / WaterTrackingStore.BOTTLE_SIZE_ML

                // Dolu şişe sayısından daha az görünür şişeye düşürmeyelim.
                val minAllowedByFilled = filledBottleCount.coerceAtLeast(WaterTrackingStore.MIN_BOTTLE_COUNT)

                if (visibleBottleCount <= minAllowedByFilled) return@setOnClickListener

                waterTrackingStore.decreaseVisibleBottleCount(
                    date = selectedDate,
                    defaultCount = defaultBottleCount,
                    amount = 1
                )

                renderWaterTracking(scrollToStart = true)
            }
        }
    }

    private fun createBottleView(
        index: Int,
        isFilled: Boolean
    ): ImageView {
        val context = requireContext()

        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(32.dp, 54.dp).apply {
                marginEnd = 10.dp
            }

            setImageResource(R.drawable.ic_water_bottle)

            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (isFilled) R.color.fitto_water_blue else R.color.fitto_divider_soft
                )
            )

            alpha = if (isFilled) 1f else 0.45f

            contentDescription = if (isFilled) {
                getString(R.string.daily_water_bottle_filled_cd, index + 1)
            } else {
                getString(R.string.daily_water_bottle_empty_cd, index + 1)
            }

            setOnClickListener {
                toggleWaterBottle(isFilled)
            }
        }
    }

    private fun addWater(amountMl: Int) {
        val bottleSizeMl = WaterTrackingStore.BOTTLE_SIZE_ML
        val normalizedAmount = (amountMl / bottleSizeMl) * bottleSizeMl

        if (normalizedAmount <= 0) return

        waterTrackingStore.addWaterMl(
            date = selectedDate,
            amountMl = normalizedAmount
        )

        renderWaterTracking()
    }
    private fun toggleWaterBottle(isFilled: Boolean) {
        val bottleSizeMl = WaterTrackingStore.BOTTLE_SIZE_ML
        val currentWaterMl = normalizeWaterToBottleSize(
            waterTrackingStore.getWaterMl(selectedDate)
        )

        val maxWaterMl = WaterTrackingStore.MAX_BOTTLE_COUNT * bottleSizeMl

        val updatedWaterMl = if (isFilled) {
            (currentWaterMl - bottleSizeMl).coerceAtLeast(0)
        } else {
            (currentWaterMl + bottleSizeMl).coerceAtMost(maxWaterMl)
        }

        waterTrackingStore.setWaterMl(
            date = selectedDate,
            value = updatedWaterMl
        )

        renderWaterTracking()
    }

    private fun normalizeWaterToBottleSize(valueMl: Int): Int {
        val bottleSizeMl = WaterTrackingStore.BOTTLE_SIZE_ML

        return (valueMl.coerceAtLeast(0) / bottleSizeMl) * bottleSizeMl
    }

    private fun startLocalStepCounterIfNeeded() {
        if (!::stepCounterManager.isInitialized) return

        localStepCount = stepCounterManager.getCachedSteps(selectedDate)
        renderActivityStepsOnly()

        if (selectedDate != LocalDate.now()) {
            stepCounterManager.stop()
            return
        }

        if (!stepCounterManager.isStepCounterAvailable()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.step_counter_sensor_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityRecognitionPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
            return
        }

        stepCounterManager.start(selectedDate) { steps ->
            localStepCount = steps
            renderActivityStepsOnly()
            renderMealTotals()
        }
    }

    private fun resolvedDisplaySteps(): Long {
        val healthSteps = if (
            healthConnectSummary.isAvailable &&
            healthConnectSummary.hasPermission
        ) {
            healthConnectSummary.steps.coerceAtLeast(0L)
        } else {
            0L
        }

        return if (healthSteps > 0L) {
            healthSteps
        } else {
            localStepCount.coerceAtLeast(0L)
        }
    }

    private fun renderActivityStepsOnly() {
        if (_binding == null) return

        binding.activityStepCountText.text = formatStepCount(
            resolvedDisplaySteps()
        )
    }

    private fun setupHealthConnectResultListener() {
        childFragmentManager.setFragmentResultListener(
            HealthConnectBottomSheetFragment.REQUEST_HEALTH_CONNECT_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            loadHealthConnectSummary()
        }
    }

    private fun showHealthConnectBottomSheet() {
        HealthConnectBottomSheetFragment()
            .show(childFragmentManager, "health_connect")
    }

    private fun loadHealthConnectSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            val date = currentDailyLocalDate()

            healthConnectSummary = healthConnectManager.readDailySummary(date)

            renderHealthConnectSummary()
        }
    }
    private fun renderHealthConnectSummary() {
        // Health Connect verisi geldikten sonra ana günlük hesapları tekrar çiziyoruz.
        // Böylece yakılan kalori, kalan kalori ve aktivite kartı aynı yerden güncelleniyor.
        renderMealTotals()
    }

    private fun calculateSafeBurnedCalories(
        healthActiveCalories: Int,
        manualExerciseCalories: Int
    ): Int {
        return maxOf(healthActiveCalories, manualExerciseCalories)
    }
     private fun currentDailyLocalDate(): LocalDate {
        return selectedDate
    }
    private fun setupExerciseAddedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddExerciseFragment.REQUEST_EXERCISE_ADDED,
            viewLifecycleOwner
        ) { _, bundle ->
            val entryDate = bundle.getString(AddExerciseFragment.BUNDLE_EXERCISE_DATE)

            if (!entryDate.isNullOrBlank()) {
                selectedDateIso = entryDate
                selectedDate = runCatching { LocalDate.parse(entryDate) }
                    .getOrDefault(selectedDate)
                renderSelectedDate()
            }

            loadHealthConnectSummary()
            renderMealTotals()
            renderActivePlan()
        }
    }
    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            renderSelectedDate()
            loadHealthConnectSummary()
            renderActivePlan()
            startLocalStepCounterIfNeeded()
            renderWaterTracking()
            loadSleepWeightSummary()
            renderSleepCard()
            renderWeeklyCalorieProgress()
        }
    }
    override fun onPause() {
        if (::stepCounterManager.isInitialized) {
            stepCounterManager.stop()
        }
        super.onPause()
    }
    private fun openDatePicker() {
        val todayMillis = LocalDate.now()
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val selectedMillis = selectedDate
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val constraints = CalendarConstraints.Builder()
            .setEnd(todayMillis)
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)
            .setTitleText(getString(R.string.daily_date_picker_title))
            .setSelection(selectedMillis)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selectedUtcMillis ->
            val pickedDate = Instant.ofEpochMilli(selectedUtcMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()

            if (pickedDate.isAfter(LocalDate.now())) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_future_date_blocked),
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnPositiveButtonClickListener
            }

            selectedDate = pickedDate
            renderSelectedDate()
            reloadDailyDataForSelectedDate()
        }

        picker.show(parentFragmentManager, "daily_date_picker")
    }

    private fun renderSelectedDate() = with(binding) {
        selectedDateIso = selectedDate.toString()

        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.getDefault())
        val formattedDate = selectedDate.format(formatter)

        dailySelectedDateText.text = if (selectedDate == LocalDate.now()) {
            getString(R.string.daily_selected_date_today, formattedDate)
        } else {
            getString(R.string.daily_selected_date_format, formattedDate)
        }
    }

    private fun reloadDailyDataForSelectedDate() {
        selectedDateIso = selectedDate.toString()
        loadHealthConnectSummary()
        renderActivePlan()
        startLocalStepCounterIfNeeded()
        renderWaterTracking()
        renderSleepCard()
        renderWeeklyCalorieProgress()
    }

    private fun setupMockUi() = with(binding) {
        dailyCalorieProgress.progress = 0
        dailyCarbProgress.progress = 0
        dailyProteinProgress.progress = 0
        dailyFatProgress.progress = 0
        weeklyWaterProgress.progress = 0
        renderDailyAiDefaultState()
    }

    private fun setupDietPlanResultListener() {
        parentFragmentManager.setFragmentResultListener(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            renderActivePlan()
            renderMealTotals()
        }
    }

    private fun setupMealAddedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddMealFragment.REQUEST_MEAL_ENTRY_ADDED,
            viewLifecycleOwner
        ) { _, bundle ->
            val mealType = MealType.fromArgument(
                bundle.getString(AddMealFragment.BUNDLE_MEAL_TYPE)
            )

            val entryDate = bundle.getString(AddMealFragment.BUNDLE_ENTRY_DATE)
            if (entryDate != null) {
                selectedDateIso = entryDate
                selectedDate = runCatching { LocalDate.parse(entryDate) }
                    .getOrDefault(selectedDate)
                renderSelectedDate()
            }

            markNextPlannedMealCompletedIfMatches(mealType)

            renderMealTotals()
            renderActivePlan()
            renderWeeklyCalorieProgress()
        }
    }

    private fun setupMealDetailChangedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            MealDetailFragment.REQUEST_MEAL_DETAIL_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            renderMealTotals()
            renderActivePlan()
            renderWeeklyCalorieProgress()

        }
    }

    private fun renderActivePlan() {
        if (_binding == null || !::dietPlanStore.isInitialized) return

        val activePlanId = dietPlanStore.getActivePlanId()
        if (activePlanId?.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX) == true) {
            val customPlan = customDietPlanStore.getPlan(activePlanId)

            if (customPlan != null) {
                renderGeneratedCustomPlan(customPlan)
                return
            }
        }

        val activePlan = if (activePlanId == null) {
            DietPlanCatalog.createEmptyActivePlan(requireContext())
        } else {
            DietPlanCatalog.getActivePlan(
                context = requireContext(),
                planId = activePlanId,
                fastingType = dietPlanStore.getActiveFastingType(),
                customFastingHours = dietPlanStore.getCustomFastingHours()
            )
        }

        currentActivePlan = activePlan.takeIf { it.id != "none" }

        if (activePlan.id == "none" ||
            activePlan.id == DietPlanCatalog.PLAN_INTERMITTENT_FASTING
        ) {
            currentDailyMealPlan = null
            renderBasicActivePlan(activePlan)
            return
        }

        val targetCalories = currentDailyTargetCalories.coerceAtLeast(1)

        viewLifecycleOwner.lifecycleScope.launch {
            val dailyPlan = withContext(Dispatchers.IO) {
                buildDailyMealPlanFromRoom(activePlan, targetCalories)
            }

            if (_binding == null) return@launch

            val safeDailyPlan = dailyPlan ?: buildDailyMealPlanFromCatalog(
                activePlan = activePlan,
                targetCalories = targetCalories
            )

            if (safeDailyPlan == null) {
                currentDailyMealPlan = null
                renderBasicActivePlan(activePlan)
            } else {
                currentDailyMealPlan = safeDailyPlan
                renderDailyMealPlan(safeDailyPlan)
            }
        }
    }

    private fun renderFastingActionsIfNeeded(planId: String) = with(binding) {
        val isFastingPlan = planId == DietPlanCatalog.PLAN_INTERMITTENT_FASTING ||
                planId.contains("fasting", ignoreCase = true) ||
                planId.contains("if", ignoreCase = true)

        if (!isFastingPlan) {
            fastingActionContainer.visibility = View.GONE
            fastingAnalysisContainer.visibility = View.GONE
            stopFastingTimer()
            stopHourglassAnimation()
            return
        }

        fastingActionContainer.visibility = View.VISIBLE
        fastingAnalysisContainer.visibility = View.VISIBLE

        fastingActionContainer.setOnClickListener {
            // Oruç kontrol alanına basınca aktif plan detayına gitmesin.
        }

        val fastingHours = resolveSelectedFastingHours()
        val remainingMillis = fastingSessionStore.getRemainingMillis(planId)

        fastingMealSuggestionText.text = buildString {
            append(getString(R.string.daily_fasting_meal_suggestion_title))
            append("\n")
            append(getFastingMealSuggestion(fastingHours))
        }

        updateFastingAnalysis(planId, remainingMillis)

        if (remainingMillis > 0L) {
            startFastingCountdown(planId)
        } else {
            fastingCountdownTitleText.text = getString(R.string.daily_fasting_countdown_title)
            fastingCountdownText.text = getString(R.string.daily_fasting_not_started)
            startFastingButton.isEnabled = true
            startFastingButton.text = getString(R.string.daily_fasting_start_button)
            stopHourglassAnimation()

            startFastingButton.setOnClickListener {
                fastingSessionStore.startFasting(
                    planId = planId,
                    fastingHours = fastingHours
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_fasting_started_message),
                    Toast.LENGTH_SHORT
                ).show()

                startFastingCountdown(planId)
            }
        }
    }
    private fun startFastingCountdown(planId: String) {
        stopFastingTimer()

        val remainingMillis = fastingSessionStore.getRemainingMillis(planId)

        if (remainingMillis <= 0L) {
            binding.fastingCountdownText.text = getString(R.string.daily_fasting_completed)
            binding.startFastingButton.isEnabled = true
            binding.startFastingButton.text = getString(R.string.daily_fasting_start_button)
            updateFastingAnalysis(planId)
            stopHourglassAnimation()
            return
        }

        binding.startFastingButton.isEnabled = true
        binding.startFastingButton.text = getString(R.string.daily_fasting_finish_button)
        binding.startFastingButton.setOnClickListener {
            finishFastingManually()
        }

        binding.fastingCountdownText.text = formatRemainingTime(remainingMillis)
        updateFastingAnalysis(planId, remainingMillis)

        startHourglassAnimation()

        fastingTimer = object : android.os.CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return

                binding.fastingCountdownText.text = formatRemainingTime(millisUntilFinished)
                updateFastingAnalysis(planId, millisUntilFinished)
            }

            override fun onFinish() {
                if (_binding == null) return

                binding.fastingCountdownText.text = getString(R.string.daily_fasting_completed)
                binding.startFastingButton.isEnabled = true
                binding.startFastingButton.text = getString(R.string.daily_fasting_start_button)

                fastingSessionStore.clear()
                stopHourglassAnimation()
                renderActivePlan()
            }
        }.start()
    }
    private data class FastingBodyAnalysisUiModel(
        val rangeTitle: String,
        val bodyText: String
    )
    private fun getFastingBodyAnalysis(elapsedHours: Long): FastingBodyAnalysisUiModel {
        return when {
            elapsedHours < 4L -> {
                FastingBodyAnalysisUiModel(
                    rangeTitle = getString(R.string.daily_fasting_elapsed_range_0_4),
                    bodyText = getString(R.string.daily_fasting_elapsed_body_0_4)
                )
            }

            elapsedHours < 8L -> {
                FastingBodyAnalysisUiModel(
                    rangeTitle = getString(R.string.daily_fasting_elapsed_range_4_8),
                    bodyText = getString(R.string.daily_fasting_elapsed_body_4_8)
                )
            }

            elapsedHours < 12L -> {
                FastingBodyAnalysisUiModel(
                    rangeTitle = getString(R.string.daily_fasting_elapsed_range_8_12),
                    bodyText = getString(R.string.daily_fasting_elapsed_body_8_12)
                )
            }

            else -> {
                FastingBodyAnalysisUiModel(
                    rangeTitle = getString(R.string.daily_fasting_elapsed_range_12_16),
                    bodyText = getString(R.string.daily_fasting_elapsed_body_12_16)
                )
            }
        }
    }
    private fun finishFastingManually() {
        stopFastingTimer()
        stopHourglassAnimation()
        fastingSessionStore.clear()

        binding.fastingCountdownTitleText.text = getString(R.string.daily_fasting_countdown_title)
        binding.fastingCountdownText.text = getString(R.string.daily_fasting_not_started)
        binding.startFastingButton.isEnabled = true
        binding.startFastingButton.text = getString(R.string.daily_fasting_start_button)

        Toast.makeText(
            requireContext(),
            getString(R.string.daily_fasting_finished_message),
            Toast.LENGTH_SHORT
        ).show()

        renderActivePlan()
    }

    private fun stopFastingTimer() {
        fastingTimer?.cancel()
        fastingTimer = null
    }

    private fun updateFastingAnalysis(
        planId: String,
        remainingMillisOverride: Long? = null
    ) = with(binding) {
        val remainingMillis = remainingMillisOverride
            ?: fastingSessionStore.getRemainingMillis(planId)

        fastingAnalysisTitleText.text = getString(R.string.daily_fasting_analysis_title)

        if (remainingMillis <= 0L) {
            fastingAnalysisRangeText.text = getString(R.string.daily_fasting_analysis_not_started_title)
            fastingAnalysisBodyText.text = getString(R.string.daily_fasting_analysis_not_started_body)
            return@with
        }

        val fastingHours = resolveSelectedFastingHours()
        val totalDurationMillis = fastingHours * 60L * 60L * 1000L
        val elapsedMillis = (totalDurationMillis - remainingMillis).coerceAtLeast(0L)
        val elapsedHours = elapsedMillis / (60L * 60L * 1000L)

        val analysis = getFastingBodyAnalysis(elapsedHours)

        fastingAnalysisRangeText.text = analysis.rangeTitle
        fastingAnalysisBodyText.text = analysis.bodyText
    }
    private fun startHourglassAnimation() {
        if (hourglassAnimator?.isRunning == true) return

        hourglassAnimator = ObjectAnimator.ofFloat(
            binding.fastingHourglassText,
            View.ROTATION,
            0f,
            180f,
            360f
        ).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        binding.fastingHourglassText.animate()
            .alpha(0.65f)
            .setDuration(700L)
            .withEndAction {
                binding.fastingHourglassText.animate()
                    .alpha(1f)
                    .setDuration(700L)
                    .start()
            }
            .start()
    }

    private fun stopHourglassAnimation() {
        hourglassAnimator?.cancel()
        hourglassAnimator = null

        binding.fastingHourglassText.rotation = 0f
        binding.fastingHourglassText.alpha = 1f
    }

    private fun formatRemainingTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
    private fun getFastingMealSuggestion(fastingHours: Int): String {
        return when {
            fastingHours >= 16 -> getString(R.string.daily_fasting_meal_suggestion_long)
            fastingHours >= 14 -> getString(R.string.daily_fasting_meal_suggestion_medium)
            else -> getString(R.string.daily_fasting_meal_suggestion_short)
        }
    }

    private fun resolveSelectedFastingHours(): Int {
        val fastingTypeName = dietPlanStore.getActiveFastingType()
            ?.name
            .orEmpty()
            .lowercase(Locale.ROOT)

        return when {
            fastingTypeName.contains("12") -> 12
            fastingTypeName.contains("15") -> 15
            fastingTypeName.contains("16") -> 16
            fastingTypeName.contains("custom") -> {
                dietPlanStore.getCustomFastingHours()
                    ?.coerceIn(8, 24)
                    ?: 16
            }
            else -> 16
        }
    }

    private fun renderGeneratedCustomPlan(
        customPlan: CustomDietPlanLocalModel
    ) {
        val generator = CustomDietMealPlanGenerator(requireContext())
        val activePlan = generator.createActivePlan(customPlan)
        currentActivePlan = activePlan

        viewLifecycleOwner.lifecycleScope.launch {
            val foods = withContext(Dispatchers.IO) {
                mealRepository.getAllFoods()
            }

            if (_binding == null) return@launch

            val dayNumber = dietPlanStore.getCurrentPlanDay(customPlan.durationDays)
            val completedMealIds = dietPlanStore.getCompletedPlannedMealIds(
                planId = customPlan.id,
                dayNumber = dayNumber
            )
            val dailyPlan = generator.buildDailyPlan(
                plan = customPlan,
                allFoods = foods,
                dayNumber = dayNumber,
                completedMealIds = completedMealIds
            )

            currentDailyMealPlan = dailyPlan

            if (dailyPlan.meals.isEmpty()) {
                renderBasicActivePlan(activePlan)
            } else {
                renderDailyMealPlan(dailyPlan)
            }
        }
    }

    private suspend fun buildDailyMealPlanFromRoom(
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ): DailyPlannedMealsUiModel? {
        val roomPlan = dietPlanSeedRepository.getDietPlanById(activePlan.id) ?: return null
        if (roomPlan.durationDays <= 0) return null

        val dayNumber = dietPlanStore.getCurrentPlanDay(roomPlan.durationDays)
        val plannedMeals = dietPlanSeedRepository.getPlannedMealUiModelsForDay(
            planId = activePlan.id,
            dayNumber = dayNumber
        )

        val completedMealIds = dietPlanStore.getCompletedPlannedMealIds(
            activePlan.id,
            dayNumber
        )

        val nextMeal = plannedMeals.firstOrNull { it.id !in completedMealIds }

        return DailyPlannedMealsUiModel(
            planId = activePlan.id,
            planTitle = roomPlan.nameTr,
            planBadge = activePlan.badge,
            planIcon = roomPlan.icon.ifBlank { activePlan.icon },
            dayNumber = dayNumber,
            durationDays = roomPlan.durationDays,
            targetCalories = targetCalories,
            meals = plannedMeals,
            nextMeal = nextMeal,
            completedMealIds = completedMealIds
        )
    }

    private fun buildDailyMealPlanFromCatalog(
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ): DailyPlannedMealsUiModel? {
        val durationDays = DietMealPlanCatalog.getPlanDurationDays(activePlan.id)
        if (durationDays <= 0) return null

        val dayNumber = dietPlanStore.getCurrentPlanDay(durationDays)
        val plannedMeals = DietMealPlanCatalog.getMealsForDay(activePlan.id, dayNumber)
        if (plannedMeals.isEmpty()) return null

        val completedMealIds = dietPlanStore.getCompletedPlannedMealIds(
            activePlan.id,
            dayNumber
        )

        val nextMeal = plannedMeals.firstOrNull { it.id !in completedMealIds }

        return DailyPlannedMealsUiModel(
            planId = activePlan.id,
            planTitle = DietMealPlanCatalog.getPlanTitle(requireContext(), activePlan.id),
            planBadge = DietMealPlanCatalog.getPlanBadge(requireContext(), activePlan.id),
            planIcon = DietMealPlanCatalog.getPlanIcon(activePlan.id),
            dayNumber = dayNumber,
            durationDays = durationDays,
            targetCalories = targetCalories,
            meals = plannedMeals,
            nextMeal = nextMeal,
            completedMealIds = completedMealIds
        )
    }

    private fun renderBasicActivePlan(activePlan: ActiveDietPlanUiModel) = with(binding) {
        activePlanTitleText.text = activePlan.title
        activePlanBadgeText.text = activePlan.badge

        val isFastingPlan = activePlan.id == DietPlanCatalog.PLAN_INTERMITTENT_FASTING ||
                activePlan.id.contains("fasting", ignoreCase = true) ||
                activePlan.id.contains("if", ignoreCase = true)

        if (isFastingPlan) {
            activePlanMetricsContainer.isVisible = false
            fastingActionContainer.isVisible = true
            fastingAnalysisContainer.isVisible = true

            renderFastingActionsIfNeeded(activePlan.id)
            updateFastingAnalysis(activePlan.id)
        } else {
            activePlanMetricsContainer.isVisible = true
            fastingActionContainer.isVisible = false
            fastingAnalysisContainer.isVisible = false

            activePlanIconText.isVisible = true
            activePlanDivider.isVisible = true
            activePlanMetricOneLabelText.isVisible = true
            activePlanMetricOneValueText.isVisible = true
            activePlanMetricTwoLabelText.isVisible = true
            activePlanMetricTwoValueText.isVisible = true

            activePlanMetricOneLabelText.text = activePlan.primaryLabel
            activePlanMetricOneValueText.text = activePlan.primaryValue
            activePlanMetricTwoLabelText.text = activePlan.secondaryLabel
            activePlanMetricTwoValueText.text = activePlan.secondaryValue
        }

        plannedMealFoodsText.isVisible = false
        applyPlannedMealButton.isVisible = false
        fastingProgress.progress = activePlan.progress
    }

    private fun renderDailyMealPlan(plan: DailyPlannedMealsUiModel) = with(binding) {
        fastingActionContainer.visibility = View.GONE
        fastingAnalysisContainer.visibility = View.GONE
        stopFastingTimer()
        stopHourglassAnimation()

        activePlanMetricsContainer.isVisible = true
        activePlanIconText.isVisible = true
        activePlanDivider.isVisible = true
        activePlanMetricOneLabelText.isVisible = true
        activePlanMetricOneValueText.isVisible = true
        activePlanMetricTwoLabelText.isVisible = true
        activePlanMetricTwoValueText.isVisible = true

        val nextMeal = plan.nextMeal

        // devamı sende olduğu gibi kalacak

        activePlanTitleText.text = plan.planTitle

        activePlanBadgeText.text = getString(
            R.string.daily_active_plan_day_badge,
            plan.dayNumber,
            plan.durationDays,
            plan.targetCalories
        )

        activePlanIconText.text = plan.planIcon
        activePlanMetricOneLabelText.text = getString(R.string.daily_next_planned_meal_label)
        activePlanMetricTwoLabelText.text = getString(R.string.daily_plan_status_label)
        activePlanMetricTwoValueText.text = getString(
            R.string.daily_plan_status_value,
            plan.completedMealIds.size,
            plan.meals.size
        )
        fastingProgress.progress = plan.progressPercent

        if (nextMeal == null) {
            activePlanMetricOneValueText.text = getString(R.string.daily_plan_completed_value)
            plannedMealFoodsText.text = getString(R.string.daily_plan_completed_description)
            plannedMealFoodsText.isVisible = true
            applyPlannedMealButton.isVisible = false
        } else {
            val mealName = getString(nextMeal.mealType.titleRes)
            val calories = nextMeal.scaledCalories(plan.targetCalories)
            val foodSummary = nextMeal.scaledFoodSummary(plan.targetCalories)

            activePlanMetricOneValueText.text = getString(
                R.string.daily_next_planned_meal_value,
                mealName,
                calories
            )

            plannedMealFoodsText.text = buildRecommendationText(
                mealTitle = nextMeal.title,
                foodSummary = foodSummary
            )

            plannedMealFoodsText.isVisible = true
            applyPlannedMealButton.isVisible = true
        }
    }

    private fun buildRecommendationText(
        mealTitle: String,
        foodSummary: String
    ): SpannableStringBuilder {
        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)

        return SpannableStringBuilder().apply {
            val mealLabelStart = length
            append(getString(R.string.daily_meal_recommendation_label))
            setSpan(
                ForegroundColorSpan(green),
                mealLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                mealLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("\n")
            append(mealTitle)

            append("\n\n")

            val foodLabelStart = length
            append(getString(R.string.daily_food_recommendation_label))
            setSpan(
                ForegroundColorSpan(green),
                foodLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                foodLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("\n")
            append(foodSummary)
        }
    }

    private fun renderMealTotals() {
        if (_binding == null || !::mealRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            val selectedDateIso = selectedDate.toString()

            val mealSummaries = withContext(Dispatchers.IO) {
                mealRepository.getMealSummariesForDate(selectedDateIso)
            }

            val nutritionSummary = withContext(Dispatchers.IO) {
                mealRepository.getNutritionSummaryForDate(selectedDateIso)
            }
            val exerciseSummary = withContext(Dispatchers.IO) {
                exerciseLogStore.getSummaryForDate(selectedDateIso)
            }
            val exerciseLogs = withContext(Dispatchers.IO) {
                exerciseLogStore.getLogsForDate(selectedDateIso)
            }

            val mealDetailsByType = withContext(Dispatchers.IO) {
                MealType.entries.associateWith { mealType ->
                    mealRepository.getMealEntryDetailsForDate(
                        dateIso = selectedDateIso,
                        mealType = mealType
                    )
                }
            }
            if (_binding == null) return@launch

            val targetCalories = resolveDailyCalorieTarget(profile)
            val targetProtein = resolveProteinTarget(profile)
            val targetCarbs = resolveCarbTarget(profile)
            val targetFat = resolveFatTarget(profile)

            val oldTargetCalories = currentDailyTargetCalories

            currentDailyTargetCalories = targetCalories
            currentProteinTargetGrams = targetProtein
            currentCarbTargetGrams = targetCarbs
            currentFatTargetGrams = targetFat

            val byMealType = mealSummaries.associateBy { it.mealType }

            binding.breakfastCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                byMealType[MealType.BREAKFAST.argumentValue]?.totalCalories ?: 0
            )

            binding.snackCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                byMealType[MealType.SNACK.argumentValue]?.totalCalories ?: 0
            )

            binding.lunchCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                byMealType[MealType.LUNCH.argumentValue]?.totalCalories ?: 0
            )

            binding.dinnerCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                byMealType[MealType.DINNER.argumentValue]?.totalCalories ?: 0
            )

            val eatenCalories = nutritionSummary.totalCalories.coerceAtLeast(0)

            val resolvedWeight = resolveProfileWeightKg(profile)
            currentProfileWeightKg = resolvedWeight
            currentWeightKg = resolvedWeight ?: currentWeightKg

            val manualExerciseCalories = exerciseSummary.totalCalories.coerceAtLeast(0)

            val healthActiveCalories = if (
                healthConnectSummary.isAvailable &&
                healthConnectSummary.hasPermission
            ) {
                healthConnectSummary.activeCaloriesKcal.coerceAtLeast(0)
            } else {
                0
            }

            val displaySteps = resolvedDisplaySteps()

            val stepCalories = if (healthActiveCalories > 0) {
                0
            } else {
                calculateStepCalories(
                    steps = displaySteps,
                    weightKg = currentWeightKg
                )
            }

            val syncedActivityCalories = maxOf(
                healthActiveCalories,
                stepCalories
            )

            val burnedCalories = manualExerciseCalories + syncedActivityCalories

            val remainingCalories = (targetCalories - eatenCalories + burnedCalories)
                .coerceAtLeast(0)

            binding.remainingCaloriesText.text = remainingCalories.toString()

            binding.dailyIntakeText.text = getString(
                R.string.daily_summary_intake_value,
                eatenCalories
            )

            binding.dailyBurnedText.text = getString(
                R.string.daily_summary_burned_value,
                burnedCalories
            )
            binding.activityBurnedCaloriesText.text = burnedCalories.toString()

            renderActivitySourceStatus(
                healthActiveCalories = syncedActivityCalories,
                manualExerciseCalories = manualExerciseCalories
            )

            binding.activityStepCountText.text = formatStepCount(displaySteps)

            val manualExerciseDurationMinutes = exerciseSummary.totalDurationMinutes.coerceAtLeast(0)

            binding.activityDurationText.text = getString(
                R.string.exercise_duration_value,
                manualExerciseDurationMinutes
            )

            binding.dailyRemainingText.text = getString(
                R.string.daily_summary_remaining_value,
                remainingCalories
            )

            binding.dailyCalorieProgress.progress = progressPercent(
                current = eatenCalories.toDouble(),
                target = targetCalories.toDouble()
            )
            binding.dailyCarbValueText.text = getString(
                R.string.daily_macro_value,
                formatGram(nutritionSummary.totalCarbs),
                targetCarbs
            )

            binding.dailyProteinValueText.text = getString(
                R.string.daily_macro_value,
                formatGram(nutritionSummary.totalProtein),
                targetProtein
            )

            binding.dailyFatValueText.text = getString(
                R.string.daily_macro_value,
                formatGram(nutritionSummary.totalFat),
                targetFat
            )

            binding.dailyCarbProgress.progress = progressPercent(
                current = nutritionSummary.totalCarbs,
                target = targetCarbs.toDouble()
            )

            binding.dailyProteinProgress.progress = progressPercent(
                current = nutritionSummary.totalProtein,
                target = targetProtein.toDouble()
            )

            binding.dailyFatProgress.progress = progressPercent(
                current = nutritionSummary.totalFat,
                target = targetFat.toDouble()
            )

            val dailyAiMeals = buildDailyAiMeals(
                mealSummariesByType = byMealType,
                mealDetailsByType = mealDetailsByType
            )

            val dailyAiTriggerType = resolveDailyAiTriggerType(
                totalCalories = eatenCalories,
                targetCalories = targetCalories,
                waterMl = currentWaterMl,
                waterGoalMl = waterGoalMl,
                proteinGrams = nutritionSummary.totalProtein,
                proteinTargetGrams = targetProtein,
                meals = dailyAiMeals,
                exerciseLogCount = exerciseSummary.logCount
            )

            currentDailyAiSnapshot = DailyAiSnapshot(
                dateIso = selectedDateIso,
                userName = resolveDailyAiUserName(profile),
                goalType = profile?.goalType.orEmpty(),
                dailyCalorieTarget = targetCalories,
                totalCalories = eatenCalories,
                proteinGrams = nutritionSummary.totalProtein,
                proteinTargetGrams = targetProtein,
                carbGrams = nutritionSummary.totalCarbs,
                fatGrams = nutritionSummary.totalFat,
                waterMl = currentWaterMl,
                waterGoalMl = waterGoalMl,
                triggerType = dailyAiTriggerType,
                meals = dailyAiMeals,
                exerciseLogCount = exerciseSummary.logCount,
                exerciseDurationMinutes = exerciseSummary.totalDurationMinutes.coerceAtLeast(0),
                exerciseCalories = burnedCalories,
                exerciseSummaryText = buildDailyAiExerciseSummary(exerciseLogs, syncedActivityCalories),
                dietPreferences = ProfilePreferenceStore.nutritionDietPreferencesText(requireContext()),
                allergies = ProfilePreferenceStore.getNutritionAllergies(requireContext()),
                dislikedFoods = ProfilePreferenceStore.getNutritionDislikedFoods(requireContext()),
                mealRoutine = ProfilePreferenceStore.nutritionMealRoutineText(requireContext())
            )

            renderDailyAiState(currentDailyAiSnapshot)

            if (oldTargetCalories != targetCalories) {
                renderActivePlan()
            }
        }
    }

    private fun buildDailyAiMeals(
        mealSummariesByType: Map<String, com.example.fitto.data.local.model.DailyMealSummary>,
        mealDetailsByType: Map<MealType, List<MealEntryDetailUiModel>>
    ): List<DailyAiMealSnapshot> {
        return MealType.entries.mapNotNull { mealType ->
            val mealTypeLabel = getString(mealType.titleRes)
            val details = mealDetailsByType[mealType].orEmpty()
            val summary = mealSummariesByType[mealType.argumentValue]

            val totalCalories = details.sumOf { it.totalCalories }
                .takeIf { it > 0 }
                ?: (summary?.totalCalories ?: 0)
            if (totalCalories <= 0) return@mapNotNull null

            val protein = details.sumOf { it.totalProtein }
                .takeIf { details.isNotEmpty() }
                ?: (summary?.totalProtein ?: 0.0)
            val carbs = details.sumOf { it.totalCarbs }
                .takeIf { details.isNotEmpty() }
                ?: (summary?.totalCarbs ?: 0.0)
            val fat = details.sumOf { it.totalFat }
                .takeIf { details.isNotEmpty() }
                ?: (summary?.totalFat ?: 0.0)

            val foodSummary = details
                .sortedBy { it.createdAt }
                .joinToString(separator = "; ") { detail ->
                    val title = detail.title.ifBlank { detail.foodSummary.ifBlank { mealTypeLabel } }
                    val foods = detail.foodSummary.ifBlank { getString(R.string.daily_ai_entry_details_unavailable) }
                    "$title: $foods"
                }
                .take(DAILY_AI_MEAL_SUMMARY_MAX_CHARS)

            DailyAiMealSnapshot(
                mealTypeLabel = mealTypeLabel,
                totalCalories = totalCalories,
                proteinGrams = protein,
                carbGrams = carbs,
                fatGrams = fat,
                entryCount = details.size.coerceAtLeast(1),
                foodSummary = foodSummary
            )
        }
    }

    private fun buildDailyAiExerciseSummary(
        logs: List<ExerciseLogLocalModel>,
        syncedActivityCalories: Int
    ): String {
        val manualSummary = logs
            .take(3)
            .joinToString(separator = ", ") { log ->
                getString(
                    R.string.daily_ai_exercise_summary_item,
                    log.exerciseTitle.ifBlank { log.categoryName },
                    log.durationMinutes,
                    log.calories
                )
            }

        return when {
            manualSummary.isNotBlank() && syncedActivityCalories > 0 -> {
                getString(R.string.daily_ai_exercise_summary_with_steps, manualSummary, syncedActivityCalories)
            }
            manualSummary.isNotBlank() -> manualSummary
            syncedActivityCalories > 0 -> getString(R.string.daily_ai_exercise_summary_synced, syncedActivityCalories)
            else -> ""
        }
    }

    private fun resolveDailyAiTriggerType(
        totalCalories: Int,
        targetCalories: Int,
        waterMl: Int,
        waterGoalMl: Int,
        proteinGrams: Double,
        proteinTargetGrams: Int,
        meals: List<DailyAiMealSnapshot>,
        exerciseLogCount: Int
    ): AiDailyTriggerType {
        if (meals.isEmpty()) return AiDailyTriggerType.NO_MEAL

        if (targetCalories > 0) {
            val redThreshold = (targetCalories * 1.2).roundToInt()
            val yellowThreshold = (targetCalories * 0.9).roundToInt()
            if (totalCalories >= redThreshold) return AiDailyTriggerType.CALORIE_EXCEEDED
            if (totalCalories >= yellowThreshold) return AiDailyTriggerType.CALORIE_REACHED
        }

        val hasInsufficientMainMeal = meals.any { meal ->
            val isMainMeal = !meal.mealTypeLabel.equals(getString(R.string.daily_snack), ignoreCase = true)
            isMainMeal && meal.totalCalories in 1..180
        }
        if (hasInsufficientMainMeal) return AiDailyTriggerType.INSUFFICIENT_MEAL

        val hasLowMealProtein = meals.any { meal ->
            val isMainMeal = !meal.mealTypeLabel.equals(getString(R.string.daily_snack), ignoreCase = true)
            isMainMeal && meal.proteinGrams < 8.0 && meal.totalCalories > 0
        }
        if (hasLowMealProtein) return AiDailyTriggerType.LOW_MEAL_PROTEIN

        if (waterGoalMl > 0 && waterMl < (waterGoalMl * 0.5).roundToInt()) {
            return AiDailyTriggerType.LOW_WATER
        }

        if (proteinTargetGrams > 0 && proteinGrams < proteinTargetGrams * 0.55) {
            return AiDailyTriggerType.LOW_DAILY_PROTEIN
        }

        if (exerciseLogCount == 0) {
            return AiDailyTriggerType.NO_EXERCISE
        }

        return AiDailyTriggerType.NORMAL
    }

    private fun resolveDailyAiUserName(profile: UserProfileEntity?): String {
        return listOf(
            profile?.displayName,
            profile?.userName,
            getString(R.string.app_name)
        ).firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }

    private fun refreshDailyAiSnapshotForWater() {
        val currentSnapshot = currentDailyAiSnapshot ?: return
        val updatedTrigger = resolveDailyAiTriggerType(
            totalCalories = currentSnapshot.totalCalories,
            targetCalories = currentSnapshot.dailyCalorieTarget,
            waterMl = currentWaterMl,
            waterGoalMl = waterGoalMl,
            proteinGrams = currentSnapshot.proteinGrams,
            proteinTargetGrams = currentSnapshot.proteinTargetGrams,
            meals = currentSnapshot.meals,
            exerciseLogCount = currentSnapshot.exerciseLogCount
        )

        val updatedSnapshot = currentSnapshot.copy(
            waterMl = currentWaterMl,
            waterGoalMl = waterGoalMl,
            triggerType = updatedTrigger
        )

        currentDailyAiSnapshot = updatedSnapshot
        renderDailyAiState(updatedSnapshot)
    }

    private fun formatStepCount(steps: Long): String {
        return java.text.NumberFormat
            .getIntegerInstance(java.util.Locale.getDefault())
            .format(steps.coerceAtLeast(0L))
    }

    private fun getActiveCustomPlanOrNull(): CustomDietPlanLocalModel? {
        val activePlanId = dietPlanStore.getActivePlanId() ?: return null

        return activePlanId
            .takeIf { it.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX) }
            ?.let { customDietPlanStore.getPlan(it) }
    }

    private fun resolveDailyCalorieTarget(
        profile: UserProfileEntity?
    ): Int {
        val activeCustomPlanCalories = getActiveCustomPlanOrNull()
            ?.dailyCalories
            ?.takeIf { it > 0 }

        return activeCustomPlanCalories
            ?: profile?.dailyCalorieTarget?.takeIf { it > 0 }
            ?: nutritionTargetStore.getDailyTargetCalories().takeIf { it > 0 }
            ?: DEFAULT_DAILY_CALORIE_TARGET
    }

    private fun resolveProteinTarget(
        profile: UserProfileEntity?
    ): Int {
        val activeCustomCalories = getActiveCustomPlanOrNull()?.dailyCalories

        return activeCustomCalories
            ?.let { calories -> ((calories * 0.25) / 4.0).roundToInt().coerceAtLeast(40) }
            ?: profile?.proteinTargetGrams?.takeIf { it > 0 }
            ?: DEFAULT_PROTEIN_TARGET_GRAMS
    }

    private fun resolveCarbTarget(
        profile: UserProfileEntity?
    ): Int {
        val activeCustomCalories = getActiveCustomPlanOrNull()?.dailyCalories

        return activeCustomCalories
            ?.let { calories -> ((calories * 0.45) / 4.0).roundToInt().coerceAtLeast(80) }
            ?: profile?.carbTargetGrams?.takeIf { it > 0 }
            ?: DEFAULT_CARB_TARGET_GRAMS
    }

    private fun resolveFatTarget(
        profile: UserProfileEntity?
    ): Int {
        val activeCustomCalories = getActiveCustomPlanOrNull()?.dailyCalories

        return activeCustomCalories
            ?.let { calories -> ((calories * 0.30) / 9.0).roundToInt().coerceAtLeast(25) }
            ?: profile?.fatTargetGrams?.takeIf { it > 0 }
            ?: DEFAULT_FAT_TARGET_GRAMS
    }

    private fun progressPercent(
        current: Double,
        target: Double
    ): Int {
        if (target <= 0.0) return 0

        return ((current / target) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }
    private fun formatGram(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
    private fun setupClicks() = with(binding) {
        dailyQuickAddFab.setOnClickListener {
            DailyQuickAddBottomSheet().show(
                childFragmentManager,
                DailyQuickAddBottomSheet.TAG
            )
        }

        dietPlansButton.setOnClickListener {
            openDietPlans()
        }

        applyTodayPlanButton.setOnClickListener {
            applyActivePlanTodayOrOpenList()
        }

        activePlanCard.setOnClickListener {
            val activePlan = currentActivePlan
            if (activePlan == null) {
                openDietPlans()
            } else {
                openActivePlanDetail(activePlan.id)
            }
        }

        applyPlannedMealButton.setOnClickListener {
            applyNextPlannedMeal()
        }

        breakfastMealRow.setOnClickListener {
            openMealDetail(MealType.BREAKFAST)
        }

        snackMealRow.setOnClickListener {
            openMealDetail(MealType.SNACK)
        }

        lunchMealRow.setOnClickListener {
            openMealDetail(MealType.LUNCH)
        }

        dinnerMealRow.setOnClickListener {
            openMealDetail(MealType.DINNER)
        }

        breakfastAddButton.setOnClickListener {
            openAddMeal(MealType.BREAKFAST)
        }

        snackAddButton.setOnClickListener {
            openAddMeal(MealType.SNACK)
        }

        lunchAddButton.setOnClickListener {
            openAddMeal(MealType.LUNCH)
        }

        dinnerAddButton.setOnClickListener {
            openAddMeal(MealType.DINNER)
        }

        addWater250Button.setOnClickListener {
            addWater(WaterTrackingStore.BOTTLE_SIZE_ML)
        }

        addWater500Button.setOnClickListener {
            addWater(WaterTrackingStore.BOTTLE_SIZE_ML * 2)
        }
        addWaterBottleSlotButton.setOnClickListener {
            val defaultBottleCount = (waterGoalMl / WaterTrackingStore.BOTTLE_SIZE_ML)
                .coerceAtLeast(WaterTrackingStore.MIN_BOTTLE_COUNT)
                .coerceAtMost(WaterTrackingStore.MAX_BOTTLE_COUNT)

            waterTrackingStore.increaseVisibleBottleCount(
                date = selectedDate,
                defaultCount = defaultBottleCount,
                amount = 4
            )

            renderWaterTracking(scrollToEnd = true)
        }
        activitySummaryCard.setOnClickListener {
            openAddExercise()
        }

        activitySummaryCard.setOnLongClickListener {
            showHealthConnectBottomSheet()
            true
        }

        activityBurnedCaloriesText.setOnClickListener {
            showHealthConnectBottomSheet()
        }

        activityDurationText.setOnClickListener {
            showHealthConnectBottomSheet()
        }


    }

    private fun applyActivePlanTodayOrOpenList() {
        val activePlan = currentActivePlan
        if (activePlan == null) {
            openDietPlans()
            return
        }

        dietPlanStore.applyActivePlanToday()
        Toast.makeText(
            requireContext(),
            getString(R.string.daily_active_plan_applied_today, activePlan.title),
            Toast.LENGTH_SHORT
        ).show()

        renderActivePlan()
        renderMealTotals()
    }

    private fun applyNextPlannedMeal() {
        val dailyPlan = currentDailyMealPlan ?: return
        val nextMeal = dailyPlan.nextMeal ?: return

        binding.applyPlannedMealButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    mealRepository.addPlannedMealEntry(
                        mealType = nextMeal.mealType,
                        title = nextMeal.title,
                        targetCalories = dailyPlan.targetCalories,
                        plannedMealCalories = nextMeal.scaledCalories(dailyPlan.targetCalories),
                        suggestedFoodNames = nextMeal.foods,
                        planId = dailyPlan.planId,
                        plannedMealId = nextMeal.id,
                        entryDate = selectedDateIso
                    )
                }
            }.onSuccess {
                dietPlanStore.markPlannedMealCompleted(
                    planId = dailyPlan.planId,
                    dayNumber = dailyPlan.dayNumber,
                    plannedMealId = nextMeal.id
                )

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.daily_planned_meal_applied_message,
                        getString(nextMeal.mealType.titleRes)
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                renderMealTotals()
                renderActivePlan()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_planned_meal_save_error_message),
                    Toast.LENGTH_SHORT
                ).show()
            }

            if (_binding != null) {
                binding.applyPlannedMealButton.isEnabled = true
            }
        }
    }

    private fun markNextPlannedMealCompletedIfMatches(mealType: MealType) {
        val dailyPlan = currentDailyMealPlan ?: return
        val nextMeal = dailyPlan.nextMeal ?: return
        if (nextMeal.mealType != mealType) return

        dietPlanStore.markPlannedMealCompleted(
            planId = dailyPlan.planId,
            dayNumber = dailyPlan.dayNumber,
            plannedMealId = nextMeal.id
        )
        renderActivePlan()
    }

    fun openDietPlans() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, DietPlansFragment())
            .addToBackStack("diet_plans")
            .commit()
    }

    private fun openActivePlanDetail(planId: String) {
        if (planId.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)) {
            openDietPlans()
            return
        }
        val fragment = when (planId) {
            DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> IntermittentFastingDetailFragment()
            else -> DietPlanDetailFragment.newInstance(planId)
        }

        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, fragment)
            .addToBackStack("active_diet_plan_detail")
            .commit()
    }

    private fun openMealDetail(mealType: MealType) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, MealDetailFragment.newInstance(mealType))
            .addToBackStack(MealDetailFragment.BACK_STACK_NAME)
            .commit()
    }


    fun openReminderTarget(
        categoryName: String?,
        reminderId: String?
    ) {
        val category = runCatching {
            ReminderCategory.valueOf(categoryName.orEmpty())
        }.getOrNull()

        when (category) {
            ReminderCategory.MEAL -> openAddMeal(mealTypeForReminder(reminderId))
            ReminderCategory.WATER -> {
                scrollToDailyCard(binding.waterTrackingCard)
                Toast.makeText(requireContext(), R.string.daily_reminder_water_focus_message, Toast.LENGTH_SHORT).show()
            }
            ReminderCategory.EXERCISE -> {
                scrollToDailyCard(binding.activitySummaryCard)
                Toast.makeText(requireContext(), R.string.daily_reminder_exercise_focus_message, Toast.LENGTH_SHORT).show()
            }
            else -> scrollToDailyCard(binding.dailySummaryCard)
        }
    }

    private fun mealTypeForReminder(reminderId: String?): MealType {
        return when (reminderId) {
            "lunch" -> MealType.LUNCH
            "dinner" -> MealType.DINNER
            "snack" -> MealType.SNACK
            else -> MealType.BREAKFAST
        }
    }

    private fun scrollToDailyCard(target: View) {
        binding.dailyScrollView.post {
            val top = (target.top - 18.dp).coerceAtLeast(0)
            binding.dailyScrollView.smoothScrollTo(0, top)
            target.animate()
                .scaleX(1.015f)
                .scaleY(1.015f)
                .setDuration(120L)
                .withEndAction {
                    target.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140L)
                        .start()
                }
                .start()
        }
    }

    fun openAddMeal(mealType: MealType = MealType.BREAKFAST) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                AddMealFragment.newInstance(
                    mealType = mealType,
                    entryDate = selectedDate.toString()
                )
            )
            .addToBackStack(AddMealFragment.BACK_STACK_NAME)
            .commit()
    }
    private fun renderActivitySourceStatus(
        healthActiveCalories: Int,
        manualExerciseCalories: Int
    ) = with(binding) {
        val hasHealthPermission =
            healthConnectSummary.isAvailable && healthConnectSummary.hasPermission

        val hasHealthData =
            hasHealthPermission &&
                    (healthConnectSummary.steps > 0L || healthActiveCalories > 0)

        val hasLocalSteps = localStepCount > 0L
        val hasManualExercise = manualExerciseCalories > 0

        val textRes = when {
            hasHealthData -> R.string.daily_activity_source_health_connect
            hasLocalSteps -> R.string.daily_activity_source_fitto_counter
            hasManualExercise -> R.string.daily_activity_source_manual_only
            else -> R.string.daily_activity_source_connect
        }

        activitySyncStatusText.text = getString(textRes)

        activitySyncStatusText.setOnClickListener {
            showHealthConnectBottomSheet()
        }
    }
    fun openAddExercise() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                AddExerciseFragment.newInstance(
                    entryDate = selectedDate.toString()
                )
            )
            .addToBackStack("add_exercise")
            .commit()
    }
    fun openCameraFoodEntry(mealType: MealType = MealType.BREAKFAST) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                CameraFoodEntryFragment.newInstance(
                    mealType = mealType
                )
            )
            .addToBackStack(CameraFoodEntryFragment.BACK_STACK_NAME)
            .commit()
    }

    fun addQuickWater() {
        addWater(WaterTrackingStore.BOTTLE_SIZE_ML)
    }


    override fun onDestroyView() {
        if (::stepCounterManager.isInitialized) {
            stepCounterManager.stop()
        }

        stopFastingTimer()
        stopHourglassAnimation()
        super.onDestroyView()
        _binding = null
    }
    private data class DailyAiSnapshot(
        val dateIso: String,
        val userName: String,
        val goalType: String,
        val dailyCalorieTarget: Int,
        val totalCalories: Int,
        val proteinGrams: Double,
        val proteinTargetGrams: Int,
        val carbGrams: Double,
        val fatGrams: Double,
        val waterMl: Int,
        val waterGoalMl: Int,
        val triggerType: AiDailyTriggerType,
        val meals: List<DailyAiMealSnapshot>,
        val exerciseLogCount: Int,
        val exerciseDurationMinutes: Int,
        val exerciseCalories: Int,
        val exerciseSummaryText: String,
        val dietPreferences: String,
        val allergies: String,
        val dislikedFoods: String,
        val mealRoutine: String
    )

    private data class DailyAiMealSnapshot(
        val mealTypeLabel: String,
        val totalCalories: Int,
        val proteinGrams: Double,
        val carbGrams: Double,
        val fatGrams: Double,
        val entryCount: Int,
        val foodSummary: String
    )

    companion object {
        private const val DEFAULT_DAILY_CALORIE_TARGET = 1800
        private const val DEFAULT_PROTEIN_TARGET_GRAMS = 120
        private const val DEFAULT_CARB_TARGET_GRAMS = 280
        private const val DEFAULT_FAT_TARGET_GRAMS = 70
        private const val DAILY_AI_MEAL_SUMMARY_MAX_CHARS = 360
        private const val DAILY_AI_RATE_LIMIT_COOLDOWN_MS = 60_000L
        private const val FITTO_AI_LOG_TAG = "FittoAI"
    }
}