package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class TestAsyncComputation<T>(private val emitter: SingleEmitter<T>) {
    var cancelled = false
        private set

    init {
        emitter.setCancellable { cancelled = true }
    }

    fun provideResult(result: T) {
        emitter.onSuccess(result)
    }
}

fun <T> Queue<TestAsyncComputation<T>>.scheduleAsync() = Single.create<T> {
    this.add(TestAsyncComputation(it))
}

class SourceIntoAsyncFlow {
    val context = Context()
    val source = context.makeNode(1)
    val flow = context.makeNode<Int>()
    val asyncComputations: Queue<TestAsyncComputation<Int>> = ArrayDeque()
    val testObserver = TestObserver<NodeStreamState<Int>>()

    @BeforeEach
    fun connectInputs() {
        flow.connectAsync(source) { asyncComputations.scheduleAsync() }
        flow.observable { subscribe(testObserver) }
    }

    @Test
    fun initialStateIsComputing() {
        assertThat(flow.state).hasClass(NodeStreamState.Computing::class)
        assertThat(asyncComputations.size).isEqualTo(1)

        testObserver.assertValue { it is NodeStreamState.Computing }
    }

    @Test
    fun becomeValidWhenComputationIsCompleted() {
        asyncComputations.remove().provideResult(3)
        Thread.sleep(1000)
        testObserver.assertValueCount(2)
        assertThat(flow.state).hasClass(NodeStreamState.Valid::class)
        assertThat(flow.value).isEqualTo(3)
    }

    @Test
    fun computationIsCancelledWhenSourceIsChanged() {
        source.value = 2
        assertThat(flow.state).hasClass(NodeStreamState.Computing::class)
        assertThat(asyncComputations).hasSize(2)
        assertThat(asyncComputations.remove().cancelled).isTrue()
    }

    @Test
    fun computationIsCancelledWhenSourceIsCleared() {
        source.value = null
        assertThat(flow.state).hasClass(NodeStreamState.Empty::class)
        assertThat(asyncComputations).hasSize(1)
        assertThat(asyncComputations.remove().cancelled).isTrue()
    }
}

class SourceToAsyncToSync {
    val context = Context()
    val source = context.makeNode(1)
    val asyncFlow = context.makeNode<Int>()
    val syncFlow = context.makeNode<Int>()
    val asyncComputations: Queue<TestAsyncComputation<Int>> = ArrayDeque()

    @BeforeEach
    fun connectInputs() {
        asyncFlow.connectAsync(source) { asyncComputations.scheduleAsync() }
        syncFlow.connect(asyncFlow) { it }
    }

    @Test
    fun initialStateIsComputing() {
        assertThat(syncFlow.state).hasClass(NodeStreamState.Computing::class)
    }

    @Test
    fun whenComputationCompletedLeafIsCompleted() {
        assertThat(asyncComputations).hasSize(1)
        asyncComputations.remove().provideResult(3)
        Thread.sleep(1000)
        assertThat(syncFlow.value).isEqualTo(3)
        assertThat(syncFlow.state).hasClass(NodeStreamState.Valid::class)
    }
}

class AsyncFlowAsBaseOfDiamond {
    val context = Context()
    val source = context.makeNode(1)
    val diamondBase = context.makeNode<Int>()
    val passThroughLeft = context.makeNode<Int>()
    val passThroughRight = context.makeNode<Int>()
    val summer = context.makeNode<Int>()
    val asyncComputations: Queue<TestAsyncComputation<Int>> = ArrayDeque()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        diamondBase.connectAsync(source) { asyncComputations.scheduleAsync() }
        passThroughLeft.connect(diamondBase) { it }
        passThroughRight.connect(diamondBase) { it }
        summer.connect(passThroughLeft, passThroughRight) { left, right -> count++; left + right }
    }

    @Test
    fun initialValue() {
        assertThat(summer.state).hasClass(NodeStreamState.Computing::class)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun leafOnlyRecomputedOnceWhenAsyncCompleted() {
        count = 0
        asyncComputations.remove().provideResult(1)
        Thread.sleep(1000)
        assertThat(summer.value).isEqualTo(2)
        assertThat(count).isEqualTo(1)
    }
}