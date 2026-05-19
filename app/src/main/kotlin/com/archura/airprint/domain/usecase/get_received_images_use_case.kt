package com.archura.airprint.domain.usecase

import com.archura.airprint.domain.repository.ReceivedImagesRepository
import javax.inject.Inject

class GetReceivedImagesUseCase @Inject constructor(
    private val receivedImagesRepository: ReceivedImagesRepository,
) {
    operator fun invoke() = receivedImagesRepository.getReceivedImages()
}
