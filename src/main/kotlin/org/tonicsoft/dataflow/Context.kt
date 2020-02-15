package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.SingleSubject
import java.util.*
import java.util.concurrent.Executors

class Context {
    private val threadName = UUID.randomUUID().toString()
    private val executor = Executors.newSingleThreadExecutor { Thread(it, threadName) }

    private var transactionCount = 0

    private val secondPhase = ManualExecutor()
    val secondPhaseScheduler: Scheduler = Schedulers.from(secondPhase)
    val asyncResultScheduler: Scheduler = Schedulers.from(executor)

    private var transactionMarker_: SingleSubject<Unit>? = null
    val transactionMarker: Single<Unit> get() = transactionMarker_ ?: throw RuntimeException("not in transaction")

    fun transaction(block: () -> Unit) {
        if (Thread.currentThread().name == threadName) {
            if (transactionCount == 0) {
                transactionMarker_ = SingleSubject.create()
            }
            transactionCount++
            block()
            if (transactionCount == 1) {
                secondPhase.runAllTasks()
                transactionMarker_?.onSuccess(Unit)
                transactionMarker_ = null
            }
            transactionCount--
        } else {
            executor.submit{
                transaction {
                    block()
                }
            }.get()
        }
    }
}