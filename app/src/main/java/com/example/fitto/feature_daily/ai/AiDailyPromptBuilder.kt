package com.example.fitto.feature_daily.ai

import java.util.Locale
import kotlin.math.roundToInt

object AiDailyPromptBuilder {

    fun build(data: AiDailyPromptData): String {
        return if (data.appLanguage.normalizedLanguage() == LANGUAGE_EN) {
            buildEnglishPrompt(data)
        } else {
            buildTurkishPrompt(data)
        }
    }

    private fun buildTurkishPrompt(data: AiDailyPromptData): String {
        val insight = data.toDailyInsight(language = LANGUAGE_TR)
        return """
            Sen Fitto uygulamasının günlük beslenme yorum asistanısın.
            Cevabı Türkçe üret.
            Kullanıcıya adıyla hitap edebilirsin ama her satırda adı tekrar etme.
            Tıbbi teşhis koyma, kesin sağlık iddiası üretme, tıbbi tavsiye gibi davranma.
            Suçlayıcı, yargılayıcı veya sert konuşma.
            Şu ifadeleri kullanma: "çok yedin", "yanlış yaptın", "saçma", "kötü beslenmişsin", "hatalı beslendin".
            Uygulamadaki tarifleri listelediğini veya buzdolabındaki malzemelere göre tarif bulduğunu iddia etme.
            Kesin diyet listesi yazma.
            Öneriler kısa, uygulanabilir ve esnek olsun.
            Cevabı sadece geçerli JSON olarak döndür; Markdown, açıklama veya kod bloğu ekleme.
            Kartın yerel durumu uygulama tarafından belirlendi: ${data.localStatus.name}. JSON status bu değerle uyumlu olsun.

            Kullanıcı bilgileri:
            Ad: ${data.userName}
            Tarih: ${data.dateIso}
            Kullanıcı hedefi: ${data.goalLabel.ifBlank { "Belirtilmedi" }}
            Hedefe özel yorum mantığı: ${data.goalGuidance.ifBlank { "Belirtilmedi" }}
            Günlük kalori hedefi: ${data.dailyCalorieTarget} kcal
            Bugünkü toplam kalori: ${data.totalCalories} kcal
            Kalori durumu: ${insight.calorieStatus}
            Protein: ${formatDecimal(data.proteinGrams)} g / hedef ${data.proteinTargetGrams} g
            Karbonhidrat: ${formatDecimal(data.carbGrams)} g
            Yağ: ${formatDecimal(data.fatGrams)} g
            Su tüketimi: ${data.waterMl} / ${data.waterGoalMl} ml
            Su durumu: ${insight.waterStatus}
            Egzersiz: ${data.exerciseSummary.toPromptText(language = LANGUAGE_TR)}
            Yerel tetikleyici: ${data.triggerType.name}
            Somut odak: ${insight.focusHint}

            Beslenme tercihleri:
            Diyet tipi: ${data.dietPreferences.ifBlank { "Belirtilmedi" }}
            Alerjiler: ${data.allergies.ifBlank { "Belirtilmedi" }}
            Sevmediği yiyecekler: ${data.dislikedFoods.ifBlank { "Belirtilmedi" }}
            Öğün düzeni: ${data.mealRoutine.ifBlank { "Belirtilmedi" }}

            Tercih kuralları:
            - Vegan ise önerileri bitkisel kaynaklarla ver.
            - Vejetaryen ise önerileri vejetaryen kaynaklarla ver.
            - Glutensiz ise glutensiz kaynaklar öner.
            - Laktozsuz ise laktozsuz kaynaklar öner.
            - Alerji listesindeki yiyecekleri önerme.
            - Sevmediği yiyecekleri önerme.
            - Emin değilsen "tercihine uygun protein kaynağı" veya "hedefine uygun doyurucu bir kaynak" gibi güvenli ifadeler kullan.
            - Tercih bilinmiyorsa özel diyet varsayımı yapma.

            Öğün bazlı günlük kayıtlar:
            ${data.meals.toPromptText(emptyText = "Henüz yemek kaydı yok.", language = LANGUAGE_TR)}

            Öğün değerlendirme kuralları:
            - Her öğün için mealComments içinde tam 1 kısa yorum yaz.
            - Sadece mealName kullan; yemek/ürün adlarını ekranda tekrar ettirme.
            - Öğün çok düşük kaloriliyse veya ana öğün için yetersizse bunu nazikçe açık söyle.
            - Protein çok düşükse bunu fark et.
            - Sadece marul, salatalık, yeşillik gibi kayıtlar ana öğün için yetersiz kalabilir; "hafif ve ferah" diye geçiştirme.
            - Kilo alma hedefinde düşük enerjili öğünü daha net belirt.
            - Kilo verme hedefinde hem kalori aşımını hem de aşırı yetersiz öğünü fark et.
            - Öğün yorumu en fazla 1 cümle olsun.
            - Su yorumu en fazla 1 cümle olsun.
            - Egzersiz yorumu en fazla 1 cümle olsun.
            - Genel yorum en fazla 2 cümle olsun.
            - Öneri en fazla 1 cümle olsun.

            Beklenen JSON:
            {
              "status": "${data.localStatus.name}",
              "mealComments": [
                {
                  "mealName": "Kahvaltı",
                  "comment": "Protein açısından iyi bir başlangıç olmuş.",
                  "level": "GREEN"
                }
              ],
              "waterComment": "",
              "exerciseComment": "",
              "generalComment": "",
              "suggestion": ""
            }
        """.trimIndent()
    }

    private fun buildEnglishPrompt(data: AiDailyPromptData): String {
        val insight = data.toDailyInsight(language = LANGUAGE_EN)
        return """
            You are the daily nutrition insight assistant for the Fitto app.
            Generate the answer in English.
            You may address the user by name, but do not repeat the name in every line.
            Do not diagnose, make certain health claims, or act as medical advice.
            Do not use blaming, shaming, or judgmental wording.
            Do not use phrases like "you ate too much", "you ate wrong", or "you failed".
            Do not claim you listed recipes from the app.
            Do not claim you found recipes based on ingredients in the user's fridge.
            Do not write a strict diet plan.
            Keep suggestions short, flexible, and practical.
            Return only valid JSON. Do not include Markdown, explanation, or code fences.
            The app already decided the local status: ${data.localStatus.name}. Keep JSON status aligned with it.

            User information:
            Name: ${data.userName}
            Date: ${data.dateIso}
            User goal: ${data.goalLabel.ifBlank { "Not specified" }}
            Goal-specific guidance: ${data.goalGuidance.ifBlank { "Not specified" }}
            Daily calorie target: ${data.dailyCalorieTarget} kcal
            Today's total calories: ${data.totalCalories} kcal
            Calorie status: ${insight.calorieStatus}
            Protein: ${formatDecimal(data.proteinGrams)} g / target ${data.proteinTargetGrams} g
            Carbohydrates: ${formatDecimal(data.carbGrams)} g
            Fat: ${formatDecimal(data.fatGrams)} g
            Water intake: ${data.waterMl} / ${data.waterGoalMl} ml
            Water status: ${insight.waterStatus}
            Exercise: ${data.exerciseSummary.toPromptText(language = LANGUAGE_EN)}
            Local trigger type: ${data.triggerType.name}
            Concrete focus: ${insight.focusHint}

            Nutrition preferences:
            Diet type: ${data.dietPreferences.ifBlank { "Not specified" }}
            Allergies: ${data.allergies.ifBlank { "Not specified" }}
            Disliked foods: ${data.dislikedFoods.ifBlank { "Not specified" }}
            Meal routine: ${data.mealRoutine.ifBlank { "Not specified" }}

            Preference rules:
            - If vegan, suggest plant-based sources.
            - If vegetarian, suggest vegetarian sources.
            - If gluten-free, suggest gluten-free sources.
            - If lactose-free, suggest lactose-free sources.
            - Do not suggest foods in the allergy list.
            - Do not suggest disliked foods.
            - If unsure, use safe wording like "a protein source that fits your preferences" or "a filling source that supports your goal".
            - Do not assume a special diet when preferences are unknown.

            Meal-based daily records:
            ${data.meals.toPromptText(emptyText = "No meal records yet.", language = LANGUAGE_EN)}

            Meal assessment rules:
            - Write exactly 1 short comment for each meal in mealComments.
            - Use only mealName; do not repeat food/product names on screen.
            - If a meal is very low in calories or insufficient for a main meal, say it clearly and gently.
            - Notice low protein.
            - If a meal is only lettuce, cucumber, greens, or similar, it may be insufficient as a main meal; do not frame it only as light and fresh.
            - For weight gain, notice low-energy meals more clearly.
            - For weight loss, notice both calorie excess and overly insufficient meals.
            - Each meal comment must be at most 1 sentence.
            - waterComment must be at most 1 sentence.
            - exerciseComment must be at most 1 sentence.
            - generalComment must be at most 2 sentences.
            - suggestion must be at most 1 sentence.

            Expected JSON:
            {
              "status": "${data.localStatus.name}",
              "mealComments": [
                {
                  "mealName": "Breakfast",
                  "comment": "This is a good start for protein.",
                  "level": "GREEN"
                }
              ],
              "waterComment": "",
              "exerciseComment": "",
              "generalComment": "",
              "suggestion": ""
            }
        """.trimIndent()
    }

    private fun AiDailyPromptData.toDailyInsight(language: String): DailyInsight {
        val calorieDelta = totalCalories - dailyCalorieTarget
        val calorieStatus = when {
            dailyCalorieTarget <= 0 -> if (language == LANGUAGE_EN) "No calorie target is available; focus on meal balance." else "Kalori hedefi yok; öğün dengesi üzerinden yorum yap."
            totalCalories >= (dailyCalorieTarget * 1.2).roundToInt() -> if (language == LANGUAGE_EN) "${calorieDelta.coerceAtLeast(0)} kcal over target, at or above 120% of the daily target." else "Hedefin ${calorieDelta.coerceAtLeast(0)} kcal üzerinde, günlük hedefin %120 seviyesinde veya üstünde."
            calorieDelta > 0 -> if (language == LANGUAGE_EN) "${calorieDelta} kcal slightly over the daily target." else "Günlük hedefin ${calorieDelta} kcal üzerinde."
            totalCalories >= (dailyCalorieTarget * 0.9).roundToInt() -> if (language == LANGUAGE_EN) "Close to the daily calorie target." else "Günlük kalori hedefine yaklaşılmış."
            else -> if (language == LANGUAGE_EN) "${-calorieDelta} kcal remaining until the daily target." else "Günlük hedefe ${-calorieDelta} kcal kaldı."
        }

        val waterMissing = (waterGoalMl - waterMl).coerceAtLeast(0)
        val waterStatus = when {
            waterGoalMl <= 0 -> if (language == LANGUAGE_EN) "No water target is available." else "Su hedefi yok."
            waterMissing == 0 -> if (language == LANGUAGE_EN) "Water target reached." else "Su hedefine ulaşılmış."
            else -> if (language == LANGUAGE_EN) "${waterMissing} ml remaining until the water target." else "Su hedefine ${waterMissing} ml kaldı."
        }

        return DailyInsight(
            calorieStatus = calorieStatus,
            waterStatus = waterStatus,
            focusHint = buildFocusHint(language, triggerType, calorieDelta, waterMissing, meals, proteinGrams, proteinTargetGrams, exerciseSummary)
        )
    }

    private fun buildFocusHint(
        language: String,
        triggerType: AiDailyTriggerType,
        calorieDelta: Int,
        waterMissing: Int,
        meals: List<AiDailyMealInfo>,
        proteinGrams: Double,
        proteinTargetGrams: Int,
        exerciseSummary: AiDailyExerciseInfo
    ): String {
        return when (triggerType) {
            AiDailyTriggerType.NO_MEAL -> if (language == LANGUAGE_EN) "No meal has been recorded yet; invite the user to add a first meal." else "Henüz yemek kaydı yok; kullanıcıyı ilk öğünü eklemeye davet et."
            AiDailyTriggerType.CALORIE_EXCEEDED -> if (language == LANGUAGE_EN) "Calories are ${calorieDelta.coerceAtLeast(0)} kcal over target and clearly high; suggest a lighter next choice." else "Kalori hedefi ${calorieDelta.coerceAtLeast(0)} kcal aşılmış; sonraki seçimde daha hafif ve dengeli öneri ver."
            AiDailyTriggerType.CALORIE_REACHED -> if (language == LANGUAGE_EN) "Calories are close to target; suggest keeping the remaining choices lighter." else "Kalori hedefe yakın; günün kalanında daha hafif seçim öner."
            AiDailyTriggerType.LOW_WATER -> if (language == LANGUAGE_EN) "Water is still below target by ${waterMissing} ml; include a clear water reminder." else "Su hedefi ${waterMissing} ml eksik; net ve nazik su hatırlatması ekle."
            AiDailyTriggerType.LOW_PROTEIN, AiDailyTriggerType.LOW_DAILY_PROTEIN -> if (language == LANGUAGE_EN) "Daily protein appears below target; suggest a preference-friendly protein source." else "Günlük protein hedefe göre düşük; tercihe uygun protein kaynağı öner."
            AiDailyTriggerType.LOW_MEAL_PROTEIN -> if (language == LANGUAGE_EN) "At least one meal seems low in protein; mention it at meal level." else "En az bir öğünde protein düşük görünüyor; bunu öğün yorumunda belirt."
            AiDailyTriggerType.INSUFFICIENT_MEAL -> if (language == LANGUAGE_EN) "At least one main meal seems too light or insufficient; say it gently and suggest balance." else "En az bir ana öğün çok hafif veya yetersiz görünüyor; bunu nazikçe belirt ve denge öner."
            AiDailyTriggerType.NO_EXERCISE -> if (language == LANGUAGE_EN) "No exercise record is visible; give a gentle goal-aware movement note." else "Egzersiz kaydı görünmüyor; hedefe uygun nazik hareket notu ekle."
            AiDailyTriggerType.NORMAL -> if (language == LANGUAGE_EN) "Day looks generally balanced; highlight the strongest useful observation." else "Gün genel olarak dengeli; en faydalı tek gözlemi vurgula."
        }
    }

    private fun List<AiDailyMealInfo>.toPromptText(emptyText: String, language: String): String {
        if (isEmpty()) return emptyText
        return mapIndexed { index, meal ->
            if (language == LANGUAGE_EN) {
                "${index + 1}. ${meal.mealName}: total ${meal.totalCalories} kcal, protein ${formatDecimal(meal.proteinGrams)}g, carbs ${formatDecimal(meal.carbGrams)}g, fat ${formatDecimal(meal.fatGrams)}g, entries ${meal.entryCount}, records: ${meal.foodSummary.ifBlank { "details unavailable" }}"
            } else {
                "${index + 1}. ${meal.mealName}: toplam ${meal.totalCalories} kcal, protein ${formatDecimal(meal.proteinGrams)}g, karbonhidrat ${formatDecimal(meal.carbGrams)}g, yağ ${formatDecimal(meal.fatGrams)}g, kayıt sayısı ${meal.entryCount}, kayıtlar: ${meal.foodSummary.ifBlank { "detay yok" }}"
            }
        }.joinToString(separator = "\n")
    }

    private fun AiDailyExerciseInfo.toPromptText(language: String): String {
        return if (!hasExercise) {
            if (language == LANGUAGE_EN) "No exercise record today." else "Bugün egzersiz kaydı yok."
        } else {
            if (language == LANGUAGE_EN) "$summary, ${totalDurationMinutes} min, ${totalCalories} kcal" else "$summary, ${totalDurationMinutes} dk, ${totalCalories} kcal"
        }
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    }

    private fun String.normalizedLanguage(): String = if (startsWith(LANGUAGE_EN, ignoreCase = true)) LANGUAGE_EN else LANGUAGE_TR

    private data class DailyInsight(
        val calorieStatus: String,
        val waterStatus: String,
        val focusHint: String
    )

    private const val LANGUAGE_TR = "tr"
    private const val LANGUAGE_EN = "en"
}
