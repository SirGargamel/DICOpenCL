package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario {

    private final String name;
    protected final ContextHandler contextHandler;

    public Scenario(final String scenarioName, final ContextHandler contextHandler) throws IOException {
        this.contextHandler = contextHandler;
        this.name = scenarioName;
    }

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

    public abstract boolean hasNext();

    public void reset() {
        if (contextHandler != null) {
            contextHandler.assignScenario(this);
        }

        resetInner();
    }

    protected abstract void resetInner();

    public String getDescription() {
        return name;
    }

    public abstract int getVariantCount();
    
    protected void fillBuffer(IntBuffer buffer, int[] data) {
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
    }

    protected void fillBuffer(FloatBuffer buffer, float[] data) {
        for (float f : data) {
            buffer.put(f);
        }
        buffer.rewind();
    }

    protected float[] readBuffer(final FloatBuffer buffer) {
        buffer.rewind();
        float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

    protected int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;

        int result;
        if (r == 0) {
            result = globalSize;
        } else {
            result = globalSize + groupSize - r;
        }

        return result;
    }

}
