package com.mskinik.stringutils

object StringUtils {
    fun reverse(text: String): String = text.reversed()

    fun capitalizeWords(text: String): String =
        text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
