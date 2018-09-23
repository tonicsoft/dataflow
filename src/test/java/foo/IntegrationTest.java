package foo;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.util.stream.Collectors.joining;

public class IntegrationTest {
    static class StepFile extends SourceNode<String> {

    }

    static class Brep extends Flows.FlowNode<String, String> {
        public Brep(StepFile source) {
            super(source);
        }

        @Override
        public String compute(String s) {
            System.out.println("calculating brep for " + s + "...");
            return "I'm a brep of {" + s + "}";
        }
    }

    static class Mesh extends Flows.AsyncFlowNode<String, String> {

        public Mesh(Brep source, Context context) {
            super(source, context);
        }

        @Override
        public String compute(String s) {
            System.out.println("calculating mesh for " + s + "...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("done.");
            return "I'm a mesh of {" + s + "}";
        }
    }

    static class FrontEnd {
        private final BlockingQueue<String> messages = new ArrayBlockingQueue<>(100);
        private final StepFile stepFile;
        private final Mesh mesh;

        FrontEnd(StepFile stepFile, Mesh mesh) {
            this.stepFile = stepFile;
            this.mesh = mesh;
            new OnStepFile();
            new OnMesh();
        }

        public void dispatch() {
            ArrayList<String> toPrint = new ArrayList<>();
            messages.drainTo(toPrint);
            String finalMessage = toPrint.stream().collect(joining("\n"));
            System.out.println("----------MESSAGES:\n" + finalMessage + "\n------------");
        }

        class OnStepFile extends EventSink<String> {
            public OnStepFile() {
                super(stepFile);
            }

            @Override
            void update(StateWrapper<String> wrapper) {
                if (wrapper.state == State.VALID) {
                    messages.add("Step file updated to " + wrapper.value);
                } else {
                    messages.add("No Step file");
                }
            }
        }

        class OnMesh extends EventSink<String> {
            public OnMesh() {
                super(mesh);
            }

            @Override
            void update(StateWrapper<String> wrapper) {
                if (wrapper.state == State.VALID) {
                    messages.add("Mesh updated to " + wrapper.value);
                } else {
                    messages.add("no mesh");
                }
            }
        }
    }

    @Test
    public void me() {
        Context context = new Context();
        StepFile stepFile = new StepFile();
        Brep brep = new Brep(stepFile);
        Mesh mesh = new Mesh(brep, context);

        FrontEnd frontEnd = new FrontEnd(stepFile, mesh);

        frontEnd.dispatch();

        stepFile.setValue("step1", context);

        frontEnd.dispatch();

        context.doJob();

        frontEnd.dispatch();





    }

}
