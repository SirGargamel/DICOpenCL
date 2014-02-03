package cz.tul.dic.opencl.test.gen.scenario.d15;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL15DIntPerDeformation extends Scenario15D {

    public CL15DIntPerDeformation(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    protected float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;
        // prepare buffers        
        final CLBuffer<IntBuffer> bufferImageA = createIntBuffer(imageA, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = createIntBuffer(imageB, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetData, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetCenters = createIntBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = createFloatBuffer(deformations, READ_ONLY);
        final CLBuffer<IntBuffer> bufferDeformationIndex = createIntBuffer(1, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult, bufferDeformationIndex)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
                .putArg(facetCount)
                .rewind();
        // prepare work sizes
        final int lws0 = getLWS0();
        final int facetGlobalWorkSize = roundUp(lws0, deformationCount);
        params.addParameter(Parameter.LWS0, lws0);
        // execute kernel                
        prepareEventList(deformationCount);
        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);

        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);

        for (int i = 0; i < deformationCount; i++) {
            fillBuffer(bufferDeformationIndex.getBuffer(), i);
            queue.putWriteBuffer(bufferDeformationIndex, false);

            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
        }
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        final float[] result = readBuffer(bufferResult.getBuffer());

        return result;
    }

    private static void fillBuffer(IntBuffer buffer, int value) {
        buffer.clear();
        buffer.put(value);
        buffer.rewind();
    }

}
