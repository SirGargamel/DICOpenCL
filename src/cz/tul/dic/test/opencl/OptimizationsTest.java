package cz.tul.dic.test.opencl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.test.opencl.scenario.ContextHandler;
import cz.tul.dic.test.opencl.scenario.ContextHandler.DeviceType;
import cz.tul.dic.test.opencl.scenario.DataStorage;
import cz.tul.dic.test.opencl.scenario.Parameter;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import cz.tul.dic.test.opencl.utils.Utils;
import cz.tul.dic.test.opencl.scenario.WorkSizeManager;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;
import cz.tul.dic.test.opencl.scenario.ScenarioResult.State;
import static cz.tul.dic.test.opencl.scenario.ScenarioResult.State.FAIL;
import static cz.tul.dic.test.opencl.scenario.ScenarioResult.State.INVALID_PARAMS;
import static cz.tul.dic.test.opencl.scenario.ScenarioResult.State.SUCCESS;
import static cz.tul.dic.test.opencl.scenario.ScenarioResult.State.WRONG_RESULT_DYNAMIC;
import static cz.tul.dic.test.opencl.scenario.ScenarioResult.State.WRONG_RESULT_FIXED;
import cz.tul.dic.test.opencl.scenario.fulldata.driven.CL1D_I_V_LL_D;
import cz.tul.dic.test.opencl.scenario.fulldata.driven.CL1D_I_V_LL_MC_D;
import cz.tul.dic.test.opencl.scenario.fulldata.driven.CL2D_I_D;
import cz.tul.dic.test.opencl.scenario.fulldata.driven.CL2D_I_V_D;
import cz.tul.dic.test.opencl.scenario.fulldata.driven.CL2D_I_V_MC_D;
import cz.tul.dic.test.opencl.scenario.fulldata.driven.CL2D_Int_D;
import cz.tul.dic.test.opencl.scenario.fulldata.Scenario;
import cz.tul.dic.test.opencl.scenario.fulldata.ScenarioDrivenOpenCL;
import cz.tul.dic.test.opencl.scenario.fulldata.ScenarioOpenCL;
import cz.tul.dic.test.opencl.scenario.fulldata.comb.CL1D_I_LL_MC;
import cz.tul.dic.test.opencl.scenario.fulldata.comb.CL1D_I_V_LL;
import cz.tul.dic.test.opencl.scenario.fulldata.comb.CL1D_I_V_LL_MC;
import cz.tul.dic.test.opencl.scenario.fulldata.comb.CL2D_I_V_MC;
import cz.tul.dic.test.opencl.scenario.fulldata.d1.CL1DIntPerDeformationSingle;
import cz.tul.dic.test.opencl.scenario.fulldata.d1.CL1DIntPerFacetSingle;
import cz.tul.dic.test.opencl.scenario.fulldata.d1.opt.CL1DImageL;
import cz.tul.dic.test.opencl.scenario.fulldata.d1.opt.CL1DImageLL;
import cz.tul.dic.test.opencl.scenario.fulldata.d15.CL15DIntPerDeformation;
import cz.tul.dic.test.opencl.scenario.fulldata.d15.CL15DIntPerFacet;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.CL2DImage;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.CL2DInt;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.opt.CL2DImageC;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.opt.CL2DImageFtoA;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.opt.CL2DImageMC;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.opt.CL2DImageV;
import cz.tul.dic.test.opencl.scenario.java.JavaPerDeformation;
import cz.tul.dic.test.opencl.scenario.java.JavaPerFacet;
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
public class OptimizationsTest {

    private static final Logger LOG = Logger.getGlobal();

    public static void runTest() throws IOException {
        for (DeviceType device : Constants.HW) {
            CLPlatform.initialize();
            DataStorage.reset();
            final ContextHandler ch = new ContextHandler(device);
            final WorkSizeManager wsm = new WorkSizeManager();
            final List<Scenario> scenarios = prepareScenarios(ch, wsm);
            final List<TestCase> testCases = prepareTestCases();

            initializeDataStorage(scenarios, testCases.size());

            int[][] images;
            int[] facetData, deformationCountsSingle;
            float[] facetCenters;
            float[] deformationsSingle, deformationsFull, defomationLimitsSingle;
            ParameterSet ps;
            ScenarioResult result;
            Scenario sc;
            TestCase tc;
            int s, facetCount;
            try {
                // execute scenarios
                for (int tci = 0; tci < testCases.size(); tci++) {
                    tc = testCases.get(tci);

                    for (int[] dim : Constants.IMAGE_SIZES) {
                        images = tc.generateImages(dim[0], dim[1]);

                        for (int sz = 0; sz < Constants.FACET_SIZES.length; sz++) {
                            s = Constants.FACET_SIZES[sz];

                            for (int fm = 0; fm < Constants.FACET_MULTI.length; fm++) {
                                facetCenters = tc.generateFacetCenters(dim[0], dim[1], s, Constants.FACET_MULTI[fm]);
                                facetData = tc.generateFacetData(facetCenters, s);
                                facetCount = facetCenters.length / 2;

                                for (int d : Constants.DEFORMATION_COUNTS) {
                                    defomationLimitsSingle = tc.generateDeformationLimits(d);
                                    deformationCountsSingle = tc.generateDeformationCounts(defomationLimitsSingle);
                                    deformationsSingle = tc.generateDeformations(defomationLimitsSingle, deformationCountsSingle);
                                    deformationsFull = Utils.repeatArray(deformationsSingle, facetCount);

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
                                                if (sc.isDriven()) {
                                                    result = runDrivenKernel(tc, sc, ps, images, facetData, facetCenters, deformationsFull);
                                                } else {
                                                    result = runNormalKernel(tc, sc, ps, images, facetData, facetCenters, deformationsFull);
                                                }
                                            } catch (CLException ex) {
                                                result = new ScenarioResult(-1, true);
                                                LOG.log(Level.SEVERE, "CL error - " + ex.getLocalizedMessage(), ex);
                                            } catch (Exception | Error ex) {
                                                result = new ScenarioResult(-1, true);
                                                LOG.log(Level.SEVERE, "Error - " + ex.getLocalizedMessage(), ex);
                                            }

                                            if (result == null) {
                                                result = new ScenarioResult(-1, true);
                                                LOG.log(Level.SEVERE, "Unknown error, NULL result.");
                                            }

                                            DataStorage.storeData(ps, result, ch.getDeviceName());

                                            switch (result.getState()) {
                                                case SUCCESS:
                                                    LOG.log(Level.INFO, "Finished {0} {1}ms ({2} ms in kernel) with params {3}, dif = {4}", new Object[]{sc.getKernelName(), result.getTotalTime() / 1000000, result.getKernelExecutionTime() / 1000000, ps, result.getMaxDifference()});
                                                    break;
                                                case WRONG_RESULT_DYNAMIC:
                                                    LOG.log(Level.INFO, "Wrong dynamic part of result for  {0} {1}ms ({2} ms in kernel) with params {3}, dif = {4}", new Object[]{sc.getKernelName(), result.getTotalTime() / 1000000, result.getKernelExecutionTime() / 1000000, ps, result.getMaxDifference()});
                                                    break;
                                                case WRONG_RESULT_FIXED:
                                                    LOG.log(Level.INFO, "Wrong fixed part of result for  {0} {1}ms ({2} ms in kernel) with params {3}, dif = {4}", new Object[]{sc.getKernelName(), result.getTotalTime() / 1000000, result.getKernelExecutionTime() / 1000000, ps, result.getMaxDifference()});
                                                    break;
                                                case FAIL:
                                                    LOG.log(Level.INFO, "Failed {0} with params {1}", new Object[]{sc.getKernelName(), ps});
                                                    ch.reset();
                                                    break;
                                                case INVALID_PARAMS:
                                                    LOG.log(Level.INFO, "Invalid params for {0} - {1}", new Object[]{sc.getKernelName(), ps});
                                            }
                                        }
                                    }
                                    ch.initContext();
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

    private static ScenarioResult runDrivenKernel(
            final TestCase tc, final Scenario sc, final ParameterSet ps,
            final int[][] images,
            final int[] facetData, final float[] facetCenters,
            final float[] deformationsFull) {
        ScenarioResult result = null, tempResult;
        long minTime = Long.MAX_VALUE;
        long time;
        int bestLwsSub = 1;
        while (sc.hasNext()) {
            time = System.nanoTime();
            tempResult = executeDrivenKernel(sc, ps, images, facetData, facetCenters, deformationsFull);
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

        return result;
    }

    private static ScenarioResult executeDrivenKernel(
            final Scenario sc, final ParameterSet ps,
            final int[][] images,
            final int[] facetData, final float[] facetCenters,
            final float[] deformationsFull) {
        ScenarioResult result;
        if (sc instanceof ScenarioDrivenOpenCL) {
            result = ((ScenarioDrivenOpenCL) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else {
            LOG.log(Level.SEVERE, "Illegal type of driven scenario - {0}", sc.getClass().toGenericString());
            result = new ScenarioResult(-1, true);
        }
        return result;
    }

    private static ScenarioResult runNormalKernel(final TestCase tc, final Scenario sc, final ParameterSet ps,
            final int[][] images,
            final int[] facetData, final float[] facetCenters,
            final float[] deformationsFull) {
        ScenarioResult result;

        long time = System.nanoTime();
        result = executeNormalKernel(sc, ps, images, facetData, facetCenters, deformationsFull);
        if (result == null || result.getResultData() == null) {
            result = new ScenarioResult(-1, false);
        } else {
            result.setTotalTime(System.nanoTime() - time);
            tc.checkResult(result, ps.getValue(Parameter.FACET_COUNT));
            if (!State.SUCCESS.equals(result.getState())) {
                result.setTotalTime(-1);
            }
        }

        return result;
    }

    private static ScenarioResult executeNormalKernel(
            final Scenario sc, final ParameterSet ps,
            final int[][] images,
            final int[] facetData, final float[] facetCenters,
            final float[] deformationsFull) {
        ScenarioResult result;
        if (sc instanceof JavaPerFacet) {
            result = ((JavaPerFacet) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof JavaPerDeformation) {
            result = ((JavaPerDeformation) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL) {
            result = ((ScenarioOpenCL) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else {
            LOG.log(Level.SEVERE, "Illegal type of normal scenario - {0}", sc.getClass().toGenericString());
            result = new ScenarioResult(-1, true);
        }
        return result;
    }

    private static void initializeDataStorage(final List<Scenario> scenarios, final int testCaseCount) {
        DataStorage.clearVariantCounts();
        for (Scenario sc : scenarios) {
            DataStorage.addVariantCount(sc.getVariantCount());
        }
        int lineCount = 1;
        lineCount *= Constants.IMAGE_SIZES.length;
        lineCount *= Constants.FACET_SIZES.length;
        lineCount *= Constants.FACET_MULTI.length;
        lineCount *= Constants.DEFORMATION_COUNTS.length;
        DataStorage.setCounts(lineCount, testCaseCount);
    }

    private static List<TestCase> prepareTestCases() {
        List<TestCase> result = new ArrayList<>(2);

        result.add(new TestCase());
//        result.add(new ShiftedImageCase(5, 2));

        return result;
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler, final WorkSizeManager wsm) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

        // OpenCL basic optimizations tests
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

        scenarios.add(new CL2D_Int_D(contextHandler, wsm)); // driven variants
        scenarios.add(new CL2D_I_D(contextHandler, wsm));
        scenarios.add(new CL2D_I_V_D(contextHandler, wsm));
        scenarios.add(new CL2D_I_V_MC_D(contextHandler, wsm));
        scenarios.add(new CL1D_I_V_LL_D(contextHandler, wsm));
        scenarios.add(new CL1D_I_V_LL_MC_D(contextHandler, wsm));

        return scenarios;
    }

}
