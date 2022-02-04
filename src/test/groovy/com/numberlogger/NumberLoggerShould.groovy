package com.numberlogger

import com.google.inject.Guice
import java.nio.file.FileSystems
import java.nio.file.Files
import spock.lang.Specification


class NumberLoggerShould extends Specification {

    final static int serverPort = 4001
    def reportEveryNMillis = 10000
    def filePath = "src/test/resources/number.log"

    NumberLogger numberLogger
    NumberLocalFileClient numberClient
    Reporter reporter
    Server server
    App app

    def setup() {
        def config = new NumberLogAppConfig(filePath, 4, reportEveryNMillis, 5, getServerPort())
        def injector = Guice.createInjector(new NumberLogModule(config))
        numberClient = injector.getInstance(NumberLocalFileClient)
        numberLogger = injector.getInstance(NumberLogger)
        reporter = injector.getInstance(Reporter)
        server = injector.getInstance(Server)
        app = new App(server, numberClient, reporter)
    }

    def cleanup() {
        app.shutdown()
    }

    def "log number to file" () {
        given: "server is listening"
        app.run()

        and: "a couple number inputs"
        def number1 = "123456789"
        def number2 = "987654321"

        when: "sending both numbers to server"
        TestClient.INSTANCE.sendLines([toInput(number1), toInput(number2)], serverPort)

        then: "numbers are logged"
        Thread.sleep(25)
        getAllLoggedNumbers() == [number1, number2]
    }

    def "not log an invalid number"() {
        given: "server is listening"
        app.run()

        and: "an invalid number input"
        def invalidLine = "blabla${System.lineSeparator()}"

        when: "sending such to the server"
        TestClient.INSTANCE.sendLine(invalidLine, serverPort)

        then: "no number was logged"
        numberClient.getAll().isEmpty()

        and: "server is still running"
        server.isRunning()

        when: "sending a valid number to the server"
        TestClient.INSTANCE.sendLine(toInput("222222222"), serverPort)

        then:
        Thread.sleep(25)
        getAllLoggedNumbers() == ["222222222"]
    }

    def "should add trailing zeros for input"() {
        given: "server is listening"
        app.run()

        and: "a few number inputs"
        String rawLine = toInput("11111111")
        String rawLine2 = toInput("1")

        and:
        def expectedLoggedNumbers = ["000000001", "011111111"]

        when: "sending both numbers to server"
        TestClient.INSTANCE.sendLines([rawLine, rawLine2], serverPort)

        then: "numbers are logged"
        Thread.sleep(25)
        getAllLoggedNumbers().containsAll(expectedLoggedNumbers)
    }

    def "stop the app after `terminate` input was given"() {
        given: "server is listening"
        app.run()

        and:
        String rawLine = toInput("terminate")

        when: "sending terminate to server"
        TestClient.INSTANCE.sendLine(rawLine, serverPort)

        then:
        app.isShuttingDown()
    }

    def getAllLoggedNumbers() {
        Files.newInputStream(FileSystems.getDefault().getPath(filePath)).readLines()
    }

    static String toInput(String data) {
        "$data${System.lineSeparator()}"
    }
}

// TODO: Add test for caching the integer
