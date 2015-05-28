package cz.tul.dic.test.opencl.scenario.limitsFD;

import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;
import cz.tul.dic.test.opencl.scenario.fulldata.Scenario;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimitsFD extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final float[] deformationLimits, final int[] deformationCounts,
            final ParameterSet params);

}
