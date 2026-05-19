package com.archura.airprint.util

fun String?.orFallback(fallback: String): String {
    return if (isNullOrBlank()) fallback else this
}
