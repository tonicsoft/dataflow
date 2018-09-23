package foo;

public class StateWrapper<T> {
    public final T value;
    public final State state;

    public StateWrapper(T value, State state) {
        this.value = value;
        this.state = state;
    }
}
