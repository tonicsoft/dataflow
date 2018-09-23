package foo;

public abstract class Flows {
    public static abstract class FlowNode<Output, Input> extends Node<Output> {
        private final SourceNode<Input> source;

        public FlowNode(SourceNode<Input> source) {
            source.connectChild(this);
            this.source = source;
        }

        @Override
        public void updateState(Context context) {
            setValue(compute(source.getValue()), context);
        }

        public abstract Output compute(Input input);
    }

    public static abstract class AsyncFlowNode<Output, Input> extends Node<Output> {
        private final Node<Input> source;
        private final boolean lazy;
        private AsyncUpdateTask currentUpdate = null;

        public AsyncFlowNode(Node<Input> source, Context context) {
            this(source, context, false);
        }

        public AsyncFlowNode(Node<Input> source, Context context, boolean lazy) {
            source.connectChild(this);
            this.source = source;
            updateState(context);
            this.lazy = lazy;
        }

        @Override
        public void updateState(Context context) {
            if (source.getState() == State.VALID) {
                if (!lazy) {
                    scheduleRefresh(context);
                } else {
                    setState(State.READY, null, context);
                }
            } else if (source.getState() == State.READY) {
                setState(State.READY, null, context);
            }
        }

        @Override
        public void pull(Context context) {
            if (getState() != State.VALID) {
                source.pull(context);
                if (source.getState() == State.VALID) {
                    scheduleRefresh(context);
                } else if (source.getState() == State.CALCULATING) {
                    setState(State.CALCULATING, null, context);
                }
            }
        }

        private void scheduleRefresh(Context context) {
            if (getState() == State.CALCULATING && currentUpdate != null) {
                currentUpdate.isCancelled = true;
            }

            setState(State.CALCULATING, null, context);
            final Input input = source.getValue();

            AsyncUpdateTask newUpdate = new AsyncUpdateTask(() -> {
                Output result = this.compute(input);
                this.setState(State.VALID, result, context);
            });
            currentUpdate = newUpdate;
            context.addTask(newUpdate);
        }

        public abstract Output compute(Input input);
    }

    public static class AsyncUpdateTask implements Runnable {
        private final Runnable delegate;
        public boolean isCancelled = false;

        protected AsyncUpdateTask(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            if (!isCancelled) {
                delegate.run();
            }
        }
    }

    public static abstract class Flow2Node<Output, Input1, Input2> extends Node<Output> {
        private final Node<Input1> source1;
        private final Node<Input2> source2;

        public Flow2Node(Node<Input1> source1, Node<Input2> source2) {
            source1.connectChild(this);
            source2.connectChild(this);
            this.source1 = source1;
            this.source2 = source2;
        }

        @Override
        public void updateState(Context context) {
            if (source1.getState() == State.VALID && source2.getState() == State.VALID) {
                setValue(compute(source1.getValue(), source2.getValue()), context);
            }
        }

        public abstract Output compute(Input1 input1, Input2 input2);
    }
}
