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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final int IMAGE_WIDTH_MIN = 128;
    private static final int IMAGE_WIDTH_MAX = 256;
    private static final int IMAGE_HEIGHT_MIN = IMAGE_WIDTH_MIN * 3 / 4;
    private static final int[] FACET_SIZES = new int[]{9, 17};
    private static final int DEFORMATION_COUNT_MIN = 200;
    private static final int DEFORMATION_COUNT_MAX = 400;
    private static final float DEFORMATION_ABS_MAX_0 = 5;
    private static final float DEFORMATION_ABS_MAX_1 = 0.1f;    
    

    public static void computeImageFillTest() throws IOException {
        CLPlatform.initialize();
        final ContextHandler ch = new ContextHandler();

        final List<Scenario> scenarios = prepareScenarios(ch);
        for (Scenario sc : scenarios) {
            DataStorage.addVariantCount(sc.getVariantCount());
        }

        int lineCount = 1;
        lineCount *= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN;
        lineCount *= FACET_SIZES.length;
        lineCount *= CustomMath.power2(DEFORMATION_COUNT_MAX / DEFORMATION_COUNT_MIN) + 1;
        DataStorage.setLineCount(lineCount);

        int[] image;
        float average;
        int[] facetData, facetCenters;
        float[] deformations;
        long time;
        ParameterSet ps;
        ScenarioResult result;
        Scenario sc;
        int s;
        try {
            // execute scenarios
            int w, h;
            for (int dim = 1; dim <= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN; dim++) {
                w = dim * IMAGE_WIDTH_MIN;
                h = dim * IMAGE_HEIGHT_MIN;
                image = generateImage(w, h);
                average = calculateAverage(image);

                for (int sz = 0; sz < FACET_SIZES.length; sz++) {
                    s = FACET_SIZES[sz];

                    facetCenters = generateFacetCenters(w, h, s);
                    facetData = generateFacetData(facetCenters, s);

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
                                ps.addParameter(Parameter.FACET_COUNT, facetData.length / Utils.calculateFacetArraySize(s));
                                ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                ps.addParameter(Parameter.VARIANT, i);

                                time = nanoTime();
                                result = sc.compute(image, average, image, average, facetData, facetCenters, deformations, ps);
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

        DataStorage.exportData(new File("D:\\DIC_OpenCL_Data.csv"));
        DataStorage.exportResultGroups(new File("D:\\DIC_OpenCL_Results.csv"));
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

//        scenarios.add(new ComputeJavaThreads());
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

    private static int[] generateFacetCenters(final int width, final int height, final int size) {
        final int count = (width / size) * (height / size);
        final int[] result = new int[count * 2];

        Random rnd = new Random();
        int centerX, centerY, baseIndex;
        int offset = (int) (DEFORMATION_ABS_MAX_0 * 4);
        for (int i = 0; i < count; i++) {
            baseIndex = i * 2;            
            centerX = rnd.nextInt(width - (2 * offset) - size) + offset;
            centerY = rnd.nextInt(height - (2 * offset) - size) + offset;
            result[baseIndex] = centerX;
            result[baseIndex + 1] = centerY;
        }

        return result;
    }

    private static int[] generateFacetData(final int[] facetCenters, final int size) {
        final int facetArraySize = Utils.calculateFacetArraySize(size);
        final int count = facetCenters.length / 2;
        final int[] result = new int[count * facetArraySize];

        int centerX, centerY, baseIndex;
        int halfSize = size / 2;
        int index;
        for (int i = 0; i < count; i++) {
            baseIndex = i * facetArraySize;

            centerX = facetCenters[i * 2];
            centerY = facetCenters[i * 2 + 1];

            // generate points
            index = 0;
            for (int dy = 0; dy < size; dy++) {
                for (int dx = 0; dx < size; dx++) {
                    result[baseIndex + index] = centerX + dx - halfSize;
                    result[baseIndex + index + 1] = centerY + dy - halfSize;
                    index += 2;
                }
            }
        }

        return result;
    }

    private static float[] generateDeformations(final int deformationCount) {
        final float[] deformations = new float[deformationCount * Utils.DEFORMATION_DIM];

        Random rnd = new Random();
        float val;
        int base;
        for (int i = 0; i < deformationCount; i++) {
            base = i * Utils.DEFORMATION_DIM;
            for (int j = 0; j < 2; j++) {
                val = rnd.nextFloat() * DEFORMATION_ABS_MAX_0 - (DEFORMATION_ABS_MAX_0 / 2);
                if (val == 0) {
                    val++;
                }
                deformations[base + j] = val;
            }
            for (int j = 2; j < Utils.DEFORMATION_DIM; j++) {
                val = (rnd.nextFloat() * DEFORMATION_ABS_MAX_1) - (DEFORMATION_ABS_MAX_1 * 0.5f);
                if (val == 0) {
                    val = DEFORMATION_ABS_MAX_1 * 0.01f;
                }
                deformations[base + j] = val;
            }
        }

        // known results
        deformations[0] = 0;
        deformations[1] = 0;
        deformations[2] = 0;
        deformations[3] = 0;
        deformations[4] = 0;
        deformations[5] = 0;

        deformations[deformations.length - 6] = 0;
        deformations[deformations.length - 5] = 0;
        deformations[deformations.length - 4] = 0;
        deformations[deformations.length - 3] = 0;
        deformations[deformations.length - 2] = 0;
        deformations[deformations.length - 1] = 0;

        return deformations;
    }

    private static void checkResult(final ScenarioResult result, final int facetCount) {
        final float[] coeffs = result.getResultData();

        if (coeffs == null) {
            result.markResultAsInvalidFixed();
        } else {
            int oneCount = 0;
            for (int i = 0; i < coeffs.length; i++) {
                if (CustomMath.areEqual(coeffs[i], 1.0f, Utils.EPS_PRECISE)) {
                    oneCount++;
                }
            }
            if (oneCount != (facetCount * 2)) {
                result.markResultAsInvalidFixed();
            }
        }
    }

}
