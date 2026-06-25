package com.example.fitto.feature_exercise

object ExerciseSearchQueryResolver {

    fun resolve(normalizedQuery: String): Set<String> {
        if (normalizedQuery.isBlank()) return emptySet()

        val queries = linkedSetOf(normalizedQuery)

        ALIASES.forEach { (key, aliases) ->
            if (normalizedQuery.contains(key)) {
                queries.add(key)
                queries.addAll(
                    aliases.map(ExerciseSearchTextNormalizer::normalize)
                )
            }
        }

        normalizedQuery.split(" ")
            .filter { word -> word.length >= 2 }
            .forEach { word ->
                queries.add(word)
                ALIASES[word]?.forEach { alias ->
                    queries.add(ExerciseSearchTextNormalizer.normalize(alias))
                }
            }

        return queries.filter { query -> query.isNotBlank() }.toSet()
    }

    private val ALIASES = mapOf(
        "kosu" to listOf("running", "run", "jogging", "jog"),
        "kosmak" to listOf("running", "run", "jogging", "jog"),
        "jogging" to listOf("kosu", "running", "run", "jog"),
        "running" to listOf("kosu", "run", "jogging", "jog"),
        "run" to listOf("kosu", "running", "jogging", "jog"),

        "yuruyus" to listOf("walking", "walk", "brisk", "steps"),
        "yuru" to listOf("walking", "walk", "brisk", "steps"),
        "walk" to listOf("yuruyus", "walking", "brisk", "steps"),
        "walking" to listOf("yuruyus", "walk", "brisk", "steps"),

        "bisiklet" to listOf("bicycling", "cycling", "cycle", "bike"),
        "bike" to listOf("bisiklet", "bicycling", "cycling"),
        "cycling" to listOf("bisiklet", "bicycling", "bike"),
        "bicycling" to listOf("bisiklet", "cycling", "bike"),

        "yuzme" to listOf("swimming", "swim", "water"),
        "swim" to listOf("yuzme", "swimming", "water"),
        "swimming" to listOf("yuzme", "swim", "water"),

        "dans" to listOf("dance", "dancing"),
        "dance" to listOf("dans", "dancing"),
        "dancing" to listOf("dans", "dance"),

        "yoga" to listOf("yoga"),
        "pilates" to listOf("pilates"),

        "esneme" to listOf("stretch", "stretching", "mobility"),
        "stretch" to listOf("esneme", "stretching", "mobility"),
        "stretching" to listOf("esneme", "stretch", "mobility"),
        "mobilite" to listOf("mobility", "stretching", "esneme"),
        "mobility" to listOf("mobilite", "stretching", "esneme"),

        "temizlik" to listOf("cleaning", "housework", "home"),
        "cleaning" to listOf("temizlik", "housework", "home"),
        "ev isi" to listOf("housework", "cleaning", "home"),
        "housework" to listOf("ev isi", "temizlik", "cleaning"),

        "market" to listOf("shopping", "grocery", "walking"),
        "alisveris" to listOf("shopping", "grocery"),
        "shopping" to listOf("market", "alisveris", "grocery"),

        "bahce" to listOf("gardening", "garden", "yard"),
        "gardening" to listOf("bahce", "garden", "yard"),

        "merdiven" to listOf("stairs", "stair", "climbing"),
        "stairs" to listOf("merdiven", "stair", "climbing"),

        "agirlik" to listOf("weight", "weights", "strength", "resistance"),
        "kuvvet" to listOf("strength", "weight", "resistance"),
        "strength" to listOf("kuvvet", "agirlik", "weight"),
        "weight" to listOf("agirlik", "kuvvet", "strength"),

        "karin" to listOf("core", "abs", "abdominal"),
        "core" to listOf("karin", "abs", "abdominal"),
        "abs" to listOf("karin", "core", "abdominal"),

        "ip atlama" to listOf("jump rope", "rope", "jumping"),
        "ip" to listOf("jump rope", "rope", "jumping"),
        "rope" to listOf("ip", "jump rope", "jumping")
    )
}
