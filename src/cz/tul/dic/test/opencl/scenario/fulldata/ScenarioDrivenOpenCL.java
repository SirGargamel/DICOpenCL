package cz.tul.dic.test.opencl.scenario.fulldata;

import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLException;
import cz.tul.dic.test.opencl.scenario.ContextHandler;
import cz.tul.dic.test.opencl.scenario.WorkSizeManager;
import cz.tul.dic.test.opencl.scenario.Parameter;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioDrivenOpenCL extends ScenarioOpenCL {

    private static final int PUBLIC_VARIANT_COUNT = 1;
    private int currentVariant, maxVariantCount;
    private final WorkSizeManager wsm;

    public ScenarioDrivenOpenCL(ContextHandler contextHandler, final WorkSizeManager wsm) throws IOException {
        super(contextHandler);

        this.wsm = wsm;
    }

    protected final int getFacetCount() {
        return wsm.getWorkSize(this.getClass());
    }

    @Override
    public ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params) throws CLException {
        currentVariant++;
        final int facetCount = getFacetCount();
        params.addParameter(Parameter.LWS_SUB, facetCount);

        float[] result = prepareAndCompute(imageA, imageB, facetData, facetCenters, deformations, params);

        final long totalKernelTime = computeTotalKernelTime();

        long maxDuration = -1;
        long dif;
        for (CLEvent event : eventList) {
            dif = event.getProfilingInfo(CLEvent.ProfilingCommand.END) - event.getProfilingInfo(CLEvent.ProfilingCommand.START);
            maxDuration = dif > maxDuration ? dif : maxDuration;
        }

        wsm.storeTime(this.getClass(), facetCount, maxDuration);
        final int newFacetCount = getFacetCount();
        final int maxFacetCount = params.getValue(Parameter.FACET_COUNT);
        if (facetCount < maxFacetCount && newFacetCount > facetCount) {
            maxVariantCount++;
        }
        if (newFacetCount > maxFacetCount) {
            wsm.forceWorkSize(this.getClass(), maxFacetCount);
        }

        resourceCleanup();

        return new ScenarioResult(result, totalKernelTime);
    }

    @Override
    public boolean hasNext() {
        return currentVariant < maxVariantCount;
    }

    @Override
    protected void resetInner() {
        currentVariant = 0;
        maxVariantCount = 1;
        wsm.reset(this.getClass());
    }

    @Override
    public int getVariantCount() {
        return PUBLIC_VARIANT_COUNT;
    }
    
    @Override
    public boolean isDriven() {
        return true;
    }

}
