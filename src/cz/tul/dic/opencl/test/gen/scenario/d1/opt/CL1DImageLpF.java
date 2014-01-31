package cz.tul.dic.opencl.test.gen.scenario.d1.opt;

import com.jogamp.common.nio.Buffers;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLEvent.ProfilingCommand;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public final class CL1DImageLpF extends Scenario {

    private static final int VARIANT_COUNT = 1;
    private boolean computed;

    public CL1DImageLpF(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        resetInner();
    }

    ScenarioResult computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = params.getValue(Parameter.DEFORMATION_COUNT);
        // prepare work sizes
        final int lws0 = (int) Math.pow(2, CustomMath.power2(deformationCount));
        final int facetGlobalWorkSize = facetCount * lws0;
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, 1);
        final int maxWorkSize = contextHandler.getDevice().getMaxWorkGroupSize();
        final int facetCoordCount = Utils.calculateFacetArraySize(facetSize);
        if (maxWorkSize < facetCoordCount) {
            return null;
        }
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
        fillBuffer(bufferFacetData.getBuffer(), facetData);
        fillBuffer(bufferFacetCenters.getBuffer(), facetCenters);
        fillBuffer(bufferDeformations.getBuffer(), deformations);
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
                .putArg(facetCount)
                .rewind();
        // execute kernel         
        CLEventList eventList = new CLEventList(1);

        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(Mode.PROFILING_MODE);

        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);
        queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
        queue.putReadBuffer(bufferResult, true);
        queue.finish();

        final float[] result = readBuffer(bufferResult.getBuffer());
        final long duration = eventList.getEvent(0).getProfilingInfo(ProfilingCommand.END) - eventList.getEvent(0).getProfilingInfo(ProfilingCommand.START);

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

    @Override
    public ScenarioResult compute(int[] imageA, int[] imageB, int[] facetData, int[] facetCenters, float[] deformations, ParameterSet params) {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        contextHandler.setFacetSize(facetSize);
        computed = true;

        ScenarioResult result = computeScenario(imageA, imageB, facetData, facetCenters, deformations, params);

        return result;
    }

    @Override
    public boolean hasNext() {
        return !computed;
    }

    @Override
    protected void resetInner() {
        computed = false;
    }

    @Override
    public int getVariantCount() {
        return VARIANT_COUNT;
    }

}
