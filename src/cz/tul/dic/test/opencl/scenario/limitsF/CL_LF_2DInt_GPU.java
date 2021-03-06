package cz.tul.dic.test.opencl.scenario.limitsF;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.test.opencl.scenario.ContextHandler;
import cz.tul.dic.test.opencl.scenario.Parameter;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL_LF_2DInt_GPU extends Scenario2D_LF_GPU {

    private final String kernelName;

    public CL_LF_2DInt_GPU(final String kernelName, final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        this.kernelName = kernelName;
    }

    @Override
    public String getKernelName() {
        return kernelName;
    }

    @Override
    public float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final CLBuffer<FloatBuffer> bufferDeformations,
            final ParameterSet params) throws CLException {
        final int lws0 = getLWS0();
        final int lws1 = getLWS1();
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, lws1);
        // prepare buffers        
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLBuffer<IntBuffer> bufferImageA = createIntBuffer(imageA, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = createIntBuffer(imageB, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferFacetCenters = createFloatBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        final long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetCenters.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare kernel arguments        
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(params.getValue(Parameter.FACET_SIZE))
                .putArg(facetCount)
                .rewind();
        // prepare work sizes
        final int facetGlobalWorkSize = roundUp(lws0, facetCount);
        final int deformationsGlobalWorkSize = roundUp(lws1, params.getValue(Parameter.DEFORMATION_COUNT));
        // execute kernel        
        prepareEventList(1);
        final CLCommandQueue queue = createCommandQueue();
        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1, eventList);
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        // create result
        final float[] result = readBuffer(bufferResult.getBuffer());
        return result;
    }

}
