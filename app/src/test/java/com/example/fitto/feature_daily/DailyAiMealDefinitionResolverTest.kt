package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiMealDefinitionResolverTest {

    @Test
    fun resolveMealDefinitions_returnsMealTypesInEntryOrder() {
        assertEquals(
            listOf(
                DailyAiMealDefinitionResolver.DefinitionRes("breakfast", R.string.daily_breakfast),
                DailyAiMealDefinitionResolver.DefinitionRes("snack", R.string.daily_snack),
                DailyAiMealDefinitionResolver.DefinitionRes("lunch", R.string.daily_lunch),
                DailyAiMealDefinitionResolver.DefinitionRes("dinner", R.string.daily_dinner)
            ),
            DailyAiMealDefinitionResolver.resolveMealDefinitions()
        )
    }
}
