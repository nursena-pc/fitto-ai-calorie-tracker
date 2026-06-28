package com.example.fitto.feature_home.presentation.social

internal object SocialLinkResolver {

    private val webUrlRegex = Regex(
        pattern = """(?i)\b((?:https?://)?(?:www\.)?[a-z0-9][a-z0-9-]*(?:\.[a-z0-9][a-z0-9-]*)+(?::\d+)?(?:/[^\s]*)?)"""
    )

    fun extractLinks(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        return webUrlRegex.findAll(text)
            .map { match -> normalize(match.value) }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun normalize(rawLink: String): String {
        val trimmed = rawLink.trim().trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
        if (trimmed.isBlank()) return ""

        return if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
