package com.apptolast.fledge

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class FoundationTemplateCleanupTest {

    @Test
    fun `AC-01 template demo content is absent from sources`() {
        // Given
        val projectRoot = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val productionSourceDirs = listOf("commonMain", "androidMain", "iosMain")
            .map { sourceSet -> File(projectRoot, "shared/src/$sourceSet") }
        val sourceFiles = productionSourceDirs.flatMap { sourceDir ->
            sourceDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
                .toList()
        }

        // When
        val joinedSources = sourceFiles.joinToString(separator = "\n") { it.readText() }

        // Then
        assertFalse(joinedSources.contains("Click me!"))
        assertFalse(joinedSources.contains("Greeting"))
        assertFalse(joinedSources.contains("compose_multiplatform"))
    }
}
