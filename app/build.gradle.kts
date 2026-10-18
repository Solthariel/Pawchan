import com.android.build.api.artifact.SingleArtifact
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

/** Fails the build when the APK lacks the video player's native libraries (otherwise video silently breaks). */
abstract class VerifyApkNativeLibraries : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apkDirectory: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val apks =
            apkDirectory
                .get()
                .asFile
                .walk()
                .filter { it.isFile && it.extension == "apk" }
                .toList()
        check(apks.isNotEmpty()) { "No APK found in ${apkDirectory.get().asFile}" }
        val required =
            listOf("avutil", "swresample", "swscale", "avcodec", "avformat", "avfilter", "dav1d", "player", "c++_shared")
                .map { "lib/arm64-v8a/lib$it.so" }
        apks.forEach { apk ->
            val entries =
                ZipFile(apk).use { zip ->
                    zip
                        .entries()
                        .asSequence()
                        .map { it.name }
                        .toSet()
                }
            val missing = required.filterNot(entries::contains)
            check(missing.isEmpty()) { "${apk.name} is missing native libraries: $missing" }
        }
        report.get().asFile.writeText(apks.joinToString("\n") { it.name })
    }
}

val releaseKeystorePath = providers.environmentVariable("PAWCHAN_KEYSTORE").orNull
val releaseKeyAlias = providers.environmentVariable("PAWCHAN_KEY_ALIAS").orNull
val releaseStorePassword = providers.environmentVariable("PAWCHAN_STORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("PAWCHAN_KEY_PASSWORD").orNull
val appVersionCode = rootProject.extra["appVersionCode"] as Int
val appVersionName = rootProject.extra["appVersionName"] as String
val updateManifestUrl =
    providers
        .gradleProperty("pawchan.updateManifestUrl")
        .orElse(providers.environmentVariable("PAWCHAN_UPDATE_MANIFEST_URL"))
        .orNull
        .orEmpty()
val hasReleaseSigning =
    listOf(
        releaseKeystorePath,
        releaseKeyAlias,
        releaseStorePassword,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

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
    namespace = "com.solthariel.pawchan"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.solthariel.pawchan"
        minSdk = 30
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                keyAlias = releaseKeyAlias
                storePassword = releaseStorePassword
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Use the real release key when supplied by CI. For local
            // performance testing, fall back to the standard debug key so
            // `assembleRelease` immediately produces an installable APK.
            signingConfig =
                signingConfigs.findByName("release")
                    ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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

    packaging {
        jniLibs {
            // Native compilation is owned by :video-ffmpeg (arm64-v8a only).
            // Filter transitive AAR libraries here so no partial non-ARM64 APK is shipped.
            excludes +=
                setOf(
                    "**/armeabi/**",
                    "**/armeabi-v7a/**",
                    "**/x86/**",
                    "**/x86_64/**",
                )
            keepDebugSymbols +=
                setOf(
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so",
                )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar(Char::uppercaseChar)
        val verify =
            tasks.register<VerifyApkNativeLibraries>("verify${name}ApkNativeLibraries") {
                apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
                report.set(layout.buildDirectory.file("apkNativeLibraries/${variant.name}.txt"))
            }
        tasks.matching { it.name == "assemble$name" }.configureEach { dependsOn(verify) }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":data:maintenance"))
    implementation(project(":feature:composer"))
    implementation(project(":feature:thread"))
    implementation(project(":core:media"))
    implementation(project(":core:ui"))
    implementation(project(":domain"))
    implementation(project(":data:drafts"))
    implementation(libs.androidx.exifinterface)
    implementation(project(":data:network"))
    implementation(project(":data:database"))
    implementation(project(":data:preferences"))
    implementation(project(":update"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
    implementation(project(":video-ffmpeg"))
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.play.services.cronet) {
        exclude(group = "org.chromium.net", module = "cronet-api")
    }
    implementation(libs.cronet.api)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
