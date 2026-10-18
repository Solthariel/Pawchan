import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.library)
}

abstract class VerifyNativeLibraries : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleDirectory: DirectoryProperty

    @get:Input
    abstract val requestedAbis: ListProperty<String>

    @get:OutputFile
    abstract val verificationStamp: RegularFileProperty

    @TaskAction
    fun verify() {
        val root = bundleDirectory.get().asFile
        val manifestFile = root.resolve("manifest.json")
        val manifest = JsonSlurper().parse(manifestFile) as Map<*, *>
        check(manifest["ffmpeg"] == "8.1.3" && manifest["dav1d"] == "1.5.4") {
            "Unexpected native library versions. Restore native-prebuilt from the project archive."
        }
        val suppliedAbis = manifest["abis"] as List<*>
        val hashes = manifest["sha256"] as Map<*, *>
        requestedAbis.get().forEach { abi ->
            check(abi in suppliedAbis) {
                "No bundled libraries for $abi. Supplied ABIs: $suppliedAbis."
            }
            val libraries = listOf("avutil", "swresample", "swscale", "avcodec", "avformat", "avfilter")
            val required =
                libraries.map { "libraries/ffmpeg/$abi/lib$it.so" } +
                    listOf("libraries/dav1d/$abi/libdav1d.so", "external/ffmpeg/include/$abi/libavformat/avformat.h")
            required.forEach { path -> check(hashes.containsKey(path)) { "Native manifest is missing $path" } }
        }
        hashes.forEach { (key, expected) ->
            val path = key as String
            check(!path.startsWith('/') && path.split('/').none { it == ".." }) { "Invalid native path: $path" }
            val file = root.resolve(path)
            check(file.isFile && sha256(file.readBytes()) == expected) {
                "Native library integrity check failed: $path. Restore native-prebuilt from the project archive."
            }
        }
        val stamp = verificationStamp.get().asFile
        stamp.parentFile.mkdirs()
        stamp.writeText(sha256(manifestFile.readBytes()))
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

/**
 * Copies the verified FFmpeg/dav1d libraries into a generated jniLibs directory. CMake IMPORTED targets alone are not
 * a reliable way to package prebuilt libraries: AGP sometimes drops them and the APK fails at runtime.
 */
abstract class PrepareFfmpegJniLibs : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleDirectory: DirectoryProperty

    @get:Input
    abstract val requestedAbis: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        val root = bundleDirectory.get().asFile
        requestedAbis.get().forEach { abi ->
            val target = output.resolve(abi).apply { mkdirs() }
            listOf("ffmpeg", "dav1d").forEach { library ->
                root
                    .resolve("libraries/$library/$abi")
                    .listFiles { file -> file.isFile && file.name.endsWith(".so") }
                    .orEmpty()
                    .forEach { it.copyTo(target.resolve(it.name), overwrite = true) }
            }
            check(target.resolve("libavutil.so").isFile && target.resolve("libdav1d.so").isFile) {
                "FFmpeg libraries for $abi are missing in native-prebuilt."
            }
        }
    }
}

val playerAbis =
    providers
        .gradleProperty("pawchan.videoAbis")
        .orElse("arm64-v8a")
        .get()
        .split(',')
        .map(String::trim)
require(playerAbis == listOf("arm64-v8a")) { "This project bundle supports arm64-v8a only." }

android {
    namespace = "com.solthariel.pawchan.ffmpeg"
    compileSdk = 37
    ndkVersion = "29.0.14206865"
    defaultConfig {
        minSdk = 30
        ndk {
            abiFilters.addAll(playerAbis)
        }
        externalNativeBuild { cmake { arguments += "-DANDROID_STL=c++_shared" } }
        consumerProguardFiles("consumer-rules.pro")
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        jniLibs {
            pickFirsts +=
                setOf("avutil", "swresample", "swscale", "avcodec", "avformat", "avfilter", "dav1d")
                    .map { "**/lib$it.so" }
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
    }
}

// Verified Android libraries ship with the project. No Bash/WSL is needed to build the app.
val verifyNativeLibraries =
    tasks.register<VerifyNativeLibraries>("verifyNativeLibraries") {
        bundleDirectory.set(layout.projectDirectory.dir("native-prebuilt"))
        requestedAbis.set(playerAbis)
        verificationStamp.set(layout.buildDirectory.file("native-verified.sha256"))
    }
val prepareFfmpeg =
    tasks.register<Sync>("prepareFfmpeg") {
        dependsOn(verifyNativeLibraries)
        inputs.property("abis", playerAbis)
        from("native-prebuilt") {
            playerAbis.forEach { abi ->
                include("libraries/ffmpeg/$abi/*.so")
                include("libraries/dav1d/$abi/*.so")
                include("external/ffmpeg/include/$abi/**")
            }
        }
        into(layout.buildDirectory.dir("native"))
    }
val prepareFfmpegJniLibs =
    tasks.register<PrepareFfmpegJniLibs>("prepareFfmpegJniLibs") {
        dependsOn(verifyNativeLibraries)
        bundleDirectory.set(layout.projectDirectory.dir("native-prebuilt"))
        requestedAbis.set(playerAbis)
        outputDirectory.set(layout.buildDirectory.dir("ffmpegJniLibs"))
    }
androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            prepareFfmpegJniLibs,
            PrepareFfmpegJniLibs::outputDirectory,
        )
    }
}
tasks.configureEach {
    if (name.startsWith("configureCMake") || name == "preBuild") dependsOn(prepareFfmpeg)
}

dependencies {
    implementation(libs.okhttp)
    testImplementation(libs.junit4)
}
