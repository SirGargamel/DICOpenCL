package cz.tul.dic.test.opencl.scenario.fulldata.driven;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.test.opencl.scenario.ContextHandler;
import cz.tul.dic.test.opencl.scenario.Parameter;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.scenario.Utils;
import cz.tul.dic.test.opencl.scenario.WorkSizeManager;
import cz.tul.dic.test.opencl.scenario.fulldata.ScenarioDrivenOpenCL;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL2D_I_V_MC_D extends ScenarioDrivenOpenCL {

    private static final int ARGUMENT_INDEX = 11;
    private static final int OPTIMAL_LWS1 = 128;

    public CL2D_I_V_MC_D(final ContextHandler contextHandler, final WorkSizeManager wsm) throws IOException {
        super(contextHandler, wsm);
    }

    @Override
    protected float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params) {
        // prepare buffers
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLImage2d<IntBuffer> imageAcl = createImage(imageA, params.getValue(Parameter.IMAGE_WIDTH));
        final CLImage2d<IntBuffer> imageBcl = createImage(imageB, params.getValue(Parameter.IMAGE_WIDTH));
        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetData.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferFacetCenters = createFloatBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = createFloatBuffer(deformations, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        final long clSize = imageAcl.getCLSize() + imageBcl.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // facet data filled mannually, taking advantage of memory coalescing
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetArea = Utils.calculateFacetArea(facetSize);
        final int facetArraySize = Utils.calculateFacetArraySize(facetSize);
        final IntBuffer facetDataBuffer = bufferFacetData.getBuffer();
        int index;
        for (int i = 0; i < facetArea; i++) {
            for (int f = 0; f < facetCount; f++) {
                index = f * facetArraySize + 2 * i;
                facetDataBuffer.put(facetData[index]);
                facetDataBuffer.put(facetData[index + 1]);
            }
        }
        facetDataBuffer.rewind();
        // prepare kernel arguments
        int facetSubCount = getFacetCount();
        final int roundCount = (int) Math.ceil(facetCount / (double) facetSubCount);
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
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
        final CLCommandQueue queue = createCommandQueue();
        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
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
