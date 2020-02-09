package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MappingFlowWithNoInitialValue {
    val context = Context()
    val source = context.makeNode<Int>()
    val doublingFlow = context.makeNode<Int>()

    @BeforeEach
    fun connect() {
        doublingFlow.connect(source) { it * 2 }
    }

    @Test
    fun initialState() {
        assertThat(doublingFlow.value).isEqualTo(null)
    }

    @Test
    fun sourceIsSet() {
        source.value = 3
        assertThat(doublingFlow.value).isEqualTo(6)
    }
}

class MappingFlowWithInitialValue {
    val context = Context()
    val source = context.makeNode(3)
    val doublingFlow = context.makeNode<Int>()

    @BeforeEach
    fun connect() {
        doublingFlow.connect(source) { it * 2 }
    }

    @Test
    fun valueAlreadyComputed() {
        assertThat(doublingFlow.value).isEqualTo(6)
    }

    @Test
    fun valueChanged() {
        source.value = 4
        assertThat(doublingFlow.value).isEqualTo(8)
    }

    @Test
    fun valueCleared() {
        source.value = null
        assertThat(doublingFlow.value).isNull()
    }
}

class SourceWithTwoFlows {
    val context = Context()
    val source = context.makeNode(3)
    val doublingFlow = context.makeNode<Int>()
    val triplingFlow = context.makeNode<Int>()

    @BeforeEach
    fun connect() {
        doublingFlow.connect(source) { it * 2 }
        triplingFlow.connect(source) { it * 3 }
    }


    @Test
    fun initialValueAlreadyComputed() {
        assertThat(doublingFlow.value).isEqualTo(6)
        assertThat(triplingFlow.value).isEqualTo(9)
    }

    @Test
    fun valueChanged() {
        source.value = 4
        assertThat(doublingFlow.value).isEqualTo(8)
        assertThat(triplingFlow.value).isEqualTo(12)
    }

    @Test
    fun valueCleared() {
        source.value = null
        assertThat(doublingFlow.value).isNull()
        assertThat(triplingFlow.value).isNull()
    }
}

class DiamondShapedGraph {
    val context = Context()
    val source = context.makeNode<Int>()
    val doublingFlow = context.makeNode<Int>()
    val triplingFlow = context.makeNode<Int>()
    val summer = context.makeNode<Int>()

    @BeforeEach
    fun connect() {
        doublingFlow.connect(source) { it * 2 }
        triplingFlow.connect(source) { it * 3 }
        summer.connect(doublingFlow, triplingFlow) { left, right -> left + right }
    }

    @Test
    fun initialValue() {
        assertThat(summer.value).isNull()
    }

    @Test
    fun computedValue() {
        source.value = 3
        assertThat(summer.value).isEqualTo(15)
    }
}

class TwoSourcesIntoOneFlow {
    val context = Context()
    val source1 = context.makeNode<Int>()
    val source2 = context.makeNode<Int>()

    val summer = context.makeNode<Int>()

    @BeforeEach
    fun connect() {
        summer.connect(source1, source2) { left: Int, right:Int -> left + right }
    }

    @Test
    fun leafInitiallyInvalid() {
        assertThat(summer.value).isNull()
    }

    @Test
    fun oneSourceSet() {
        source1.value = 2
        assertThat(summer.value).isNull()
    }

    @Test
    fun bothSourcesSet() {
        source1.value = 2
        source2.value = 3
        assertThat(summer.value).isEqualTo(5)
    }
}