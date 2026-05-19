package com.archura.airprint.infrastructure.airprint.ipp

import com.archura.airprint.domain.model.DocumentFormat
import com.archura.airprint.domain.model.IppOperation
import com.archura.airprint.domain.model.PrintJob
import com.archura.airprint.domain.repository.PrintJobRepository
import com.archura.airprint.domain.repository.PrinterStatusRepository
import com.archura.airprint.domain.repository.ReceivedImagesRepository
import com.archura.airprint.infrastructure.airprint.document.DecodedDocument
import com.archura.airprint.infrastructure.airprint.document.PrintJobDecoder
import com.archura.airprint.infrastructure.network.LocalNetworkAddressResolver
import java.util.UUID
import javax.inject.Inject

class IppProtocolHandler @Inject constructor(
    private val ippParser: IppParser,
    private val ippResponseBuilder: IppResponseBuilder,
    private val printJobDecoder: PrintJobDecoder,
    private val printJobRepository: PrintJobRepository,
    private val printerStatusRepository: PrinterStatusRepository,
    private val receivedImagesRepository: ReceivedImagesRepository,
    private val localNetworkAddressResolver: LocalNetworkAddressResolver,
) {
    suspend fun handle(
        requestBytes: ByteArray,
        senderAddress: String?,
    ): ByteArray {
        val request = ippParser.parse(requestBytes)

        return when (request.operation) {
            IppOperation.GET_PRINTER_ATTRIBUTES -> handleGetPrinterAttributes(request)
            IppOperation.VALIDATE_JOB -> ippResponseBuilder.buildOperationOkResponse(request.requestId)
            IppOperation.CREATE_JOB -> handleCreateJob(request)
            IppOperation.PRINT_JOB,
            IppOperation.SEND_DOCUMENT,
            -> handleDocumentRequest(request, senderAddress)
            IppOperation.GET_JOBS,
            IppOperation.GET_JOB_ATTRIBUTES,
            IppOperation.CANCEL_JOB,
            -> ippResponseBuilder.buildOperationOkResponse(request.requestId)
            IppOperation.UNKNOWN,
            -> ippResponseBuilder.buildBadRequestResponse(request.requestId)
        }
    }

    private fun handleGetPrinterAttributes(request: IppRequest): ByteArray {
        val status = printerStatusRepository.printerStatus.value
        return ippResponseBuilder.buildPrinterAttributesResponse(
            requestId = request.requestId,
            printerName = status.serviceName,
            printerUri = printerUri(status.port),
        )
    }

    private fun handleCreateJob(request: IppRequest): ByteArray {
        return ippResponseBuilder.buildPrintJobAcceptedResponse(
            requestId = request.requestId,
            jobId = request.requestId,
            printerUri = printerUri(printerStatusRepository.printerStatus.value.port),
        )
    }

    private suspend fun handleDocumentRequest(
        request: IppRequest,
        senderAddress: String?,
    ): ByteArray {
        val decodedDocument = printJobDecoder.decode(request.payload)
            ?: DecodedDocument(
                format = DocumentFormat.UNKNOWN,
                bytes = request.payload,
            )

        receivedImagesRepository.saveReceivedDocument(
            documentBytes = decodedDocument.bytes,
            format = decodedDocument.format,
            senderAddress = senderAddress,
        )
        printJobRepository.recordPrintJob(
            PrintJob(
                id = UUID.randomUUID().toString(),
                operation = request.operation,
                senderAddress = senderAddress,
                documentFormat = decodedDocument.format,
                receivedAtMillis = System.currentTimeMillis(),
                sizeBytes = decodedDocument.bytes.size.toLong(),
            ),
        )

        return ippResponseBuilder.buildPrintJobAcceptedResponse(
            requestId = request.requestId,
            jobId = request.requestId,
            printerUri = printerUri(printerStatusRepository.printerStatus.value.port),
        )
    }

    private fun printerUri(port: Int): String {
        val host = localNetworkAddressResolver.resolveIpv4Address()?.hostAddress ?: LOCALHOST
        return "ipp://$host:$port/ipp/print"
    }

    private companion object {
        const val LOCALHOST = "127.0.0.1"
    }
}
