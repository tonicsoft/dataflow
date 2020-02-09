package org.tonicsoft.dataflow

sealed class NodeStreamState<T> {
    class Empty<T> : NodeStreamState<T>()
    class Valid<T>(val value: T) : NodeStreamState<T>()
    class Computing<T> : NodeStreamState<T>()
}