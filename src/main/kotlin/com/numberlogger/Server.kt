package com.numberlogger

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import javax.inject.Inject

class Server @Inject constructor(
    private val numberLogger: NumberLogger,
    private val port: Int,
    private val maxConnections: Int
) {
    private val log = LoggerFactory.getLogger(object {}::class.java.`package`.name)

    private val executor = Executors.newCachedThreadPool()
    private lateinit var serverSocket: ServerSocket

    fun run(app: App) {
        serverSocket = ServerSocket(port)
        log.info("Server started at port: $port")

        repeat(maxConnections) {
            executor.submit {
                try {
                    NumberInputClientHandler(serverSocket.accept(), numberLogger, app).run()
                } catch (e: SocketException) {
                    if (e.message != "Socket closed") {
                        throw e
                    }
                } catch (e: Exception) {
                    log.error(e.message)
                    app.shutdown()
                }
            }
        }
    }

    fun stop() {
        if (!executor.gracefulShutdown()) {
            log.warn("Failed to gracefully shut down executor")
        }
        serverSocket.close()
    }

    fun isRunning() = !serverSocket.isClosed
}

private class NumberInputClientHandler(
    private val socket: Socket,
    private val numberLogger: NumberLogger,
    private val app: App
) : Runnable {
    private val log = LoggerFactory.getLogger(this::class.java)

    private lateinit var input: BufferedReader

    override fun run() {
        log.info("Connection from $socket!")
        input = BufferedReader(InputStreamReader(socket.getInputStream()))

        try {
            for (line in input.lines()) {
                if (line.startsWith("terminate")) {
                    app.shutdown()
                    break
                }
                numberLogger.logNumber(rawInput = line)
            }
        } catch (e: InvalidNumberException) {
        } catch (e: IOException) {
            log.error(e.message)
        } finally {
            log.info("closing socket")
            input.close()
            socket.close()
        }
    }
}
