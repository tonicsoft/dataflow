package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UninitialisedSourceTest {
    val context = Context()
    val source = context.makeNode<String>()
    val observer = TestObserver<NodeStreamState<String>>()
    @Test
    fun initialStateInvalid() {
        assertThat(source.value).isNull()
    }

    @Test
    fun setState() {
        source.observable { subscribe(observer) }
        observer.awaitCount(1)

        val newValue = "value"
        source.value = newValue

        observer.awaitCount(2)
        assertThat(source.value).isEqualTo(newValue)
    }
}

class InitialisedSourceTest {
    val context = Context()
    val initialValue = "foo"
    val source = context.makeNode(initialValue)
    val observer = TestObserver<NodeStreamState<String>>()

    @BeforeEach
    fun subscribe() {
        source.observable { subscribe(observer) }
        observer.awaitCount(1)
    }

    @Test
    fun clearSource() {
        source.value = null

        observer.awaitCount(2)
        assertThat(source.value).isNull()
    }

    @Test
    fun getInitial() {
        assertThat(source.value).isEqualTo(initialValue)
        assertThat(source.value).isEqualTo(initialValue)
    }

    @Test
    fun updated() {
        val newValue = "value"
        source.value = newValue

        observer.awaitCount(2)

        assertThat(source.value).isEqualTo(newValue)
    }
}