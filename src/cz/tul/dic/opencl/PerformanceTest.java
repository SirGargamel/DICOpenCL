package cz.tul.dic.opencl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.ContextHandler.DeviceType;
import cz.tul.dic.opencl.test.gen.DataStorage;
import cz.tul.dic.opencl.test.gen.WorkSizeManager;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.testcase.TestCase;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.ScenarioFullData;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.ScenarioDrivenOpenCL;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State;
import cz.tul.dic.opencl.test.gen.scenario.comb.CL1D_I_V_LL_MC;
import cz.tul.dic.opencl.test.gen.scenario.comb.CL1D_I_V_LL;
import cz.tul.dic.opencl.test.gen.scenario.comb.CL2D_I_V_MC;
import cz.tul.dic.opencl.test.gen.scenario.d1.CL1DIntPerDeformationSingle;
import cz.tul.dic.opencl.test.gen.scenario.d1.CL1DIntPerFacetSingle;
import cz.tul.dic.opencl.test.gen.scenario.d1.opt.CL1DImageL;
import cz.tul.dic.opencl.test.gen.scenario.d1.opt.CL1DImageLL;
import cz.tul.dic.opencl.test.gen.scenario.d15.CL15DIntPerDeformation;
import cz.tul.dic.opencl.test.gen.scenario.d15.CL15DIntPerFacet;
import cz.tul.dic.opencl.test.gen.scenario.d2.CL2DImage;
import cz.tul.dic.opencl.test.gen.scenario.d2.CL2DInt;
import cz.tul.dic.opencl.test.gen.scenario.driven.CL1D_I_V_LL_D;
import cz.tul.dic.opencl.test.gen.scenario.driven.CL1D_I_V_LL_MC_D;
import cz.tul.dic.opencl.test.gen.scenario.driven.CL2D_I_D;
import cz.tul.dic.opencl.test.gen.scenario.driven.CL2D_I_V_D;
import cz.tul.dic.opencl.test.gen.scenario.driven.CL2D_I_V_MC_D;
import cz.tul.dic.opencl.test.gen.scenario.driven.CL2D_Int_D;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.comb.CL1D_I_LL_MC;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.opt.CL2DImageC;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.opt.CL2DImageFtoA;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.opt.CL2DImageMC;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.opt.CL2DImageV;
import cz.tul.dic.opencl.test.gen.scenario.java.JavaPerDeformation;
import cz.tul.dic.opencl.test.gen.scenario.java.JavaPerFacet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final Logger log = Logger.getGlobal();

    public static void computeImageFillTest() throws IOException {
        for (DeviceType device : Constants.HW) {
            CLPlatform.initialize();
            DataStorage.reset();
            final ContextHandler ch = new ContextHandler(device);
            final WorkSizeManager wsm = new WorkSizeManager();
            final List<ScenarioFullData> scenarios = prepareScenarios(ch, wsm);
            final List<TestCase> testCases = prepareTestCases();
            
            initializeDataStorage(scenarios, testCases.size());

            int[][] images;
            int[] facetData;
            float[] facetCenters;
            float[] deformations;
            long time, minTime;
            ParameterSet ps;
            ScenarioResult result, tempResult;
            ScenarioFullData sc;
            TestCase tc;
            int s, bestLwsSub = 1;
            try {
                // execute scenarios
                for (int tci = 0; tci < testCases.size(); tci++) {
                    tc = testCases.get(tci);

                    for (int[] dim : Constants.IMAGE_SIZES) {
                        images = tc.generateImages(dim[0], dim[1]);

                        for (int sz = 0; sz < Constants.FACET_SIZES.length; sz++) {
                            s = Constants.FACET_SIZES[sz];

                            facetCenters = tc.generateFacetCenters(dim[0], dim[1], s);
                            facetData = tc.generateFacetData(facetCenters, s);

                            for (int d : Constants.DEFORMATION_COUNTS) {
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

                                        try {
                                            result = null;
                                            if (sc instanceof ScenarioDrivenOpenCL) {
                                                // driven kernel
                                                minTime = Long.MAX_VALUE;
                                                while (sc.hasNext()) {
                                                    time = System.nanoTime();
                                                    tempResult = sc.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
                                                    time = System.nanoTime() - time;

                                                    if (tempResult != null && time < minTime) {
                                                        tc.checkResult(tempResult, ps.getValue(Parameter.FACET_COUNT));

                                                        if (tempResult.getState() == State.SUCCESS) {
                                                            result = tempResult;
                                                            minTime = time;
                                                            bestLwsSub = ps.getValue(Parameter.LWS_SUB);
                                                            result.setTotalTime(minTime);
                                                        }
                                                    }
                                                }

                                                if (result == null) {
                                                    result = new ScenarioResult(-1, false);
                                                } else {
                                                    ps.addParameter(Parameter.LWS_SUB, bestLwsSub);
                                                }
                                            } else {
                                                // not driven kernel 
                                                time = System.nanoTime();
                                                result = sc.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
                                                if (result == null || result.getResultData() == null) {
                                                    result = new ScenarioResult(-1, false);
                                                } else {
                                                    result.setTotalTime(System.nanoTime() - time);
                                                    tc.checkResult(result, ps.getValue(Parameter.FACET_COUNT));
                                                    if (!State.SUCCESS.equals(result.getState())) {
                                                        result.setTotalTime(-1);
                                                    }
                                                }
                                            }
                                        } catch (CLException ex) {
                                            result = new ScenarioResult(-1, true);
                                            log.log(Level.SEVERE, "CL error - " + ex.getLocalizedMessage(), ex);
                                        } catch (Exception | Error ex) {
                                            result = new ScenarioResult(-1, true);
                                            log.log(Level.SEVERE, "Error - " + ex.getLocalizedMessage(), ex);
                                        }

                                        DataStorage.storeData(ps, result, ch.getDeviceName());

                                        switch (result.getState()) {
                                            case SUCCESS:
                                                log.log(Level.INFO, "Finished {0} {1}ms ({2} ms in kernel) with params {3}, dif = {4}", new Object[]{sc.getKernelName(), result.getTotalTime() / 1000000, result.getKernelExecutionTime() / 1000000, ps, result.getMaxDifference()});
                                                break;
                                            case WRONG_RESULT_DYNAMIC:
                                                log.log(Level.INFO, "Wrong dynamic part of result for  {0} {1}ms ({2} ms in kernel) with params {3}, dif = {4}", new Object[]{sc.getKernelName(), result.getTotalTime() / 1000000, result.getKernelExecutionTime() / 1000000, ps, result.getMaxDifference()});
                                                break;
                                            case WRONG_RESULT_FIXED:
                                                log.log(Level.INFO, "Wrong fixed part of result for  {0} {1}ms ({2} ms in kernel) with params {3}, dif = {4}", new Object[]{sc.getKernelName(), result.getTotalTime() / 1000000, result.getKernelExecutionTime() / 1000000, ps, result.getMaxDifference()});
                                                break;
                                            case FAIL:
                                                log.log(Level.INFO, "Failed {0} with params {1}", new Object[]{sc.getKernelName(), ps});
                                                ch.reset();
                                                break;
                                            case INVALID_PARAMS:
                                                log.log(Level.INFO, "Invalid params for {0} - {1}", new Object[]{sc.getKernelName(), ps});
                                        }
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

            initializeDataStorage(scenarios, testCases.size());
            String fileName = "D:\\DIC_OpenCL_Data_" + device + ".csv";
            DataStorage.exportData(new File(fileName));
            DataStorage.exportResultGroups(new File("D:\\DIC_OpenCL_Results_" + device + ".csv"));
        }
    }

    private static void initializeDataStorage(final List<ScenarioFullData> scenarios, final int testCaseCount) {
        DataStorage.clearVariantCounts();
        for (ScenarioFullData sc : scenarios) {
            DataStorage.addVariantCount(sc.getVariantCount());
        }
        int lineCount = 1;
        lineCount *= Constants.IMAGE_SIZES.length;
        lineCount *= Constants.FACET_SIZES.length;
        lineCount *= Constants.DEFORMATION_COUNTS.length;
        DataStorage.setCounts(lineCount, testCaseCount);
    }

    private static List<TestCase> prepareTestCases() {
        List<TestCase> result = new ArrayList<>(2);

        result.add(new TestCase());
//        result.add(new ShiftedImageCase(5, 2));

        return result;
    }

    private static List<ScenarioFullData> prepareScenarios(final ContextHandler contextHandler, final WorkSizeManager fcm) throws IOException {
        final List<ScenarioFullData> scenarios = new LinkedList<>();

        scenarios.add(new JavaPerFacet());    // Java threads computation
        scenarios.add(new JavaPerDeformation());

        scenarios.add(new CL1DIntPerFacetSingle(contextHandler)); // OpenCL computation
        scenarios.add(new CL1DIntPerDeformationSingle(contextHandler));
        scenarios.add(new CL15DIntPerFacet(contextHandler));
        scenarios.add(new CL15DIntPerDeformation(contextHandler));
        scenarios.add(new CL2DInt("CL2DInt", contextHandler));
        scenarios.add(new CL2DInt("CL2DIntOpt", contextHandler));
        scenarios.add(new CL2DImage(contextHandler));

        scenarios.add(new CL2DImageFtoA(contextHandler)); // Optimizations
        scenarios.add(new CL2DImageMC(contextHandler));
        scenarios.add(new CL2DImageV(contextHandler));
        scenarios.add(new CL2DImageC(contextHandler));
        scenarios.add(new CL1DImageL(contextHandler));
        scenarios.add(new CL1DImageLL(contextHandler));

        scenarios.add(new CL2D_I_V_MC(contextHandler));    // Combined optimizations
        scenarios.add(new CL1D_I_V_LL(contextHandler));
        scenarios.add(new CL1D_I_LL_MC(contextHandler));
        scenarios.add(new CL1D_I_V_LL_MC(contextHandler));

        scenarios.add(new CL2D_Int_D(contextHandler, fcm)); // driven variants
        scenarios.add(new CL2D_I_D(contextHandler, fcm));
        scenarios.add(new CL2D_I_V_D(contextHandler, fcm));
        scenarios.add(new CL2D_I_V_MC_D(contextHandler, fcm));
        scenarios.add(new CL1D_I_V_LL_D(contextHandler, fcm));
        scenarios.add(new CL1D_I_V_LL_MC_D(contextHandler, fcm));

        return scenarios;
    }

}
