package org.tonicsoft.dataflow

import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.Test

class SinkFromValueNode {
    val context = Context()
    val source = context.makeNode<Int>()
    val testSubscriber = TestObserver<NodeStreamState<Int>>()

    @Test
    fun sinkCalledOnSubscriptionWhenSourceIsEmpty() {
        source.observable { subscribe(testSubscriber) }
        testSubscriber.assertValue { it is NodeStreamState.Empty }
    }

    @Test
    fun sinkCalledOnSubscriptionWhenSourceIsValid() {
        source.value = 5
        source.observable { subscribe(testSubscriber) }
        testSubscriber.assertValue(NodeStreamState.Valid(5))
    }

    @Test
    fun sinkCalledWhenSourceChanged() {
        source.value = 5
        source.observable { skip(1).subscribe(testSubscriber) }
        source.value = 6
        testSubscriber.assertValue(NodeStreamState.Valid(6))
    }
}