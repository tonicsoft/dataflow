package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SourceChangedTwiceInTransaction {
    val context = Context()
    val source = context.makeNode(1)
    val countingDoubler = context.makeNode<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        countingDoubler.connect(source) { i -> count++; i * 2 }
    }

    @Test
    fun sourceUpdatedTwiceInOneTransaction() {
        count = 0
        context.transaction {
            source.value = 2
            source.value = 3
        }

        assertThat(countingDoubler.value).isEqualTo(6)
        assertThat(count).isEqualTo(1)
    }
}

class TwoChangedSourcesIntoOneFlow {
    val context = Context()
    val source1 = context.makeNode(1)
    val source2 = context.makeNode(1)
    val summer = context.makeNode<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        summer.connect(source1, source2) { left, right -> count++; left + right}
    }

    @Test
    fun bothSourcesChangedInATransaction() {
        count = 0
        context.transaction {
            source1.value = 2
            source2.value = 2
        }

        assertThat(summer.value).isEqualTo(4)
        assertThat(count).isEqualTo(1)
    }
}

class DiamondConnectedInATransaction {
    val context = Context()
    val source = context.makeNode(1)
    val passThrough = context.makeNode<Int>()
    val passThrough2 = context.makeNode<Int>()
    val summer = context.makeNode<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        context.transaction {
            passThrough.connect(source) { it }
            passThrough2.connect(source) { it }
            summer.connect(passThrough, passThrough2) { left, right -> count++; left + right }
        }
    }

    @Test
    fun initialValue() {
        assertThat(summer.value).isEqualTo(2)
        assertThat(count).isEqualTo(1)

    }

    @Test
    fun sourceUpdatedInTransaction() {
        count = 0
        context.transaction {
            source.value = 2
        }

        assertThat(summer.value).isEqualTo(4)
        assertThat(count).isEqualTo(1)
    }
}

class DiamondConnectedNotInATransaction {
    val context = Context()
    val source = context.makeNode(1)
    val passThrough = context.makeNode<Int>()
    val passThrough2 = context.makeNode<Int>()
    val summer = context.makeNode<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        passThrough.connect(source) { it }
        passThrough2.connect(source) { it }
        summer.connect(passThrough, passThrough2) { left, right -> count++; left + right }
    }

    @Test
    fun initialValue() {
        assertThat(summer.value).isEqualTo(2)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun sourceUpdatedTwiceInOneTransaction() {
        count = 0
        source.value = 2

        assertThat(summer.value).isEqualTo(4)
        assertThat(count).isEqualTo(1)
    }
}

class IndirectDiamond {
    val context = Context()
    val source = context.makeNode(1)
    val passThroughLeft = context.makeNode<Int>()
    val passThroughRight1 = context.makeNode<Int>()
    val passThroughRight2 = context.makeNode<Int>()
    val summer = context.makeNode<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        passThroughLeft.connect(source) { it }
        passThroughRight1.connect(source) { it }
        passThroughRight2.connect(passThroughRight1) { it }
        summer.connect(passThroughLeft, passThroughRight2) { left, right -> count++; left + right }
    }

    @Test
    fun initialValue() {
        assertThat(summer.value).isEqualTo(2)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun leafOnlyRecomputedOnceWhenSourceUpdated() {
        count = 0
        source.value = 2

        assertThat(summer.value).isEqualTo(4)
        assertThat(count).isEqualTo(1)
    }
}

class DiamondWithFlowAsBase {
    val context = Context()
    val source = context.makeNode(1)
    val diamondBase = context.makeNode<Int>()
    val passThroughLeft = context.makeNode<Int>()
    val passThroughRight = context.makeNode<Int>()
    val summer = context.makeNode<Int>()
    var count = 0

    @BeforeEach
    fun connectInputs() {
        diamondBase.connect(source) { it }
        passThroughLeft.connect(diamondBase) { it }
        passThroughRight.connect(diamondBase) { it }
        summer.connect(passThroughLeft, passThroughRight) { left, right -> count++; left + right }
    }

    @Test
    fun initialValue() {
        assertThat(summer.value).isEqualTo(2)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun leafOnlyRecomputedOnceWhenSourceUpdated() {
        count = 0
        source.value = 2

        assertThat(summer.value).isEqualTo(4)
        assertThat(count).isEqualTo(1)
    }
}