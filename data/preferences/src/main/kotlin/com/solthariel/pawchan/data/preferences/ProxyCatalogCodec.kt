package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.domain.proxy.validProxyCatalog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

internal fun encodeProxyCatalog(catalog: List<SavedProxy>): EncodedProxyCatalog {
    require(validProxyCatalog(catalog))
    val output = ByteArrayOutputStream()
    val secrets = ByteArrayOutputStream()
    DataOutputStream(output).use { data ->
        DataOutputStream(secrets).use { secret ->
            data.writeInt(VERSION)
            data.writeInt(catalog.size)
            catalog.forEach { proxy ->
                data.writeUTF(proxy.id)
                data.writeUTF(proxy.name)
                data.writeUTF(proxy.mode.name)
                data.writeUTF(proxy.address)
                data.writeBoolean(proxy.enabled)
                data.writeInt(proxy.services.size)
                proxy.services.sortedBy { it.name }.forEach { data.writeUTF(it.name) }
                data.writeBoolean(proxy.credentials != null)
                proxy.credentials?.let {
                    secret.writeUTF(it.username)
                    secret.writeUTF(it.password)
                }
            }
        }
    }
    val encoded = Base64.getEncoder().encodeToString(output.toByteArray())
    require(encoded.length <= MAX_ENCODED_LENGTH)
    return EncodedProxyCatalog(encoded, secrets.toByteArray().takeIf { it.isNotEmpty() })
}

internal fun decodeProxyCatalog(
    encoded: String,
    secrets: ByteArray? = null,
): List<SavedProxy>? =
    try {
        if (encoded.length > MAX_ENCODED_LENGTH) {
            null
        } else {
            decodeCatalogBytes(Base64.getDecoder().decode(encoded), secrets)
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.io.IOException) {
        null
    }

private fun decodeCatalogBytes(
    bytes: ByteArray,
    secrets: ByteArray?,
): List<SavedProxy>? =
    DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        DataInputStream(ByteArrayInputStream(secrets ?: byteArrayOf())).use { secret ->
            if (input.readInt() != VERSION) return null
            val count = input.readInt()
            if (count !in 0..MAX_PROFILES) return null
            val catalog = List(count) { readSavedProxy(input, secret) }
            if (input.available() != 0 || secret.available() != 0 || !validProxyCatalog(catalog)) null else catalog
        }
    }

private fun readSavedProxy(
    input: DataInputStream,
    secret: DataInputStream,
): SavedProxy {
    val id = input.readUTF()
    val name = input.readUTF()
    val mode = ProxyMode.valueOf(input.readUTF())
    val address = input.readUTF()
    val enabled = input.readBoolean()
    val count = input.readInt()
    require(count in 0..ProxyService.entries.size)
    val services = List(count) { ProxyService.valueOf(input.readUTF()) }.toSet()
    require(services.size == count)
    val credentials = if (input.readBoolean()) ProxyCredentials(secret.readUTF(), secret.readUTF()) else null
    return SavedProxy(id, name, mode, address, enabled, services, credentials)
}

private const val VERSION = 2
private const val MAX_PROFILES = 100
private const val MAX_ENCODED_LENGTH = 250_000
