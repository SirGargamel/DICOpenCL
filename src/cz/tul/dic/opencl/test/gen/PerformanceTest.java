package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.data.ShiftedImageCase;
import cz.tul.dic.opencl.test.gen.data.TestCase;
import cz.tul.dic.opencl.test.gen.scenario.ComputeJavaPerDeformation;
import cz.tul.dic.opencl.test.gen.scenario.ComputeJavaPerFacet;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.d1.Compute1DIntPerDeformationSingle;
import cz.tul.dic.opencl.test.gen.scenario.d1.Compute1DIntPerFacetSingle;
import cz.tul.dic.opencl.test.gen.scenario.d15.Compute15DIntPerDeformation;
import cz.tul.dic.opencl.test.gen.scenario.d15.Compute15DIntPerFacet;
import cz.tul.dic.opencl.test.gen.scenario.d2.Compute2DImageGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.d2.Compute2DInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {
//  static data
    private static final double IMAGE_RATIO = 3 / (double) 4;
    private static final int IMAGE_WIDTH_MIN = 128;
    private static final int DEFORMATION_COUNT_MIN = 200;
//  Devices for computation
//    private static final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU};
//    private static final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.CPU, ContextHandler.DeviceType.GPU};
    private static final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU, ContextHandler.DeviceType.iGPU, ContextHandler.DeviceType.CPU};
//  Large task    
//    private static final int IMAGE_WIDTH_MAX = 1024;    
//    private static final int[] FACET_SIZES = new int[]{9, 17, 35};
//    private static final int DEFORMATION_COUNT_MAX = 800;
//  Small task
    private static final int IMAGE_WIDTH_MAX = 128;
    private static final int[] FACET_SIZES = new int[]{9};
    private static final int DEFORMATION_COUNT_MAX = 200;

    public static void computeImageFillTest() throws IOException {
        for (ContextHandler.DeviceType device : HW) {
            CLPlatform.initialize();
            final ContextHandler ch = new ContextHandler(device);

            final List<Scenario> scenarios = prepareScenarios(ch);
            for (Scenario sc : scenarios) {
                DataStorage.addVariantCount(sc.getVariantCount());
            }

            final List<TestCase> testCases = prepareTestCases();

            int lineCount = 1;
            lineCount *= CustomMath.power2(IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN) + 1;
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

                                        time = System.nanoTime();
                                        result = sc.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
                                        time = System.nanoTime() - time;
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

            String fileName = "D:\\DIC_OpenCL_Data_" + ch.getDeviceName() + ".csv";
            DataStorage.exportData(new File(fileName));
            DataStorage.exportResultGroups(new File("D:\\DIC_OpenCL_Results_" + ch.getDeviceName() + ".csv"));
        }
    }

    private static List<TestCase> prepareTestCases() {
        List<TestCase> result = new ArrayList<>(2);

        result.add(new TestCase());
        result.add(new ShiftedImageCase(5, 2));

        return result;
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

        scenarios.add(new ComputeJavaPerFacet());
        scenarios.add(new ComputeJavaPerDeformation());
        scenarios.add(new Compute1DIntPerFacetSingle(contextHandler));
        scenarios.add(new Compute1DIntPerDeformationSingle(contextHandler));
        scenarios.add(new Compute15DIntPerFacet(contextHandler));
        scenarios.add(new Compute15DIntPerDeformation(contextHandler));
        scenarios.add(new Compute2DInt("Compute2DNaive", contextHandler));
        scenarios.add(new Compute2DInt("Compute2DIntGpuDirect", contextHandler));
        scenarios.add(new Compute2DImageGpuDirect(contextHandler));

        return scenarios;
    }

}
