package com.archura.airprint.data.repository_impl

import com.archura.airprint.domain.model.PrintJob
import com.archura.airprint.domain.repository.PrintJobRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintJobRepositoryImpl @Inject constructor() : PrintJobRepository {
    private val printJobs = mutableListOf<PrintJob>()

    override suspend fun recordPrintJob(printJob: PrintJob) {
        printJobs += printJob
    }
}
