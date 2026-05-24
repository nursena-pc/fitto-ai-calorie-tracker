package com.example.fitto.feature_auth.register

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class OnboardingNutritionPromptContext(
    val dietPreferences: String = "",
    val allergies: String = "",
    val dislikedFoods: String = "",
    val mealRoutine: String = ""
) {
    fun signature(): String {
        return listOf(dietPreferences, allergies, dislikedFoods, mealRoutine)
            .joinToString(separator = "|") { it.trim() }
    }

    companion object {
        val EMPTY = OnboardingNutritionPromptContext()
    }
}

object AiTargetPromptBuilder {

    fun build(
        state: OnboardingState,
        appLanguage: String,
        nutritionContext: OnboardingNutritionPromptContext = OnboardingNutritionPromptContext.EMPTY,
        userEnteredTargetWeightKg: Int? = state.targetWeight.takeIf { it > 0 }
    ): String {
        val language = appLanguage.normalizedPromptLanguage()
        return if (language == PROMPT_LANGUAGE_EN) {
            buildEnglishPrompt(state, nutritionContext, userEnteredTargetWeightKg)
        } else {
            buildTurkishPrompt(state, nutritionContext, userEnteredTargetWeightKg)
        }
    }

    private fun buildTurkishPrompt(
        state: OnboardingState,
        nutritionContext: OnboardingNutritionPromptContext,
        userEnteredTargetWeightKg: Int?
    ): String {
        return """
Sen Fitto uygulamasının hedef belirleme asistanısın.
Cevabı Türkçe üret.
Kullanıcıya adıyla hitap et: ${state.safeName(isEnglish = false)}.
Tıbbi teşhis koyma, kesin sağlık iddiası üretme ve "ideal kilo" ifadesini kullanma.
"Sağlıklı kilon budur" gibi kesin ifadeler kurma.
Kesin hedef değil, sürdürülebilir başlangıç/ara hedef öner.
Kullanıcının yaşını, boyunu, mevcut kilosunu, hedefini, aktivite seviyesini, motivasyonunu ve varsa tarih bilgisini birlikte değerlendir.
Kilo vermek isteyen kullanıcıya mevcut kilodan yüksek hedef önerme.
Kilo almak isteyen kullanıcıya mevcut kilodan düşük hedef önerme.
Hedef tarih varsa çok hızlı değişim gerektiren öneri yapma.
Bilgi eksikse varsayım yapmadan belirt.
Cevap kısa, nazik, destekleyici ve motive edici olsun.
Sadece geçerli JSON döndür; Markdown, açıklama veya kod bloğu ekleme.

Ekran bağlamı:
Kullanıcı hedef kilo ekranında. Fitto AI'dan başlangıç/ara hedef önerisi isteniyor. Kullanıcı inputta hedef yazmışsa bu sadece bağlamdır, zorunlu kabul etme.

Onboarding verileri:
${state.toFullOnboardingPromptBlock(isEnglish = false, nutritionContext = nutritionContext, userEnteredTargetWeightKg = userEnteredTargetWeightKg)}

Beklenen JSON:
{
  "suggestedTargetWeightKg": 64,
  "suggestedTargetRangeMinKg": 63,
  "suggestedTargetRangeMaxKg": 65,
  "title": "Ulaşılabilir ilk hedef",
  "explanation": "Bu hedef mevcut kilona ve seçtiğin amaca göre sürdürülebilir bir başlangıç olabilir.",
  "timelineComment": "Seçtiğin tarihe göre bu hedef daha dengeli bir ilerleme gerektirir.",
  "motivationComment": "Küçük ve takip edilebilir bir ara hedef motivasyonunu korumana yardımcı olabilir.",
  "riskLevel": "SAFE",
  "reasons": ["Mevcut kilona göre aşırı büyük bir değişim gerektirmiyor."],
  "alternativeTargetsKg": [62, 64, 66],
  "warning": "Bu öneri tıbbi tavsiye değildir."
}

riskLevel yalnızca SAFE, CAUTION veya NOT_RECOMMENDED olabilir.
""".trimIndent()
    }

    private fun buildEnglishPrompt(
        state: OnboardingState,
        nutritionContext: OnboardingNutritionPromptContext,
        userEnteredTargetWeightKg: Int?
    ): String {
        return """
You are Fitto's target-weight assistant.
Generate the answer in English.
Address the user by name: ${state.safeName(isEnglish = true)}.
Do not diagnose, make certain health claims, or use the phrase "ideal weight".
Do not say "this is your healthy weight" or make absolute claims.
Suggest a sustainable starting/intermediate target, not a final absolute target.
Consider the user's age, height, current weight, goal, activity level, motivation, and target date if available.
For weight loss, do not suggest a target above current weight.
For weight gain, do not suggest a target below current weight.
If a target date exists, do not suggest a target that requires very rapid change.
If information is missing, say so without guessing.
Keep the response short, gentle, supportive, and motivating.
Return only valid JSON. Do not include Markdown, explanation, or code fences.

Screen context:
The user is on the target-weight screen. Fitto AI is being asked for a starting/intermediate target. If the user already typed a target, treat it only as context.

Onboarding context:
${state.toFullOnboardingPromptBlock(isEnglish = true, nutritionContext = nutritionContext, userEnteredTargetWeightKg = userEnteredTargetWeightKg)}

Expected JSON:
{
  "suggestedTargetWeightKg": 64,
  "suggestedTargetRangeMinKg": 63,
  "suggestedTargetRangeMaxKg": 65,
  "title": "Achievable first target",
  "explanation": "This can be a sustainable starting target for your current weight and goal.",
  "timelineComment": "Based on the date you selected, this would require steady progress.",
  "motivationComment": "A smaller, trackable intermediate target can help protect your motivation.",
  "riskLevel": "SAFE",
  "reasons": ["It does not require an extreme change from your current weight."],
  "alternativeTargetsKg": [62, 64, 66],
  "warning": "This is not medical advice."
}

riskLevel must be only SAFE, CAUTION, or NOT_RECOMMENDED.
""".trimIndent()
    }

    private fun OnboardingState.toFullOnboardingPromptBlock(
        isEnglish: Boolean,
        nutritionContext: OnboardingNutritionPromptContext,
        userEnteredTargetWeightKg: Int?
    ): String {
        return buildString {
            appendLine(label("Ad", "Name", isEnglish) + ": ${safeName(isEnglish)}")
            appendLine(label("Yaş", "Age", isEnglish) + ": ${age.ifZeroUnknown(unit = null, isEnglish = isEnglish)}")
            appendLine(label("Cinsiyet", "Gender", isEnglish) + ": ${gender.toPromptLabel(isEnglish)}")
            appendLine(label("Boy", "Height", isEnglish) + ": ${height.ifZeroUnknown(unit = "cm", isEnglish = isEnglish)}")
            appendLine(label("Mevcut kilo", "Current weight", isEnglish) + ": ${weight.ifZeroUnknown(unit = "kg", isEnglish = isEnglish)}")
            appendLine(label("Mevcut BMI", "Current BMI", isEnglish) + ": ${currentBmiLabel(isEnglish)}")
            appendLine(label("Aktivite seviyesi", "Activity level", isEnglish) + ": ${activityLevel.toPromptLabel(isEnglish)}")
            appendLine(label("Hedef tipi", "Goal type", isEnglish) + ": ${goalType?.name ?: label("Bilinmiyor", "Unknown", isEnglish)}")
            appendLine(label("Hedef açık adı", "Goal label", isEnglish) + ": ${goalType.toPromptLabel(isEnglish)}")
            appendLine(label("Kullanıcının girdiği hedef kilo", "User-entered target weight", isEnglish) + ": ${userEnteredTargetWeightKg?.let { "$it kg" } ?: label("Yok", "None", isEnglish)}")
            appendLine(label("Hedef motivasyonu", "Goal motivation", isEnglish) + ": ${weightLossReason.toPromptLabel(isEnglish)}")
            appendLine(label("Hedef tarih / özel gün", "Target date / special day", isEnglish) + ": ${eventTimeMillis.toPromptDateLabel(isEnglish)}")
            appendLine(label("Geçmişte zorlayan durumlar", "Past barriers", isEnglish) + ": ${barriers.toPromptLabel(otherBarrierText, isEnglish)}")
            appendLine(label("Günlük ihtiyaç verileri", "Daily need values", isEnglish) + ": ${label("Bu ekrandan önce hesaplanmadı / bilinmiyor", "Not calculated before this screen / unknown", isEnglish)}")
            appendLine(label("Beslenme tercihleri", "Nutrition preferences", isEnglish) + ":")
            appendLine("- " + label("Diyet tipi", "Diet type", isEnglish) + ": ${nutritionContext.dietPreferences.ifBlank { label("Bilinmiyor", "Unknown", isEnglish) }}")
            appendLine("- " + label("Alerjiler", "Allergies", isEnglish) + ": ${nutritionContext.allergies.ifBlank { label("Bilinmiyor", "Unknown", isEnglish) }}")
            appendLine("- " + label("Sevmediği yiyecekler", "Disliked foods", isEnglish) + ": ${nutritionContext.dislikedFoods.ifBlank { label("Bilinmiyor", "Unknown", isEnglish) }}")
            appendLine("- " + label("Öğün düzeni", "Meal routine", isEnglish) + ": ${nutritionContext.mealRoutine.ifBlank { label("Bilinmiyor", "Unknown", isEnglish) }}")
            appendLine(label("Geçmiş kilo verisi", "Weight history", isEnglish) + ": ${label("Bu ekranda mevcut değil / bilinmiyor", "Not available on this screen / unknown", isEnglish)}")
            appendLine(label("Beden ölçüleri", "Body measurements", isEnglish) + ": ${label("Bu ekranda mevcut değil / bilinmiyor", "Not available on this screen / unknown", isEnglish)}")
        }.trim()
    }

    private fun label(turkish: String, english: String, isEnglish: Boolean): String {
        return if (isEnglish) english else turkish
    }

    private fun OnboardingState.safeName(isEnglish: Boolean): String {
        return name.trim().ifBlank { if (isEnglish) "Fitto user" else "Fitto kullanıcısı" }
    }

    private fun OnboardingState.currentBmiLabel(isEnglish: Boolean): String {
        if (height <= 0 || weight <= 0) return if (isEnglish) "Unknown" else "Bilinmiyor"
        val heightMeter = height / 100.0
        val bmi = weight / (heightMeter * heightMeter)
        return String.format(Locale.US, "%.1f", bmi)
    }

    private fun Int.ifZeroUnknown(
        unit: String? = null,
        isEnglish: Boolean
    ): String {
        if (this <= 0) return if (isEnglish) "Unknown" else "Bilinmiyor"
        return if (unit == null) toString() else "$this $unit"
    }

    private fun Gender?.toPromptLabel(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                Gender.FEMALE -> "Female"
                Gender.MALE -> "Male"
                Gender.PREFER_NOT_TO_SAY -> "Prefers not to say"
                null -> "Unknown"
            }
        } else {
            when (this) {
                Gender.FEMALE -> "Kadın"
                Gender.MALE -> "Erkek"
                Gender.PREFER_NOT_TO_SAY -> "Belirtmek istemiyor"
                null -> "Bilinmiyor"
            }
        }
    }

    fun GoalType?.toPromptLabel(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                GoalType.LOSE_WEIGHT -> "Lose weight"
                GoalType.MAINTAIN_WEIGHT -> "Maintain weight"
                GoalType.GAIN_WEIGHT -> "Gain weight"
                GoalType.LEARN_NUTRITION -> "Learn what I eat"
                GoalType.EAT_HEALTHY -> "Eat healthier"
                null -> "Unknown"
            }
        } else {
            when (this) {
                GoalType.LOSE_WEIGHT -> "Kilo vermek"
                GoalType.MAINTAIN_WEIGHT -> "Kiloyu korumak"
                GoalType.GAIN_WEIGHT -> "Kilo almak"
                GoalType.LEARN_NUTRITION -> "Ne yediğimi öğrenmek"
                GoalType.EAT_HEALTHY -> "Sağlıklı beslenmek"
                null -> "Bilinmiyor"
            }
        }
    }

    private fun List<BarrierType>.toPromptLabel(
        otherBarrierText: String?,
        isEnglish: Boolean
    ): String {
        if (isEmpty()) return if (isEnglish) "Not selected" else "Seçilmedi"

        val labels = map { it.toPromptLabel(isEnglish) }.toMutableList()
        val otherText = otherBarrierText.orEmpty().trim()
        if (any { it == BarrierType.OTHER } && otherText.isNotBlank()) {
            val otherLabel = BarrierType.OTHER.toPromptLabel(isEnglish)
            labels[labels.indexOf(otherLabel)] = if (isEnglish) "Other: $otherText" else "Diğer: $otherText"
        }

        return labels.joinToString(separator = ", ")
    }

    private fun BarrierType.toPromptLabel(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                BarrierType.SUPPORT -> "Lack of support"
                BarrierType.BODY_CONCERN -> "Body concerns"
                BarrierType.FAMILY_DIET -> "Family/partner eating routine"
                BarrierType.TIME -> "Not enough time"
                BarrierType.MOTIVATION -> "Difficulty maintaining motivation"
                BarrierType.EMOTIONAL_EATING -> "Emotional eating"
                BarrierType.MEAL_PLANNING -> "Difficulty with meal planning"
                BarrierType.HEALTH -> "Health or medical concerns"
                BarrierType.KNOWLEDGE -> "Lack of knowledge"
                BarrierType.OTHER -> "Other"
            }
        } else {
            when (this) {
                BarrierType.SUPPORT -> "Destek eksikliği"
                BarrierType.BODY_CONCERN -> "Vücut kaygısı"
                BarrierType.FAMILY_DIET -> "Aile/eş beslenme düzeni"
                BarrierType.TIME -> "Yeterli zaman olmaması"
                BarrierType.MOTIVATION -> "Motivasyonu korumakta zorlanma"
                BarrierType.EMOTIONAL_EATING -> "Duygusal yeme"
                BarrierType.MEAL_PLANNING -> "Yemek planlamada zorlanma"
                BarrierType.HEALTH -> "Sağlık veya tıbbi problemler"
                BarrierType.KNOWLEDGE -> "Bilgi eksikliği"
                BarrierType.OTHER -> "Diğer"
            }
        }
    }

    private fun ReasonType?.toPromptLabel(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                ReasonType.WEDDING -> "Upcoming wedding"
                ReasonType.GRADUATION -> "Graduation"
                ReasonType.HOLIDAY -> "Vacation"
                ReasonType.LOOK_FITTER -> "Looking fitter"
                ReasonType.IMPROVE_HEALTH -> "Improving health"
                ReasonType.MORE_ENERGY -> "Increasing energy"
                ReasonType.CLOTHES -> "Feeling more comfortable in clothes"
                ReasonType.DOCTOR -> "Doctor recommendation"
                ReasonType.FEEL_BETTER -> "Feeling better"
                ReasonType.OTHER -> "Other"
                null -> "Not selected"
            }
        } else {
            when (this) {
                ReasonType.WEDDING -> "Yaklaşan düğün"
                ReasonType.GRADUATION -> "Mezuniyet"
                ReasonType.HOLIDAY -> "Tatil"
                ReasonType.LOOK_FITTER -> "Daha fit görünmek"
                ReasonType.IMPROVE_HEALTH -> "Sağlığını iyileştirmek"
                ReasonType.MORE_ENERGY -> "Enerjiyi artırmak"
                ReasonType.CLOTHES -> "Kıyafetlerde daha rahat hissetmek"
                ReasonType.DOCTOR -> "Doktor önerisi"
                ReasonType.FEEL_BETTER -> "Kendini daha iyi hissetmek"
                ReasonType.OTHER -> "Diğer"
                null -> "Seçilmedi"
            }
        }
    }

    private fun Long?.toPromptDateLabel(isEnglish: Boolean): String {
        if (this == null) return if (isEnglish) "Not selected" else "Seçilmedi"

        val locale = if (isEnglish) Locale.ENGLISH else Locale("tr", "TR")
        val formatter = SimpleDateFormat(if (isEnglish) "MMM dd, yyyy" else "dd MMM yyyy", locale)
        val dateText = formatter.format(Date(this))
        val remainingDays = TimeUnit.MILLISECONDS.toDays(this - System.currentTimeMillis())

        return if (remainingDays >= 0) {
            if (isEnglish) "$dateText ($remainingDays days left)" else "$dateText ($remainingDays gün kaldı)"
        } else {
            if (isEnglish) "$dateText (past date)" else "$dateText (geçmiş tarih)"
        }
    }

    private fun ActivityLevel?.toPromptLabel(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                ActivityLevel.LOW -> "Low"
                ActivityLevel.MODERATE -> "Moderate"
                ActivityLevel.HIGH -> "High"
                ActivityLevel.VERY_HIGH -> "Very high"
                null -> "Not selected yet"
            }
        } else {
            when (this) {
                ActivityLevel.LOW -> "Düşük"
                ActivityLevel.MODERATE -> "Orta"
                ActivityLevel.HIGH -> "Yüksek"
                ActivityLevel.VERY_HIGH -> "Çok yüksek"
                null -> "Henüz seçilmedi"
            }
        }
    }

    private fun String.normalizedPromptLanguage(): String {
        return if (startsWith(PROMPT_LANGUAGE_EN, ignoreCase = true)) {
            PROMPT_LANGUAGE_EN
        } else {
            PROMPT_LANGUAGE_TR
        }
    }

    private const val PROMPT_LANGUAGE_TR = "tr"
    private const val PROMPT_LANGUAGE_EN = "en"
}
