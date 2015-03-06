package cz.tul.dic.opencl.test.gen.scenario.fulldata;

import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;

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
