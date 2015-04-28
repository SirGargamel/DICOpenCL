package cz.tul.dic.test.opencl.test.gen.scenario.limitsFD;

import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.test.opencl.test.gen.scenario.fulldata.Scenario;

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
