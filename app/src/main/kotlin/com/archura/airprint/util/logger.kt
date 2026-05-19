package com.archura.airprint.util

import android.util.Log
import javax.inject.Inject

class Logger @Inject constructor() {
    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
