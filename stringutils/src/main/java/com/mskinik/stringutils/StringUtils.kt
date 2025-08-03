package com.mskinik.stringutils

object StringUtils {
    fun reverse(text: String): String = text.reversed()

    fun capitalizeWords(text: String): String =
        text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    fun removeTurkishChars(text: String): String {
        val turkishMap = mapOf(
            'ç' to 'c', 'Ç' to 'C',
            'ğ' to 'g', 'Ğ' to 'G',
            'ı' to 'i', 'İ' to 'I',
            'ö' to 'o', 'Ö' to 'O',
            'ş' to 's', 'Ş' to 'S',
            'ü' to 'u', 'Ü' to 'U'
        )
        return text.map { turkishMap[it] ?: it }.joinToString("")
    }

    fun newfun() = "newfun"

    fun tagv140funtion() = "v1.4.0"

}
