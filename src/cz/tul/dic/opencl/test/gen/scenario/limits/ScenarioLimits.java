package cz.tul.dic.opencl.test.gen.scenario.limits;

import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.Scenario;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimits extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final float[] deformationLimits,
            final ParameterSet params);

}
