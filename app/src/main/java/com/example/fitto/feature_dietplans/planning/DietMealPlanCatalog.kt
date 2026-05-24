package com.example.fitto.feature_dietplans.planning

import android.content.Context
import com.example.fitto.R
import com.example.fitto.feature_dietplans.DietPlanDetailFragment
import com.example.fitto.feature_meal.model.MealType

object DietMealPlanCatalog {

    fun getPlanDurationDays(planId: String): Int {
        return when (planId) {
            DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 7
            DietPlanDetailFragment.PLAN_KETOGENIC -> 21
            DietPlanDetailFragment.PLAN_DASH -> 14
            DietPlanDetailFragment.PLAN_MIND -> 14
            DietPlanDetailFragment.PLAN_ELIMINATION -> 21
            DietPlanDetailFragment.PLAN_PLANT_BASED -> 14
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 21
            else -> 0
        }
    }

    fun getPlanTitle(context: Context, planId: String): String {
        return when (planId) {
            DietPlanDetailFragment.PLAN_KETOGENIC -> context.getString(R.string.diet_plan_keto_title)
            DietPlanDetailFragment.PLAN_DASH -> context.getString(R.string.diet_plan_dash_title)
            DietPlanDetailFragment.PLAN_MIND -> context.getString(R.string.diet_plan_mind_title)
            DietPlanDetailFragment.PLAN_ELIMINATION -> context.getString(R.string.diet_plan_elimination_title)
            DietPlanDetailFragment.PLAN_PLANT_BASED -> context.getString(R.string.diet_plan_plant_based_title)
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> context.getString(R.string.diet_plan_weight_gain_title)
            else -> context.getString(R.string.diet_plan_mediterranean_title)
        }
    }

    fun getPlanBadge(context: Context, planId: String): String {
        val durationDays = getPlanDurationDays(planId)
        return if (durationDays > 0) {
            context.getString(R.string.diet_plan_day_badge, durationDays)
        } else {
            context.getString(R.string.daily_no_active_plan_badge)
        }
    }

    fun getPlanIcon(planId: String): String {
        return when (planId) {
            DietPlanDetailFragment.PLAN_KETOGENIC -> "🥑"
            DietPlanDetailFragment.PLAN_DASH -> "💚"
            DietPlanDetailFragment.PLAN_MIND -> "🧠"
            DietPlanDetailFragment.PLAN_ELIMINATION -> "🛡️"
            DietPlanDetailFragment.PLAN_PLANT_BASED -> "🌱"
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> "💪"
            else -> "🥗"
        }
    }

    fun getMealsForDay(planId: String, dayNumber: Int): List<PlannedMealUiModel> {
        val durationDays = getPlanDurationDays(planId)
        if (durationDays == 0) return emptyList()

        val normalizedDay = ((dayNumber - 1).floorMod(durationDays)) + 1
        return when (planId) {
            DietPlanDetailFragment.PLAN_KETOGENIC -> ketogenicMealSet(normalizedDay)
            DietPlanDetailFragment.PLAN_DASH -> dashMealSet(normalizedDay)
            DietPlanDetailFragment.PLAN_MIND -> mindMealSet(normalizedDay)
            DietPlanDetailFragment.PLAN_ELIMINATION -> eliminationMealSet(normalizedDay)
            DietPlanDetailFragment.PLAN_PLANT_BASED -> plantBasedMealSet(normalizedDay)
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> weightGainMealSet(normalizedDay)
            else -> mediterraneanMealSet(normalizedDay)
        }
    }

    private fun mediterraneanMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Yulaflı Akdeniz kahvaltısı", 430, listOf("yulaf", "süzme yoğurt", "ceviz", "çilek"), "Zeytinyağlı öğle tabağı", 560, listOf("ızgara tavuk", "bulgur", "çoban salata", "zeytinyağı"), "Meyve & badem", 210, listOf("elma", "badem"), "Balık ve salata", 600, listOf("ızgara levrek", "roka salatası", "tam tahıllı ekmek")),
            DayTemplate("Peynirli tam tahıllı tost", 410, listOf("tam tahıllı ekmek", "beyaz peynir", "domates", "salatalık"), "Mercimek + salata", 570, listOf("mercimek çorbası", "zeytinyağlı salata", "yoğurt"), "Yoğurtlu meyve", 220, listOf("yoğurt", "muz", "tarçın"), "Hindi & sebze", 600, listOf("hindi sote", "ızgara sebze", "bulgur")),
            DayTemplate("Menemen tabağı", 430, listOf("yumurta", "domates", "biber", "tam buğday ekmeği"), "Ton balıklı salata", 550, listOf("ton balığı", "yeşillik", "nohut", "zeytinyağı"), "Kefir & ceviz", 210, listOf("kefir", "ceviz"), "Zeytinyağlı sebze", 610, listOf("zeytinyağlı kabak", "yoğurt", "bulgur pilavı")),
            DayTemplate("Yoğurtlu granola", 420, listOf("yoğurt", "granola", "orman meyvesi"), "Tavuklu Akdeniz kasesi", 590, listOf("tavuk", "kinoa", "salata", "zeytinyağı"), "Armut & fındık", 210, listOf("armut", "fındık"), "Izgara köfte dengesi", 580, listOf("az yağlı köfte", "salata", "tam tahıllı lavaş")),
            DayTemplate("Avokadolu yumurta", 450, listOf("yumurta", "avokado", "tam tahıllı ekmek"), "Nohutlu sebze yemeği", 560, listOf("nohut", "sebze", "yoğurt"), "Havuç & humus", 220, listOf("havuç", "humus"), "Somon & yeşillik", 570, listOf("somon", "roka", "haşlanmış patates")),
            DayTemplate("Lor peynirli kahvaltı", 410, listOf("lor", "zeytin", "domates", "tam tahıllı ekmek"), "Izgara tavuklu makarna", 590, listOf("tam buğday makarna", "tavuk", "sebze"), "Meyve kasesi", 210, listOf("mevsim meyvesi", "yoğurt"), "Sebzeli omlet", 590, listOf("yumurta", "sebze", "salata")),
            DayTemplate("Akdeniz kahvaltı tabağı", 440, listOf("peynir", "zeytin", "yumurta", "tam tahıl"), "Balıklı bulgur salatası", 570, listOf("balık", "bulgur", "yeşillik"), "Kuru meyve & badem", 220, listOf("kuru kayısı", "badem"), "Tavuklu sebze güveç", 570, listOf("tavuk", "sebze", "yoğurt"))
        )
        return templates[day - 1].toMeals(DietPlanDetailFragment.PLAN_MEDITERRANEAN, day)
    }

    private fun ketogenicMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Avokadolu yumurta", 480, listOf("yumurta", "avokado", "beyaz peynir"), "Tavuklu yeşil salata", 620, listOf("tavuk", "zeytinyağı", "yeşillik"), "Salatalık & peynir", 220, listOf("salatalık", "peynir"), "Somon ve brokoli", 650, listOf("somon", "brokoli", "zeytinyağı")),
            DayTemplate("Mantarlı omlet", 470, listOf("yumurta", "mantar", "kaşar"), "Hindi marul kasesi", 610, listOf("hindi", "marul", "avokado"), "Cevizli yoğurt", 230, listOf("süzme yoğurt", "ceviz"), "Kabak spagetti", 650, listOf("kabak", "kıyma", "domates sos")),
            DayTemplate("Peynirli yumurta", 470, listOf("yumurta", "peynir", "zeytin"), "Ton balıklı keto salata", 620, listOf("ton balığı", "yeşillik", "zeytinyağı"), "Badem", 230, listOf("badem"), "Tavuk & karnabahar", 650, listOf("tavuk", "karnabahar püresi", "salata")),
            DayTemplate("Ispanaklı omlet", 460, listOf("yumurta", "ıspanak", "peynir"), "Etli sebze kasesi", 640, listOf("dana eti", "kabak", "zeytinyağı"), "Kefir", 210, listOf("şekersiz kefir"), "Hindi köfte", 660, listOf("hindi köfte", "salata", "yoğurt sos")),
            DayTemplate("Lor & avokado", 480, listOf("lor", "avokado", "zeytin"), "Somon salata", 630, listOf("somon", "yeşillik", "zeytinyağı"), "Fındık", 220, listOf("fındık"), "Tavuk sote", 650, listOf("tavuk", "mantar", "biber")),
            DayTemplate("Keto kahvaltı tabağı", 490, listOf("yumurta", "peynir", "zeytin", "salatalık"), "Kıymalı kabak", 620, listOf("kıyma", "kabak", "yoğurt"), "Peynir küpleri", 220, listOf("peynir"), "Balık & roka", 650, listOf("balık", "roka", "zeytinyağı")),
            DayTemplate("Menemen keto", 470, listOf("yumurta", "domates", "biber", "peynir"), "Tavuklu avokado salatası", 630, listOf("tavuk", "avokado", "yeşillik"), "Ceviz", 230, listOf("ceviz"), "Etli brokoli", 640, listOf("dana eti", "brokoli", "zeytinyağı"))
        )
        return templates[(day - 1) % templates.size].toMeals(DietPlanDetailFragment.PLAN_KETOGENIC, day)
    }

    private fun dashMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Yulaf & meyve", 420, listOf("yulaf", "süt", "muz", "tarçın"), "Az tuzlu tavuk tabağı", 590, listOf("tavuk", "bulgur", "sebze"), "Yoğurt & çilek", 220, listOf("yoğurt", "çilek"), "Baklagil salatası", 570, listOf("nohut", "yeşillik", "zeytinyağı")),
            DayTemplate("Tam tahıllı kahvaltı", 410, listOf("tam tahıl ekmek", "az yağlı peynir", "domates"), "Sebzeli hindi", 600, listOf("hindi", "esmer pirinç", "sebze"), "Meyve & badem", 220, listOf("elma", "badem"), "Balık & patates", 570, listOf("balık", "haşlanmış patates", "salata")),
            DayTemplate("Kefirli yulaf", 430, listOf("kefir", "yulaf", "orman meyvesi"), "Mercimek yemeği", 590, listOf("mercimek", "bulgur", "salata"), "Armut", 180, listOf("armut"), "Tavuklu sebze", 600, listOf("tavuk", "sebze", "yoğurt")),
            DayTemplate("Sebzeli omlet", 410, listOf("yumurta", "sebze", "tam tahıl"), "Ton balıklı tam tahıl", 590, listOf("ton balığı", "tam tahıllı ekmek", "salata"), "Yoğurt", 190, listOf("az yağlı yoğurt"), "Nohutlu sebze", 610, listOf("nohut", "sebze", "pirinç")),
            DayTemplate("Peynirli tost", 420, listOf("tam tahıllı tost", "az yağlı peynir"), "Izgara tavuk salata", 600, listOf("tavuk", "yeşillik", "bulgur"), "Muz", 180, listOf("muz"), "Sebzeli makarna", 600, listOf("tam buğday makarna", "sebze", "yoğurt")),
            DayTemplate("Meyveli yoğurt", 410, listOf("yoğurt", "meyve", "yulaf"), "Fırın balık", 610, listOf("balık", "sebze", "esmer pirinç"), "Havuç & humus", 220, listOf("havuç", "humus"), "Hindi sote", 560, listOf("hindi", "sebze", "bulgur")),
            DayTemplate("Yumurta & tam tahıl", 420, listOf("yumurta", "tam tahıl", "domates"), "Kuru fasulye dengesi", 600, listOf("kuru fasulye", "bulgur", "salata"), "Kefir", 180, listOf("kefir"), "Tavuklu çorba", 600, listOf("tavuk çorbası", "salata", "tam tahıl"))
        )
        return templates[(day - 1) % templates.size].toMeals(DietPlanDetailFragment.PLAN_DASH, day)
    }

    private fun mindMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Yaban mersinli yulaf", 420, listOf("yulaf", "yoğurt", "yaban mersini"), "Yeşil salata & tavuk", 590, listOf("tavuk", "yeşillik", "zeytinyağı"), "Ceviz & meyve", 220, listOf("ceviz", "elma"), "Balık ve sebze", 570, listOf("balık", "brokoli", "bulgur")),
            DayTemplate("Ispanaklı yumurta", 420, listOf("yumurta", "ıspanak", "tam tahıl"), "Nohutlu yeşil kase", 580, listOf("nohut", "yeşillik", "zeytinyağı"), "Yoğurt", 190, listOf("yoğurt"), "Hindi & sebze", 610, listOf("hindi", "sebze", "esmer pirinç")),
            DayTemplate("Yoğurt & orman meyvesi", 420, listOf("yoğurt", "orman meyvesi", "badem"), "Ton balıklı salata", 590, listOf("ton balığı", "yeşillik", "tam tahıl"), "Meyve", 170, listOf("armut"), "Mercimek & salata", 620, listOf("mercimek", "salata", "bulgur")),
            DayTemplate("Cevizli kahvaltı", 430, listOf("peynir", "ceviz", "tam tahıl"), "Tavuklu bulgur", 590, listOf("tavuk", "bulgur", "sebze"), "Kefir", 180, listOf("kefir"), "Somon salata", 610, listOf("somon", "yeşillik", "zeytinyağı")),
            DayTemplate("Meyveli yulaf", 410, listOf("yulaf", "muz", "süt"), "Fasulye salatası", 590, listOf("fasulye", "yeşillik", "zeytinyağı"), "Badem", 220, listOf("badem"), "Tavuk sebze", 590, listOf("tavuk", "sebze", "yoğurt")),
            DayTemplate("Omlet & yeşillik", 420, listOf("yumurta", "yeşillik", "tam tahıl"), "Balıklı kase", 600, listOf("balık", "kinoa", "salata"), "Yoğurt & meyve", 220, listOf("yoğurt", "çilek"), "Nohut yemeği", 560, listOf("nohut", "sebze", "bulgur")),
            DayTemplate("Tam tahıl tost", 410, listOf("tam tahıl", "peynir", "domates"), "Hindi salata", 590, listOf("hindi", "yeşillik", "zeytinyağı"), "Ceviz", 220, listOf("ceviz"), "Balık çorbası", 600, listOf("balık çorbası", "salata", "tam tahıl"))
        )
        return templates[(day - 1) % templates.size].toMeals(DietPlanDetailFragment.PLAN_MIND, day)
    }

    private fun eliminationMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Sade pirinç lapası", 420, listOf("pirinç", "zeytinyağı", "salatalık"), "Hindi & kabak", 600, listOf("hindi", "kabak", "pirinç"), "Armut", 180, listOf("armut"), "Fırın patates & tavuk", 600, listOf("tavuk", "patates", "kabak")),
            DayTemplate("Patatesli kahvaltı", 430, listOf("haşlanmış patates", "zeytinyağı", "salatalık"), "Pirinçli tavuk", 610, listOf("tavuk", "pirinç", "havuç"), "Elma", 170, listOf("elma"), "Hindi sebze", 590, listOf("hindi", "kabak", "patates")),
            DayTemplate("Karabuğday lapası", 420, listOf("karabuğday", "armut"), "Sebzeli pirinç", 590, listOf("pirinç", "kabak", "havuç"), "Salatalık", 120, listOf("salatalık"), "Tavuk çorbası", 670, listOf("tavuk", "pirinç", "havuç")),
            DayTemplate("Pirinç keki tabağı", 410, listOf("pirinç keki", "armut"), "Hindi patates", 620, listOf("hindi", "patates", "kabak"), "Elma püresi", 180, listOf("elma"), "Balık & pirinç", 610, listOf("beyaz balık", "pirinç", "kabak")),
            DayTemplate("Sade kahvaltı", 420, listOf("haşlanmış patates", "havuç", "zeytinyağı"), "Tavuklu sebze", 610, listOf("tavuk", "kabak", "pirinç"), "Armut", 180, listOf("armut"), "Hindi çorba", 590, listOf("hindi", "pirinç", "havuç")),
            DayTemplate("Karabuğday & elma", 420, listOf("karabuğday", "elma"), "Balık & patates", 610, listOf("beyaz balık", "patates", "kabak"), "Salatalık", 120, listOf("salatalık"), "Tavuk pirinç", 670, listOf("tavuk", "pirinç", "havuç")),
            DayTemplate("Pirinç lapası", 420, listOf("pirinç", "armut"), "Hindi pirinç", 610, listOf("hindi", "pirinç", "kabak"), "Elma", 170, listOf("elma"), "Tavuk patates", 600, listOf("tavuk", "patates", "havuç"))
        )
        return templates[(day - 1) % templates.size].toMeals(DietPlanDetailFragment.PLAN_ELIMINATION, day)
    }

    private fun plantBasedMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Yulaf & fıstık ezmesi", 450, listOf("yulaf", "bitkisel süt", "fıstık ezmesi"), "Nohutlu kinoa", 610, listOf("nohut", "kinoa", "sebze"), "Meyve & badem", 220, listOf("elma", "badem"), "Mercimek köftesi", 560, listOf("mercimek", "salata", "tam tahıl")),
            DayTemplate("Tofulu kahvaltı", 430, listOf("tofu", "domates", "tam tahıl"), "Fasulye bowl", 620, listOf("fasulye", "bulgur", "yeşillik"), "Humus & havuç", 230, listOf("humus", "havuç"), "Sebzeli makarna", 560, listOf("tam buğday makarna", "sebze")),
            DayTemplate("Muzlu smoothie", 440, listOf("muz", "bitkisel süt", "yulaf"), "Mercimek çorbası", 610, listOf("mercimek", "tam tahıl", "salata"), "Fındık", 220, listOf("fındık"), "Nohutlu sebze", 570, listOf("nohut", "sebze", "pirinç")),
            DayTemplate("Avokadolu tost", 430, listOf("avokado", "tam tahıl", "domates"), "Tofu salata", 610, listOf("tofu", "yeşillik", "kinoa"), "Meyve", 190, listOf("armut"), "Kuru fasulye", 600, listOf("kuru fasulye", "bulgur", "salata")),
            DayTemplate("Chia puding", 440, listOf("chia", "bitkisel süt", "meyve"), "Sebzeli nohut", 610, listOf("nohut", "sebze", "bulgur"), "Badem", 220, listOf("badem"), "Mercimek makarna", 570, listOf("mercimek makarna", "domates sos")),
            DayTemplate("Tahıllı kahvaltı", 430, listOf("tam tahıl", "tahin", "muz"), "Falafel bowl", 620, listOf("falafel", "salata", "bulgur"), "Humus", 220, listOf("humus", "salatalık"), "Sebzeli tofu", 570, listOf("tofu", "sebze", "pirinç")),
            DayTemplate("Yulaflı meyve kasesi", 440, listOf("yulaf", "meyve", "badem"), "Mercimek salatası", 610, listOf("mercimek", "yeşillik", "zeytinyağı"), "Kuru meyve", 210, listOf("kuru kayısı", "ceviz"), "Nohut dürüm", 580, listOf("nohut", "tam tahıllı lavaş", "salata"))
        )
        return templates[(day - 1) % templates.size].toMeals(DietPlanDetailFragment.PLAN_PLANT_BASED, day)
    }

    private fun weightGainMealSet(day: Int): List<PlannedMealUiModel> {
        val templates = listOf(
            DayTemplate("Yulaf güç kasesi", 560, listOf("yulaf", "süt", "fıstık ezmesi", "muz"), "Tavuklu makarna", 760, listOf("tavuk", "tam buğday makarna", "zeytinyağı"), "Smoothie", 420, listOf("süt", "muz", "fındık"), "Köfte & bulgur", 760, listOf("köfte", "bulgur", "yoğurt")),
            DayTemplate("Peynirli omlet", 540, listOf("yumurta", "peynir", "tam tahıl"), "Etli pilav", 780, listOf("dana eti", "pirinç", "salata"), "Yoğurt & granola", 400, listOf("yoğurt", "granola"), "Somon & patates", 780, listOf("somon", "patates", "sebze")),
            DayTemplate("Avokadolu tost", 550, listOf("avokado", "yumurta", "tam tahıl"), "Hindi sandviç", 760, listOf("hindi", "tam tahıl", "peynir"), "Kuruyemiş", 420, listOf("badem", "ceviz", "kuru üzüm"), "Tavuklu bulgur", 760, listOf("tavuk", "bulgur", "yoğurt")),
            DayTemplate("Kahvaltı tabağı", 560, listOf("yumurta", "peynir", "zeytin", "ekmek"), "Nohutlu et tabağı", 760, listOf("nohut", "et", "pirinç"), "Protein smoothie", 430, listOf("süt", "muz", "fıstık ezmesi"), "Fırın tavuk", 750, listOf("tavuk", "patates", "salata")),
            DayTemplate("Granola kasesi", 540, listOf("granola", "yoğurt", "meyve"), "Tavuk dürüm", 760, listOf("tavuk", "tam tahıllı lavaş", "avokado"), "Peynirli tost", 410, listOf("peynir", "tam tahıl"), "Balık & makarna", 790, listOf("balık", "makarna", "sebze")),
            DayTemplate("Menemen & ekmek", 550, listOf("yumurta", "domates", "tam tahıl"), "Köfteli makarna", 780, listOf("köfte", "makarna", "yoğurt"), "Kefir & kuruyemiş", 400, listOf("kefir", "kuruyemiş"), "Hindi & pirinç", 770, listOf("hindi", "pirinç", "sebze")),
            DayTemplate("Yulaf pankek", 560, listOf("yulaf", "yumurta", "muz"), "Tavuklu kinoa", 760, listOf("tavuk", "kinoa", "zeytinyağı"), "Muzlu süt", 400, listOf("süt", "muz"), "Etli sebze", 790, listOf("dana eti", "sebze", "bulgur"))
        )
        return templates[(day - 1) % templates.size].toMeals(DietPlanDetailFragment.PLAN_WEIGHT_GAIN, day)
    }

    private data class DayTemplate(
        val breakfastTitle: String,
        val breakfastCalories: Int,
        val breakfastFoods: List<String>,
        val lunchTitle: String,
        val lunchCalories: Int,
        val lunchFoods: List<String>,
        val snackTitle: String,
        val snackCalories: Int,
        val snackFoods: List<String>,
        val dinnerTitle: String,
        val dinnerCalories: Int,
        val dinnerFoods: List<String>
    ) {
        fun toMeals(planId: String, day: Int): List<PlannedMealUiModel> {
            return listOf(
                PlannedMealUiModel(
                    id = "$planId-day${day}-breakfast",
                    planId = planId,
                    dayNumber = day,
                    mealType = MealType.BREAKFAST,
                    title = breakfastTitle,
                    baseCalories = breakfastCalories,
                    foods = breakfastFoods
                ),
                PlannedMealUiModel(
                    id = "$planId-day${day}-lunch",
                    planId = planId,
                    dayNumber = day,
                    mealType = MealType.LUNCH,
                    title = lunchTitle,
                    baseCalories = lunchCalories,
                    foods = lunchFoods
                ),
                PlannedMealUiModel(
                    id = "$planId-day${day}-snack",
                    planId = planId,
                    dayNumber = day,
                    mealType = MealType.SNACK,
                    title = snackTitle,
                    baseCalories = snackCalories,
                    foods = snackFoods
                ),
                PlannedMealUiModel(
                    id = "$planId-day${day}-dinner",
                    planId = planId,
                    dayNumber = day,
                    mealType = MealType.DINNER,
                    title = dinnerTitle,
                    baseCalories = dinnerCalories,
                    foods = dinnerFoods
                )
            ).sortedBy { meal ->
                when (meal.mealType) {
                    MealType.BREAKFAST -> 0
                    MealType.SNACK -> 1
                    MealType.LUNCH -> 2
                    MealType.DINNER -> 3
                }
            }
        }
    }
}

private fun Int.floorMod(other: Int): Int {
    return ((this % other) + other) % other
}
