import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.library)
}

/** Reject damaged or incomplete bundled player libraries before any Android build. */
abstract class VerifyMpvLibraries : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraries: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val checksums: RegularFileProperty

    @TaskAction
    fun verify() {
        val expected =
            checksums.get().asFile.readLines().filter { it.isNotBlank() }.associate { line ->
                val parts = line.split("  ", limit = 2)
                check(parts.size == 2 && parts[0].matches(Regex("[0-9a-f]{64}"))) { "Invalid mpv checksum line" }
                check(parts[1].matches(Regex("lib[a-zA-Z0-9_+]+\\.so"))) { "Invalid mpv library name" }
                parts[1] to parts[0]
            }
        val required =
            setOf(
                "libc++_shared.so",
                "libmpv.so",
                "libmpvkt_jni.so",
                "libmpv_avcodec.so",
                "libmpv_avdevice.so",
                "libmpv_avfilter.so",
                "libmpv_avformat.so",
                "libmpv_avutil.so",
                "libmpv_swresample.so",
                "libmpv_swscale.so",
            )
        check(expected.keys == required) { "Incomplete mpv checksum manifest" }
        required.forEach { name ->
            val file = libraries.get().file(name).asFile
            check(file.isFile) { "Missing mpv library: $name" }
            val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            check(hash == expected.getValue(name)) { "Incorrect mpv library checksum: $name" }
        }
    }
}

val verifyMpvLibraries =
    tasks.register<VerifyMpvLibraries>("verifyMpvLibraries") {
        libraries.set(layout.projectDirectory.dir("src/main/jniLibs/arm64-v8a"))
        checksums.set(layout.projectDirectory.file("mpv-libraries.sha256"))
    }
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verifyMpvLibraries) }

val playerAbis =
    providers
        .gradleProperty("pawchan.videoAbis")
        .orElse("arm64-v8a")
        .get()
        .split(',')
        .map(String::trim)
require(playerAbis == listOf("arm64-v8a")) { "This project bundle supports arm64-v8a only." }

// The player is Kotlin-only now; it follows the same strict compiler policy as the other Android modules.
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
    namespace = "com.solthariel.pawchan.ffmpeg"
    compileSdk = 37
    defaultConfig {
        minSdk = 30
        ndk {
            abiFilters.addAll(playerAbis)
        }
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
    }
}

dependencies {
    implementation(libs.okhttp)
    testImplementation(libs.junit4)
}
