package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.reactivex.rxjava3.core.Observable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class SourceIntoAsyncFlow {
    val context = Context()
    val source = context.makeNode(1)
    val flow = context.makeNode<Int>()
    val asyncComputation = CompletableFuture<Int>()

    @BeforeEach
    fun connectInputs() {
        flow.connectAsync(source) { Observable.fromCompletionStage(asyncComputation).doOnDispose{ asyncComputation.cancel(true)} }
    }

    @Test
    fun initialStateIsComputing() {
        assertThat(flow.state).hasClass(NodeStreamState.Computing::class)
    }

    @Test
    fun becomeValidWhenComputationIsCompleted() {
        asyncComputation.complete(3)
        assertThat(flow.state).hasClass(NodeStreamState.Valid::class)
        assertThat(flow.value).isEqualTo(3)
    }

    @Test
    fun computationIsCancelledWhenSourceIsChanged() {
        source.value = 2
        assertThat(asyncComputation.isCancelled()).isTrue()
    }
}

class SourceToAsyncToSync {
    val context = Context()
    val source = context.makeNode(1)
    val asyncFlow = context.makeNode<Int>()
    val syncFlow = context.makeNode<Int>()
    val asyncComputation = CompletableFuture<Int>()

    @BeforeEach
    fun connectInputs() {
        asyncFlow.connectAsync(source) { Observable.fromCompletionStage(asyncComputation).doOnDispose{ asyncComputation.cancel(true)} }
        syncFlow.connect(asyncFlow) { it }
    }

    @Test
    fun initialStateIsComputing() {
        assertThat(syncFlow.state).hasClass(NodeStreamState.Computing::class)
    }

    @Test
    fun whenComputationCompletedLeafIsCompleted() {
        asyncComputation.complete(3)
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
    val asyncComputation = CompletableFuture<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        diamondBase.connectAsync(source) { Observable.fromCompletionStage(asyncComputation).doOnDispose{ asyncComputation.cancel(true)} }
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
        asyncComputation.complete(1)
        assertThat(summer.value).isEqualTo(2)
        assertThat(count).isEqualTo(1)
    }
}