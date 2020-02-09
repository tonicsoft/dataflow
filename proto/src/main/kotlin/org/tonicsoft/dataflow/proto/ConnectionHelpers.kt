package org.tonicsoft.dataflow.proto

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.stub.ClientCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import io.reactivex.rxjava3.core.Observable
import org.tonicsoft.dataflow.Node

fun <P1, RequestT, ResponseT> Node<ResponseT>.connectGrpc(
    p1: Node<P1>,
    channel: Channel,
    methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
    function: (P1) -> RequestT
) {
    @Suppress("UNCHECKED_CAST")
    connectNodes(listOf(p1)) {
        Observable.create { emitter ->

            val call = channel.newCall(
                methodDescriptor,
                CallOptions.DEFAULT
            )
            emitter.setCancellable { call.cancel(null, null) }

            val request: RequestT = function(it[0] as P1)

            asyncUnaryCall(
                call, request, object : StreamObserver<ResponseT> {
                    override fun onNext(value: ResponseT) {
                        emitter.onNext(value)
                    }

                    override fun onError(t: Throwable) {
                        emitter.onError(t)
                    }

                    override fun onCompleted() {
                        emitter.onComplete()
                    }
                }
            )
        }
    }
}