package com.mskinik.examplelibrary

object IntegerUtil {
    fun isPrime(number: Int): Boolean {
        if (number < 2) return false
        for (i in 2..Math.sqrt(number.toDouble()).toInt()) {
            if (number % i == 0) return false
        }
        return true
    }
}

//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