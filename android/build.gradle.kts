plugins {
    base
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**", "**/generated/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**", "**/generated/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}

val androidModules = provider {
    allprojects.filter {
        it.plugins.hasPlugin("com.android.application") || it.plugins.hasPlugin("com.android.library")
    }
}

val androidLint = tasks.register("androidLint") {
    group = "verification"
    description = "Runs Android Lint on every Android module once the app is initialized."
    dependsOn(androidModules.map { modules -> modules.map { it.tasks.named("lint") } })
    onlyIf("Android modules must exist to run Android Lint") {
        androidModules.get().isNotEmpty().also { hasModules ->
            if (!hasModules) {
                logger.lifecycle("Android Lint: SKIPPED — Android 앱 모듈이 아직 없습니다.")
            }
        }
    }
}

tasks.check {
    dependsOn(androidLint)
}
