package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.data.RandomCase;
import cz.tul.dic.opencl.test.gen.data.TestCase;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DImageGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DIntGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.ComputeJavaThreads;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.File;
import java.io.IOException;
import static java.lang.System.nanoTime;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final int IMAGE_WIDTH_MIN = 128;
    private static final int IMAGE_WIDTH_MAX = 512;
    private static final int IMAGE_HEIGHT_MIN = IMAGE_WIDTH_MIN * 3 / 4;
    private static final int[] FACET_SIZES = new int[]{9, 17};
    private static final int DEFORMATION_COUNT_MIN = 200;
    private static final int DEFORMATION_COUNT_MAX = 400;

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

        final TestCase tc = new RandomCase();

        int[][] images;
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
                images = tc.generateImages(w, h);
                average = calculateAverage(images[0]);

                for (int sz = 0; sz < FACET_SIZES.length; sz++) {
                    s = FACET_SIZES[sz];

                    facetCenters = tc.generateFacetCenters(w, h, s);
                    facetData = tc.generateFacetData(facetCenters, s);

                    for (int d = DEFORMATION_COUNT_MIN; d <= DEFORMATION_COUNT_MAX; d *= 2) {
                        deformations = tc.generateDeformations(d);

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
                                result = sc.compute(images[0], average, images[0], average, facetData, facetCenters, deformations, ps);
                                time = nanoTime() - time;
                                result.setTotalTime(time);

                                tc.checkResult(result, ps.getValue(Parameter.FACET_COUNT));

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

        scenarios.add(new ComputeJavaThreads());
        scenarios.add(new Compute2DIntGpuDirect(contextHandler));
        scenarios.add(new Compute2DImageGpuDirect(contextHandler));

        return scenarios;
    }

    private static float calculateAverage(final int[] image) {
        float sum = 0;

        for (int i = 0; i < image.length; i++) {
            sum += image[i];
        }
        sum /= (float) image.length;

        return sum;
    }

}
