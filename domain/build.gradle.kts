plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        progressiveMode = true
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xtype-enhancement-improvements-strict-mode",
            "-Xenhance-type-parameter-types-to-def-not-null",
        )
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
