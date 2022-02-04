package com.numberlogger

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class Reporter @Inject constructor(
    private val numberLogger: NumberLogger,
    private val reportEveryNMillis: Long
) : Runnable, Closeable {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val executor = Executors.newSingleThreadScheduledExecutor()

    private var lastKnownStats: Stats = Stats.empty()

    override fun run() {
        executor.scheduleAtFixedRate(
            {
                val latestStats = numberLogger.stats()
                val newUnique = latestStats.uniqueTotal - lastKnownStats.uniqueTotal
                val newDuplicates = latestStats.duplicatesCount - lastKnownStats.duplicatesCount
                log.info("Received $newUnique unique numbers, $newDuplicates duplicates. Unique total: ${latestStats.uniqueTotal}")
                lastKnownStats = latestStats
            },
            reportEveryNMillis,
            reportEveryNMillis,
            TimeUnit.MILLISECONDS
        )
    }

    override fun close() {
        if (!executor.gracefulShutdown()) {
            log.warn("Could not shut down report scheduler gracefully")
        }
    }
}
