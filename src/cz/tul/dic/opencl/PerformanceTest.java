package cz.tul.dic.opencl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.DataStorage;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.data.TestCase;
import cz.tul.dic.opencl.test.gen.scenario.java.JavaPerDeformation;
import cz.tul.dic.opencl.test.gen.scenario.java.JavaPerFacet;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.d1.CL1DIntPerDeformationSingle;
import cz.tul.dic.opencl.test.gen.scenario.d1.CL1DIntPerFacetSingle;
import cz.tul.dic.opencl.test.gen.scenario.d1.opt.CL1DImageLpF;
import cz.tul.dic.opencl.test.gen.scenario.d1.opt.CL1DImageLpF_LWS;
import cz.tul.dic.opencl.test.gen.scenario.d15.CL15DIntPerDeformation;
import cz.tul.dic.opencl.test.gen.scenario.d15.CL15DIntPerFacet;
import cz.tul.dic.opencl.test.gen.scenario.d2.CL2DImage;
import cz.tul.dic.opencl.test.gen.scenario.d2.CL2DInt;
import cz.tul.dic.opencl.test.gen.scenario.d2.opt.CL2DImageC;
import cz.tul.dic.opencl.test.gen.scenario.d2.opt.CL2DImageFtoA;
import cz.tul.dic.opencl.test.gen.scenario.d2.opt.CL2DImageMC;
import cz.tul.dic.opencl.test.gen.scenario.d2.opt.CL2DImageV;
import cz.tul.dic.opencl.test.gen.scenario.d2.opt.CL2DImage_MC_V;
import cz.tul.dic.opencl.test.gen.scenario.d2.opt.CL2DImage_MC_V_C;
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

//  Devices for computation
//    private static final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.iGPU};
//    private static final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.CPU, ContextHandler.DeviceType.iGPU};
    private static final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU, ContextHandler.DeviceType.iGPU, ContextHandler.DeviceType.CPU};
//  Large task
//    private static final int[][] IMAGE_SIZES = new int[]{{1024, 768}};
//    private static final int[] DEFORMATION_COUNTS = new int[]{800};
//    private static final int[] FACET_SIZES = new int[]{35};
//  Medium task
//    private static final int[][] IMAGE_SIZES = new int[][]{{512, 384}};
//    private static final int[] DEFORMATION_COUNTS = new int[]{400};
//    private static final int[] FACET_SIZES = new int[]{21};
//  Small task
//    private static final int[][] IMAGE_SIZES = new int[][]{{128, 96}};
//    private static final int[] DEFORMATION_COUNTS = new int[]{100};
//    private static final int[] FACET_SIZES = new int[]{9};
//  Full task
//    private static final int[][] IMAGE_SIZES = new int[][]{{128, 96}, {512, 384}, {1024, 768}};
//    private static final int[] DEFORMATION_COUNTS = new int[]{100, 200, 400, 800};
//    private static final int[] FACET_SIZES = new int[]{9, 21, 35};
//  Real task 1st order
    private static final int[][] IMAGE_SIZES = new int[][]{{44, 240}, {110, 712}};
    private static final int[] DEFORMATION_COUNTS = new int[]{500};
    private static final int[] FACET_SIZES = new int[]{5, 11};
//  Real task 0 order
//    private static final int[][] IMAGE_SIZES = new int[][]{{52, 52}, {143,143}};
//    private static final int[] DEFORMATION_COUNTS = new int[]{200};
//    private static final int[] FACET_SIZES = new int[]{17, 25};
//  Generic task
//    private static final int[][] IMAGE_SIZES = new int[][]{{512, 384}};
//    private static final int[] DEFORMATION_COUNTS = new int[]{1000};
//    private static final int[] FACET_SIZES = new int[]{15};

    public static void computeImageFillTest() throws IOException {
        for (ContextHandler.DeviceType device : HW) {
            CLPlatform.initialize();
            DataStorage.reset();
            final ContextHandler ch = new ContextHandler(device);
            final List<Scenario> scenarios = prepareScenarios(ch);
            for (Scenario sc : scenarios) {
                DataStorage.addVariantCount(sc.getVariantCount());
            }

            final List<TestCase> testCases = prepareTestCases();
            int lineCount = 1;
            lineCount *= IMAGE_SIZES.length;
            lineCount *= FACET_SIZES.length;
            lineCount *= DEFORMATION_COUNTS.length;
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

                    for (int[] dim : IMAGE_SIZES) {                        
                        images = tc.generateImages(dim[0], dim[1]);

                        for (int sz = 0; sz < FACET_SIZES.length; sz++) {
                            s = FACET_SIZES[sz];

                            facetCenters = tc.generateFacetCenters(dim[0], dim[1], s);
                            facetData = tc.generateFacetData(facetCenters, s);

                            for (int d : DEFORMATION_COUNTS) {
                                deformations = tc.generateDeformations(d);

                                for (int sci = 0; sci < scenarios.size(); sci++) {
                                    sc = scenarios.get(sci);
                                    sc.reset();
                                    while (sc.hasNext()) {
                                        ps = new ParameterSet();
                                        ps.addParameter(Parameter.IMAGE_WIDTH, dim[0]);
                                        ps.addParameter(Parameter.IMAGE_HEIGHT, dim[1]);
                                        ps.addParameter(Parameter.FACET_SIZE, s);
                                        ps.addParameter(Parameter.FACET_COUNT, facetData.length / Utils.calculateFacetArraySize(s));
                                        ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                        ps.addParameter(Parameter.VARIANT, sci);
                                        ps.addParameter(Parameter.TEST_CASE, tci);

                                        sc.prepare(ps);

                                        time = System.nanoTime();
                                        try {
                                            result = sc.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
                                            if (result == null) {
                                                result = new ScenarioResult(-1, false);
                                            } else {
                                                result.setTotalTime(System.nanoTime() - time);
                                                tc.checkResult(result, ps.getValue(Parameter.FACET_COUNT));
                                            }
                                        } catch (CLException ex) {
                                            result = new ScenarioResult(-1, true);
                                            System.err.println("CL error - " + ex.getLocalizedMessage());
                                        } catch (Exception | Error ex) {
                                            result = new ScenarioResult(-1, true);
                                            System.err.println("Error - " + ex.getLocalizedMessage());
                                        }

                                        switch (result.getState()) {
                                            case SUCCESS:
                                                System.out.println("Finished " + sc.getKernelName() + " " + (result.getTotalTime() / 1000000) + "ms (" + (result.getKernelExecutionTime() / 1000000) + " ms in kernel) with params " + ps);
                                                break;
                                            case WRONG_RESULT_DYNAMIC:
                                                System.out.println("Wrong dynamic part of result for  " + sc.getKernelName() + " " + (result.getTotalTime() / 1000000) + "ms (" + (result.getKernelExecutionTime() / 1000000) + " ms in kernel) with params " + ps);
                                                break;
                                            case WRONG_RESULT_FIXED:
                                                System.out.println("Wrong fixed part of result for  " + sc.getKernelName() + " " + (result.getTotalTime() / 1000000) + "ms (" + (result.getKernelExecutionTime() / 1000000) + " ms in kernel) with params " + ps);
                                                break;
                                            case FAIL:
                                                System.out.println("Failed " + sc.getKernelName() + " with params " + ps);
                                                ch.reset();
                                                break;
                                            case INVALID_PARAMS:
                                                System.out.println("Invalid params for " + sc.getKernelName() + " - " + ps);
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
            String fileName = "D:\\DIC_OpenCL_Data_" + device + ".csv";
            DataStorage.exportData(new File(fileName));
            DataStorage.exportResultGroups(new File("D:\\DIC_OpenCL_Results_" + device + ".csv"));
        }
    }

    private static List<TestCase> prepareTestCases() {
        List<TestCase> result = new ArrayList<>(2);

        result.add(new TestCase());
//        result.add(new ShiftedImageCase(5, 2));

        return result;
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

        scenarios.add(new JavaPerFacet());
        scenarios.add(new JavaPerDeformation());
        scenarios.add(new CL1DIntPerFacetSingle(contextHandler));
        scenarios.add(new CL1DIntPerDeformationSingle(contextHandler));
        scenarios.add(new CL15DIntPerFacet(contextHandler));
        scenarios.add(new CL15DIntPerDeformation(contextHandler));
        scenarios.add(new CL2DInt("CL2DInt", contextHandler));
        scenarios.add(new CL2DInt("CL2DIntOpt", contextHandler));
        scenarios.add(new CL2DImage(contextHandler));
        scenarios.add(new CL2DImageFtoA(contextHandler));
        scenarios.add(new CL2DImageMC(contextHandler));
        scenarios.add(new CL2DImageV(contextHandler));
        scenarios.add(new CL2DImageC(contextHandler));
        scenarios.add(new CL2DImage_MC_V(contextHandler));
        scenarios.add(new CL2DImage_MC_V_C(contextHandler));
        scenarios.add(new CL1DImageLpF(contextHandler));
        scenarios.add(new CL1DImageLpF_LWS(contextHandler));

        return scenarios;
    }

}
