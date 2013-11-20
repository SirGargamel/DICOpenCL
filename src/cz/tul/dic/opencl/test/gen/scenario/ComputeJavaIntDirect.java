package cz.tul.dic.opencl.test.gen.scenario;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLMemory;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class ComputeJavaIntDirect extends Scenario {

    private static final String NAME = "ComputeJavaIntDirect";
    private static final int NAME_INT = 0;

    public ComputeJavaIntDirect(final CLDevice device) throws IOException {
        super(NAME, device);
    }

    @Override
    public boolean computeScenario(
            int[] imageA, int[] imageB,
            int[] facets, int[] deformations,
            ParameterSet params, final CLDevice device,
            int variant) {
        boolean result = false;
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = facets.length / (facetSize * facetSize);
        // prepare buffers
        CLBuffer<IntBuffer> bufferImageA = context.createIntBuffer(imageA.length, READ_ONLY);
        CLBuffer<IntBuffer> bufferImageB = context.createIntBuffer(imageB.length, READ_ONLY);
        CLBuffer<IntBuffer> bufferFacets = context.createIntBuffer(facets.length, READ_ONLY);
        CLBuffer<IntBuffer> bufferDeformations = context.createIntBuffer(deformations.length, READ_ONLY);
        CLBuffer<FloatBuffer> bufferResult = context.createFloatBuffer(facetCount * (deformations.length / 2), CLMemory.Mem.WRITE_ONLY);
        // fill buffers
        fillBuffer(bufferImageA.getBuffer(), imageA);
        fillBuffer(bufferImageB.getBuffer(), imageB);
        fillBuffer(bufferFacets.getBuffer(), facets);
        fillBuffer(bufferDeformations.getBuffer(), deformations);
        // prepare kernel arguments
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacets, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(facetSize).rewind();
        // prepare local work size
        final int lws0 = getLWS0();
        final int lws1 = getLWS1();
        final int facetGlobalWorkSize = roundUp(lws0, facetCount);
        final int deformationsGlobalWorkSize = roundUp(lws1, params.getValue(Parameter.DEFORMATION_COUNT));
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, lws1);
        // execute kernel
        try {
            CLCommandQueue queue = device.createCommandQueue();

            queue.putWriteBuffer(bufferImageA, false)
                    .putWriteBuffer(bufferImageB, false)
                    .putWriteBuffer(bufferFacets, false)
                    .putWriteBuffer(bufferDeformations, false)
                    .put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1)
                    .putReadBuffer(bufferResult, true);
            // data cleanup
            bufferImageA.release();
            bufferImageB.release();
            bufferFacets.release();
            bufferDeformations.release();
            bufferResult.release();
            result = true;
        } catch (CLException ex) {
            System.err.println("Queue error - " + ex.getCLErrorString() + " - " + ex.getLocalizedMessage());
        }
        return result;
    }

    private static void fillBuffer(IntBuffer buffer, int[] data) {
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
    }

    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }

    @Override
    protected int getIntDescription() {
        return NAME_INT;
    }

}
