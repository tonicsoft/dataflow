package foo;

import java.util.ArrayDeque;
import java.util.Queue;

public class Context {
    public Queue<Runnable> jobs = new ArrayDeque<>();

    public void addTask(Runnable job) {
        jobs.add(job);
    }

    public void doJob() {
        jobs.poll().run();
    }
}
