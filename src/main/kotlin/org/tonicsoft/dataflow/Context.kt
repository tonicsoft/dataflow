package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors

class Context {
    private val threadName = UUID.randomUUID().toString()
    private val executor = Executors.newSingleThreadExecutor { Thread(it, threadName) }

    private var transactionCount = 0

    private val secondPhase = ManualExecutor()
    val secondPhaseScheduler: Scheduler = Schedulers.from(secondPhase)
    val asyncResultScheduler: Scheduler = Schedulers.from(executor)



    fun transaction(block: () -> Unit) {
        if (Thread.currentThread().name == threadName) {
            transactionCount++
            block()
            if (transactionCount == 1) {
                secondPhase.runAllTasks()
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