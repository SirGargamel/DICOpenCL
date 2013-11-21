package cz.tul.dic.opencl.test.gen.scenario;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;
import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario {

    private static final String CL_EXTENSION = ".cl";
    protected final CLKernel kernel;
    protected final CLContext context;
    private final int maxVariantCount;
    private int max;
    private int currentVariant;
    private WorkSize currentWorkSize;

    public Scenario(final String scenarioName, final CLDevice device) throws IOException {
        context = device.getContext();
        final CLProgram program = context.createProgram(Scenario.class.getResourceAsStream(scenarioName.concat(CL_EXTENSION))).build();
        kernel = program.createCLKernel(scenarioName);

        currentVariant = 0;
        maxVariantCount = CustomMath.power2(device.getMaxWorkGroupSize()) + 1;
        max = maxVariantCount;
        currentWorkSize = WorkSize.FULL;
    }

    public float[] compute(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
            final int[] facets, final int[] deformations,
            final ParameterSet params, final CLDevice queue) {
        float[] result = computeScenario(imageA, imageAavg, imageB, imageBavg, facets, deformations, params, queue);        
        currentVariant++;
        if (currentVariant == max) {
            switch (currentWorkSize) {
                case FULL:
                    currentWorkSize = WorkSize.HALF;
                    max--;
                    currentVariant = 0;
                    break;
                case HALF:
                    currentWorkSize = WorkSize.QUARTER;
                    max--;
                    currentVariant = 0;
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    abstract float[] computeScenario(
            final int[] imageA, final float imageAavg,
            final int[] imageB, final float imageBavg,
            final int[] facets, final int[] deformations,
            final ParameterSet params, 
            final CLDevice queue);

    public boolean hasNext() {
        return (currentWorkSize != WorkSize.QUARTER) || (currentVariant < max);
    }

    public void reset() {
        currentVariant = 0;
        max = maxVariantCount;
        currentWorkSize = WorkSize.FULL;
    }

    public String getDescription() {
        return kernel.name;
    }

    public int getVariantCount() {
        return maxVariantCount + (maxVariantCount - 1) + (maxVariantCount - 2);
    }    

    protected int getLWS0() {
        return (int) Math.pow(2, currentVariant);
    }

    protected int getLWS1() {
        return (int) (Math.pow(2, max - currentVariant - 1));
    }

    private static enum WorkSize {

        FULL,
        HALF,
        QUARTER
    }

}
