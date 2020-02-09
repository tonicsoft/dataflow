package org.tonicsoft.dataflow.proto

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tonicsoft.dataflow.Context
import org.tonicsoft.dataflow.makeNode

class CalculatorService : CalculatorGrpc.CalculatorImplBase() {
    override fun double_(request: Service.Number, responseObserver: StreamObserver<Service.Number>) {
        responseObserver.onNext(Service.Number.newBuilder().setValue(request.value * 2).build())
        responseObserver.onCompleted()
    }
}

val server = InProcessServerBuilder.forName("test").addService(CalculatorService())
    .build().start()

val channel = InProcessChannelBuilder.forName("test").build()

val client = CalculatorGrpc.newStub(channel)

class DelegateAsyncCallToGrpcCall {
    val context = Context()
    val source = context.makeNode<Int>()
    val flow = context.makeNode<Service.Number>()

    @BeforeEach
    fun connectInputs() {
        flow.connectGrpc(source, channel, CalculatorGrpc.getDoubleMethod()) {
            Service.Number.newBuilder().setValue(it).build()
        }
    }

    @Test
    fun doIt() {
        source.value = 1
        Thread.sleep(1000)
        assertThat(flow.value?.value).isEqualTo(2)
    }
}