package org.tonicsoft.dataflow

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

fun <T> Context.makeNode(initialValue: T) = Node<T>(this).apply { value = initialValue }
fun <T> Context.makeNode() = Node<T>(this)

class Node<T>(private val context: Context) {
    private var subscription: Disposable? = null
    private val subject: BehaviorSubject<NodeStreamState<T>> =
        BehaviorSubject.createDefault(NodeStreamState.Empty())

    private val externalUpdates: Observable<NodeStreamState<T>> =
        subject.debounce { context.transactionMarker.toObservable() }

    val state: NodeStreamState<T> get() = subject.value

    var value: T?
        get() = subject.value.let { if (it is NodeStreamState.Valid) it.value else null }
        set(value) {
            connectValue(value)
        }

    private fun connectBase(
        inputs: Observable<List<NodeStreamState<*>>>,
        function: (List<*>) -> Single<T>
    ) = context.transaction {
        subscription?.dispose()
        subscription = inputs
            .switchMap { inputStates -> mapStates(inputStates, function) }
            .subscribe { subject.onNext(it) }
    }

    fun connectValue(value: T?) {
        val newState: NodeStreamState<T> = if (value == null) NodeStreamState.Empty() else NodeStreamState.Valid(value)
        connectBase(Observable.just(listOf(newState))) {
            @Suppress("UNCHECKED_CAST")
            Single.just<T>(it[0] as T)
                .observeOn(context.secondPhaseScheduler)
        }
    }

    fun connectNodes(inputs: List<Node<*>>, function: (List<*>) -> T) {
        connectBase(inputs.observeStates()) { Single.just(function(it)) }
    }

    fun connectNodesAsync(inputs: List<Node<*>>, function: (List<*>) -> Single<T>) {
        connectBase(inputs.observeStates()) {
            function(it).observeOn(context.asyncResultScheduler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun List<Node<*>>.observeStates(): Observable<List<NodeStreamState<*>>> =
        Observable.combineLatest(this.map { it.subject }) { it.asList() as List<NodeStreamState<*>> }

    fun observable(block: Observable<NodeStreamState<T>>.() -> Unit) {
        context.transaction {
            block(externalUpdates)
        }
    }
}

private fun <T> mapStates(
    inputStates: List<NodeStreamState<*>>,
    mapping: (List<*>) -> Single<T>
): Observable<NodeStreamState<T>> = when {
    inputStates.any { it is NodeStreamState.Empty } -> Observable.just(NodeStreamState.Empty())
    inputStates.any { it is NodeStreamState.Computing } -> Observable.just(NodeStreamState.Computing())
    inputStates.all { it is NodeStreamState.Valid } -> {
        val computingState = Single.just(NodeStreamState.Computing<T>())
        val newState: Single<NodeStreamState<T>> = mapping(inputStates.values())
            .map { NodeStreamState.Valid(it) }
        Single.concat(Observable.just(computingState, newState))
    }
    else -> throw IllegalStateException()
}

fun List<NodeStreamState<*>>.values() = this.map { (it as NodeStreamState.Valid).value }