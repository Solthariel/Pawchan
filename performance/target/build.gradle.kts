plugins {
    alias(libs.plugins.android.application)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        progressiveMode = true
    }
}

android {
    namespace = "com.solthariel.pawchan.performance"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.solthariel.pawchan.performance"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.38-composer-probe"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":feature:composer"))
    implementation(libs.androidx.profileinstaller)
}
