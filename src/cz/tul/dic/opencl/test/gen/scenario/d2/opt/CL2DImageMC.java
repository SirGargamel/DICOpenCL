package cz.tul.dic.opencl.test.gen.scenario.d2.opt;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.d2.Scenario2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL2DImageMC extends Scenario2D {

    public CL2DImageMC(ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    protected ScenarioResult computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        // prepare buffers
        final CLContext context = contextHandler.getContext();

        CLImageFormat format = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);

        final IntBuffer imageAbuffer = Buffers.newDirectIntBuffer(imageA);
        final CLImage2d<IntBuffer> imageAcl = context.createImage2d(imageAbuffer, params.getValue(Parameter.IMAGE_WIDTH), params.getValue(Parameter.IMAGE_HEIGHT), format, READ_ONLY);

        final IntBuffer imageBbuffer = Buffers.newDirectIntBuffer(imageB);
        final CLImage2d<IntBuffer> imageBcl = context.createImage2d(imageBbuffer, params.getValue(Parameter.IMAGE_WIDTH), params.getValue(Parameter.IMAGE_HEIGHT), format, READ_ONLY);

        final CLBuffer<IntBuffer> bufferFacetData = context.createIntBuffer(facetData.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetCenters = context.createIntBuffer(facetCenters.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = context.createFloatBuffer(deformations.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = context.createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        long clSize = imageAcl.getCLSize() + imageBcl.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // fill buffers                
        fillBuffer(bufferFacetCenters.getBuffer(), facetCenters);
        fillBuffer(bufferDeformations.getBuffer(), deformations);
        // facet data filled mannually, taking advantage of memory coalescing
        final int facetArea = Utils.calculateFacetArea(facetSize);
        final int facetArraySize = Utils.calculateFacetArraySize(facetSize);
        final IntBuffer facetDataBuffer = bufferFacetData.getBuffer();
        int index;
        for (int f = 0; f < facetCount; f++) {
            for (int i = 0; i < facetArea; i++) {
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
        CLEventList eventList = new CLEventList(1);
        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);

        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);
        queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1, eventList);
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        final float[] result = readBuffer(bufferResult.getBuffer());

        final long start = eventList.getEvent(0).getProfilingInfo(CLEvent.ProfilingCommand.START);
        final long end = eventList.getEvent(0).getProfilingInfo(CLEvent.ProfilingCommand.END);
        final long duration = end - start;

        // data cleanup
        imageAcl.release();
        imageBcl.release();
        bufferFacetData.release();
        bufferFacetCenters.release();
        bufferDeformations.release();
        bufferResult.release();
        eventList.release();

        return new ScenarioResult(result, duration);
    }

}
