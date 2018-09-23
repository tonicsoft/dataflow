package foo;

public abstract class EventSink<T> {
    public EventSink(Node<T> source) {
        source.registerEventSink(this);
    }
    abstract void update(StateWrapper<T> value);
}
