import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val localProperties = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.map {
    Properties().apply { load(it.reader()) }
}
val naverMapKeyId = providers.gradleProperty("dalmoi.naverMapKeyId")
    .orElse(providers.environmentVariable("NAVER_MAP_KEY_ID"))
    .orElse(localProperties.map { it.getProperty("dalmoi.naverMapKeyId", "") })
    .getOrElse("")
    .trim()

android {
    namespace = "com.sor999.dalmoi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sor999.dalmoi"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["naverMapKeyId"] = naverMapKeyId
        buildConfigField("boolean", "NAVER_MAP_CONFIGURED", naverMapKeyId.isNotBlank().toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${providers.gradleProperty("dalmoi.apiBaseUrl").getOrElse("http://10.0.2.2:8080/api/v1/")}\"",
            )
        }
        release {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${providers.gradleProperty("dalmoi.apiBaseUrl").getOrElse("")}\"",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(libs.naver.map)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(platform(libs.compose.bom))
    implementation(libs.navigation.compose)
    implementation(libs.androidsvg)
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test)
    androidTestImplementation(libs.espresso)
}
