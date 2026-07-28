import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ktlint.jlleitschuh)
}

val localProperties: Properties by lazy {
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
}

val appEnv = rootProject.extra["appEnv"] as String
val firestoreDatabaseId = (project.findProperty("FIRESTORE_DATABASE_ID") as String?)
    ?: localProperties.getProperty(
        "FIRESTORE_DATABASE_ID",
        if (appEnv == "release") "(default)" else "debug",
    )

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.apptolast.fledge.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            // GitLive publishes its Android artifacts without a version for the Firebase SDK,
            // delegating the resolution to this BOM.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.baselogin)
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            implementation(libs.multiplatform.settings)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
    }
}

buildkonfig {
    packageName = "com.apptolast.fledge.shared"
    defaultConfigs {
        buildConfigField(STRING, "APP_ENV", appEnv)
        buildConfigField(STRING, "FIREBASE_API_KEY", localProperties.getProperty("FIREBASE_API_KEY", ""))
        buildConfigField(
            STRING,
            "FIREBASE_PROJECT_ID",
            localProperties.getProperty("FIREBASE_PROJECT_ID", "fledge-c685d"),
        )
        buildConfigField(STRING, "FIRESTORE_DATABASE_ID", firestoreDatabaseId)
        buildConfigField(STRING, "GOOGLE_WEB_CLIENT_ID", localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", ""))
        // Public Firebase client values used to build explicit FirebaseOptions (no google-services.json).
        // Every field defaults to "" so the build keeps working on a machine without local.properties.
        buildConfigField(
            STRING,
            "FIREBASE_APP_ID_ANDROID",
            localProperties.getProperty("FIREBASE_APP_ID_ANDROID", ""),
        )
        buildConfigField(STRING, "FIREBASE_APP_ID_IOS", localProperties.getProperty("FIREBASE_APP_ID_IOS", ""))
        buildConfigField(STRING, "FIREBASE_GCM_SENDER_ID", localProperties.getProperty("FIREBASE_GCM_SENDER_ID", ""))
        buildConfigField(
            STRING,
            "FIREBASE_STORAGE_BUCKET",
            localProperties.getProperty("FIREBASE_STORAGE_BUCKET", ""),
        )
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

ktlint {
    android = false
    ignoreFailures = false
    outputToConsole = true
}
