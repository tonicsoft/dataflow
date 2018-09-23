package foo;

import foo.Flows.Flow2Node;
import foo.Flows.FlowNode;
import foo.TestNodes.Doubler;
import foo.TestNodes.Sink;
import foo.TestNodes.Summer;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SyncTest {

    private final Context context = new Context();

    @Test
    public void sourceHasMissingInputState() {
        SourceNode source = new SourceNode();

        assertThat(source.getState(), is(State.MISSING_INPUT));
    }

    @Test
    public void setStateOnSource() {
        SourceNode<Integer> source = new SourceNode<>();

        source.setValue(1, context);

        assertThat(source.getState(), is(State.VALID));
        assertThat(source.getValue(), is(1));
    }

    @Test
    public void whenValuesAreSetEventSinksAreNotified() {
        SourceNode<Integer> source = new SourceNode<>();

        Sink sink = new Sink(source);

        source.setValue(1, context);
        assertThat(sink.received.poll().value, is(1));
    }



    @Test
    public void sourceAndFlow() {
        SourceNode<Integer> source = new SourceNode<>();

        FlowNode<Integer, Integer> doubler = new Doubler(source);

        assertThat(doubler.getState(), is(State.MISSING_INPUT));

        source.setValue(1, context);

        assertThat(doubler.getState(), is(State.VALID));
        assertThat(doubler.getValue(), is(2));
    }

    @Test
    public void flowWithEventSink() {
        SourceNode<Integer> source = new SourceNode<>();
        FlowNode<Integer, Integer> doubler = new Doubler(source);

        Sink sink = new Sink(doubler);

        source.setValue(1, context);
        assertThat(sink.received.poll().value, is(2));
    }

    @Test
    public void sourceWithTwoFlows() {
        SourceNode<Integer> source = new SourceNode<>();
        FlowNode<Integer, Integer> doubler1 = new Doubler(source);
        FlowNode<Integer, Integer> doubler2 = new Doubler(source);

        source.setValue(1, context);
        assertThat(doubler1.getState(), is(State.VALID));
        assertThat(doubler2.getState(), is(State.VALID));
        assertThat(doubler1.getValue(), is(2));
        assertThat(doubler2.getValue(), is(2));
    }

    @Test
    public void diamond() {
        SourceNode<Integer> source = new SourceNode<>();
        FlowNode<Integer, Integer> doubler1 = new Doubler(source);
        FlowNode<Integer, Integer> doubler2 = new Doubler(source);

        Flow2Node<Integer, Integer, Integer> sum = new Summer(doubler1, doubler2);

        assertThat(sum.getState(), is(State.MISSING_INPUT));

        source.setValue(1, context);

        assertThat(sum.getState(), is(State.VALID));
        assertThat(sum.getValue(), is(4));
    }

    @Test
    public void flow2Node() {
        SourceNode<Integer> source1 = new SourceNode<>();
        SourceNode<Integer> source2 = new SourceNode<>();

        Flow2Node<Integer, Integer, Integer> sum = new Summer(source1, source2);

        assertThat(sum.getState(), is(State.MISSING_INPUT));
        source1.setValue(1, context);
        assertThat(sum.getState(), is(State.MISSING_INPUT));
        source2.setValue(1, context);
        assertThat(sum.getState(), is(State.VALID));
        assertThat(sum.getValue(), is(2));
    }
}