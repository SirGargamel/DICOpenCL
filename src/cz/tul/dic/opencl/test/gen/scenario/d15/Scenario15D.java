package cz.tul.dic.opencl.test.gen.scenario.d15;

import com.jogamp.opencl.CLException;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioOpenCL;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario15D extends ScenarioOpenCL {

    private final int maxVariantCount;
    private int currentVariant;

    public Scenario15D(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        currentVariant = 0;
        maxVariantCount = CustomMath.power2(contextHandler.getDevice().getMaxWorkGroupSize()) + 1;
    }

    @Override
    public float[] prepareAndCompute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params) {
        float[] result = null;
        try {
            result = computeScenario(imageA, imageB, facetData, facetCenters, deformations, params);
        } catch (CLException ex) {
            throw ex;
        } finally {
            currentVariant++;
        }
        return result;
    }

    protected abstract float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
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
