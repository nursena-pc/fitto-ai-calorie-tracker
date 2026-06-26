package com.example.fitto.feature_reports

object ReportMacroSummaryResolver {

    enum class MacroType {
        PROTEIN,
        CARBS,
        FAT
    }

    data class MacroEntry(
        val type: MacroType,
        val grams: Double
    )

    data class State(
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val total: Double,
        val chartEntries: List<MacroEntry>,
        val days: List<ReportDay>
    ) {
        val hasData: Boolean = total > 0.0
    }

    fun resolve(days: List<ReportDay>): State {
        val selectedDay = days.lastOrNull()
        val protein = selectedDay?.protein ?: 0.0
        val carbs = selectedDay?.carbs ?: 0.0
        val fat = selectedDay?.fat ?: 0.0

        return State(
            protein = protein,
            carbs = carbs,
            fat = fat,
            total = protein + carbs + fat,
            chartEntries = buildList {
                if (protein > 0.0) add(MacroEntry(MacroType.PROTEIN, protein))
                if (carbs > 0.0) add(MacroEntry(MacroType.CARBS, carbs))
                if (fat > 0.0) add(MacroEntry(MacroType.FAT, fat))
            },
            days = days
        )
    }
}
