package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Single

fun <T, P1> Node<T>.connect(p1: Node<P1>, function: (P1) -> T) {
    @Suppress("UNCHECKED_CAST")
    connectNodes(listOf(p1)) { function(it[0] as P1) }
}

fun <T, P1, P2> Node<T>.connect(p1: Node<P1>, p2: Node<P2>, function: (P1, P2) -> T) {
    @Suppress("UNCHECKED_CAST")
    connectNodes(listOf(p1, p2)) { function(it[0] as P1, it[1] as P2) }
}

fun <T, P1> Node<T>.connectAsync(p1: Node<P1>, function: (P1) -> Single<T>) {
    @Suppress("UNCHECKED_CAST")
    connectNodesAsync(listOf(p1)) { function(it[0] as P1) }
}

fun <T, P1, P2> Node<T>.connectAsync(p1: Node<P1>, p2: Node<P2>, function: (P1, P2) -> Single<T>) {
    @Suppress("UNCHECKED_CAST")
    connectNodesAsync(listOf(p1, p2)) { function(it[0] as P1, it[1] as P2) }
}