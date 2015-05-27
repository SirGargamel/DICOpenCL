package cz.tul.dic.test.opencl.test.gen.scenario.limitsF;

import com.jogamp.common.nio.Buffers;
import cz.tul.dic.test.opencl.test.gen.ContextHandler;
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
import cz.tul.dic.test.opencl.test.gen.CustomMath;
import cz.tul.dic.test.opencl.test.gen.Parameter;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public final class CL_LF_1DImageLL_GPU extends ScenarioOpenCL_LF_GPU {

    private int maxVariantCount, lws0base, lws0base2;
    private int currentVariant;
    private boolean inited;

    public CL_LF_1DImageLL_GPU(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        inited = false;

        resetInner();
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
            lws0base2 = CustomMath.power2(lws0base);

            final int max = contextHandler.getDevice().getMaxWorkGroupSize();
            maxVariantCount = CustomMath.power2(max / lws0base) + 1;

            inited = true;
        }
    }

    @Override
    public float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final CLBuffer<FloatBuffer> bufferDeformations,
            final ParameterSet params) {
        float[] result = null;
        try {
            result = computeScenario(imageA, imageB, facetCenters, bufferDeformations, params);
        } catch (CLException ex) {
            throw ex;
        } finally {
            currentVariant++;
        }

        return result;
    }

    float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final CLBuffer<FloatBuffer> bufferDeformations,
            final ParameterSet params) throws CLException {
        final int lws0 = (int) Math.pow(2, currentVariant + lws0base2);
        params.addParameter(Parameter.LWS0, lws0);
        params.addParameter(Parameter.LWS1, 1);
        // prepare buffers
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLImage2d<IntBuffer> imageAcl = createImage(imageA, params.getValue(Parameter.IMAGE_WIDTH));
        final CLImage2d<IntBuffer> imageBcl = createImage(imageB, params.getValue(Parameter.IMAGE_WIDTH));
        final CLBuffer<FloatBuffer> bufferFacetCenters = createFloatBuffer(facetCenters, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(facetCount * params.getValue(Parameter.DEFORMATION_COUNT), WRITE_ONLY);
        final long clSize = imageAcl.getCLSize() + imageBcl.getCLSize() + bufferFacetCenters.getCLSize() + bufferDeformations.getCLSize() + bufferResult.getCLSize();
        params.addParameter(Parameter.DATASIZE, (int) (clSize / 1000));
        // prepare work sizes        
        final int deformationCount = params.getValue(Parameter.DEFORMATION_COUNT);

        final int facetGlobalWorkSize = roundUp(lws0, deformationCount) * facetCount;
        int groupCountPerFacet = deformationCount / lws0;
        if (deformationCount % lws0 > 0) {
            groupCountPerFacet++;
        }
        // prepare kernel arguments
        final CLKernel kernel = contextHandler.getKernel();
        kernel.putArgs(imageAcl, imageBcl, bufferFacetCenters, bufferDeformations, bufferResult)
                .putArg(params.getValue(Parameter.IMAGE_WIDTH))
                .putArg(deformationCount)
                .putArg(params.getValue(Parameter.FACET_SIZE))
                .putArg(facetCount)
                .putArg(groupCountPerFacet)
                .rewind();
        // execute kernel         
        prepareEventList(1);
        final CLCommandQueue queue = createCommandQueue();
        queue.putWriteImage(imageAcl, false);
        queue.putWriteImage(imageBcl, false);
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);
        queue.putReadBuffer(bufferResult, true);
        queue.finish();
        // create result
        final float[] result = readBuffer(bufferResult.getBuffer());
        return result;
    }

    @Override
    public boolean hasNext() {
        return currentVariant < maxVariantCount;
    }

    @Override
    protected void resetInner() {
        currentVariant = 0;
        maxVariantCount = 1;
        inited = false;
    }

    @Override
    public int getVariantCount() {
        return maxVariantCount;
    }

}
