package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

class Context {
    private var transactionCount = 0

    private val secondPhase = ManualExecutor()
    val secondPhaseScheduler: Scheduler = Schedulers.from(secondPhase)


    fun transaction(block: () -> Unit) {
        transactionCount++
        block()
        if (transactionCount == 1) {
            secondPhase.runAllTasks()
        }
        transactionCount--
    }
}