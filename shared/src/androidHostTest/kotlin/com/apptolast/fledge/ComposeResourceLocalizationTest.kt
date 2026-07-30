package com.apptolast.fledge

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.w3c.dom.Element

class ComposeResourceLocalizationTest {

    @Test
    fun `FLE-56 localized resources cover every translatable default string`() {
        // Given
        val defaultStrings = loadStrings("values")
        val translatableKeys = defaultStrings.values
            .filter { it.translatable }
            .map { it.name }
            .toSet()

        listOf("values-en", "values-pt").forEach { locale ->
            val localizedStrings = loadStrings(locale)

            // When
            val missingKeys = translatableKeys - localizedStrings.keys
            val unknownKeys = localizedStrings.keys - defaultStrings.keys

            // Then
            assertTrue(
                missingKeys.isEmpty(),
                "$locale is missing translatable strings: ${missingKeys.sorted().joinToString()}",
            )
            assertTrue(
                unknownKeys.isEmpty(),
                "$locale contains strings absent from the default resources: ${unknownKeys.sorted().joinToString()}",
            )
        }
    }

    @Test
    fun `FLE-56 localized placeholders stay indexed and compatible`() {
        // Given
        val defaultStrings = loadStrings("values")
        val translatableKeys = defaultStrings.values
            .filter { it.translatable }
            .map { it.name }
            .toSet()

        listOf("values", "values-en", "values-pt").forEach { locale ->
            val strings = loadStrings(locale)
            val badKeys = strings.values
                .filter { UnindexedPlaceholderRegex.containsMatchIn(it.value) }
                .map { it.name }

            assertTrue(
                badKeys.isEmpty(),
                "$locale contains unindexed placeholders. Use %1\$s, %1\$d and escape percent as %%: " +
                    badKeys.sorted().joinToString(),
            )
        }

        listOf("values-en", "values-pt").forEach { locale ->
            val localizedStrings = loadStrings(locale)

            translatableKeys.forEach { key ->
                val defaultPlaceholders = placeholders(defaultStrings.getValue(key).value)
                val localizedPlaceholders = placeholders(localizedStrings.getValue(key).value)

                assertEquals(
                    defaultPlaceholders,
                    localizedPlaceholders,
                    "$locale/$key must keep the same placeholders as the default resource",
                )
            }
        }
    }

    private fun loadStrings(localeDirectory: String): Map<String, ComposeResourceString> {
        val file = File(
            projectRoot(),
            "shared/src/commonMain/composeResources/$localeDirectory/strings.xml",
        )
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = document.getElementsByTagName("string")

        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .associate { element ->
                val name = element.getAttribute("name")
                name to ComposeResourceString(
                    name = name,
                    value = element.textContent.orEmpty(),
                    translatable = element.getAttribute("translatable") != "false",
                )
            }
    }

    private fun projectRoot(): File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private fun placeholders(value: String): List<String> = PlaceholderRegex.findAll(value).map { it.value }.toList()

    private data class ComposeResourceString(val name: String, val value: String, val translatable: Boolean)

    private companion object {
        val PlaceholderRegex = Regex("%(?:\\d+\\$)?[dsf]")
        val UnindexedPlaceholderRegex = Regex("(?<!%)%(?!%|\\d+\\$)[dsf]")
    }
}
