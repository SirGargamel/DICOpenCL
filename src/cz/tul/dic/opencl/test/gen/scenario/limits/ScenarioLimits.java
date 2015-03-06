package cz.tul.dic.opencl.test.gen.scenario.limits;

import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimits {
    
    public void prepare(final ParameterSet params) {
        // nothing to prepare here
    }

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final float[] deformationLimits,
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
