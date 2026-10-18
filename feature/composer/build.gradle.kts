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
    namespace = "com.solthariel.pawchan.feature.composer"
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
            check(path in setOf(":domain", ":core:ui", ":data:network", ":core:media")) {
                ":feature:composer cannot depend on $path"
            }
        }
    }
}

dependencies {
    api(project(":domain"))
    api(project(":core:ui"))
    implementation(project(":data:network"))
    implementation(project(":core:media"))
    api(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.exifinterface)
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
}
