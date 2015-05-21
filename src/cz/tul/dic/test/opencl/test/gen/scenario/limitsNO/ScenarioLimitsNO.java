package cz.tul.dic.test.opencl.test.gen.scenario.limitsNO;

import cz.tul.dic.test.opencl.test.gen.scenario.fulldata.*;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimitsNO extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

}
