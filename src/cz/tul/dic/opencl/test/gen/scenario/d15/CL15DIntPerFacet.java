package cz.tul.dic.opencl.test.gen.scenario.d15;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class CL15DIntPerFacet extends Scenario15D {    

    public CL15DIntPerFacet(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);
    }

    @Override
    public ScenarioResult computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        float[] result = null;
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;
        // prepare buffers
        final CLContext context = contextHandler.getContext();
        final CLBuffer<IntBuffer> bufferImageA = context.createIntBuffer(imageA.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = context.createIntBuffer(imageB.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetData = context.createIntBuffer(facetData.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetCenters = context.createIntBuffer(facetCenters.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = context.createFloatBuffer(deformations.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetIndex = context.createIntBuffer(1, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = context.createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // fill buffers
        fillBuffer(bufferImageA.getBuffer(), imageA);
        fillBuffer(bufferImageB.getBuffer(), imageB);
        fillBuffer(bufferFacetData.getBuffer(), facetData);
        fillBuffer(bufferFacetCenters.getBuffer(), facetCenters);
        fillBuffer(bufferDeformations.getBuffer(), deformations);
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult, bufferFacetIndex)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(facetSize)
                .putArg(facetCount)
                .rewind();
        // prepare work sizes
        final int lws0 = getLWS0();
        final int facetGlobalWorkSize = roundUp(lws0, deformationCount);
        params.addParameter(Parameter.LWS0, lws0);
        // execute kernel
        long duration = -1;
        CLEventList eventList = new CLEventList(facetCount);

        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);

        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);

        for (int i = 0; i < facetCount; i++) {
            fillBuffer(bufferFacetIndex.getBuffer(), i);
            queue.putWriteBuffer(bufferFacetIndex, false);

            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
        }
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        result = readBuffer(bufferResult.getBuffer());

        for (CLEvent ev : eventList) {
            duration += ev.getProfilingInfo(CLEvent.ProfilingCommand.END) - ev.getProfilingInfo(CLEvent.ProfilingCommand.START);
        }

        // data cleanup
        bufferImageA.release();
        bufferImageB.release();
        bufferFacetData.release();
        bufferFacetCenters.release();
        bufferDeformations.release();
        bufferResult.release();
        eventList.release();

        return new ScenarioResult(result, duration);
    }

    private static void fillBuffer(IntBuffer buffer, int value) {
        buffer.clear();
        buffer.put(value);
        buffer.rewind();
    }

}
