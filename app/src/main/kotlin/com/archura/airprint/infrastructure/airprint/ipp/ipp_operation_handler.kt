package com.archura.airprint.infrastructure.airprint.ipp

interface IppOperationHandler {
    suspend fun handle(request: IppRequest): ByteArray
}
