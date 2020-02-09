package org.tonicsoft.dataflow

import java.util.*
import java.util.concurrent.Executor

class ManualExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()

    override fun execute(command: Runnable) = tasks.push(command)

    fun runAllTasks() {
        while (tasks.isNotEmpty()) {
            tasks.pop().run()
        }
    }
}