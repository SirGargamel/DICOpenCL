package cz.tul.dic.test.opencl.test.gen.scenario.limitsNO;

import cz.tul.dic.test.opencl.test.gen.ContextHandler;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.test.opencl.test.gen.Parameter;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL_NO_2DImageV_GPU extends Scenario2D_NO_GPU {

    public CL_NO_2DImageV_GPU(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    public float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final CLBuffer<IntBuffer> bufferFacetData, final CLBuffer<FloatBuffer> bufferFacetCenters,
            final CLBuffer<FloatBuffer> bufferDeformations,
            final ParameterSet params) throws CLException {
        final int lws0 = getLWS0();
        final int lws1 = getLWS1();
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, lws1);
        // prepare buffers
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLImage2d<IntBuffer> imageAcl = createImage(imageA, params.getValue(Parameter.IMAGE_WIDTH));
        final CLImage2d<IntBuffer> imageBcl = createImage(imageB, params.getValue(Parameter.IMAGE_WIDTH));        
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        final long clSize = imageAcl.getCLSize() + imageBcl.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
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
        final CLCommandQueue queue = contextHandler.getQueue();
        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
        queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1, eventList);
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        return readBuffer(bufferResult.getBuffer());
    }

}
