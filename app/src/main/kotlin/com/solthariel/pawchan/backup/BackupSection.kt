package com.solthariel.pawchan.backup

internal enum class BackupSection(
    val label: String,
) {
    SETTINGS("Настройки"),
    FAVORITES("Избранное"),
    FILTERS("Правила автоскрытия"),
    HIDDEN("Скрытые треды"),
    HISTORY("История посещений"),
    OWN_POSTS("Свои посты и ответы"),
}

internal val DEFAULT_BACKUP_SECTIONS =
    setOf(
        BackupSection.SETTINGS,
        BackupSection.FAVORITES,
        BackupSection.FILTERS,
        BackupSection.HIDDEN,
    )
