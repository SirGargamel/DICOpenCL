package cz.tul.dic.test.opencl.scenario.limitsF;

import com.jogamp.opencl.CLBuffer;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;
import cz.tul.dic.test.opencl.scenario.fulldata.Scenario;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimitsF_GPU extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final float[] facetCenters,
            final CLBuffer<FloatBuffer> bufferDeformations,
            final ParameterSet params);

}
