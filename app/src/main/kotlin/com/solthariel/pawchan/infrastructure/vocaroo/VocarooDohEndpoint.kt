package com.solthariel.pawchan.infrastructure.vocaroo

/**
 * Хосты, для которых действует настройка «DNS для Vocaroo» (vocaroo.com, voca.ro, media.vocaroo.com
 * и зеркала media1…); остальные запросы её не видят.
 */
internal fun isVocarooHost(host: String): Boolean {
    val name = host.lowercase().trimEnd('.')
    return name == "vocaroo.com" || name == "voca.ro" || name.endsWith(".vocaroo.com")
}
