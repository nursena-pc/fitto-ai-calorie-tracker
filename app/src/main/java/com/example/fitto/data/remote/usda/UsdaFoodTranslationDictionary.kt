package com.example.fitto.data.remote.usda

import java.util.Locale

object UsdaFoodTranslationDictionary {

    private val trLocale = Locale("tr", "TR")

    private val trPhraseToEn = linkedMapOf(
        "tavuk gogsu" to "chicken breast",
        "haslanmis yumurta" to "boiled egg",
        "suzme yogurt" to "greek yogurt",
        "beyaz peynir" to "feta cheese",
        "kasar peyniri" to "cheddar cheese",
        "ton baligi" to "tuna",
        "zeytin yagi" to "olive oil",
        "zeytinyagi" to "olive oil",
        "kirmizi mercimek" to "red lentils",
        "yesil mercimek" to "green lentils",
        "tavuk budu" to "chicken thigh",
        "dana eti" to "beef",
        "kiyma" to "ground beef",
        "pide" to "pita bread",
        "yulaf ezmesi" to "oats",
        "tam bugday ekmegi" to "whole wheat bread"
    )

    private val trTokenToEn = mapOf(
        "yumurta" to "egg",
        "sut" to "milk",
        "yogurt" to "yogurt",
        "peynir" to "cheese",
        "lor" to "cottage cheese",
        "tavuk" to "chicken",
        "hindi" to "turkey",
        "et" to "beef",
        "balik" to "fish",
        "somon" to "salmon",
        "levrek" to "sea bass",
        "ton" to "tuna",
        "pirinc" to "rice",
        "bulgur" to "bulgur",
        "makarna" to "pasta",
        "ekmek" to "bread",
        "yulaf" to "oats",
        "muz" to "banana",
        "elma" to "apple",
        "portakal" to "orange",
        "cilek" to "strawberry",
        "domates" to "tomato",
        "salatalik" to "cucumber",
        "patates" to "potato",
        "mercimek" to "lentils",
        "nohut" to "chickpeas",
        "fasulye" to "beans",
        "zeytin" to "olive",
        "ceviz" to "walnuts",
        "badem" to "almonds",
        "findik" to "hazelnuts"
    )

    private val enContainsToTr = listOf(
        "chicken breast" to "Tavuk Göğsü",
        "chicken thigh" to "Tavuk Budu",
        "ground beef" to "Kıyma",
        "beef" to "Dana Eti",
        "boiled egg" to "Haşlanmış Yumurta",
        "egg" to "Yumurta",
        "banana" to "Muz",
        "apple" to "Elma",
        "orange" to "Portakal",
        "strawberry" to "Çilek",
        "milk" to "Süt",
        "greek yogurt" to "Süzme Yoğurt",
        "yogurt" to "Yoğurt",
        "cheddar cheese" to "Kaşar Peyniri",
        "feta cheese" to "Beyaz Peynir",
        "cottage cheese" to "Lor Peyniri",
        "tuna" to "Ton Balığı",
        "salmon" to "Somon",
        "sea bass" to "Levrek",
        "rice" to "Pirinç",
        "bulgur" to "Bulgur",
        "pasta" to "Makarna",
        "whole wheat bread" to "Tam Buğday Ekmeği",
        "pita bread" to "Pide",
        "pita" to "Pide",
        "bread" to "Ekmek",
        "oatmeal" to "Yulaf Ezmesi",
        "oats" to "Yulaf",
        "tomato" to "Domates",
        "cucumber" to "Salatalık",
        "potato" to "Patates",
        "red lentils" to "Kırmızı Mercimek",
        "green lentils" to "Yeşil Mercimek",
        "lentils" to "Mercimek",
        "chickpeas" to "Nohut",
        "beans" to "Fasulye",
        "olive oil" to "Zeytinyağı",
        "olive" to "Zeytin",
        "walnuts" to "Ceviz",
        "almonds" to "Badem",
        "hazelnuts" to "Fındık",
        "peanut butter" to "Fıstık Ezmesi"
    )

    fun translateQueryForUsda(query: String): String {
        val normalized = normalize(query)
        if (normalized.isBlank()) return query

        trPhraseToEn[normalized]?.let { return it }

        var translated = normalized.replace("_", " ")

        trPhraseToEn.entries
            .sortedByDescending { it.key.length }
            .forEach { (tr, en) ->
                translated = translated.replaceWord(tr, en)
            }

        trTokenToEn.forEach { (tr, en) ->
            translated = translated.replaceWord(tr, en)
        }

        return translated.ifBlank { query }
    }

    fun translateDescriptionToTurkish(description: String): String {
        val normalized = normalize(description)

        enContainsToTr.firstOrNull { (keyword, _) ->
            normalized.contains(normalize(keyword))
        }?.let { (_, trName) ->
            return trName
        }

        return cleanUsdaDescription(description).toTurkishTitleCase()
    }

    fun simplifyEnglishDescription(description: String): String {
        val normalized = normalize(description)

        val knownEnglishName = enContainsToTr.firstOrNull { (keyword, _) ->
            normalized.contains(normalize(keyword))
        }?.first

        return knownEnglishName
            ?.toEnglishTitleCase()
            ?: cleanUsdaDescription(description).toEnglishTitleCase()
    }

    private fun cleanUsdaDescription(description: String): String {
        val parts = description
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (parts.isEmpty()) return description.trim()

        val mainName = parts.first()

        val usefulDetails = parts
            .drop(1)
            .filterNot { part ->
                val normalized = normalize(part)
                normalized in setOf(
                    "raw",
                    "cooked",
                    "boiled",
                    "fried",
                    "roasted",
                    "grilled",
                    "without salt",
                    "with salt",
                    "not specified",
                    "ns as to fat added",
                    "ns as to color",
                    "ns as to form"
                ) || normalized.startsWith("ns as to")
            }
            .take(1)

        return buildString {
            append(mainName)
            if (usefulDetails.isNotEmpty()) {
                append(", ")
                append(usefulDetails.joinToString(", "))
            }
        }
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(trLocale)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun String.replaceWord(
        oldValue: String,
        newValue: String
    ): String {
        return replace(
            Regex("\\b${Regex.escape(oldValue)}\\b"),
            newValue
        )
    }

    private fun String.toTurkishTitleCase(): String {
        return lowercase(trLocale)
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(trLocale) else char.toString()
                }
            }
    }

    private fun String.toEnglishTitleCase(): String {
        return lowercase(Locale.US)
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                }
            }
    }
}