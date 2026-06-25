package com.example.fitto.feature_exercise

import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchQueryResolverTest {

    @Test
    fun resolve_addsAliasesForWholeQuery() {
        val queries = ExerciseSearchQueryResolver.resolve("kosu")

        assertTrue(queries.contains("kosu"))
        assertTrue(queries.contains("running"))
        assertTrue(queries.contains("run"))
        assertTrue(queries.contains("jogging"))
    }

    @Test
    fun resolve_addsAliasesForIndividualWords() {
        val queries = ExerciseSearchQueryResolver.resolve("hizli yuruyus")

        assertTrue(queries.contains("hizli"))
        assertTrue(queries.contains("yuruyus"))
        assertTrue(queries.contains("walking"))
        assertTrue(queries.contains("steps"))
    }
}
