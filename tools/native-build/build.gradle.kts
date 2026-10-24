import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.10"
    application
}

require(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_25)) { "Native tooling uses the project JDK 25 or newer." }

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}
dependencies {
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.tukaani:xz:1.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
application { mainClass.set("com.solthariel.pawchan.nativebuild.MainKt") }
tasks.test {
    useJUnit()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}
val ktlint =
    configurations.create("ktlintTool") {
        attributes {
            attribute(
                org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE,
                objects.named(org.gradle.api.attributes.Bundling.SHADOWED),
            )
        }
    }
val detekt = configurations.create("detektTool")
dependencies {
    add(ktlint.name, "com.pinterest.ktlint:ktlint-cli:1.8.0")
    add(detekt.name, "io.gitlab.arturbosch.detekt:detekt-cli:1.23.8")
}
tasks.register<JavaExec>("ktlintCheck") {
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt", "*.kts")
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}
tasks.register<JavaExec>("ktlintFormat") {
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format", "src/**/*.kt", "*.kts")
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}
tasks.register<JavaExec>("detekt") {
    classpath = detekt
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args("--input", "src/main/kotlin,src/test/kotlin", "--config", "../../detekt.yml")
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
}
tasks.check { dependsOn("ktlintCheck", "detekt") }

val nativeActions =
    mapOf(
        "planNative" to "plan",
        "prepareNativeSources" to "prepare",
        "rebuildNative" to "build",
        "verifyNativeBundle" to "verify",
    )
nativeActions.forEach { (taskName, action) ->
    tasks.register<JavaExec>(taskName) {
        group = "native tools"
        dependsOn(tasks.classes)
        classpath = sourceSets.named("main").get().runtimeClasspath
        mainClass.set(application.mainClass)
        args(action, "--project-root", rootDir.resolve("../..").canonicalPath)
        val options =
            mapOf(
                "abis" to "abi",
                "ndk" to "ndk",
                "jobs" to "jobs",
                "cache" to "cache",
                "ffmpegSource" to "ffmpeg-source",
                "dav1dSource" to "dav1d-source",
            )
        options.forEach { (property, flag) ->
            providers.gradleProperty("native.$property").orNull?.let { args("--$flag", it) }
        }
        if (providers.gradleProperty("native.offline").map(String::toBoolean).getOrElse(false)) args("--offline")
    }
}
