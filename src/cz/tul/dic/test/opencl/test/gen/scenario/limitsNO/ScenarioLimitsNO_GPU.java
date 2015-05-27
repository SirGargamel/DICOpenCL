package cz.tul.dic.test.opencl.test.gen.scenario.limitsNO;

import com.jogamp.opencl.CLBuffer;
import cz.tul.dic.test.opencl.test.gen.scenario.fulldata.*;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult;
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
