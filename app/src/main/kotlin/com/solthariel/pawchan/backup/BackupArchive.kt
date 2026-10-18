package com.solthariel.pawchan.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal fun encodeClientBackup(source: ClientBackup): ByteArray {
    require(source.sections.isNotEmpty())
    val selected =
        selectedBackup(source.settings, source.favorites, source.bookmarks, source.sections)
            .copy(
                ownMarkers = source.ownMarkers.takeIf { BackupSection.OWN_POSTS in source.sections }.orEmpty(),
                metadata = source.metadata,
            )
    val payload = backupPayload(selected)
    val manifest =
        buildJsonObject {
            put("format", "pawchan-backup")
            put("version", BACKUP_FORMAT_VERSION)
            put("createdAt", selected.metadata.createdAt)
            put("appVersion", selected.metadata.appVersion)
            put(
                "sections",
                buildJsonObject {
                    backupCounts(selected).forEach { (section, count) -> put(section.name, count) }
                },
            )
        }.toString()
    val files = mapOf("manifest.json" to manifest) + payload
    require(files.values.sumOf { it.toByteArray(Charsets.UTF_8).size.toLong() } <= BACKUP_MAX_UNPACKED_BYTES)
    return ByteArrayOutputStream()
        .also { output ->
            ZipOutputStream(output).use { zip ->
                files.forEach { (name, text) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }.toByteArray()
        .also { require(it.size <= CLIENT_BACKUP_MAX_BYTES) }
}

internal fun decodeClientBackup(bytes: ByteArray): ClientBackup {
    val files = readArchive(bytes)
    val manifest = Json.parseToJsonElement(files.getValue("manifest.json")).jsonObject
    require(manifest.getValue("format").jsonPrimitive.content == "pawchan-backup")
    require(manifest.getValue("version").jsonPrimitive.int == BACKUP_FORMAT_VERSION) { "Неподдерживаемая версия копии" }
    val sections =
        manifest
            .getValue("sections")
            .jsonObject
            .map { (name, count) ->
                BackupSection.valueOf(name) to count.jsonPrimitive.int.also { require(it >= 0) }
            }.toMap()
    require(sections.isNotEmpty())
    require(files.keys == sections.keys.map { it.fileName() }.toSet() + "manifest.json")
    val metadata =
        BackupMetadata(
            manifest
                .getValue("createdAt")
                .jsonPrimitive.long
                .also { require(it >= 0L) },
            manifest
                .getValue("appVersion")
                .jsonPrimitive.content
                .also { require(it.length <= MAX_VERSION_CHARS) },
        )
    return readBackupPayload(files - "manifest.json", metadata).also {
        require(backupCounts(it) == sections) { "Состав копии не соответствует манифесту" }
    }
}

private fun readArchive(bytes: ByteArray): Map<String, String> {
    require(bytes.size in MIN_ZIP_BYTES..CLIENT_BACKUP_MAX_BYTES)
    require(bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte())
    val expectedEntries = validateZipDirectory(bytes)
    val allowed = BackupSection.entries.map { it.fileName() }.toSet() + "manifest.json"
    val files = linkedMapOf<String, String>()
    var total = 0L
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            require(name in allowed && name !in files && !entry.isDirectory) { "Неизвестный или повторяющийся раздел" }
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = zip.read(buffer)
            while (read >= 0) {
                total += read
                require(total <= BACKUP_MAX_UNPACKED_BYTES) { "Распакованная копия слишком велика" }
                output.write(buffer, 0, read)
                read = zip.read(buffer)
            }
            zip.closeEntry()
            files[name] = output.toByteArray().toString(Charsets.UTF_8)
            entry = zip.nextEntry
        }
    }
    require("manifest.json" in files && files.size == expectedEntries)
    return files
}

private const val BACKUP_FORMAT_VERSION = 3
private const val MIN_ZIP_BYTES = 4
private const val MAX_VERSION_CHARS = 128
