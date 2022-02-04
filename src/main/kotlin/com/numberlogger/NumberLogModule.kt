package com.numberlogger

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import java.nio.file.FileSystems

data class NumberLogAppConfig(
    val filePath: String,
    val maxConnections: Int,
    val reportEveryNMillis: Long,
    val flushBufferEveryNMillis: Long,
    val serverPort: Int
)

class NumberLogModule(
    private val config: NumberLogAppConfig
) : AbstractModule() {

    @Provides
    @Singleton
    fun providesServer(numberLogger: NumberLogger) =
        Server(numberLogger, config.serverPort, config.maxConnections)

    @Provides
    @Singleton
    fun providesNumberLocalFileClient() =
        NumberLocalFileClient(FileSystems.getDefault().getPath(config.filePath), config.flushBufferEveryNMillis)

    @Provides
    @Singleton
    fun providesReporter(numberLogger: NumberLogger) =
        Reporter(numberLogger, config.reportEveryNMillis)
}
