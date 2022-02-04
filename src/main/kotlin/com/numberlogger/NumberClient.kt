package com.numberlogger

import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class NumberLocalFileClient(
    private val filePath: Path,
    private val flushBufferEveryNMillis: Long
) : Runnable, Closeable {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val cachedNumbers = mutableSetOf<Int>()
    private val buffer: StringBuffer = StringBuffer()

    override fun run() {
        Files.write(filePath, ByteArray(0), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        executor.scheduleAtFixedRate(
            { flushBuffer() },
            flushBufferEveryNMillis,
            flushBufferEveryNMillis,
            TimeUnit.MILLISECONDS
        )
    }

    override fun close() {
        flushBuffer()
        executor.gracefulShutdown()
    }

    fun save(number: Int) {
        buffer.append("${number.addLeadingZeroes(MAX_LENGTH)}${System.lineSeparator()}")
        cachedNumbers.add(number)
    }

    fun isLogged(number: Int) = cachedNumbers.contains(number)

    fun getAll(): List<Int> =
        Files.newInputStream(filePath)
            .bufferedReader()
            .use { bR -> bR.lines().map { it.toInt() }.collect(Collectors.toList()) }

    private fun flushBuffer() {
        if (buffer.isEmpty()) {
            return
        }
        val bwr = BufferedWriter(FileWriter(filePath.toString(), true))
        bwr.write(buffer.toString())
        bwr.flush()
        bwr.close()
        buffer.delete(0, buffer.length)
    }
}
