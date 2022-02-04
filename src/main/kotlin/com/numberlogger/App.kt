package com.numberlogger

import com.google.inject.Guice
import com.google.inject.Injector
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.system.exitProcess

class App @Inject constructor(
    private val server: Server,
    private val numberClient: NumberLocalFileClient,
    private val reporter: Reporter
) : Runnable {
    private var isShuttingDown = false

    override fun run() {
        numberClient.run()
        server.run(this)
        reporter.run()
    }

    fun shutdown() {
        log.info("Shutting down app")
        isShuttingDown = true
        server.stop()
        reporter.close()
        numberClient.close()
    }

    fun isShuttingDown() { isShuttingDown }
}

private val log = LoggerFactory.getLogger(object {}::class.java.`package`.name)

fun main() {
    try {
        val config = NumberLogAppConfig(
            filePath = "src/main/resources/numbers.log",
            maxConnections = 5,
            reportEveryNMillis = 10000,
            flushBufferEveryNMillis = 1,
            serverPort = 4000)
        val module = NumberLogModule(config)
        val injector: Injector = Guice.createInjector(module)
        val app = injector.getInstance(App::class.java)
        app.run()
    } catch (t: Throwable) {
        log.error(t.message, t)
        exitProcess(1)
    }
}
