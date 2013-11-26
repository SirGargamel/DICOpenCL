package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLKernel;
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
public class Compute2DIntGpuDirect extends Scenario2D {

    private static final String NAME = "Compute2DIntGpuDirect";

    public Compute2DIntGpuDirect(final ContextHandler contextHandler) throws IOException {
        super(NAME, contextHandler);
    }

    @Override
    public float[] computeScenario(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
            final int[] facets, final float[] deformations,
            final ParameterSet params) {
        float[] result = null;
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        // prepare buffers
        final CLContext context = contextHandler.getContext();
        final CLBuffer<IntBuffer> bufferImageA = context.createIntBuffer(imageA.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = context.createIntBuffer(imageB.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacets = context.createIntBuffer(facets.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = context.createFloatBuffer(deformations.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = context.createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacets.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // fill buffers
        fillBuffer(bufferImageA.getBuffer(), imageA);
        fillBuffer(bufferImageB.getBuffer(), imageB);
        fillBuffer(bufferFacets.getBuffer(), facets);
        fillBuffer(bufferDeformations.getBuffer(), deformations);
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacets, bufferDeformations, bufferResult)
                .putArg(imageAavg)
                .putArg(imageBavg)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
                .putArg(facetCount)
                .rewind();
        // prepare work sizes
        final int lws0 = getLWS0();
        final int lws1 = getLWS1();
        final int facetGlobalWorkSize = roundUp(lws0, facetCount);
        final int deformationsGlobalWorkSize = roundUp(lws1, params.getValue(Parameter.DEFORMATION_COUNT));
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, lws1);
        // execute kernel
        try {
            final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue();

            queue.putWriteBuffer(bufferImageA, false);
            queue.putWriteBuffer(bufferImageB, false);
            queue.putWriteBuffer(bufferFacets, false);
            queue.putWriteBuffer(bufferDeformations, false);
            queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1);            
            queue.putReadBuffer(bufferResult, true);
            result = readBuffer(bufferResult.getBuffer());

            // data cleanup
            bufferImageA.release();
            bufferImageB.release();
            bufferFacets.release();
            bufferDeformations.release();
            bufferResult.release();
        } catch (CLException ex) {
            System.err.println("CL error - " + ex.getLocalizedMessage());            
        }

        return result;
    }

    private static void fillBuffer(IntBuffer buffer, int[] data) {
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
    }

    private static void fillBuffer(FloatBuffer buffer, float[] data) {
        for (float f : data) {
            buffer.put(f);
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
        
        int result;
        if (r == 0) {
            result = globalSize;
        } else {
            result = globalSize + groupSize - r;
        }
        
        return result;
    }

}
