package com.example.fitto.feature_home.presentation.profile

import java.util.Locale

internal data class ProfileCountryUiModel(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val isRecommended: Boolean = false
)

internal class ProfileCountryCatalog {

    fun countries(language: ProfileLanguage): List<ProfileCountryUiModel> {
        val displayLocale = when (language) {
            ProfileLanguage.TURKISH -> Locale.forLanguageTag("tr-TR")
            ProfileLanguage.ENGLISH -> Locale.ENGLISH
        }

        return Locale.getISOCountries()
            .mapNotNull { code ->
                val countryLocale = Locale.Builder()
                    .setRegion(code)
                    .build()
                val countryName = countryLocale.getDisplayCountry(displayLocale)

                countryName
                    .takeIf { it.isNotBlank() }
                    ?.let { name ->
                        ProfileCountryUiModel(
                            code = code,
                            name = name,
                            flagEmoji = flagEmoji(code),
                            isRecommended = code == TURKEY_CODE
                        )
                    }
            }
            .sortedWith(
                compareBy<ProfileCountryUiModel> {
                    if (it.code == TURKEY_CODE) 0 else 1
                }.thenBy {
                    it.name.lowercase(displayLocale)
                }
            )
    }

    fun filter(
        countries: List<ProfileCountryUiModel>,
        query: String
    ): List<ProfileCountryUiModel> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return countries

        return countries.filter { country ->
            country.name.contains(cleanQuery, ignoreCase = true) ||
                country.code.contains(cleanQuery, ignoreCase = true)
        }
    }

    fun flagEmoji(countryCode: String): String {
        val normalizedCode = countryCode.uppercase(Locale.US)
        if (normalizedCode.length != 2 || normalizedCode.any { it !in 'A'..'Z' }) {
            return WORLD_EMOJI
        }

        val firstLetter = normalizedCode[0].code - 'A'.code + REGIONAL_INDICATOR_OFFSET
        val secondLetter = normalizedCode[1].code - 'A'.code + REGIONAL_INDICATOR_OFFSET

        return String(Character.toChars(firstLetter)) +
            String(Character.toChars(secondLetter))
    }

    private companion object {
        const val TURKEY_CODE = "TR"
        const val REGIONAL_INDICATOR_OFFSET = 0x1F1E6
        const val WORLD_EMOJI = "🌍"
    }
}
