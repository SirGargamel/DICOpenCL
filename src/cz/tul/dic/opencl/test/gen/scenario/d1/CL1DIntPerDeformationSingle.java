package cz.tul.dic.opencl.test.gen.scenario.d1;

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
public class CL1DIntPerDeformationSingle extends Scenario1D {

    private static final String KERNEL_NAME = "CL1DIntPerDeformationSingle";

    public CL1DIntPerDeformationSingle(final ContextHandler contextHandler) throws IOException {
        super(KERNEL_NAME, contextHandler);
    }

    @Override
    ScenarioResult computeScenario(int[] imageA, int[] imageB, int[] facetData, int[] facetCenters, float[] deformations, ParameterSet params) throws CLException {
        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;
        final float[] result = new float[facetCount * deformationCount];
        // prepare buffers
        final CLContext context = contextHandler.getContext();
        final CLBuffer<IntBuffer> bufferImageA = context.createIntBuffer(imageA.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferImageB = context.createIntBuffer(imageB.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetData = context.createIntBuffer(facetData.length, READ_ONLY);
        final CLBuffer<IntBuffer> bufferFacetCenters = context.createIntBuffer(facetCenters.length, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformation = context.createFloatBuffer(Utils.DEFORMATION_DIM, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = context.createFloatBuffer(facetCount, WRITE_ONLY);
        long clSize = bufferImageA.getCLSize() + bufferImageB.getCLSize() + bufferFacetData.getCLSize() + bufferDeformation.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // fill constant buffers
        fillBuffer(bufferImageA.getBuffer(), imageA);
        fillBuffer(bufferImageB.getBuffer(), imageB);
        fillBuffer(bufferFacetData.getBuffer(), facetData);
        fillBuffer(bufferFacetCenters.getBuffer(), facetCenters);
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(bufferImageA, bufferImageB, bufferFacetData, bufferFacetCenters, bufferDeformation, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(facetCount)
                .putArg(facetSize)
                .rewind();
        // prepare work sizes
        final int lws0 = getLWS0();
        final int facetGlobalWorkSize = roundUp(lws0, facetCount);
        params.addParameter(Parameter.LWS0, lws0);
        // execute kernel        
        long duration = 0;
        float[] oneResult;

        CLEventList eventList = new CLEventList(deformationCount);

        final CLCommandQueue queue = contextHandler.getDevice().createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);

        queue.putWriteBuffer(bufferImageA, false);
        queue.putWriteBuffer(bufferImageB, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);

        // obalit forem
        for (int i = 0; i < deformationCount; i++) {
            // plnit buffer spravnou deformaci
            fillBuffer(bufferDeformation.getBuffer(), deformations, i * Utils.DEFORMATION_DIM, Utils.DEFORMATION_DIM);
            // write new data and run kernel
            queue.putWriteBuffer(bufferDeformation, false);

            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
            queue.putReadBuffer(bufferResult, true);
            oneResult = readBuffer(bufferResult.getBuffer());
            // store result                
            for (int r = 0; r < facetCount; r++) {
                result[(r * deformationCount) + i] = oneResult[r];
            }

            duration += eventList.getEvent(i).getProfilingInfo(CLEvent.ProfilingCommand.END) - eventList.getEvent(i).getProfilingInfo(CLEvent.ProfilingCommand.START);
        }
        queue.finish();

        // data cleanup
        bufferImageA.release();
        bufferImageB.release();
        bufferFacetData.release();
        bufferFacetCenters.release();
        bufferDeformation.release();
        bufferResult.release();
        eventList.release();

        return new ScenarioResult(result, duration);
    }

    private static void fillBuffer(FloatBuffer buffer, float[] data, int offset, int length) {
        buffer.clear();
        for (int i = offset; i < offset + length; i++) {
            buffer.put(data[i]);
        }
        buffer.rewind();
    }
}
