package cz.tul.dic.opencl.test.gen.scenario.driven;

import com.jogamp.common.nio.Buffers;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.CLKernelBinding;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.WorkSizeManager;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioDrivenOpenCL;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public final class CL1D_I_V_LL_D extends ScenarioDrivenOpenCL {

    private static final int ARGUMENT_INDEX = 12;
    private int lws0base;
    private boolean inited;

    public CL1D_I_V_LL_D(final ContextHandler contextHandler, final WorkSizeManager wsm) throws IOException {
        super(contextHandler, wsm);

        inited = false;
    }

    @Override
    public void prepare(ParameterSet ps) {
        super.prepare(ps);

        if (!inited) {
            contextHandler.assignScenario(this);

            final IntBuffer val = Buffers.newDirectIntBuffer(2);
            final CLContext context = contextHandler.getContext();
            final CL cl = context.getCL();
            cl.clGetKernelWorkGroupInfo(contextHandler.getKernel().getID(), contextHandler.getDevice().getID(), CLKernelBinding.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, Integer.SIZE, val, null);
            lws0base = val.get(0);

            inited = true;
        }
    }

    @Override
    public float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        // prepare buffers
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLImage2d<IntBuffer> imageAcl = createImage(imageA, params.getValue(Parameter.IMAGE_WIDTH));
        final CLImage2d<IntBuffer> imageBcl = createImage(imageB, params.getValue(Parameter.IMAGE_WIDTH));
        final CLBuffer<IntBuffer> bufferFacetData = createIntBuffer(facetData, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferFacetCenters = createFloatBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferDeformations = createFloatBuffer(deformations, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        final long clSize = imageAcl.getCLSize() + imageBcl.getCLSize() + bufferFacetData.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare work sizes      
        final int facetSubCount = getFacetCount();        
        final int deformationCount = params.getValue(Parameter.DEFORMATION_COUNT);
        final int facetArea = Utils.calculateFacetArea(params.getValue(Parameter.FACET_SIZE));
        final int lws0;
        if (deformationCount > facetArea) {
            lws0 = roundUp(lws0base, facetArea);
        } else {
            lws0 = roundUp(lws0base, deformationCount);
        }
        final int facetGlobalWorkSize = roundUp(lws0, deformationCount) * facetSubCount;
        params.addParameter(Parameter.LWS0, lws0);
        int groupCountPerFacet = deformationCount / lws0;
        if (deformationCount % lws0 > 0) {
            groupCountPerFacet++;
        }
        // prepare kernel arguments        
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetData, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(deformationCount)
                .putArg(params.getValue(Parameter.FACET_SIZE))
                .putArg(facetCount)
                .putArg(groupCountPerFacet)
                .putArg(facetSubCount)
                .putArg(0)
                .rewind();
        // execute kernel 
        final int roundCount = (int) Math.ceil(facetCount / (double) facetSubCount);
        prepareEventList(roundCount);
        final CLCommandQueue queue = createCommandQueue();
        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
        queue.putWriteBuffer(bufferFacetData, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.putWriteBuffer(bufferDeformations, false);
        for (int i = 0; i < roundCount; i++) {
            kernel.setArg(ARGUMENT_INDEX, i * facetSubCount);
            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
        }
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        // create result
        final float[] result = readBuffer(bufferResult.getBuffer());
        return result;
    }

}
