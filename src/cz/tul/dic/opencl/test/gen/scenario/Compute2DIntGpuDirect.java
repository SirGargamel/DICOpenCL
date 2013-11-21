package cz.tul.dic.opencl.test.gen.scenario;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLException;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class Compute2DIntGpuDirect extends Scenario {

    private static final String NAME = "Compute2DIntGpuDirect";    

    public Compute2DIntGpuDirect(final CLDevice device) throws IOException {
        super(NAME, device);
    }

    @Override
    public float[] computeScenario(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
            final int[] facets, final int[] deformations,
            final ParameterSet params, 
            final CLDevice device) {
        float[] result = null;
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = facets.length / (facetSize * facetSize);
        // prepare buffers
        CLBuffer<IntBuffer> bufferImageA = context.createIntBuffer(imageA.length, READ_ONLY);
        CLBuffer<IntBuffer> bufferImageB = context.createIntBuffer(imageB.length, READ_ONLY);
        CLBuffer<IntBuffer> bufferFacets = context.createIntBuffer(facets.length, READ_ONLY);
        CLBuffer<IntBuffer> bufferDeformedFacets = context.createIntBuffer(facets.length);
        CLBuffer<IntBuffer> bufferDeformations = context.createIntBuffer(deformations.length, READ_ONLY);
        CLBuffer<FloatBuffer> bufferResult = context.createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT),WRITE_ONLY);
        // fill buffers
        fillBuffer(bufferImageA.getBuffer(), imageA);
        fillBuffer(bufferImageB.getBuffer(), imageB);
        fillBuffer(bufferFacets.getBuffer(), facets);
        fillBuffer(bufferDeformations.getBuffer(), deformations);
        // prepare kernel arguments
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacets, bufferDeformedFacets, bufferDeformations, bufferResult)
                .putArg(imageAavg)
                .putArg(imageBavg)
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
            final CLCommandQueue queue = device.createCommandQueue();

            queue.putWriteBuffer(bufferImageA, false)
                    .putWriteBuffer(bufferImageB, false)
                    .putWriteBuffer(bufferFacets, false)                    
                    .putWriteBuffer(bufferDeformations, false)
                    .put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1)
                    .putReadBuffer(bufferResult, true);
            result = readBuffer(bufferResult.getBuffer());
        } catch (CLException ex) {            
            System.err.println("Queue error - " + ex.getCLErrorString() + " - " + ex.getLocalizedMessage());
        }

        // data cleanup
        bufferImageA.release();
        bufferImageB.release();
        bufferFacets.release();
        bufferDeformedFacets.release();
        bufferDeformations.release();
        bufferResult.release();

        return result;
    }

    private static void fillBuffer(IntBuffer buffer, int[] data) {
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
    }    
    
    private static float[] readBuffer(final FloatBuffer buffer) {               
        buffer.rewind();
        float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }

}
