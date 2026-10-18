package com.solthariel.pawchan.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

internal class UpdateVerifier(
    private val context: Context,
) {
    fun verify(
        apk: File,
        release: UpdatePackage,
    ) {
        check(apk.length() == release.length) { "APK length mismatch" }
        val expectedHash = normalizeHash(checkNotNull(release.sha256) { "APK SHA-256 is missing" })
        check(SHA_256_PATTERN.matches(expectedHash)) { "APK SHA-256 format is invalid" }
        check(sha256(apk) == expectedHash) { "APK SHA-256 mismatch" }

        val archive = archivePackageInfo(apk)
        val installed = installedPackageInfo()
        check(archive.packageName == context.packageName) { "APK package name mismatch" }
        check(PackageInfoCompat.getLongVersionCode(archive) == release.versionCode) {
            "APK version code mismatch"
        }
        check(archive.versionName == release.versionName) { "APK version name mismatch" }
        check(signerDigests(archive) == signerDigests(installed)) { "APK signer mismatch" }
    }

    private fun archivePackageInfo(apk: File): PackageInfo =
        checkNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    apk.absolutePath,
                    PackageManager.PackageInfoFlags.of(SIGNING_FLAGS),
                )
            } else {
                legacyPackageInfo("getPackageArchiveInfo", apk.absolutePath)
            },
        ) { "Cannot inspect update APK" }

    private fun installedPackageInfo(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(SIGNING_FLAGS),
            )
        } else {
            checkNotNull(legacyPackageInfo("getPackageInfo", context.packageName)) {
                "Cannot inspect installed APK"
            }
        }

    private fun legacyPackageInfo(
        methodName: String,
        packageReference: String,
    ): PackageInfo? {
        val method =
            context.packageManager.javaClass.getMethod(
                methodName,
                String::class.java,
                Int::class.javaPrimitiveType,
            )
        return method.invoke(
            context.packageManager,
            packageReference,
            PackageManager.GET_SIGNING_CERTIFICATES,
        ) as? PackageInfo
    }

    private fun signerDigests(packageInfo: PackageInfo): Set<String> {
        val signingInfo = checkNotNull(packageInfo.signingInfo) { "APK signing information is missing" }
        val signatures =
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        return signatures.mapTo(mutableSetOf()) { sha256(it.toByteArray()) }
    }
}

internal fun sha256(file: File): String =
    FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(HASH_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().toHex()
    }

internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance(SHA_256).digest(bytes).toHex()

internal fun normalizeHash(value: String): String = value.filter(Char::isLetterOrDigit).lowercase()

private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

private const val HASH_BUFFER_SIZE = 64 * 1024
private const val SHA_256 = "SHA-256"
private val SIGNING_FLAGS = PackageManager.GET_SIGNING_CERTIFICATES.toLong()
private val SHA_256_PATTERN = Regex("[0-9a-f]{64}")
