rootProject.name = "Fledge"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val baseLoginDir = file("../BaseLogin")
if (baseLoginDir.exists()) {
    includeBuild(baseLoginDir) {
        dependencySubstitution {
            substitute(module("com.github.apptolast:baselogin"))
                .using(project(":custom-login"))
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://gitlive.github.io/firebase-kotlin-sdk/maven/")
    }
}

include(":androidApp")
include(":shared")
