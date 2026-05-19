package com.archura.airprint.data.mapper

import com.archura.airprint.domain.model.ReceivedImage
import java.io.File

fun ReceivedImage.toFile(): File {
    return File(path)
}
