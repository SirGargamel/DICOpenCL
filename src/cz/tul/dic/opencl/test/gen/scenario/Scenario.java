package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.ParameterSet;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario {
    
    public void prepare(final ParameterSet params) {
        // nothing to prepare here
    }

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

    public abstract boolean hasNext();

    public void reset() {
        resetInner();
    }

    protected abstract void resetInner();

    public String getKernelName() {
        return getClass().getSimpleName();
    }

    public abstract int getVariantCount();

}
