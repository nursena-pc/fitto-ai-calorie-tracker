package com.example.fitto.feature_daily

import android.content.Context
import android.os.Build
import com.example.fitto.R
import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.AiDailyPromptBuilder
import com.example.fitto.feature_daily.ai.GeminiDailyAiRepository
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher

object DailyAiAnalysisActionFactory {

    fun create(
        context: Context,
        ioDispatcher: CoroutineDispatcher
    ): DailyAiAnalysisAction {
        val appContext = context.applicationContext
        val repository = GeminiDailyAiRepository(ioDispatcher = ioDispatcher)
        val labels = DailyAiStructuredMessageBuilder.Labels(
            mealComments = appContext.getString(R.string.daily_ai_meal_comments_label),
            unknownEntry = appContext.getString(R.string.daily_ai_entry_unknown),
            water = appContext.getString(R.string.daily_ai_water_label),
            exercise = appContext.getString(R.string.daily_ai_exercise_label),
            generalComment = appContext.getString(R.string.daily_ai_general_comment_label),
            suggestion = appContext.getString(R.string.daily_ai_suggestion_label)
        )

        return DailyAiAnalysisAction(
            cacheStore = AndroidDailyAiCacheStore(AiDailyAnalysisCache(appContext)),
            requestDailyAnalysis = repository::requestDailyAnalysis,
            buildPrompt = { snapshot, language ->
                AiDailyPromptBuilder.build(
                    DailyAiPromptDataMapper.toPromptData(
                        snapshot = snapshot,
                        appLanguage = language,
                        goalLabel = appContext.getString(
                            DailyAiGoalTextResolver.resolveLabelRes(snapshot.goalType)
                        ),
                        goalGuidance = DailyAiGoalTextResolver.resolveGuidance(
                            snapshot.goalType,
                            language
                        )
                    )
                )
            },
            currentLanguage = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appContext.resources.configuration.locales[0]?.language
                        ?: Locale.getDefault().language
                } else {
                    @Suppress("DEPRECATION")
                    appContext.resources.configuration.locale.language
                        ?: Locale.getDefault().language
                }
            },
            buildStructuredMessage = { analysis ->
                DailyAiStructuredMessageBuilder.build(analysis, labels)
            },
            resolveLocalMessage = { snapshot ->
                appContext.getString(
                    DailyAiMessageTextResolver.resolveLocalMessageRes(snapshot.triggerType)
                )
            },
            resolveCacheLocalMessages = { snapshot ->
                DailyAiMessageTextResolver
                    .resolveCacheLocalMessageResList(snapshot.triggerType)
                    .map { resId -> appContext.getString(resId) }
            }
        )
    }
}
