package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DImageGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DIntGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.File;
import java.io.IOException;
import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final int IMAGE_WIDTH_MIN = 128;
    private static final int IMAGE_WIDTH_MAX = 1024;
    private static final int IMAGE_HEIGHT_MIN = IMAGE_WIDTH_MIN * 3 / 4;
    private static final int FACET_SIZE_MIN = 10;
    private static final int FACET_SIZE_MAX = 40;
    private static final int DEFORMATION_COUNT_MIN = 200;
    private static final int DEFORMATION_COUNT_MAX = 800;
    private static final int DEFORMATION_ABS_MAX = 5;
    private static final float EPS = 0.0001f;

    public static void computeImageFillTest() throws IOException {
        CLPlatform.initialize();
        final ContextHandler ch = new ContextHandler();

        final List<Scenario> scenarios = prepareScenarios(ch);
        for (Scenario sc : scenarios) {
            DataStorage.addVariantCount(sc.getVariantCount());
        }

        int lineCount = 1;
        lineCount *= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN;
        lineCount *= CustomMath.power2(FACET_SIZE_MAX / FACET_SIZE_MIN) + 1;
        lineCount *= CustomMath.power2(DEFORMATION_COUNT_MAX / DEFORMATION_COUNT_MIN) + 1;
        DataStorage.setLineCount(lineCount);

        int[] image;
        float average;
        int[] facets;
        float[] deformations;
        long time;
        ParameterSet ps;
        ScenarioResult result;
        Scenario sc;
        try {
            // execute scenarios
            int w, h;
            for (int dim = 1; dim <= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN; dim++) {
                w = dim * IMAGE_WIDTH_MIN;
                h = dim * IMAGE_HEIGHT_MIN;
                image = generateImage(w, h);
                average = calculateAverage(image);

                for (int s = FACET_SIZE_MIN; s <= FACET_SIZE_MAX; s *= 2) {
                    facets = generateFacets(w, h, s);

                    for (int d = DEFORMATION_COUNT_MIN; d <= DEFORMATION_COUNT_MAX; d *= 2) {
                        deformations = generateDeformations(d);

                        for (int i = 0; i < scenarios.size(); i++) {
                            sc = scenarios.get(i);
                            sc.reset();
                            while (sc.hasNext()) {
                                ps = new ParameterSet();
                                ps.addParameter(Parameter.IMAGE_WIDTH, w);
                                ps.addParameter(Parameter.IMAGE_HEIGHT, h);
                                ps.addParameter(Parameter.FACET_SIZE, s);
                                ps.addParameter(Parameter.FACET_COUNT, facets.length / (s * s * 2));
                                ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                ps.addParameter(Parameter.VARIANT, i);

                                time = nanoTime();
                                result = sc.compute(image, average, image, average, facets, deformations, ps);
                                time = nanoTime() - time;
                                result.setTotalTime(time);

                                checkResult(result, ps.getValue(Parameter.FACET_COUNT));

                                switch (result.getState()) {
                                    case SUCCESS:
                                        System.out.println("Finished " + sc.getDescription() + " " + (time / 1000000) + "ms (" + (result.getKernelExecutionTime() / 1000000) + " ms in kernel) with params " + ps);
                                        break;
                                    case WRONG_RESULT_DYNAMIC:
                                        System.out.println("Wrong dynamic part of result for  " + sc.getDescription() + " " + (time / 1000000) + "ms (" + (result.getKernelExecutionTime() / 1000000) + " ms in kernel) with params " + ps);
                                        break;
                                    case WRONG_RESULT_FIXED:
                                        System.out.println("Wrong fixed part of result for  " + sc.getDescription() + " " + (time / 1000000) + "ms (" + (result.getKernelExecutionTime() / 1000000) + " ms in kernel) with params " + ps);
                                        break;
                                    case FAIL:
                                        System.out.println("Failed " + sc.getDescription() + " with params " + ps);
                                        ch.reset();
                                }

                                DataStorage.storeData(ps, result);
                            }
                        }
                    }
                }
            }
        } catch (Exception | Error ex) {
            ex.printStackTrace(System.err);
        } finally {
            // cleanup all resources associated with this context.
            CLContext context = ch.getContext();
            if (!context.isReleased()) {
                context.release();
            }
        }

        DataStorage.exportData(new File("D:\\openCL_DIC.csv"));
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new ArrayList<>(1);

        scenarios.add(new Compute2DIntGpuDirect(contextHandler));
        scenarios.add(new Compute2DImageGpuDirect(contextHandler));

        return scenarios;
    }

    private static int[] generateImage(final int width, final int height) {
        final int length = width * height;
        final int[] result = new int[length];

        Random rnd = new Random();
        for (int j = 0; j < length; j++) {
            result[j] = rnd.nextInt(256);
        }

        return result;
    }

    private static float calculateAverage(final int[] image) {
        float sum = 0;

        for (int i = 0; i < image.length; i++) {
            sum += image[i];
        }
        sum /= (float) image.length;

        return sum;
    }

    private static int[] generateFacets(final int width, final int height, final int size) {
        final int count = (width / size) * (height / size);
        final int facetCordSize = size * size * 2;
        final int[] result = new int[count * facetCordSize];

        Random rnd = new Random();
        int x, y, divX, divY;
        int baseX, baseY, base;
        int offset = DEFORMATION_ABS_MAX;
        for (int i = 0; i < count; i++) {
            base = i * facetCordSize;
            // generate baseX and baseY of facet       
            baseX = rnd.nextInt(width - (2 * offset) - size) + offset;
            baseY = rnd.nextInt(height - (2 * offset) - size) + offset;
            // generate points
            for (int dy = 0; dy < size; dy++) {
                y = baseY + dy;
                divY = dy * size * 2;
                for (int dx = 0; dx < size; dx++) {
                    x = baseX + dx;
                    divX = dx * 2;
                    result[base + divY + divX] = x;
                    result[base + divY + divX + 1] = y;
                }
            }
        }

        return result;
    }

    private static float[] generateDeformations(final int deformationCount) {
        final float[] deformations = new float[deformationCount * 2];

        Random rnd = new Random();
        int val;
        for (int i = 0; i < deformationCount; i++) {
            val = rnd.nextInt(DEFORMATION_ABS_MAX) - (DEFORMATION_ABS_MAX / 2);
            if (val == 0) {
                val++;
            }
            deformations[i * 2] = val;
            val = rnd.nextInt(DEFORMATION_ABS_MAX) - (DEFORMATION_ABS_MAX / 2);
            if (val == 0) {
                val++;
            }
            deformations[(i * 2) + 1] = val;
        }

        // known results
        deformations[0] = 0;
        deformations[1] = 0;

        deformations[deformations.length - 2] = 0;
        deformations[deformations.length - 1] = 0;

        return deformations;
    }

    private static void checkResult(final ScenarioResult result, final int facetCount) {
        final float[] coeffs = result.getResultData();

        if (coeffs == null
                || !areEqual(coeffs[0], 1, EPS)
                || !areEqual(coeffs[coeffs.length - 1], 1, EPS)) {
            result.markResultAsInvalidFixed();
        } else {
            int oneCount = 0;
            for (int i = 0; i < coeffs.length; i++) {
                if (areEqual(coeffs[i], 1, EPS)) {
                    oneCount++;
                }
            }
            if (oneCount != (facetCount * 2)) {
                result.markResultAsInvalidFixed();
            }
        }
    }

    private static boolean areEqual(final float a, final float b, final float eps) {
        final float dif = Math.abs(a - b);
        return dif <= eps;
    }

}
