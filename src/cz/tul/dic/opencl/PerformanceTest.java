package cz.tul.dic.opencl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.ContextHandler.DeviceType;
import cz.tul.dic.opencl.test.gen.DataStorage;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.WorkSizeManager;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State;
import static cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State.FAIL;
import static cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State.INVALID_PARAMS;
import static cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State.SUCCESS;
import static cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State.WRONG_RESULT_DYNAMIC;
import static cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State.WRONG_RESULT_FIXED;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.ScenarioDrivenOpenCL;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.ScenarioFullData;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.ScenarioOpenCL;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.comb.CL1D_I_V_LL;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d1.opt.CL1DImageLL;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.CL2DImage;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.CL2DInt;
import cz.tul.dic.opencl.test.gen.scenario.fulldata.d2.opt.CL2DImageV;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.ScenarioDrivenOpenCL_LFD;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.ScenarioLimitsFD;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.ScenarioOpenCL_LFD;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.comb.CL_L_1D_I_V_LL;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.d1.opt.CL_L_1DImageLL;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.d2.CL_L_2DImage;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.d2.CL_L_2DInt;
import cz.tul.dic.opencl.test.gen.scenario.limitsFD.d2.opt.CL_L_2DImageV;
import cz.tul.dic.opencl.test.gen.testcase.TestCase;
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
            final List<Scenario> scenarios = prepareScenarios(ch, wsm);
            final List<TestCase> testCases = prepareTestCases();

            initializeDataStorage(scenarios, testCases.size());

            int[][] images;
            int[] facetData, deformationCountsSingle, deformationCountsFull;
            float[] facetCenters;
            float[] deformations, defomationLimitsSingle, defomationLimitsFull;
            long time, minTime;
            ParameterSet ps;
            ScenarioResult result, tempResult;
            Scenario sc;
            ScenarioFullData scf;
            ScenarioLimitsFD scl;
            TestCase tc;
            int s, bestLwsSub = 1, facetCount;
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
                            facetCount = facetCenters.length / 2;

                            for (int d : Constants.DEFORMATION_COUNTS) {
                                deformations = tc.generateDeformations(d);
                                defomationLimitsSingle = tc.generateDeformationLimits(d);
                                defomationLimitsFull = repeatArray(defomationLimitsSingle, facetCount);
                                deformationCountsSingle = tc.generateDeformationCounts(defomationLimitsSingle);
                                deformationCountsFull = repeatArray(deformationCountsSingle, facetCount);

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
                                            if (sc instanceof ScenarioFullData) {
                                                scf = (ScenarioFullData) sc;
                                                if (sc instanceof ScenarioDrivenOpenCL) {
                                                    // driven kernel
                                                    minTime = Long.MAX_VALUE;
                                                    while (sc.hasNext()) {
                                                        time = System.nanoTime();
                                                        tempResult = scf.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
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
                                                } else if (sc instanceof ScenarioOpenCL) {
                                                    // not driven kernel 
                                                    time = System.nanoTime();
                                                    result = scf.compute(images[0], images[1], facetData, facetCenters, deformations, ps);
                                                    if (result == null || result.getResultData() == null) {
                                                        result = new ScenarioResult(-1, false);
                                                    } else {
                                                        result.setTotalTime(System.nanoTime() - time);
                                                        tc.checkResult(result, ps.getValue(Parameter.FACET_COUNT));
                                                        if (!State.SUCCESS.equals(result.getState())) {
                                                            result.setTotalTime(-1);
                                                        }
                                                    }
                                                } else {
                                                    log.log(Level.SEVERE, "Illegal type of full data scenario - {0}", sc.getClass().toGenericString());
                                                    result = new ScenarioResult(-1, true);
                                                }
                                            } else if (sc instanceof ScenarioLimitsFD) {
                                                scl = (ScenarioLimitsFD) sc;
                                                if (sc instanceof ScenarioDrivenOpenCL_LFD) {
                                                    // driven kernel
                                                    minTime = Long.MAX_VALUE;
                                                    while (sc.hasNext()) {
                                                        time = System.nanoTime();
                                                        tempResult = scl.compute(images[0], images[1], facetCenters, defomationLimitsFull, deformationCountsFull, ps);
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
                                                } else if (sc instanceof ScenarioOpenCL_LFD) {
                                                    // not driven kernel 
                                                    time = System.nanoTime();
                                                    result = scl.compute(images[0], images[1], facetCenters, defomationLimitsFull, deformationCountsFull, ps);
                                                    if (result == null || result.getResultData() == null) {
                                                        result = new ScenarioResult(-1, false);
                                                    } else {
                                                        result.setTotalTime(System.nanoTime() - time);
                                                        tc.checkResult(result, ps.getValue(Parameter.FACET_COUNT));
                                                        if (!State.SUCCESS.equals(result.getState())) {
                                                            result.setTotalTime(-1);
                                                        }
                                                    }
                                                } else {
                                                    log.log(Level.SEVERE, "Illegal type of limite scenario - {0}", sc.getClass().toGenericString());
                                                    result = new ScenarioResult(-1, true);
                                                }
                                            } else {
                                                log.log(Level.SEVERE, "Illegal type of scenario - {0}", sc.getClass().toGenericString());
                                                result = new ScenarioResult(-1, true);
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

    private static void initializeDataStorage(final List<Scenario> scenarios, final int testCaseCount) {
        DataStorage.clearVariantCounts();
        for (Scenario sc : scenarios) {
            DataStorage.addVariantCount(sc.getVariantCount());
        }
        int lineCount = 1;
        lineCount *= Constants.IMAGE_SIZES.length;
        lineCount *= Constants.FACET_SIZES.length;
        lineCount *= Constants.DEFORMATION_COUNTS.length;
        DataStorage.setCounts(lineCount, testCaseCount);
    }

    private static float[] repeatArray(final float[] input, final int repetitionCount) {
        final int l = input.length;
        final float[] result = new float[l * repetitionCount];
        for (int i = 0; i < repetitionCount; i++) {
            System.arraycopy(input, 0, result, i * l, l);
        }
        return result;
    }

    private static int[] repeatArray(final int[] input, final int repetitionCount) {
        final int l = input.length;
        final int[] result = new int[l * repetitionCount];
        for (int i = 0; i < repetitionCount; i++) {
            System.arraycopy(input, 0, result, i * l, l);
        }
        return result;
    }

    private static List<TestCase> prepareTestCases() {
        List<TestCase> result = new ArrayList<>(2);

        result.add(new TestCase());
//        result.add(new ShiftedImageCase(5, 2));

        return result;
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler, final WorkSizeManager fcm) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

//        scenarios.add(new JavaPerFacet());    // Java threads computation
//        scenarios.add(new JavaPerDeformation());
//
//        scenarios.add(new CL1DIntPerFacetSingle(contextHandler)); // OpenCL computation
//        scenarios.add(new CL1DIntPerDeformationSingle(contextHandler));
//        scenarios.add(new CL15DIntPerFacet(contextHandler));
//        scenarios.add(new CL15DIntPerDeformation(contextHandler));
        scenarios.add(new CL2DInt("CL2DInt", contextHandler));
//        scenarios.add(new CL2DInt("CL2DIntOpt", contextHandler));
        scenarios.add(new CL2DImage(contextHandler));
//
//        scenarios.add(new CL2DImageFtoA(contextHandler)); // Optimizations
//        scenarios.add(new CL2DImageMC(contextHandler));
        scenarios.add(new CL2DImageV(contextHandler));
//        scenarios.add(new CL2DImageC(contextHandler));
//        scenarios.add(new CL1DImageL(contextHandler));
        scenarios.add(new CL1DImageLL(contextHandler));
//
//        scenarios.add(new CL2D_I_V_MC(contextHandler));    // Combined optimizations
        scenarios.add(new CL1D_I_V_LL(contextHandler));
//        scenarios.add(new CL1D_I_LL_MC(contextHandler));
//        scenarios.add(new CL1D_I_V_LL_MC(contextHandler));
//
//        scenarios.add(new CL2D_Int_D(contextHandler, fcm)); // driven variants
//        scenarios.add(new CL2D_I_D(contextHandler, fcm));
//        scenarios.add(new CL2D_I_V_D(contextHandler, fcm));
//        scenarios.add(new CL2D_I_V_MC_D(contextHandler, fcm));
//        scenarios.add(new CL1D_I_V_LL_D(contextHandler, fcm));
//        scenarios.add(new CL1D_I_V_LL_MC_D(contextHandler, fcm));

        scenarios.add(new CL_L_2DInt("CL_L_2DInt", contextHandler)); // variants with both facet and deformation limits
        scenarios.add(new CL_L_2DImage(contextHandler));
        scenarios.add(new CL_L_2DImageV(contextHandler));
        scenarios.add(new CL_L_1DImageLL(contextHandler));
        scenarios.add(new CL_L_1D_I_V_LL(contextHandler));

        return scenarios;
    }

}
