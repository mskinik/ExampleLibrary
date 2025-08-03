package com.mskinik.booleanutils

object BooleanUtils {
    fun isTrueOrDefault(value: Boolean?, default: Boolean = false): Boolean {
        return value ?: default
    }
}