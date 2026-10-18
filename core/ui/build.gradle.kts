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
    namespace = "com.solthariel.pawchan.core.ui"
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

// A presentation-only leaf must not acquire app, feature, domain or data dependencies.
configurations.configureEach {
    val production = !name.contains("test", ignoreCase = true)
    if (production) {
        dependencies.withType<ProjectDependency>().configureEach {
            error(":core:ui must remain independent of other project modules")
        }
    }
}

dependencies {
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
}
