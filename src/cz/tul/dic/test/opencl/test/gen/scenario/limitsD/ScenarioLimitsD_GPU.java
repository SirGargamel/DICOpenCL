package cz.tul.dic.test.opencl.test.gen.scenario.limitsD;

import com.jogamp.opencl.CLBuffer;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.test.opencl.test.gen.scenario.fulldata.Scenario;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ScenarioLimitsD_GPU extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final CLBuffer<IntBuffer> bufferFacetData, final CLBuffer<FloatBuffer> bufferFacetCenters,
            final float[] deformationLimits, final int[] deformationCounts,
            final ParameterSet params);

}
