package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario1D extends Scenario {

    private final int maxVariantCount;
    private int currentVariant;

    public Scenario1D(final String scenarioName, final ContextHandler contextHandler) throws IOException {
        super(scenarioName, contextHandler);

        currentVariant = 0;
        maxVariantCount = CustomMath.power2(contextHandler.getDevice().getMaxWorkGroupSize()) + 1;
    }

    @Override
    public ScenarioResult compute(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
            final int[] facetData, final int[] facetCenters, 
            final float[] deformations,
            final ParameterSet params) {
        final ScenarioResult result = computeScenario(imageA, imageAavg, imageB, imageBavg, facetData, facetCenters, deformations, params);
        currentVariant++;
        return result;
    }
    
    abstract ScenarioResult computeScenario(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
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
