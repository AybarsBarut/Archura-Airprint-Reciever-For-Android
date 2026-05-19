package com.archura.airprint.domain.repository

import com.archura.airprint.domain.model.PrintJob

interface PrintJobRepository {
    suspend fun recordPrintJob(printJob: PrintJob)
}
