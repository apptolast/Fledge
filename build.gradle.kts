import java.util.Properties

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.ktlint.jlleitschuh) apply false
}

val appEnv: String = run {
    val fromProp = (findProperty("APP_ENV") as String?)
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }
    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    val fromLocal = localProperties.getProperty("APP_ENV")
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }
    val fromTasks = if (gradle.startParameter.taskNames.any { it.lowercase().contains("release") }) {
        "release"
    } else {
        "debug"
    }
    (fromProp ?: fromLocal ?: fromTasks).also {
        require(it == "debug" || it == "release") {
            "APP_ENV invalido: '$it' (usa 'debug' o 'release')"
        }
    }
}
extra["appEnv"] = appEnv
logger.lifecycle("APP_ENV = $appEnv")
