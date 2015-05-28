package cz.tul.dic.test.opencl.scenario.fulldata;

import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioFullData extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final float[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

}
