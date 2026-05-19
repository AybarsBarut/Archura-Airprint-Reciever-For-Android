package com.archura.airprint.util

import java.io.File

fun File.ensureDirectory(): File {
    if (!exists()) {
        mkdirs()
    }
    return this
}
