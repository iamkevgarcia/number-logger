package com.numberlogger

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

fun ExecutorService.gracefulShutdown(): Boolean {
    this.shutdown()
    if (!this.awaitTermination(3, TimeUnit.SECONDS)) {
        this.shutdownNow()
        return this.awaitTermination(3, TimeUnit.SECONDS)
    }
    return true
}

