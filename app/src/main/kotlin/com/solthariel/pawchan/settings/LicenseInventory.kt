package com.solthariel.pawchan.settings

internal data class DeclaredLicense(
    val name: String,
    val url: String,
)

internal data class LicensedComponent(
    val coordinate: String,
    val licenses: List<DeclaredLicense>,
)

/** Read the release auditor's declarations without confusing embedded notice text for metadata. */
internal fun parseLicenseInventory(lines: List<String>): List<LicensedComponent> {
    val components = mutableListOf<LicensedComponent>()
    var index = 0
    while (index + 1 < lines.size) {
        val coordinate = lines[index]
        if (COORDINATE.matches(coordinate) && lines[index + 1].startsWith("Licensing source: ")) {
            val licenses = mutableListOf<DeclaredLicense>()
            index += 2
            while (index < lines.size && lines[index].isNotBlank() && !lines[index].startsWith("--- ")) {
                val pair = lines[index].split(" — ", limit = 2)
                if (pair.size == 2 && pair[0].isNotBlank()) licenses.add(DeclaredLicense(pair[0], pair[1]))
                index++
            }
            if (licenses.isNotEmpty()) components.add(LicensedComponent(coordinate, licenses))
        } else {
            index++
        }
    }
    return components
}

private val COORDINATE = Regex("[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9_.+\\-]+")
