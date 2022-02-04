package com.numberlogger

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.net.Socket
import java.time.Duration


object TestClient {

    fun sendLine(line: String, throughPort: Int) {
        sendLines(listOf(line), throughPort)
    }

    fun sendLines(lines: List<String>, throughPort: Int) {
        val socket = connect(throughPort)

        val out = PrintWriter(socket.getOutputStream(), true)
        lines.forEach { out.write(it) }
        out.flush()

        out.close()
        socket.close()
    }

    private fun connect(port: Int): Socket {
        var socket: Socket? = null
        retry(times = 3, delay = Duration.ofMillis(30)) {
            socket = Socket("127.0.0.1", port)
        }
        return socket!!
    }
}

fun retry(times: Int, delay: Duration, action: () -> Unit) {
    var tries = 1
    while (true) {
        try {
            action()
            break
        } catch (ex: Exception) {
            if (tries == times) {
                throw ex
            }
            ++tries
            log.warn("processing failed, retrying (attempt $tries / $times) in ${delay.toMillis()} millis.", ex)
            Thread.sleep(delay.toMillis())
        }
    }
}

private val log = LoggerFactory.getLogger(TestClient::class.java)

fun main() {
    val lines = mutableListOf<String>()
    repeat(6700000) {
        lines.add("$it${System.lineSeparator()}")
    }

    log.info("bombarding server")
    val socket = Socket("127.0.0.1", 4000)
    val out = PrintWriter(socket.getOutputStream(), true)

    lines.forEach {
        out.write(it)
        out.flush()
    }

    out.close()
    socket.close()
}
