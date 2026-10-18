import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        progressiveMode = true
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xtype-enhancement-improvements-strict-mode",
            "-Xenhance-type-parameter-types-to-def-not-null",
        )
    }
}

android {
    namespace = "com.solthariel.pawchan.core.media"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        disable +=
            setOf(
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable",
            )
    }
}

// Implementation may use transport/native capabilities, never app or feature modules.
configurations.configureEach {
    if (!name.contains("test", ignoreCase = true)) {
        dependencies.withType<ProjectDependency>().configureEach {
            check(path in setOf(":domain", ":core:ui", ":data:network", ":video-ffmpeg")) {
                ":core:media cannot depend on $path"
            }
        }
    }
}

dependencies {
    api(project(":domain"))
    api(project(":core:ui"))
    implementation(project(":data:network"))
    implementation(project(":video-ffmpeg"))
    api(libs.coil.compose)
    api(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.kotlinx.coroutines.test)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.coil.network.okhttp)
}
