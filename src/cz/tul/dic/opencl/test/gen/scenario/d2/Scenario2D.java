package cz.tul.dic.opencl.test.gen.scenario.d2;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioOpenCL;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario2D extends ScenarioOpenCL {

    private static final int MIN_WORK = 16;
    private final int maxVariant;
    private final int lws0count, lws1count, maxWorkSize;
    private int currentVariant;

    public Scenario2D(final ContextHandler contextHandler) throws IOException {
        super(contextHandler);

        currentVariant = 0;

        maxWorkSize = contextHandler.getDevice().getMaxWorkGroupSize();
        lws0count = Math.min(
                CustomMath.power2(64) + 1,
                CustomMath.power2(maxWorkSize / 4) + 1);
        lws1count = CustomMath.power2(contextHandler.getDevice().getMaxWorkGroupSize()) + 1;
        maxVariant = lws1count * lws0count;

        prepareNextVariant();
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
        } catch (Exception ex) {
            throw ex;
        } finally {
            prepareNextVariant();
        }
        return result;
    }

    protected abstract float[] computeScenario(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

    private void prepareNextVariant() {
        boolean inc = true;
        int lws0, lws1, size;
        while (inc && currentVariant < maxVariant) {
            currentVariant++;
            lws0 = getLWS0();
            lws1 = getLWS1();
            size = lws0 * lws1;
            inc = (size > maxWorkSize) || (size < MIN_WORK);
        }
    }

    protected int getLWS0() {
        return (int) Math.pow(2, currentVariant % lws0count);
    }

    protected int getLWS1() {
        return (int) (Math.pow(2, currentVariant / lws0count));
    }

    @Override
    public boolean hasNext() {
        return currentVariant < maxVariant;
    }

    @Override
    protected void resetInner() {
        currentVariant = 0;
        prepareNextVariant();
    }

    @Override
    public int getVariantCount() {
        final int oldCurrentVariant = currentVariant;
        currentVariant = 0;

        int count = -1;
        while (hasNext()) {
            count++;
            prepareNextVariant();
        }

        currentVariant = oldCurrentVariant;

        return count;
    }

}
