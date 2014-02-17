package cz.tul.dic.opencl.test.gen.scenario.comb;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.d2.Scenario2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL2DImage_MC_V extends Scenario2D {

    public CL2DImage_MC_V(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    protected float[] computeScenario(
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
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
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
        prepareEventList(1);
        final CLCommandQueue queue = createCommandQueue();
        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);
        queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1, eventList);
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        // create result
        final float[] result = readBuffer(bufferResult.getBuffer());
        return result;
    }

}
