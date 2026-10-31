package com.solthariel.pawchan.update

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal interface UpdateTokenStore {
    fun read(): String?

    fun save(token: String)

    fun clear()
}

/** Blocking operations are called only from IO/UI-owned workers or OkHttp's network thread. */
internal class AndroidUpdateTokenStore(
    context: Context,
) : UpdateTokenStore {
    private val app = context.applicationContext
    private val applicationId = app.packageName
    private val alias = "$applicationId.private-updates.v1"
    private val file by lazy { AtomicFile(File(app.noBackupFilesDir, "private-update-access.bin")) }

    @Synchronized
    override fun read(): String? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        val bytes =
            file.openRead().use { input ->
                val buffer = ByteArray(MAX_CIPHERTEXT_BYTES + 1)
                var count = 0
                while (count < buffer.size) {
                    val size = input.read(buffer, count, buffer.size - count)
                    if (size < 0) break
                    count += size
                }
                check(count <= MAX_CIPHERTEXT_BYTES) { "Stored access data exceeds limit" }
                buffer.copyOf(count)
            }
        return UpdateTokenCipher(key(), applicationId).decrypt(bytes)
    }

    @Synchronized
    override fun save(token: String) {
        val bytes = UpdateTokenCipher(key(), applicationId).encrypt(token)
        val output = file.startWrite()
        var committed = false
        try {
            output.write(bytes)
            file.finishWrite(output)
            committed = true
        } finally {
            if (!committed) file.failWrite(output)
        }
    }

    @Synchronized
    override fun clear() {
        file.delete()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .run {
                init(
                    KeyGenParameterSpec
                        .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(UPDATE_KEY_BITS)
                        .build(),
                )
                generateKey()
            }
    }
}

private const val UPDATE_KEY_BITS = 256
