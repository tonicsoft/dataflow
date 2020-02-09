package org.tonicsoft.dataflow

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class UninitialisedSourceTest {
    val context = Context()
    val source = context.makeNode<String>()
    @Test
    fun initialStateInvalid() {
        assertThat(source.value).isNull()
    }

    @Test
    fun setState() {
        val newValue = "value"
        source.value = newValue

        assertThat(source.value).isEqualTo(newValue)
    }
}

class InitialisedSourceTest {
    val context = Context()
    val initialValue = "foo"
    val source = context.makeNode(initialValue)

    @Test
    fun clearSource() {
        source.value = null

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

        assertThat(source.value).isEqualTo(newValue)
    }
}