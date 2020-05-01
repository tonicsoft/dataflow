package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.SingleSubject
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class TransactionExecutor {
    val threadName = UUID.randomUUID().toString()
    private val executor = Executors.newSingleThreadExecutor { Thread(it, threadName) }

    private val firstPhaseTasks = LinkedBlockingQueue<Runnable>()
    val scheduler = Schedulers.from {
        firstPhaseTasks.add(it)
        scheduleTransaction()
    }

    private var transactionMarker_: SingleSubject<Unit> = SingleSubject.create()
    val transactionMarker: Single<Unit> get() = transactionMarker_

    private fun runTransaction() {
        firstPhaseTasks.runAllTasks()

        transactionMarker_.onSuccess(Unit)
        transactionMarker_ = SingleSubject.create()
    }

    fun BlockingQueue<Runnable>.runAllTasks() {
        while (isNotEmpty()) {
            take().run()
        }
    }

    fun scheduleTransaction() = executor.submit { runTransaction() }
}

class Context {

    val transactionExecutor = TransactionExecutor()

    fun transaction(block: () -> Unit) {
        if (Thread.currentThread().name == transactionExecutor.threadName) {
            block()
        } else {
            transactionExecutor.scheduler.scheduleDirect(block)
        }
    }
}