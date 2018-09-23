package foo;

import java.util.ArrayList;
import java.util.List;

public abstract class Node<T> {
    private State state = State.MISSING_INPUT;
    private List<EventSink<T>> sinks = new ArrayList<>();
    private List<Node> children = new ArrayList<>();
    private T value;

    public State getState() {
        return state;
    }

    public T getValue() {
        return value;
    }

    public void setState(State state, T value, Context context) {
        boolean stateIsChanging = state != this.state;
        this.state = state;
        this.value = value;
        if (state == State.VALID || stateIsChanging) {
            sinks.forEach(s -> s.update(new StateWrapper<>(value, state)));
            children.forEach(n -> n.updateState(context));
        }
    }
    public void setValue(T value, Context context) {
        setState(State.VALID, value, context);
    }


    public void connectChild(Node child) {
        children.add(child);
    }

    public void registerEventSink(EventSink<T> sink) {
        sinks.add(sink);
    }

    public abstract void updateState(Context context);

    public void pull(Context context) {

    }
}
