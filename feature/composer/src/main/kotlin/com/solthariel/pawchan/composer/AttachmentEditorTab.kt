package com.solthariel.pawchan.composer

internal enum class AttachmentEditorTab(
    val label: String,
) {
    FILE("Файл"),
    NAME("Имя"),
    CROP("Пропорции"),
    ROTATE("Поворот"),

    DRAW("Рисовать"),
    REDACT("Замазать"),
}
