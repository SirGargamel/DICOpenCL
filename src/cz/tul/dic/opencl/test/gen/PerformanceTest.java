package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.data.ShiftedImageCase;
import cz.tul.dic.opencl.test.gen.data.TestCase;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DImageGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DIntGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DNaive;
import cz.tul.dic.opencl.test.gen.scenario.ComputeJavaThreads;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.File;
import java.io.IOException;
import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final int IMAGE_WIDTH_MIN = 128;
    private static final int IMAGE_WIDTH_MAX = 512;
    private static final double IMAGE_RATIO = 3 / 4;
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

        final List<TestCase> testCases = prepareTestCases();

        int lineCount = 1;
        lineCount *= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN;
        lineCount *= FACET_SIZES.length;
        lineCount *= CustomMath.power2(DEFORMATION_COUNT_MAX / DEFORMATION_COUNT_MIN) + 1;
        DataStorage.setCounts(lineCount, testCases.size());

        int[][] images;
        int[] facetData, facetCenters;
        float[] deformations;
        long time;
        ParameterSet ps;
        ScenarioResult result;
        Scenario sc;
        TestCase tc;
        int s, h;
        try {
            // execute scenarios
            for (int tci = 0; tci < testCases.size(); tci++) {
                tc = testCases.get(tci);

                for (int w = IMAGE_WIDTH_MIN; w <= IMAGE_WIDTH_MAX; w *= 2) {
                    h = (int) Math.round(w * IMAGE_RATIO);
                    images = tc.generateImages(w, h);

                    for (int sz = 0; sz < FACET_SIZES.length; sz++) {
                        s = FACET_SIZES[sz];

                        facetCenters = tc.generateFacetCenters(w, h, s);
                        facetData = tc.generateFacetData(facetCenters, s);

                        for (int d = DEFORMATION_COUNT_MIN; d <= DEFORMATION_COUNT_MAX; d *= 2) {
                            deformations = tc.generateDeformations(d);

                            for (int sci = 0; sci < scenarios.size(); sci++) {
                                sc = scenarios.get(sci);
                                sc.reset();
                                while (sc.hasNext()) {
                                    ps = new ParameterSet();
                                    ps.addParameter(Parameter.IMAGE_WIDTH, w);
                                    ps.addParameter(Parameter.IMAGE_HEIGHT, h);
                                    ps.addParameter(Parameter.FACET_SIZE, s);
                                    ps.addParameter(Parameter.FACET_COUNT, facetData.length / Utils.calculateFacetArraySize(s));
                                    ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                    ps.addParameter(Parameter.VARIANT, sci);
                                    ps.addParameter(Parameter.TEST_CASE, tci);

                                    time = nanoTime();
                                    result = sc.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
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

    private static List<TestCase> prepareTestCases() {
        List<TestCase> result = new ArrayList<>(2);

        result.add(new TestCase());
        result.add(new ShiftedImageCase(5, 2));

        return result;
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

        scenarios.add(new ComputeJavaThreads());
        scenarios.add(new Compute2DNaive(contextHandler));
        scenarios.add(new Compute2DIntGpuDirect(contextHandler));
        scenarios.add(new Compute2DImageGpuDirect(contextHandler));

        return scenarios;
    }

}
