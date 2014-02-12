package cz.tul.dic.opencl.test.gen.scenario.d1.opt;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLEvent.ProfilingCommand;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioOpenCL;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public final class CL1DImageL extends ScenarioOpenCL {

    private static final int VARIANT_COUNT = 1;
    private boolean computed;

    public CL1DImageL(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        resetInner();
    }

    @Override
    public float[] prepareAndCompute(int[] imageA, int[] imageB, int[] facetData, int[] facetCenters, float[] deformations, ParameterSet params) {
        computed = true;

        float[] result = computeScenario(imageA, imageB, facetData, facetCenters, deformations, params);

        return result;
    }

    private float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = params.getValue(Parameter.DEFORMATION_COUNT);        
        // prepare buffers        
        final CLImage2d<IntBuffer> imageAcl = createImage(imageA, params.getValue(Parameter.IMAGE_WIDTH));
        final CLImage2d<IntBuffer> imageBcl = createImage(imageB, params.getValue(Parameter.IMAGE_WIDTH));

        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetData, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetCenters = createIntBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = createFloatBuffer(deformations, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        long clSize = imageAcl.getCLSize() + imageBcl.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare work sizes
        final int facetCoordCount = Utils.calculateFacetArraySize(facetSize);
        final int maxWorkSize = contextHandler.getDevice().getMaxWorkGroupSize();        
        if (maxWorkSize < facetCoordCount) {
            return null;
        }
        
        final int lws0;
        if (facetCoordCount < deformationCount) {
            lws0 = deformationCount;
        } else {
            lws0 = facetCoordCount;
        }
        final int facetGlobalWorkSize = facetCount * lws0;
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, 1);
        
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
                .putArg(facetCount)
                .rewind();
        // execute kernel         
        prepareEventList(1);
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