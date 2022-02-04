package com.numberlogger

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.math.sign

class InvalidNumberException : RuntimeException()

data class Stats(
    val duplicatesCount: Int,
    val uniqueTotal: Int
) {
    companion object {
        fun empty () = Stats(0, 0)
    }
}

const val MAX_LENGTH = 9

class NumberLogger @Inject constructor(
    private val client: NumberLocalFileClient
) {

    fun logNumber(rawInput: String) {
        val number = parseNumber(input = rawInput, newline = System.lineSeparator())
            .also { ensureValidLength(it) }

        if (client.isLogged(number)) {
            duplicateTotalCount.incrementAndGet()
            return
        }

        client.save(number)

        uniqueTotalCount.incrementAndGet()
    }

    fun stats() = Stats(duplicateTotalCount.toInt(), uniqueTotalCount.toInt())

    private fun parseNumber(input: String, newline: String) =
        input.removeSuffix(newline).toIntOrNull() ?: throw InvalidNumberException()

    private fun ensureValidLength(number: Int) =
        number.takeIf { it.length() <= MAX_LENGTH } ?: throw InvalidNumberException()

    companion object {
        var duplicateTotalCount = AtomicInteger(0)
        var uniqueTotalCount = AtomicInteger(0)
    }
}

/**
 * Performant way of knowing the length of an integer
 */
fun Int.length(): Int {
    var length = 1
    var tmp = this
    if (tmp >= 100000000) {
        length += 8
        tmp /= 100000000
    }
    if (tmp >= 10000) {
        length += 4
        tmp /= 10000
    }
    if (tmp >= 100) {
        length += 2
        tmp /= 100
    }
    if (tmp >= 10) {
        length += 1
    }
    return length
}

fun Int.addLeadingZeroes(upTo: Int): String {
    val length = this.length()
    if (length == upTo) {
        return this.toString()
    }

    return String.format("%0${ if (this.sign == -1) upTo + 1 else upTo }d", this)
}
