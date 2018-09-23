package foo;

import java.util.ArrayDeque;
import java.util.Queue;

public class TestNodes {
    static class Sink extends EventSink<Integer> {
        public Queue<StateWrapper<Integer>> received = new ArrayDeque<>();
        public boolean valid = false;
        public int invalidationCount = 0;

        public Sink(Node<Integer> source) {
            super(source);
        }

        @Override
        void update(StateWrapper<Integer> value) {
            received.add(value);
            if (value.state == State.VALID) {
                valid = true;
            } else {
                invalidationCount++;
                valid = false;
            }
        }
    }

    public static class Doubler extends Flows.FlowNode<Integer, Integer> {
        public Doubler(SourceNode<Integer> source) {
            super(source);
        }

        @Override
        public Integer compute(Integer integer) {
            return integer * 2;
        }
    }

    public static class AsyncDoubler extends Flows.AsyncFlowNode<Integer, Integer> {
        public AsyncDoubler(Node<Integer> source, Context context) {
            super(source, context);
        }

        @Override
        public Integer compute(Integer integer) {
            return integer * 2;
        }
    }

    public static class CountingAsyncDoubler extends Flows.AsyncFlowNode<Integer, Integer> {
        public CountingAsyncDoubler(Node<Integer> source, Context context) {
            super(source, context);
        }

        public int computations = 0;

        @Override
        public Integer compute(Integer integer) {
            computations++;
            return integer * 2;
        }
    }

    public static class LazyAsyncDoubler extends Flows.AsyncFlowNode<Integer, Integer> {
        public LazyAsyncDoubler(Node<Integer> source, Context context) {
            super(source, context, true);
        }

        @Override
        public Integer compute(Integer integer) {
            return integer * 2;
        }
    }

    static class Summer extends Flows.Flow2Node<Integer, Integer, Integer> {

        public Summer(Node<Integer> source1, Node<Integer> source2) {
            super(source1, source2);
        }

        @Override
        public Integer compute(Integer integer, Integer integer2) {
            return integer + integer;
        }
    }
}
