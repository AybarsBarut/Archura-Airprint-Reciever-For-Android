package com.archura.airprint.di

import com.archura.airprint.data.repository_impl.PrintJobRepositoryImpl
import com.archura.airprint.data.repository_impl.PrinterStatusRepositoryImpl
import com.archura.airprint.data.repository_impl.ReceivedImagesRepositoryImpl
import com.archura.airprint.domain.repository.PrintJobRepository
import com.archura.airprint.domain.repository.PrinterStatusRepository
import com.archura.airprint.domain.repository.ReceivedImagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindPrintJobRepository(
        implementation: PrintJobRepositoryImpl,
    ): PrintJobRepository

    @Binds
    @Singleton
    abstract fun bindPrinterStatusRepository(
        implementation: PrinterStatusRepositoryImpl,
    ): PrinterStatusRepository

    @Binds
    @Singleton
    abstract fun bindReceivedImagesRepository(
        implementation: ReceivedImagesRepositoryImpl,
    ): ReceivedImagesRepository
}
