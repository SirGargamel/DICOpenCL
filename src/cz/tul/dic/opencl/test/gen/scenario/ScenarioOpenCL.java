package cz.tul.dic.opencl.test.gen.scenario;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLMemory.Mem;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import com.jogamp.opencl.CLResource;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioOpenCL extends Scenario {

    private static final CLImageFormat IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);
    protected final ContextHandler contextHandler;
    protected final List<CLResource> memoryObjects;
    protected CLEventList eventList;

    public ScenarioOpenCL(ContextHandler contextHandler) throws IOException {
        super();

        this.contextHandler = contextHandler;
        memoryObjects = new LinkedList<>();

        eventList = null;
    }

    @Override
    public void prepare(final ParameterSet ps) {
        contextHandler.assignScenario(this);
        contextHandler.setFacetSize(ps.getValue(Parameter.FACET_SIZE));
    }

    @Override
    public ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) {
        params.addParameter(Parameter.LWS0, 1);
        params.addParameter(Parameter.LWS1, 1);

        float[] result = prepareAndCompute(imageA, imageB, facetData, facetCenters, deformations, params);
        
        final long duration = computeTotalKernelTime();
        
        resourceCleanup();

        return new ScenarioResult(result, duration);
    }

    protected long computeTotalKernelTime() {
        long duration = 0;
        for (CLEvent event : eventList) {
            duration += event.getProfilingInfo(CLEvent.ProfilingCommand.END) - event.getProfilingInfo(CLEvent.ProfilingCommand.START);
        }
        return duration;
    }

    protected void resourceCleanup() {
        for (CLResource m : memoryObjects) {
            if (!m.isReleased()) {
                m.release();
            }
        }
        memoryObjects.clear();        
    }

    protected abstract float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

    @Override
    public void reset() {
        super.reset();

        if (contextHandler != null) {
            contextHandler.assignScenario(this);
        }
    }

    protected void fillBuffer(final IntBuffer buffer, final int[] data) {
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
    }

    protected void fillBuffer(final FloatBuffer buffer, final float[] data) {
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

    protected CLImage2d<IntBuffer> createImage(final int[] imageData, final int imageWidth) {
        final IntBuffer imageAbuffer = Buffers.newDirectIntBuffer(imageData);
        final CLImage2d<IntBuffer> result = contextHandler.getContext().createImage2d(imageAbuffer, imageWidth, imageData.length / imageWidth, IMAGE_FORMAT, READ_ONLY);
        memoryObjects.add(result);
        return result;
    }

    protected CLBuffer<IntBuffer> createIntBuffer(final int bufferLength, Mem... params) {
        final CLBuffer<IntBuffer> result = contextHandler.getContext().createIntBuffer(bufferLength, params);
        memoryObjects.add(result);
        return result;
    }

    protected CLBuffer<IntBuffer> createIntBuffer(final int[] data, Mem... params) {
        final CLBuffer<IntBuffer> result = contextHandler.getContext().createIntBuffer(data.length, params);
        fillBuffer(result.getBuffer(), data);
        memoryObjects.add(result);
        return result;
    }

    protected CLBuffer<FloatBuffer> createFloatBuffer(final int bufferLength, Mem... params) {
        final CLBuffer<FloatBuffer> result = contextHandler.getContext().createFloatBuffer(bufferLength, params);
        memoryObjects.add(result);
        return result;
    }

    protected CLBuffer<FloatBuffer> createFloatBuffer(final float[] data, Mem... params) {
        final CLBuffer<FloatBuffer> result = contextHandler.getContext().createFloatBuffer(data.length, params);
        fillBuffer(result.getBuffer(), data);
        memoryObjects.add(result);
        return result;
    }

    protected void prepareEventList(final int size) {
        eventList = new CLEventList(size);
        memoryObjects.add(eventList);
    }

}
