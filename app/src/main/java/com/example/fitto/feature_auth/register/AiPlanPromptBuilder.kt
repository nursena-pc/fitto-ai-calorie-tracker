package com.example.fitto.feature_auth.register

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object AiPlanPromptBuilder {

    fun build(
        state: OnboardingState,
        localResult: DailyNeedsResult,
        appLanguage: String,
        nutritionContext: OnboardingNutritionPromptContext = OnboardingNutritionPromptContext.EMPTY
    ): String {
        val isEnglish = appLanguage.startsWith("en", ignoreCase = true)
        return if (isEnglish) buildEnglishPrompt(state, localResult, nutritionContext) else buildTurkishPrompt(state, localResult, nutritionContext)
    }

    private fun buildTurkishPrompt(
        state: OnboardingState,
        localResult: DailyNeedsResult,
        nutritionContext: OnboardingNutritionPromptContext
    ): String {
        return """
Sen Fitto uygulamasının onboarding plan asistanısın.
Cevabı Türkçe üret.
Tıbbi teşhis koyma ve kesin sağlık iddiası üretme.
Çok düşük kalori veya çok agresif haftalık kilo değişimi önerme.
Kullanıcının hedefini, hedef tarihini, aktivite seviyesini, motivasyonunu ve yerel Fitto hesaplamasını birlikte değerlendir.
Hedefe göre yorum mantığı:
${goalGuidance(state.goalType, isEnglish = false)}
Beslenme tercihi kuralları:
${nutritionGuidance(isEnglish = false)}
Cevap kısa, motive edici ve güvenli olsun.
Sadece geçerli JSON döndür; Markdown veya açıklama ekleme.

Kullanıcı bilgileri:
${commonBlock(state, localResult, nutritionContext, isEnglish = false)}

Beklenen JSON:
{
  "planStatus": "SAFE",
  "recommendedDailyCalories": 1800,
  "recommendedWeeklyChangeKg": 0.5,
  "targetComment": "Bu hedef başlangıç için sürdürülebilir görünüyor.",
  "timelineComment": "Seçtiğin tarihe göre bu ilerleme dengeli olabilir.",
  "calorieComment": "Günlük hedef, bakım kalorine göre makul bir aralıkta tutuldu.",
  "motivationComment": "Daha küçük ara hedeflerle ilerlemek motivasyonunu korumana yardımcı olabilir.",
  "summary": "Bu plan hedefinle uyumlu ve sürdürülebilir bir başlangıç sunar.",
  "warning": "Bu öneri tıbbi tavsiye değildir."
}

planStatus yalnızca SAFE, CAUTION, AGGRESSIVE veya NOT_RECOMMENDED olabilir.
""".trimIndent()
    }

    private fun buildEnglishPrompt(
        state: OnboardingState,
        localResult: DailyNeedsResult,
        nutritionContext: OnboardingNutritionPromptContext
    ): String {
        return """
You are Fitto's onboarding plan assistant.
Generate the answer in English.
Do not diagnose or make certain health claims.
Do not recommend very low calories or overly aggressive weekly weight changes.
Consider the user's goal, target date, activity level, motivation, and the local Fitto calculation together.
Goal-specific guidance:
${goalGuidance(state.goalType, isEnglish = true)}
Nutrition preference rules:
${nutritionGuidance(isEnglish = true)}
Keep the response short, motivating, and safe.
Return only valid JSON. Do not include Markdown or extra explanation.

User information:
${commonBlock(state, localResult, nutritionContext, isEnglish = true)}

Expected JSON:
{
  "planStatus": "SAFE",
  "recommendedDailyCalories": 1800,
  "recommendedWeeklyChangeKg": 0.5,
  "targetComment": "This target looks sustainable as a starting point.",
  "timelineComment": "Based on the date you selected, this pace can be balanced.",
  "calorieComment": "The daily target stays in a reasonable range around maintenance calories.",
  "motivationComment": "Smaller intermediate steps can help protect your motivation.",
  "summary": "This plan provides a sustainable starting point for your goal.",
  "warning": "This is not medical advice."
}

planStatus must be only SAFE, CAUTION, AGGRESSIVE, or NOT_RECOMMENDED.
""".trimIndent()
    }

    private fun commonBlock(
        state: OnboardingState,
        localResult: DailyNeedsResult,
        nutritionContext: OnboardingNutritionPromptContext,
        isEnglish: Boolean
    ): String {
        return buildString {
            appendLine(label("Ad", "Name", isEnglish) + ": ${state.name.trim().ifBlank { label("Fitto kullanıcısı", "Fitto user", isEnglish) }}")
            appendLine(label("Yaş", "Age", isEnglish) + ": ${state.age.ifUnknown(isEnglish)}")
            appendLine(label("Cinsiyet", "Gender", isEnglish) + ": ${state.gender.toLabel(isEnglish)}")
            appendLine(label("Boy", "Height", isEnglish) + ": ${state.height.ifUnknown(isEnglish)} cm")
            appendLine(label("Mevcut kilo", "Current weight", isEnglish) + ": ${state.weight.ifUnknown(isEnglish)} kg")
            appendLine(label("Mevcut BMI", "Current BMI", isEnglish) + ": ${state.currentBmiLabel(isEnglish)}")
            appendLine(label("Hedef tipi", "Goal type", isEnglish) + ": ${state.goalType?.name.orEmpty()}")
            appendLine(label("Hedef açık adı", "Goal label", isEnglish) + ": ${goalLabel(state.goalType, isEnglish)}")
            appendLine(label("Hedef kilo", "Target weight", isEnglish) + ": ${state.targetWeight.takeIf { it > 0 }?.let { "$it kg" } ?: label("Yok", "None", isEnglish)}")
            appendLine(label("Aktivite seviyesi", "Activity level", isEnglish) + ": ${state.activityLevel.toLabel(isEnglish)}")
            appendLine(label("Hedef motivasyonu", "Goal motivation", isEnglish) + ": ${state.weightLossReason.toLabel(isEnglish)}")
            appendLine(label("Hedef tarih / özel gün", "Target date / special day", isEnglish) + ": ${state.eventTimeMillis.toDateLabel(isEnglish)}")
            appendLine(label("Zorluklar", "Barriers", isEnglish) + ": ${state.barriers.joinToString { it.name }.ifBlank { label("Seçilmedi", "Not selected", isEnglish) }}")
            appendLine(nutritionPreferencesBlock(nutritionContext, isEnglish))
            appendLine(label("Yerel bakım kalorisi", "Local maintenance calories", isEnglish) + ": ${localResult.maintenanceCalories} kcal")
            appendLine(label("Yerel günlük hedef", "Local daily calorie target", isEnglish) + ": ${localResult.appliedCalories} kcal")
            appendLine(label("Yerel hedef kalori", "Local target calories", isEnglish) + ": ${localResult.targetCalories} kcal")
            appendLine(label("Yerel haftalık değişim", "Local weekly change", isEnglish) + ": ${localResult.weeklyWeightChangeKg} kg")
            appendLine(label("Yerel plan durumu", "Local plan status", isEnglish) + ": ${localResult.status.name}")
        }.trim()
    }

    private fun nutritionPreferencesBlock(
        nutritionContext: OnboardingNutritionPromptContext,
        isEnglish: Boolean
    ): String {
        val notSpecified = label("Belirtilmemiş", "Not specified", isEnglish)
        val diet = nutritionContext.dietPreferences.trim().ifBlank { notSpecified }
        val allergies = nutritionContext.allergies.trim().ifBlank { notSpecified }
        val dislikedFoods = nutritionContext.dislikedFoods.trim().ifBlank { notSpecified }
        val mealRoutine = nutritionContext.mealRoutine.trim().ifBlank { notSpecified }

        return if (isEnglish) {
            """
Nutrition preferences:
- Diet type: $diet
- Allergies: $allergies
- Disliked foods: $dislikedFoods
- Meal routine: $mealRoutine
""".trimIndent()
        } else {
            """
Beslenme tercihleri:
- Diyet tipi: $diet
- Alerjiler: $allergies
- Sevmediği yiyecekler: $dislikedFoods
- Öğün düzeni: $mealRoutine
""".trimIndent()
        }
    }

    private fun nutritionGuidance(isEnglish: Boolean): String {
        return if (isEnglish) {
            """
- If the user is vegan, do not suggest animal products.
- If the user is vegetarian, do not suggest meat, chicken, or fish.
- If the user is gluten-free, avoid suggestions that may contain gluten.
- If the user is lactose-free, do not suggest milk, yogurt, or cheese.
- Do not suggest foods listed under allergies.
- Do not suggest foods listed under disliked foods.
- If unsure, use general phrases such as "a protein source that fits your preferences".
""".trimIndent()
        } else {
            """
- Kullanıcı vegan ise hayvansal ürün önerme.
- Kullanıcı vejetaryen ise et, tavuk veya balık önerme.
- Kullanıcı glutensiz ise gluten içerebilecek önerilerden kaçın.
- Kullanıcı laktozsuz ise süt, yoğurt veya peynir önerme.
- Alerjiler listesindeki yiyecekleri önerme.
- Sevmediği yiyecekleri önerme.
- Emin değilsen "tercihine uygun protein kaynağı" gibi genel ifade kullan.
""".trimIndent()
        }
    }

    private fun goalGuidance(goalType: GoalType?, isEnglish: Boolean): String {
        return if (isEnglish) {
            when (goalType) {
                GoalType.LOSE_WEIGHT -> "Weight loss: emphasize a sustainable calorie deficit, filling meals, and avoiding overly low targets."
                GoalType.GAIN_WEIGHT -> "Weight gain: emphasize enough energy, protein support, and avoiding overly rapid gain."
                GoalType.MAINTAIN_WEIGHT -> "Maintenance: emphasize maintenance calories, balance, and sustainable habits."
                GoalType.LEARN_NUTRITION -> "Learn nutrition: focus on awareness, macro/meal balance, and educational explanations without over-focusing on calories."
                GoalType.EAT_HEALTHY -> "Eat healthier: emphasize variety, water, movement, balanced meals, and sustainability."
                null -> "Unknown goal: do not assume a special goal."
            }
        } else {
            when (goalType) {
                GoalType.LOSE_WEIGHT -> "Kilo vermek: sürdürülebilir kalori açığı, tok tutan öğünler ve aşırı düşük hedeflerden kaçınma vurgula."
                GoalType.GAIN_WEIGHT -> "Kilo almak: yeterli enerji, protein desteği ve çok hızlı artıştan kaçınma vurgula."
                GoalType.MAINTAIN_WEIGHT -> "Kiloyu korumak: bakım kalorisi, denge ve sürdürülebilir alışkanlık vurgula."
                GoalType.LEARN_NUTRITION -> "Ne yediğimi öğrenmek: kaloriye fazla takılmadan farkındalık, makro/öğün dengesi ve öğretici açıklama sun."
                GoalType.EAT_HEALTHY -> "Sağlıklı beslenmek: çeşitlilik, su, hareket, dengeli öğün ve sürdürülebilirlik vurgula."
                null -> "Hedef bilinmiyor: özel hedef varsayma."
            }
        }
    }

    private fun goalLabel(goalType: GoalType?, isEnglish: Boolean): String {
        return if (isEnglish) {
            when (goalType) {
                GoalType.LOSE_WEIGHT -> "Lose weight"
                GoalType.MAINTAIN_WEIGHT -> "Maintain weight"
                GoalType.GAIN_WEIGHT -> "Gain weight"
                GoalType.LEARN_NUTRITION -> "Learn what I eat"
                GoalType.EAT_HEALTHY -> "Eat healthier"
                null -> "Unknown"
            }
        } else {
            when (goalType) {
                GoalType.LOSE_WEIGHT -> "Kilo vermek"
                GoalType.MAINTAIN_WEIGHT -> "Kiloyu korumak"
                GoalType.GAIN_WEIGHT -> "Kilo almak"
                GoalType.LEARN_NUTRITION -> "Ne yediğimi öğrenmek"
                GoalType.EAT_HEALTHY -> "Sağlıklı beslenmek"
                null -> "Bilinmiyor"
            }
        }
    }

    private fun label(tr: String, en: String, isEnglish: Boolean): String = if (isEnglish) en else tr

    private fun Int.ifUnknown(isEnglish: Boolean): String = if (this > 0) toString() else label("Bilinmiyor", "Unknown", isEnglish)

    private fun OnboardingState.currentBmiLabel(isEnglish: Boolean): String {
        if (height <= 0 || weight <= 0) return label("Bilinmiyor", "Unknown", isEnglish)
        val heightMeter = height / 100.0
        return String.format(Locale.US, "%.1f", weight / (heightMeter * heightMeter))
    }

    private fun Gender?.toLabel(isEnglish: Boolean): String {
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

    private fun ActivityLevel?.toLabel(isEnglish: Boolean): String {
        return if (isEnglish) {
            when (this) {
                ActivityLevel.LOW -> "Low"
                ActivityLevel.MODERATE -> "Moderate"
                ActivityLevel.HIGH -> "High"
                ActivityLevel.VERY_HIGH -> "Very high"
                null -> "Unknown"
            }
        } else {
            when (this) {
                ActivityLevel.LOW -> "Düşük"
                ActivityLevel.MODERATE -> "Orta"
                ActivityLevel.HIGH -> "Yüksek"
                ActivityLevel.VERY_HIGH -> "Çok yüksek"
                null -> "Bilinmiyor"
            }
        }
    }

    private fun ReasonType?.toLabel(isEnglish: Boolean): String {
        return if (this == null) label("Seçilmedi", "Not selected", isEnglish) else name
    }

    private fun Long?.toDateLabel(isEnglish: Boolean): String {
        if (this == null) return label("Seçilmedi", "Not selected", isEnglish)
        val locale = if (isEnglish) Locale.ENGLISH else Locale("tr", "TR")
        val formatter = SimpleDateFormat(if (isEnglish) "MMM dd, yyyy" else "dd MMM yyyy", locale)
        val remainingDays = TimeUnit.MILLISECONDS.toDays(this - System.currentTimeMillis())
        return formatter.format(Date(this)) + if (remainingDays >= 0) {
            if (isEnglish) " ($remainingDays days left)" else " ($remainingDays gün kaldı)"
        } else {
            if (isEnglish) " (past date)" else " (geçmiş tarih)"
        }
    }
}
