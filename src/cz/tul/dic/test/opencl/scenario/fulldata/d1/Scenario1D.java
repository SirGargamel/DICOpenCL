package cz.tul.dic.test.opencl.scenario.fulldata.d1;

import cz.tul.dic.test.opencl.scenario.ContextHandler;
import cz.tul.dic.test.opencl.utils.CustomMath;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.scenario.fulldata.ScenarioOpenCL;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario1D extends ScenarioOpenCL {

    private final int maxVariantCount;
    private int currentVariant;

    public Scenario1D(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        currentVariant = 0;
        maxVariantCount = CustomMath.power2(contextHandler.getDevice().getMaxWorkGroupSize()) + 1;
    }

    @Override
    public float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params) {
        float[] result = null;
        try {
            result = computeScenario(imageA, imageB, facetData, facetCenters, deformations, params);
        } catch (Exception | Error ex) {
            throw ex;
        } finally {
            currentVariant++;
        }

        return result;
    }

    protected abstract float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

    protected int getLWS0() {
        return (int) Math.pow(2, currentVariant);
    }

    @Override
    public boolean hasNext() {
        return currentVariant < maxVariantCount;
    }

    @Override
    public void resetInner() {
        currentVariant = 0;
    }

    @Override
    public int getVariantCount() {
        return maxVariantCount;
    }

}
