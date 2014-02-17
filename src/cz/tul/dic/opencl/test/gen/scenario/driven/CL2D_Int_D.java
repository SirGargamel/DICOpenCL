package cz.tul.dic.opencl.test.gen.scenario.driven;

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
import cz.tul.dic.opencl.test.gen.WorkSizeManager;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioDrivenOpenCL;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL2D_Int_D extends ScenarioDrivenOpenCL {

    private static final int ARGUMENT_INDEX = 11;
    private static final int OPTIMAL_LWS1 = 128;

    public CL2D_Int_D(final ContextHandler contextHandler, final WorkSizeManager fcm) throws IOException {
        super(contextHandler, fcm);
    }

    @Override
    public float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        // prepare buffers        
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLBuffer<IntBuffer> bufferImageA = createIntBuffer(imageA, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = createIntBuffer(imageB, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetData, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferFacetCenters = createFloatBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = createFloatBuffer(deformations, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        final long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare kernel arguments
        int facetSubCount = getFacetCount();
        final int roundCount = (int) Math.ceil(facetCount / (double) facetSubCount);
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(params.getValue(Parameter.FACET_SIZE))
                .putArg(facetCount)
                .putArg(facetSubCount)
                .putArg(0)
                .rewind();
        // prepare work sizes
        final int lws0 = 1;
        final int lws1 = OPTIMAL_LWS1;
        final int facetGlobalWorkSize = roundUp(lws0, facetSubCount);
        final int deformationsGlobalWorkSize = roundUp(lws1, params.getValue(Parameter.DEFORMATION_COUNT));
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, lws1);
        // execute kernel        
        prepareEventList(roundCount);
        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(Mode.PROFILING_MODE);
        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);
        for (int i = 0; i < roundCount; i++) {
            kernel.setArg(ARGUMENT_INDEX, i * facetSubCount);
            queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1, eventList);
        }        
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        // create result
        final float[] result = readBuffer(bufferResult.getBuffer());
        return result;
    }

}
