package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

fun <T> Context.makeThreadRememberingNode(): ThreadRememberingFlow<T> {
    return ThreadRememberingFlow(makeNode())
}

class ThreadRememberingFlow<T>(val node: Node<T>) {
    private val threadId = AtomicLong()
    private val computedOnce = CountDownLatch(1)

    fun connect(input: Node<T>) {
        node.connect(input, this::compute)
    }

    fun compute(input: T): T {
        threadId.set(Thread.currentThread().id)
        computedOnce.countDown()
        return input
    }

    val lastComputationThreadId get() = threadId.get()
    fun await() = assert(computedOnce.await(5, TimeUnit.SECONDS))
}

class SyncFlowsAlwaysComputedOnSameThread {
    val context = Context()
    val source = context.makeNode<Int>()
    val flow1 = context.makeThreadRememberingNode<Int>()
    val async = context.makeNode<Int>()
    val flow2 = context.makeThreadRememberingNode<Int>()

    val asyncExecutor = Executors.newSingleThreadExecutor()
    val barrier = CountDownLatch(1)

    @BeforeEach
    fun connectNodes() {
        flow1.connect(source)
        async.connectAsync(flow1.node) {
            val result = CompletableFuture<Int>()
            asyncExecutor.submit {
                assert(barrier.await(5, TimeUnit.SECONDS))
                result.complete(it)
            }
            Single.fromCompletionStage(result)
        }
        flow2.connect(async)
    }

    @Test
    fun allFlowsOnSameThreadWhenSourceIsSet() {
        source.value = 1
        flow1.await()
        barrier.countDown()
        flow2.await()

        assertThat(flow1.lastComputationThreadId).isEqualTo(flow2.lastComputationThreadId)
    }
}