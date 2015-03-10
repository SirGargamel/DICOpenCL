package cz.tul.dic.opencl.test.gen.scenario.limitsD;

import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.Scenario;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimitsD extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformationLimits, final int[] deformationCounts,
            final ParameterSet params);

}
