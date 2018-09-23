package foo;

import foo.Flows.AsyncFlowNode;
import foo.Flows.Flow2Node;
import foo.Flows.FlowNode;
import foo.TestNodes.*;
import org.junit.Test;

import static foo.State.CALCULATING;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AsyncTest {

    private final Context context = new Context();

    @Test
    public void sourceAndAsyncFlow() {
        SourceNode<Integer> source = new SourceNode<>();

        AsyncFlowNode<Integer, Integer> asyncDoubler = new AsyncDoubler(source, context);

        assertThat(asyncDoubler.getState(), is(State.MISSING_INPUT));

        source.setValue(1, context);

        assertThat(asyncDoubler.getState(), is(CALCULATING));

        context.doJob();

        assertThat(asyncDoubler.getState(), is(State.VALID));
        assertThat(asyncDoubler.getValue(), is(2));
    }

    @Test
    public void asyncFlowUpdatesStateWhenItStartsCalculating() {
        SourceNode<Integer> source = new SourceNode<>();

        AsyncFlowNode<Integer, Integer> asyncDoubler = new AsyncDoubler(source, context);

        Sink sink = new Sink(asyncDoubler);

        source.setValue(1, context);

        assertThat(sink.received.poll().state, is(CALCULATING));
    }

    @Test
    public void updatedAsyncFlowWithEventSink() {
        SourceNode<Integer> source = new SourceNode<>();
        AsyncFlowNode<Integer, Integer> asyncDoubler = new AsyncDoubler(source, context);
        Sink sink = new Sink(asyncDoubler);

        source.setValue(1, context);
        assertThat(sink.received.poll().state, is(CALCULATING));

        context.doJob();

        assertThat(sink.received.poll().value, is(2));
        assertTrue(sink.valid);

        source.setValue(2, context);

        assertThat(sink.received.poll().state, is(CALCULATING));

        context.doJob();

        assertThat(sink.received.poll().value, is(4));
        assertTrue(sink.valid);
        assertThat(asyncDoubler.getState(), is(State.VALID));
    }

    @Test
    public void sourceAlreadySetWithAsyncFlow() {
        SourceNode<Integer> source = new SourceNode<>();
        source.setValue(1, context);

        AsyncFlowNode<Integer, Integer> asyncDoubler = new AsyncDoubler(source, context);

        assertThat(asyncDoubler.getState(), is(CALCULATING));

        context.doJob();

        assertThat(asyncDoubler.getState(), is(State.VALID));
        assertThat(asyncDoubler.getValue(), is(2));
    }

    @Test
    public void sourceWithLazyAsyncFlow() {
        SourceNode<Integer> source = new SourceNode<>();

        AsyncFlowNode<Integer, Integer> asyncDoubler = new LazyAsyncDoubler(source, context);

        assertThat(asyncDoubler.getState(), is(State.MISSING_INPUT));

        source.setValue(1, context);

        assertThat(asyncDoubler.getState(), is(State.READY));

        asyncDoubler.pull(context);

        assertThat(asyncDoubler.getState(), is(CALCULATING));

        context.doJob();

        assertThat(asyncDoubler.getState(), is(State.VALID));
        assertThat(asyncDoubler.getValue(), is(2));
    }

    @Test
    public void sourceToAsycFlowToFlow() {
        SourceNode<Integer> source = new SourceNode<>();
        AsyncFlowNode<Integer, Integer> lazyAsyncDoubler = new LazyAsyncDoubler(source, context);
        AsyncFlowNode<Integer, Integer> asyncDoubler = new AsyncDoubler(lazyAsyncDoubler, context);

        assertThat(asyncDoubler.getState(), is(State.MISSING_INPUT));

        source.setValue(1, context);

        assertThat(asyncDoubler.getState(), is(State.READY));
        assertThat(lazyAsyncDoubler.getState(), is(State.READY));

        asyncDoubler.pull(context);

        assertThat(asyncDoubler.getState(), is(CALCULATING));
        assertThat(lazyAsyncDoubler.getState(), is(CALCULATING));

        context.doJob();

        assertThat(lazyAsyncDoubler.getState(), is(State.VALID));
        assertThat(asyncDoubler.getState(), is(CALCULATING));

        context.doJob();

        assertThat(asyncDoubler.getState(), is(State.VALID));
        assertThat(asyncDoubler.getValue(), is(4));
    }

    @Test
    public void sourceToAsyncFlowToFlowWithSink() {
        SourceNode<Integer> source = new SourceNode<>();
        AsyncFlowNode<Integer, Integer> lazyAsyncDoubler = new LazyAsyncDoubler(source, context);
        AsyncFlowNode<Integer, Integer> asyncDoubler = new AsyncDoubler(lazyAsyncDoubler, context);

        Sink lazySink = new Sink(lazyAsyncDoubler);
        Sink sink = new Sink(asyncDoubler);

        source.setValue(1, context);

        assertThat(sink.received.poll().state, is(State.READY));
        assertThat(lazySink.received.poll().state, is(State.READY));

        asyncDoubler.pull(context);

        assertThat(sink.received.poll().state, is(State.CALCULATING));
        assertThat(lazySink.received.poll().state, is(State.CALCULATING));

        context.doJob();

        assertThat(sink.received.size(), is(0));
        StateWrapper<Integer> lazyUpdate = lazySink.received.poll();
        assertThat(lazyUpdate.state, is(State.VALID));
        assertThat(lazyUpdate.value, is(2));

        context.doJob();

        assertThat(lazySink.received.size(), is(0));
        StateWrapper<Integer> update = sink.received.poll();
        assertThat(update.state, is(State.VALID));
        assertThat(update.value, is(4));
    }

    @Test
    public void sourceIsChangedBeforeAsyncFlowCompletes() {
        SourceNode<Integer> source = new SourceNode<>();
        CountingAsyncDoubler doubler = new CountingAsyncDoubler(source, context);
        Sink sink = new Sink(doubler);

        source.setValue(1, context);

        assertThat(sink.received.poll().state, is(State.CALCULATING));
        assertThat(doubler.computations, is(0));

        source.setValue(2, context);

        assertTrue(sink.received.isEmpty());

        context.doJob();
        assertTrue(sink.received.isEmpty());
        assertThat(doubler.computations, is(0));

        context.doJob();
        StateWrapper<Integer> update = sink.received.poll();
        assertThat(update.state, is(State.VALID));
        assertThat(update.value, is(4));
        assertThat(doubler.computations, is(1));
    }
}