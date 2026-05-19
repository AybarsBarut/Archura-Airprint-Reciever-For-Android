package com.archura.airprint.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(timestampMillis: Long): String {
    return DateFormat
        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(timestampMillis))
}
