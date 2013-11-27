package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario2D extends Scenario {

    private static final int MIN_WORK = 16;
    private final int maxVariant;
    private final int lws0count, lws1count, maxWorkSize;
    private int currentVariant;

    public Scenario2D(final String scenarioName, final ContextHandler contextHandler) throws IOException {
        super(scenarioName, contextHandler);

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
    public ScenarioResult compute(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
            final int[] facets, final float[] deformations,
            final ParameterSet params) {
        final ScenarioResult result = computeScenario(imageA, imageAavg, imageB, imageBavg, facets, deformations, params);
        prepareNextVariant();

        return result;
    }

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
    public void resetInner() {
        currentVariant = 0;
        prepareNextVariant();
    }

    @Override
    public int getVariantCount() {
        int count = 0;
        final int lws0 = getLWS0();
        final int lws1 = getLWS1();
        
        int variant = 0;
        int size;
        while (variant < maxVariant) {            
            size = lws0 * lws1;
            if ((size <= maxWorkSize) || (size >= MIN_WORK)) {
                count++;
            }
            variant++;
        }

        return count;
    }

}
