package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

fun <T> Context.makeNode(initialValue: T) = Node<T>(this).apply { value = initialValue }
fun <T> Context.makeNode() = Node<T>(this)

class Node<T>(private val context: Context) {
    private var subscription: Disposable? = null
    private val subject: BehaviorSubject<NodeStreamState<T>> =
        BehaviorSubject.createDefault(NodeStreamState.Empty())

    private val observable: Observable<NodeStreamState<T>> get() = subject

    val state: NodeStreamState<T> get() = subject.value

    var value: T?
        get() = subject.value.let { if (it is NodeStreamState.Valid) it.value else null }
        set(value) {
            connectValue(value)
        }

    private fun connectBase(
        inputs: Observable<List<NodeStreamState<*>>>,
        function: (List<*>) -> Observable<T>
    ) = context.transaction {
        subscription?.dispose()
        subscription = inputs
            .switchMap { inputStates -> mapStates(inputStates, function) }
            .subscribe { subject.onNext(it) }
    }

    fun connectValue(value: T?) {
        val newState: NodeStreamState<T> = if (value == null) NodeStreamState.Empty() else NodeStreamState.Valid(value)
        connectBase(Observable.just(listOf(newState))) {
            Observable.just<T>(it[0] as T)
                .subscribeOn(context.secondPhaseScheduler)
        }
    }

    fun connectNodes(inputs: List<Node<*>>, function: (List<*>) -> Observable<T>) {
        connectBase(
            Observable.combineLatest(inputs.map { it.observable }) { it.asList() as List<NodeStreamState<*>> },
            function
        )
    }
}

private fun <T> mapStates(
    inputStates: List<NodeStreamState<*>>,
    mapping: (List<*>) -> Observable<T>
): Observable<NodeStreamState<T>> = when {
    inputStates.any { it is NodeStreamState.Empty } -> Observable.just(NodeStreamState.Empty())
    inputStates.any { it is NodeStreamState.Computing } -> Observable.just(NodeStreamState.Computing())
    inputStates.all { it is NodeStreamState.Valid } -> {
        val computingState = Observable.just(NodeStreamState.Computing<T>())
        val newState: Observable<NodeStreamState<T>> = mapping(inputStates.values())
            .map { NodeStreamState.Valid(it) }
        Observable.concat(computingState, newState)
    }
    else -> throw IllegalStateException()
}

fun List<NodeStreamState<*>>.values() = this.map { (it as NodeStreamState.Valid).value }