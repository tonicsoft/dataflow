package org.tonicsoft.dataflow

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.Test

class SinkFromValueNode {
    val context = Context()
    val source = context.makeNode<Int>()
    val sink = TestObserver<NodeStreamState<Int>>()

    @Test
    fun sinkCalledOnSubscriptionWhenSourceIsEmpty() {
        source.observable { subscribe(sink) }
        sink.awaitCount(1)
        sink.assertValue { it is NodeStreamState.Empty }
    }

    @Test
    fun sinkCalledOnSubscriptionWhenSourceIsValid() {
        source.value = 5
        source.observable { subscribe(sink) }
        sink.awaitCount(1)
        sink.assertValue(NodeStreamState.Valid(5))
    }

    @Test
    fun sinkCalledWhenSourceChanged() {
        source.value = 5
        source.observable { subscribe(sink) }
        sink.awaitCount(1)
        source.value = 6
        sink.awaitCount(2)
        sink.assertValueAt(1, NodeStreamState.Valid(6))
    }
}