package cz.tul.dic.opencl.test.gen.scenario.d1;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
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
public class CL1DIntPerFacetSingle extends Scenario1D {

    private static final int FACET_DIMENSION = 2;

    public CL1DIntPerFacetSingle(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    protected float[] computeScenario(int[] imageA, int[] imageB, int[] facetData, int[] facetCenters, float[] deformations, ParameterSet params) throws CLException {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetDataSize = Utils.calculateFacetArraySize(facetSize);
        final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final float[] result = new float[facetCount * deformationCount];
        // prepare buffers        
        final CLBuffer<IntBuffer> bufferImageA = createIntBuffer(imageA, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = createIntBuffer(imageB, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetDataSize, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetCenter = createIntBuffer(FACET_DIMENSION, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = createFloatBuffer(deformations, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(deformationCount, WRITE_ONLY);
        final long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetData, bufferFacetCenter, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
                .rewind();
        // prepare work sizes
        final int lws0 = getLWS0();
        final int facetGlobalWorkSize = roundUp(lws0, deformationCount);
        params.addParameter(Parameter.LWS0, lws0);
        // execute kernel                
        float[] oneResult;
        prepareEventList(facetCount);
        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(Mode.PROFILING_MODE);
        // copy static data
        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferDeformations, false);
        for (int i = 0; i < facetCount; i++) {
            // prepare dynamic data
            fillBuffer(bufferFacetData.getBuffer(), facetData, i * facetDataSize, facetDataSize);
            fillBuffer(bufferFacetCenter.getBuffer(), facetCenters, i * 2, 2);
            // write new data and run kernel
            queue.putWriteBuffer(bufferFacetData, false);
            queue.putWriteBuffer(bufferFacetCenter, false);

            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
            queue.putReadBuffer(bufferResult, true);
            oneResult = readBuffer(bufferResult.getBuffer());
            // store result
            System.arraycopy(oneResult, 0, result, i * deformationCount, deformationCount);
        }
        queue.finish();

        return result;
    }

    private static void fillBuffer(IntBuffer buffer, int[] data, int offset, int length) {
        buffer.clear();
        for (int i = offset; i < offset + length; i++) {
            buffer.put(data[i]);
        }
        buffer.rewind();
    }
}
