import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.attributes.Bundling
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyReleaseMetadata : DefaultTask() {
    @get:InputFile
    abstract val metadataFile: RegularFileProperty

    @get:InputFile
    abstract val changelogFile: RegularFileProperty

    @get:InputFile
    abstract val bundledChangelogFile: RegularFileProperty

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val currentVersionCode: Property<Int>

    @get:Input
    abstract val currentVersionName: Property<String>

    @TaskAction
    fun verify() {
        val metadata = JsonSlurper().parse(metadataFile.get().asFile) as Map<*, *>
        val versions = metadata["versions"] as? List<*> ?: error("versions.json: missing versions")
        check(versions.isNotEmpty()) { "versions.json must not be empty" }
        val codes = mutableSetOf<Int>()
        val names = mutableSetOf<String>()
        var previousCode = 0
        val namePattern = Regex("(0|[1-9][0-9]*)[.](0|[1-9][0-9]*)[.](0|[1-9][0-9]*)")

        versions.forEach { rawVersion ->
            val version = rawVersion as? Map<*, *> ?: error("Invalid version entry: $rawVersion")
            val code = (version["code"] as? Number)?.toInt() ?: error("Version code is missing")
            val name = version["name"] as? String ?: error("Version name is missing for code $code")
            val date = version["date"] as? String ?: error("Version date is missing for code $code")
            check(code > 0 && codes.add(code)) { "Invalid or duplicate version code: $code" }
            check(code > previousCode) { "Version codes must increase in release order" }
            previousCode = code
            check(namePattern.matches(name) && names.add(name)) { "Invalid or duplicate version name: $name" }
            check(Regex("[0-9]{2}[.][0-9]{2}[.][0-9]{4}").matches(date)) { "Invalid version date: $date" }
        }

        val currentVersion =
            versions
                .map { it as Map<*, *> }
                .maxBy { (it["code"] as Number).toInt() }
        check((currentVersion["code"] as Number).toInt() == currentVersionCode.get()) {
            "Gradle version code does not match versions.json"
        }
        check(currentVersion["name"] == currentVersionName.get()) {
            "Gradle version name does not match versions.json"
        }
        val changelog = changelogFile.get().asFile.readText()
        check(changelog.isNotBlank()) { "Current Russian changelog is missing or empty" }
        check(bundledChangelogFile.get().asFile.readText() == changelog) {
            "Bundled changelog does not match current release notes"
        }

        val manifest = JsonSlurper().parse(manifestFile.get().asFile) as Map<*, *>
        val applications = manifest["applications"] as? List<*> ?: error("Update manifest has no applications")
        val client = applications.map { it as Map<*, *> }.firstOrNull { it["name"] == "client" }
        val packages = client?.get("packages") as? List<*> ?: error("Update manifest has no client package")
        val release = packages.map { it as Map<*, *> }.maxBy { (it["version_code"] as Number).toInt() }
        check((release["version_code"] as Number).toInt() == currentVersionCode.get()) {
            "Update manifest version code does not match versions.json"
        }
        check(release["version_name"] == currentVersionName.get()) {
            "Update manifest version name does not match versions.json"
        }
    }
}

abstract class VerifySourcePolicy : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val policyFiles: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val root = rootDirectory.get().asFile
        val forbiddenAnnotations = listOf("@" + "Suppress(", "@" + "SuppressLint(")
        val annotationViolations =
            policyFiles.files
                .filter { source -> source.extension == "kt" || source.extension == "kts" }
                .flatMap { source ->
                    source
                        .readLines()
                        .mapIndexedNotNull { index, line ->
                            line
                                .takeIf { text ->
                                    forbiddenAnnotations.any { annotation -> text.contains(annotation) }
                                }?.let { "${source.relativeTo(root)}:${index + 1}" }
                        }
                }
        val baselineViolations =
            policyFiles.files
                .filter { source -> source.extension == "xml" && "baseline" in source.name }
                .map { source -> source.relativeTo(root).path }
        val performanceViolations = findPerformanceViolations(root)
        val violations =
            annotationViolations + baselineViolations + performanceViolations +
                findLayerViolations(root) + findReplyBoundaryViolations(root) + findCoreUiViolations(root) +
                findMediaBoundaryViolations(root) + findComposerBoundaryViolations(root) + findThreadSessionViolations(root) +
                findThreadExportHostViolations(root) + findSavedPostHostViolations(root) + findCatalogThreadViolations(root)
        check(violations.isEmpty()) {
            "Forbidden source patterns: ${violations.joinToString()}"
        }
    }

    private fun findThreadSessionViolations(root: File): List<String> {
        val allowed = listOf("thread", "domain", "core.ui", "media", "feature.thread")
        val hostPrefixes =
            listOf(
                "workspace",
                "infrastructure",
                "navigation",
                "archive",
                "catalog",
                "watch",
                "composer",
                "ui",
                "library",
                "data",
                "network",
                "database",
                "preferences",
                "update",
            )
        val linkFiles = setOf("PostLinkTarget.kt", "PostLinkBindings.kt", "PostMenuText.kt", "PostLinkHitTest.kt")
        val siteTokens = listOf("2ch.hk", "2ch.life", "DVACH_SITE_ID")
        return policyFiles.files
            .filter { source ->
                val path = source.relativeTo(root).invariantSeparatorsPath
                path.startsWith("feature/thread/src/main/")
            }.flatMap { source ->
                source.readLines().mapIndexedNotNull { index, line ->
                    val dependency = line.trim().removePrefix("import ")
                    val own = dependency.startsWith("com.solthariel.pawchan.")
                    val accepted =
                        allowed.any { dependency.startsWith("com.solthariel.pawchan.$it.") }
                    val qualifiedHost = hostPrefixes.any { line.contains("com.solthariel.pawchan.$it.") }
                    val siteSpecific = source.name in linkFiles && siteTokens.any(line::contains)
                    if ((line.trim().startsWith("import ") && own && !accepted) || qualifiedHost || siteSpecific) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-thread-session-host-import"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findThreadExportHostViolations(root: File): List<String> {
        val forbidden = listOf("ThreadUiState", "ThreadPostUi", "ThreadToolsState", "ThreadScreenMemory", "WorkspaceController")
        return policyFiles.files
            .filter {
                it.extension == "kt" &&
                    it.relativeTo(root).invariantSeparatorsPath.startsWith(
                        "app/src/main/kotlin/com/solthariel/pawchan/infrastructure/thread/export/",
                    )
            }.flatMap { source ->
                source.readLines().mapIndexedNotNull { index, line ->
                    if (forbidden.any { Regex("\\b$it\\b").containsMatchIn(line) }) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-thread-export-state"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findSavedPostHostViolations(root: File): List<String> {
        val internals =
            listOf(
                "ThreadUiState",
                "ThreadPostUi",
                "ThreadPostCard",
                "ThreadPostOptions",
                "ThreadPostInteractions",
                "PostPreviewDialog",
                "PreviewPostMenu",
                "PostPreviewTarget",
                "PostPreviewBindings",
                "LocalPostPreview",
            )
        return policyFiles.files
            .filter {
                it.extension == "kt" &&
                    it.relativeTo(root).invariantSeparatorsPath.startsWith(
                        "app/src/main/kotlin/com/solthariel/pawchan/workspace/",
                    )
            }.flatMap { source ->
                source.readLines().mapIndexedNotNull { index, line ->
                    if (internals.any { line.contains("com.solthariel.pawchan.thread.$it") }) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-saved-post-renderer-import"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findCatalogThreadViolations(root: File): List<String> =
        policyFiles.files
            .filter {
                it.extension == "kt" &&
                    it.relativeTo(root).invariantSeparatorsPath.startsWith(
                        "app/src/main/kotlin/com/solthariel/pawchan/catalog/",
                    )
            }.flatMap { source ->
                source.readLines().mapIndexedNotNull { index, line ->
                    val ownedMenu = source.name in setOf("CatalogThreadInteractions.kt", "CatalogThreadMenuHost.kt")
                    val host =
                        listOf("navigation", "workspace", "infrastructure").any {
                            line.contains("com.solthariel.pawchan.$it.")
                        }
                    if (line.contains("com.solthariel.pawchan.thread.") || (ownedMenu && host)) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-catalog-host-import"
                    } else {
                        null
                    }
                }
            }

    private fun findComposerBoundaryViolations(root: File): List<String> {
        val composerAllowed = listOf("composer", "domain", "core.ui", "media", "feature.composer", "data.network")
        val lockAllowed = listOf("security", "domain", "core.ui", "feature.applock")
        val projectReference = Regex("com\\.solthariel\\.pawchan\\.[A-Za-z0-9_.]+")
        return policyFiles.files
            .filter { source ->
                val path = source.relativeTo(root).invariantSeparatorsPath
                source.extension == "kt" &&
                    (path.startsWith("feature/composer/src/main/") || path.startsWith("feature/applock/src/main/"))
            }.flatMap { source ->
                val lock = source.relativeTo(root).invariantSeparatorsPath.startsWith("feature/applock/")
                val allowed = if (lock) lockAllowed else composerAllowed
                source.readLines().mapIndexedNotNull { index, line ->
                    val references = projectReference.findAll(line).map { it.value }
                    val foreign =
                        references.any { dependency ->
                            allowed.none {
                                dependency == "com.solthariel.pawchan.$it" || dependency.startsWith("com.solthariel.pawchan.$it.")
                            }
                        }
                    val checked = lock || line.trim().startsWith("import ")
                    if (checked && foreign) {
                        val owner = if (lock) "applock" else "composer"
                        "${source.relativeTo(root)}:${index + 1}:forbidden-$owner-host-import"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findMediaBoundaryViolations(root: File): List<String> {
        val ownerPrefix = "com.solthariel.pawchan.media"
        val peers = listOf("com.solthariel.pawchan.thread.", "com.solthariel.pawchan.composer.")
        return policyFiles.files
            .filter { source -> source.extension == "kt" && "/src/main/" in source.invariantSeparatorsPath }
            .flatMap { source ->
                val lines = source.readLines()
                val owner = lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")
                val media = owner == ownerPrefix || owner?.startsWith("$ownerPrefix.") == true
                lines.mapIndexedNotNull { index, line ->
                    val dependency = line.trim().takeIf { it.startsWith("import ") }?.removePrefix("import ")
                    val allowed =
                        listOf(
                            "com.solthariel.pawchan.media.",
                            "com.solthariel.pawchan.core.ui.",
                            "com.solthariel.pawchan.core.media.",
                            "com.solthariel.pawchan.domain.",
                            "com.solthariel.pawchan.data.network.",
                            "com.solthariel.pawchan.ffmpeg.",
                        )
                    val capability = source.relativeTo(root).invariantSeparatorsPath.startsWith("core/media/src/main/")
                    val foreign =
                        capability && dependency?.startsWith("com.solthariel.pawchan.") == true &&
                            allowed.none(dependency::startsWith)
                    if (foreign || (media && peers.any { line.trim().startsWith("import $it") })) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-media-feature-import"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findCoreUiViolations(root: File): List<String> {
        val ownPrefix = "com.solthariel.pawchan.core.ui."
        val forbidden = listOf("okhttp3.", "coil3.", "androidx.room.", "androidx.datastore.", "androidx.media3.")
        return policyFiles.files
            .filter { source ->
                source.extension == "kt" &&
                    source.relativeTo(root).invariantSeparatorsPath.startsWith("core/ui/src/main/")
            }.flatMap { source ->
                source.readLines().mapIndexedNotNull { index, line ->
                    val imported = line.trim().takeIf { it.startsWith("import ") }?.removePrefix("import ")
                    val peer = imported?.startsWith("com.solthariel.pawchan.") == true && !imported.startsWith(ownPrefix)
                    val infrastructure = imported != null && forbidden.any(imported::startsWith)
                    if (peer || infrastructure) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-core-ui-import"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findReplyBoundaryViolations(root: File): List<String> {
        val peers =
            mapOf(
                "com.solthariel.pawchan.thread" to "com.solthariel.pawchan.composer",
                "com.solthariel.pawchan.composer" to "com.solthariel.pawchan.thread",
            )
        return policyFiles.files
            .filter { source -> source.extension == "kt" && "/src/main/" in source.invariantSeparatorsPath }
            .flatMap { source ->
                val lines = source.readLines()
                val owner = lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")
                val peer = peers.entries.firstOrNull { (name, _) -> owner == name || owner?.startsWith("$name.") == true }?.value
                lines.mapIndexedNotNull { index, line ->
                    if (peer != null && line.trim().startsWith("import $peer.")) {
                        "${source.relativeTo(root)}:${index + 1}:forbidden-reply-feature-import"
                    } else {
                        null
                    }
                }
            }
    }

    private fun findLayerViolations(root: File): List<String> {
        val projectPrefix = "com.solthariel.pawchan."
        val domainPrefix = "${projectPrefix}domain."
        val dataPrefix = "${projectPrefix}data."
        val platformImports = listOf("android.", "androidx.", "okhttp3.", "coil3.", "org.jsoup.", "kotlinx.serialization.", "java.sql.")
        return policyFiles.files
            .filter { source -> source.extension == "kt" && "/src/main/" in source.invariantSeparatorsPath }
            .flatMap { source ->
                val path = source.relativeTo(root).invariantSeparatorsPath
                source.readLines().mapIndexedNotNull { index, line ->
                    val dependency = line.trim().takeIf { it.startsWith("import ") }?.removePrefix("import ")
                    val outsideDomain = dependency?.startsWith(projectPrefix) == true && !dependency.startsWith(domainPrefix)
                    val platform = dependency != null && platformImports.any(dependency::startsWith)
                    val appDependency = outsideDomain && dependency?.startsWith(dataPrefix) != true
                    val invalid =
                        (path.startsWith("domain/") && (outsideDomain || platform)) ||
                            (path.startsWith("data/") && appDependency)
                    if (invalid) "$path:${index + 1}:forbidden-layer-import" else null
                }
            }
    }

    private fun findPerformanceViolations(root: File): List<String> {
        val forbiddenByPath =
            mapOf(
                "app/src/main/kotlin/com/solthariel/pawchan/catalog/CatalogCards.kt" to
                    listOf(
                        "SubcomposeAsyncImage",
                        "RichPostText(",
                        "buildAnnotatedString",
                        "Formatter.",
                        "mutableListOf(",
                        ".matching(",
                        "originalUrl",
                        "filter {",
                        "map {",
                        "sortedBy",
                        "joinToString",
                    ),
                "app/src/main/kotlin/com/solthariel/pawchan/boards/BoardsDirectoryList.kt" to
                    listOf("BoardRows(", "BoardCategorySection(", "AnimatedVisibility"),
                "app/src/main/kotlin/com/solthariel/pawchan/boards/BoardDirectoryRow.kt" to
                    listOf("mutableListOf(", "filter {", "map {", "sortedBy", "joinToString"),
                "core/media/src/main/kotlin/com/solthariel/pawchan/media/PostAttachments.kt" to
                    listOf(
                        "SubcomposeAsyncImage",
                        "LazyRow",
                        "buildAnnotatedString",
                        "originalUrl",
                        "filter {",
                        "map {",
                        "sortedBy",
                        "joinToString",
                    ),
                "feature/thread/src/main/kotlin/com/solthariel/pawchan/thread/RichPostText.kt" to
                    listOf("buildAnnotatedString", "filter {", "map {", "sortedBy", "joinToString"),
                "feature/thread/src/main/kotlin/com/solthariel/pawchan/thread/ThreadPostCard.kt" to
                    listOf(
                        "SubcomposeAsyncImage",
                        "buildAnnotatedString",
                        "filter {",
                        "map {",
                        "sortedBy",
                        "joinToString",
                    ),
                "core/media/src/main/kotlin/com/solthariel/pawchan/media/thumbnail/ThumbnailLoading.kt" to
                    listOf(
                        "SubcomposeAsyncImage",
                        "originalUrl",
                    ),
            )
        return forbiddenByPath.flatMap { (path, forbidden) ->
            val source = root.resolve(path)
            if (!source.exists()) {
                listOf("$path:missing-hot-path-source")
            } else {
                val text = source.readText()
                forbidden.filter(text::contains).map { token -> "$path:$token" }
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

val versionsRoot =
    JsonSlurper().parseText(
        providers
            .fileContents(layout.projectDirectory.file("update/publishing/versions.json"))
            .asText
            .get(),
    ) as? Map<*, *> ?: error("versions.json root must be an object")
val versionsData =
    (versionsRoot["versions"] as? List<*>)
        ?.map { version -> version as? Map<*, *> ?: error("Invalid version entry") }
        ?: error("versions.json must contain versions")
val latestVersion =
    versionsData
        .maxByOrNull { (it["code"] as? Number)?.toInt() ?: error("Version code must be a number") }
        ?: error("update/publishing/versions.json has no versions")
val appVersionCode =
    (latestVersion["code"] as? Number)?.toInt()
        ?: error("Version code must be a number")
val appVersionName =
    latestVersion["name"] as? String
        ?: error("Version name must be a string")

rootProject.extra.apply {
    set("appVersionCode", appVersionCode)
    set("appVersionName", appVersionName)
}

val minimumUnsafeFlagJavaVersion = 24
val detektCliDependency = libs.detekt.cli
val ktlintCliDependency = libs.ktlint.cli
val productionProjects = subprojects.filter { it.buildFile.exists() }

configure(productionProjects) {
    val detektTool = configurations.create("detektTool")
    dependencies.add(detektTool.name, detektCliDependency)

    tasks.register<JavaExec>("detekt") {
        group = "verification"
        description = "Runs Detekt without the deprecated Gradle plugin API."
        classpath = detektTool
        mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
        args(
            "--input",
            "$projectDir/src/main,$projectDir/src/test",
            "--config",
            rootProject.file("detekt.yml"),
            "--build-upon-default-config",
            "--parallel",
        )
        if (JavaVersion.current().majorVersion.toInt() >= minimumUnsafeFlagJavaVersion) {
            jvmArgs("--sun-misc-unsafe-memory-access=allow")
        }
    }
}

val ktlintTool =
    configurations.create("ktlintTool") {
        attributes {
            attribute(
                Bundling.BUNDLING_ATTRIBUTE,
                objects.named(Bundling.SHADOWED),
            )
        }
    }
dependencies.add(ktlintTool.name, ktlintCliDependency)

tasks.register<VerifyReleaseMetadata>("verifyReleaseMetadata") {
    group = "verification"
    description = "Validates versions, the Russian changelog, and the update manifest."
    metadataFile.set(layout.projectDirectory.file("update/publishing/versions.json"))
    changelogFile.set(
        layout.projectDirectory.file("update/publishing/changelogs/ru/$appVersionCode.txt"),
    )
    bundledChangelogFile.set(layout.projectDirectory.file("app/src/main/assets/settings-changelog.txt"))
    manifestFile.set(layout.projectDirectory.file("update/publishing/manifest/data-v1.json"))
    currentVersionCode.set(appVersionCode)
    currentVersionName.set(appVersionName)
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks all Kotlin sources and build scripts."
    classpath = ktlintTool
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = rootDir
    args(
        "--relative",
        "**/*.kt",
        "**/*.kts",
        "!**/build/**",
        "!**/.gradle/**",
    )
    if (JavaVersion.current().majorVersion.toInt() >= minimumUnsafeFlagJavaVersion) {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
}

// Explicit source-writing task. qualityCheck and LOCAL CHECK remain read-only checks.
tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formats Kotlin sources with the same KtLint version and rules as ktlintCheck."
    classpath = ktlintTool
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = rootDir
    args(
        "--format",
        "--relative",
        "**/*.kt",
        "**/*.kts",
        "!**/build/**",
        "!**/.gradle/**",
    )
    if (JavaVersion.current().majorVersion.toInt() >= minimumUnsafeFlagJavaVersion) {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
}

val policySources =
    fileTree(rootDir) {
        include("**/*.kt", "**/*.kts")
        exclude("**/build/**", "**/.gradle/**")
    }
val policyBaselines =
    fileTree(rootDir) {
        include("**/*baseline*.xml")
        exclude("**/build/**", "**/.gradle/**")
    }
tasks.register<VerifySourcePolicy>("verifySourcePolicy") {
    group = "verification"
    description = "Rejects source-level suppressions and quality baselines."
    policyFiles.from(policySources, policyBaselines)
    rootDirectory.set(layout.projectDirectory)
}

tasks.register("nativeBuildToolCheck") {
    group = "verification"
    description = "Runs isolated native-build Kotlin tests, KtLint and Detekt without an Android SDK."
    dependsOn(gradle.includedBuild("pawchan-native-build").task(":check"))
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs checks for every production module."
    dependsOn("nativeBuildToolCheck")
    dependsOn(productionProjects.map { "${it.path}:check" })
    dependsOn(productionProjects.map { "${it.path}:detekt" })
    dependsOn("ktlintCheck")
    dependsOn("verifySourcePolicy")
    dependsOn("verifyReleaseMetadata")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
