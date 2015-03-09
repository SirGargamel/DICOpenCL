package cz.tul.dic.opencl.test.gen.scenario.fulldata.d1;

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
import cz.tul.dic.opencl.test.gen.scenario.d1.Scenario1D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL1DIntPerDeformationSingle extends Scenario1D {

    public CL1DIntPerDeformationSingle(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    protected float[] computeScenario(int[] imageA, int[] imageB, int[] facetData, final float[] facetCenters, float[] deformations, ParameterSet params) throws CLException {
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;
        final float[] result = new float[facetCount * deformationCount];
        // prepare buffers        
        final CLBuffer<IntBuffer> bufferImageA = createIntBuffer(imageA, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = createIntBuffer(imageB, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetData, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferFacetCenters = createFloatBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformation = createFloatBuffer(Utils.DEFORMATION_DIM, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount, WRITE_ONLY);
        final long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetData.getCLSize() + bufferDeformation.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetData, bufferFacetCenters, bufferDeformation, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(facetCount)
                .putArg(params.getValue(Parameter.FACET_SIZE))
                .rewind();
        // prepare work sizes
        final int lws0 = getLWS0();
        final int facetGlobalWorkSize = roundUp(lws0, facetCount);
        params.addParameter(Parameter.LWS0, lws0);
        // execute kernel                
        float[] oneResult;
        prepareEventList(deformationCount);
        final CLCommandQueue queue = createCommandQueue();
        // copy static data
        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        for (int i = 0; i < deformationCount; i++) {
            // prepare dynamic data
            fillBuffer(bufferDeformation.getBuffer(), deformations, i * Utils.DEFORMATION_DIM, Utils.DEFORMATION_DIM);
            // write new data and run kernel
            queue.putWriteBuffer(bufferDeformation, false);
            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
            queue.putReadBuffer(bufferResult, true);
            oneResult = readBuffer(bufferResult.getBuffer());
            // store result                
            for (int r = 0; r < facetCount; r++) {
                result[(r * deformationCount) + i] = oneResult[r];
            }
        }
        queue.finish();

        return result;
    }

    private static void fillBuffer(FloatBuffer buffer, float[] data, int offset, int length) {
        buffer.clear();
        for (int i = offset; i < offset + length; i++) {
            buffer.put(data[i]);
        }
        buffer.rewind();
    }
}
