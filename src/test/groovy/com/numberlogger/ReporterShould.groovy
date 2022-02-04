package com.numberlogger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.google.inject.Guice
import spock.lang.Specification

class ReporterShould extends Specification {

    def final static serverPort = 4002

    def filePath = "src/test/resources/numbers-report.log"

    NumberLogger numberLogger
    NumberLocalFileClient numberClient
    Reporter reporter
    Server server
    App app

    def setup() {
        def config = new NumberLogAppConfig(filePath, 4, 1000, 5, serverPort)
        def injector = Guice.createInjector(new NumberLogModule(config))
        numberClient = injector.getInstance(NumberLocalFileClient)
        numberLogger = injector.getInstance(NumberLogger)
        reporter = injector.getInstance(Reporter)
        server = injector.getInstance(Server)
        app = new App(server, numberClient, reporter)
    }

    def "log report"() {
        given: "app is running"
        app.run()

        and:
        def loggedMessages = new ListAppender<ILoggingEvent>()
        loggedMessages.start()
        reporter.log.addAppender(loggedMessages)

        and: "multiple input numbers"
        def number1 = "1${System.lineSeparator()}"
        def number2 = "222${System.lineSeparator()}"

        when: "sending those numbers out"
        TestClient.INSTANCE.sendLines([number1, number2, number1], serverPort)

        then: "report has been logged"
        app.shutdown()
        loggedMessages.list.any {
            it.level == Level.INFO &&
                it.formattedMessage =~ ~/Received 2 unique numbers, 1 duplicates. Unique total: 2.*/
        }

        cleanup:
        reporter.log.detachAppender(loggedMessages)
        loggedMessages.stop()
    }
 }
