package cz.tul.dic.test.opencl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.test.opencl.generators.GPUDataGenerator;
import cz.tul.dic.test.opencl.generators.GPUDataGenerator.DataType;
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
import cz.tul.dic.test.opencl.scenario.fulldata.Scenario;
import cz.tul.dic.test.opencl.scenario.fulldata.ScenarioDrivenOpenCL;
import cz.tul.dic.test.opencl.scenario.fulldata.ScenarioOpenCL;
import cz.tul.dic.test.opencl.scenario.fulldata.comb.CL1D_I_V_LL;
import cz.tul.dic.test.opencl.scenario.fulldata.d1.opt.CL1DImageLL;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.CL2DImage;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.CL2DInt;
import cz.tul.dic.test.opencl.scenario.fulldata.d2.opt.CL2DImageV;
import cz.tul.dic.test.opencl.scenario.java.JavaPerDeformation;
import cz.tul.dic.test.opencl.scenario.java.JavaPerFacet;
import cz.tul.dic.test.opencl.scenario.limitsD.ScenarioOpenCL_LD;
import cz.tul.dic.test.opencl.scenario.limitsD.ScenarioOpenCL_LD_GPU;
import cz.tul.dic.test.opencl.scenario.limitsF.ScenarioOpenCL_LF;
import cz.tul.dic.test.opencl.scenario.limitsF.ScenarioOpenCL_LF_GPU;
import cz.tul.dic.test.opencl.scenario.limitsFD.ScenarioOpenCL_LFD;
import cz.tul.dic.test.opencl.scenario.limitsNO.CL_NO_1DImageLL;
import cz.tul.dic.test.opencl.scenario.limitsNO.CL_NO_1D_I_V_LL;
import cz.tul.dic.test.opencl.scenario.limitsNO.CL_NO_2DImage;
import cz.tul.dic.test.opencl.scenario.limitsNO.CL_NO_2DImageV;
import cz.tul.dic.test.opencl.scenario.limitsNO.CL_NO_2DInt;
import cz.tul.dic.test.opencl.scenario.limitsNO.ScenarioOpenCL_NO;
import cz.tul.dic.test.opencl.scenario.limitsNO.ScenarioOpenCL_NO_GPU;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class GeneralTest {

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

            Map<DataType, int[]> bestLws;
            int[][] images;
            int[] facetData;
            float[] facetCenters;
            float[] defomationLimitsSingle;
            ParameterSet ps;
            ScenarioResult result;
            Scenario sc;
            TestCase tc;
            int s;
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

                                for (int d : Constants.DEFORMATION_COUNTS) {
                                    defomationLimitsSingle = tc.generateDeformationLimits(d);

                                    ///// GPU DATA GENERATION TEST
                                    ps = new ParameterSet();
                                    ps.addParameter(Parameter.FACET_SIZE, s);
                                    ps.addParameter(Parameter.FACET_COUNT, facetCenters.length / 2);
                                    ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                    bestLws = GPUDataGenerator.runGPUDataGenerationTest(ch, ps, defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle), facetCenters);
                                    System.out.println("-- Best LWS DEF: " + Arrays.toString(bestLws.get(DataType.DEFORMATION)) + "; best LWS FAC: " + Arrays.toString(bestLws.get(DataType.FACET)));
                                    // TEST END

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
                                                    result = runDrivenKernel(tc, sc, ps, images, facetCenters, defomationLimitsSingle);
                                                } else {
                                                    result = runNormalKernel(tc, sc, ps, ch, images, facetCenters, defomationLimitsSingle, bestLws);
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

                                            GPUDataGenerator.resourceCleanup();
                                        }
                                    }

                                    ch.initContext();
                                    System.gc();
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
            final float[] facetCenters,
            final float[] defomationLimitsSingle) {
        ScenarioResult result = null, tempResult;
        long minTime = Long.MAX_VALUE;
        long time;
        int bestLwsSub = 1;
        while (sc.hasNext()) {
            time = System.nanoTime();
            tempResult = executeDrivenKernel(tc, sc, ps, images, facetCenters, defomationLimitsSingle);
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
            final TestCase tc, final Scenario sc, final ParameterSet ps,
            final int[][] images,
            final float[] facetCenters,
            final float[] defomationLimitsSingle) {
        ScenarioResult result;
        if (sc instanceof ScenarioDrivenOpenCL) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] deformationsFull = Utils.repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCenters.length / 2);
            result = ((ScenarioDrivenOpenCL) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else {
            LOG.log(Level.SEVERE, "Illegal type of driven scenario - {0}", sc.getClass().toGenericString());
            result = new ScenarioResult(-1, true);
        }
        return result;
    }

    private static ScenarioResult runNormalKernel(
            final TestCase tc, final Scenario sc, final ParameterSet ps,
            final ContextHandler context,
            final int[][] images,
            final float[] facetCenters,
            final float[] defomationLimitsSingle,
            final Map<DataType, int[]> bestLws) {
        ScenarioResult result;

        long time = System.nanoTime();
        result = executeNormalKernel(sc, ps, tc, context, images, facetCenters, defomationLimitsSingle, bestLws);
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
            final Scenario sc, final ParameterSet ps, final TestCase tc,
            final ContextHandler context,
            final int[][] images,
            final float[] facetCenters,
            final float[] defomationLimitsSingle,
            final Map<GPUDataGenerator.DataType, int[]> bestLws) {
        ScenarioResult result;
        final int facetCount = facetCenters.length / 2;
        if (sc instanceof JavaPerFacet) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] deformationsFull = Utils.repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((JavaPerFacet) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof JavaPerDeformation) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] deformationsFull = Utils.repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((JavaPerDeformation) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] deformationsFull = Utils.repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((ScenarioOpenCL) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_NO) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] deformationsFull = Utils.repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((ScenarioOpenCL_NO) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_NO_GPU) {
            final CLBuffer<FloatBuffer> bufferFacetCenters = GPUDataGenerator.storeCenters(context, facetCenters);
            final CLBuffer<IntBuffer> bufferFacetData = GPUDataGenerator.generateFacets(context, bufferFacetCenters, ps, bestLws.get(DataType.FACET));
            final CLBuffer<FloatBuffer> bufferDeformations = GPUDataGenerator.generateDeformations(context, defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle), ps, bestLws.get(DataType.DEFORMATION));
            result = ((ScenarioOpenCL_NO_GPU) sc).compute(images[0], images[1], bufferFacetData, bufferFacetCenters, bufferDeformations, ps);
        } else if (sc instanceof ScenarioOpenCL_LD) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] defomationLimitsFull = Utils.repeatArray(defomationLimitsSingle, facetCount);
            final int[] deformationCountsFull = Utils.repeatArray(tc.generateDeformationCounts(defomationLimitsSingle), facetCount);
            result = ((ScenarioOpenCL_LD) sc).compute(images[0], images[1], facetData, facetCenters, defomationLimitsFull, deformationCountsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_LD_GPU) {
            final CLBuffer<FloatBuffer> bufferFacetCenters = GPUDataGenerator.storeCenters(context, facetCenters);
            final CLBuffer<IntBuffer> bufferFacetData = GPUDataGenerator.generateFacets(context, bufferFacetCenters, ps, bestLws.get(DataType.FACET));
            final float[] defomationLimitsFull = Utils.repeatArray(defomationLimitsSingle, facetCount);
            final int[] deformationCountsFull = Utils.repeatArray(tc.generateDeformationCounts(defomationLimitsSingle), facetCount);
            result = ((ScenarioOpenCL_LD_GPU) sc).compute(images[0], images[1], bufferFacetData, bufferFacetCenters, defomationLimitsFull, deformationCountsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_LF) {
            final float[] deformationsFull = Utils.repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((ScenarioOpenCL_LF) sc).compute(images[0], images[1], facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_LF_GPU) {
            final CLBuffer<FloatBuffer> bufferDeformations = GPUDataGenerator.generateDeformations(context, defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle), ps, bestLws.get(DataType.DEFORMATION));
            result = ((ScenarioOpenCL_LF_GPU) sc).compute(images[0], images[1], facetCenters, bufferDeformations, ps);
        } else if (sc instanceof ScenarioOpenCL_LFD) {
            final float[] defomationLimitsFull = Utils.repeatArray(defomationLimitsSingle, facetCount);
            final int[] deformationCountsFull = Utils.repeatArray(tc.generateDeformationCounts(defomationLimitsSingle), facetCount);
            result = ((ScenarioOpenCL_LFD) sc).compute(images[0], images[1], facetCenters, defomationLimitsFull, deformationCountsFull, ps);
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

        scenarios.add(new CL2DInt("CL2DInt", contextHandler));
        scenarios.add(new CL_NO_2DInt("CL_NO_2DInt", contextHandler));

        scenarios.add(new CL2DImage(contextHandler));
        scenarios.add(new CL_NO_2DImage(contextHandler));

        scenarios.add(new CL2DImageV(contextHandler));
        scenarios.add(new CL_NO_2DImageV(contextHandler));

        scenarios.add(new CL1DImageLL(contextHandler));
        scenarios.add(new CL_NO_1DImageLL(contextHandler));

        scenarios.add(new CL1D_I_V_LL(contextHandler));
        scenarios.add(new CL_NO_1D_I_V_LL(contextHandler));

        return scenarios;
    }

}
