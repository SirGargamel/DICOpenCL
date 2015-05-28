package cz.tul.dic.test.opencl.scenario.limitsNO;

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
public abstract class ScenarioLimitsNO_GPU extends Scenario {

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final CLBuffer<IntBuffer> bufferFacetData, final CLBuffer<FloatBuffer> bufferFacetCenters,
            final CLBuffer<FloatBuffer> bufferDeformations,
            final ParameterSet params);

}
