package com.archura.airprint.ui.screens.received_images

import com.archura.airprint.domain.model.ReceivedImage

data class ReceivedImageDetailState(
    val image: ReceivedImage? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
)
