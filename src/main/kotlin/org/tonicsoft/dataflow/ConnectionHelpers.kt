package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Observable

fun <T, P1> Node<T>.connect(p1: Node<P1>, function: (P1) -> T) {
    connectAsync(p1) { a1 -> Observable.just(function(a1)) }
}

fun <T, P1, P2> Node<T>.connect(p1: Node<P1>, p2: Node<P2>, function: (P1, P2) -> T) {
    connectAsync(p1, p2) { a1, a2 -> Observable.just(function(a1, a2)) }
}

fun <T, P1> Node<T>.connectAsync(p1: Node<P1>, function: (P1) -> Observable<T>) {
    connectNodes(listOf(p1)) { function(it[0] as P1) }
}

fun <T, P1, P2> Node<T>.connectAsync(p1: Node<P1>, p2: Node<P2>, function: (P1, P2) -> Observable<T>) {
    connectNodes(listOf(p1, p2)) { function(it[0] as P1, it[1] as P2) }
}