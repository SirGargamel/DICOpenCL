package cz.tul.dic.test.opencl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import cz.tul.dic.test.opencl.generator.GPUDataGenerator;
import cz.tul.dic.test.opencl.test.gen.ContextHandler;
import cz.tul.dic.test.opencl.test.gen.ContextHandler.DeviceType;
import cz.tul.dic.test.opencl.test.gen.DataStorage;
import cz.tul.dic.test.opencl.test.gen.Parameter;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult.State;
import static cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult.State.FAIL;
import static cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult.State.INVALID_PARAMS;
import static cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult.State.SUCCESS;
import static cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult.State.WRONG_RESULT_DYNAMIC;
import static cz.tul.dic.test.opencl.test.gen.scenario.ScenarioResult.State.WRONG_RESULT_FIXED;
import cz.tul.dic.test.opencl.test.gen.scenario.fulldata.Scenario;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_1DImageLL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_1DImageLL_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_1D_I_V_LL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_1D_I_V_LL_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_2DImage;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_2DImageV;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_2DImageV_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_2DImage_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_2DInt;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.CL_LD_2DInt_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.ScenarioOpenCL_LD;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsD.ScenarioOpenCL_LD_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_1DImageLL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_1DImageLL_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_1D_I_V_LL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_1D_I_V_LL_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_2DImage;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_2DImageV;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_2DImageV_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_2DImage_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_2DInt;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.CL_LF_2DInt_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.ScenarioOpenCL_LF;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsF.ScenarioOpenCL_LF_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsFD.CL_L_1DImageLL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsFD.CL_L_1D_I_V_LL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsFD.CL_L_2DImage;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsFD.CL_L_2DImageV;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsFD.CL_L_2DInt;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsFD.ScenarioOpenCL_LFD;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_1DImageLL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_1DImageLL_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_1D_I_V_LL;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_1D_I_V_LL_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_2DImage;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_2DImageV;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_2DImageV_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_2DImage_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_2DInt;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.CL_NO_2DInt_GPU;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.ScenarioOpenCL_NO;
import cz.tul.dic.test.opencl.test.gen.scenario.limitsNO.ScenarioOpenCL_NO_GPU;
import cz.tul.dic.test.opencl.test.gen.testcase.TestCase;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class LimitsTest {

    private static final Logger LOG = Logger.getGlobal();

    public static void runTest() throws IOException {
        for (DeviceType device : Constants.HW) {
            CLPlatform.initialize();
            DataStorage.reset();
            final ContextHandler ch = new ContextHandler(device);
            final List<Scenario> scenarios = prepareScenarios(ch);
            final List<TestCase> testCases = prepareTestCases();

            initializeDataStorage(scenarios, testCases.size());

            int[][] images;
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

                                for (int d : Constants.DEFORMATION_COUNTS) {
                                    defomationLimitsSingle = tc.generateDeformationLimits(d);

                                    for (int sci = 0; sci < scenarios.size(); sci++) {
                                        sc = scenarios.get(sci);
                                        sc.reset();
                                        while (sc.hasNext()) {
                                            ps = new ParameterSet();
                                            ps.addParameter(Parameter.IMAGE_WIDTH, dim[0]);
                                            ps.addParameter(Parameter.IMAGE_HEIGHT, dim[1]);
                                            ps.addParameter(Parameter.FACET_SIZE, s);
                                            ps.addParameter(Parameter.FACET_COUNT, facetCenters.length / 2);
                                            ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                            ps.addParameter(Parameter.VARIANT, sci);
                                            ps.addParameter(Parameter.TEST_CASE, tci);

                                            sc.prepare(ps);

                                            try {
                                                if (sc.isDriven()) {
                                                    throw new UnsupportedOperationException("No driven version available.");
                                                } else {
                                                    result = runNormalKernel(tc, sc, ps, ch, images, facetCenters, defomationLimitsSingle);
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
                                    GPUDataGenerator.resourceCleanup();
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

    private static ScenarioResult runNormalKernel(
            final TestCase tc, final Scenario sc, final ParameterSet ps,
            final ContextHandler context,
            final int[][] images,
            final float[] facetCenters,
            final float[] defomationLimitsSingle) {
        ScenarioResult result;

        long time = System.nanoTime();
        result = executeNormalKernel(sc, ps, tc, context, images, facetCenters, defomationLimitsSingle);
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
            final float[] defomationLimitsSingle) {
        ScenarioResult result;
        final int facetCount = facetCenters.length / 2;
        if (sc instanceof ScenarioOpenCL_NO) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] deformationsFull = repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((ScenarioOpenCL_NO) sc).compute(images[0], images[1], facetData, facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_NO_GPU) {
            final CLBuffer<FloatBuffer> bufferFacetCenters = GPUDataGenerator.storeCenters(context, facetCenters);
            final CLBuffer<IntBuffer> bufferFacetData = GPUDataGenerator.generateFacets(context, bufferFacetCenters, ps);
            final CLBuffer<FloatBuffer> bufferDeformations = GPUDataGenerator.generateDeformations(context, defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle), ps);
            result = ((ScenarioOpenCL_NO_GPU) sc).compute(images[0], images[1], bufferFacetData, bufferFacetCenters, bufferDeformations, ps);
        } else if (sc instanceof ScenarioOpenCL_LD) {
            final int[] facetData = tc.generateFacetData(facetCenters, ps.getValue(Parameter.FACET_SIZE));
            final float[] defomationLimitsFull = repeatArray(defomationLimitsSingle, facetCount);
            final int[] deformationCountsFull = repeatArray(tc.generateDeformationCounts(defomationLimitsSingle), facetCount);
            result = ((ScenarioOpenCL_LD) sc).compute(images[0], images[1], facetData, facetCenters, defomationLimitsFull, deformationCountsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_LD_GPU) {
            final CLBuffer<FloatBuffer> bufferFacetCenters = GPUDataGenerator.storeCenters(context, facetCenters);
            final CLBuffer<IntBuffer> bufferFacetData = GPUDataGenerator.generateFacets(context, bufferFacetCenters, ps);
            final float[] defomationLimitsFull = repeatArray(defomationLimitsSingle, facetCount);
            final int[] deformationCountsFull = repeatArray(tc.generateDeformationCounts(defomationLimitsSingle), facetCount);
            result = ((ScenarioOpenCL_LD_GPU) sc).compute(images[0], images[1], bufferFacetData, bufferFacetCenters, defomationLimitsFull, deformationCountsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_LF) {
            final float[] deformationsFull = repeatArray(tc.generateDeformations(defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle)), facetCount);
            result = ((ScenarioOpenCL_LF) sc).compute(images[0], images[1], facetCenters, deformationsFull, ps);
        } else if (sc instanceof ScenarioOpenCL_LF_GPU) {
            final CLBuffer<FloatBuffer> bufferDeformations = GPUDataGenerator.generateDeformations(context, defomationLimitsSingle, tc.generateDeformationCounts(defomationLimitsSingle), ps);
            result = ((ScenarioOpenCL_LF_GPU) sc).compute(images[0], images[1], facetCenters, bufferDeformations, ps);
        } else if (sc instanceof ScenarioOpenCL_LFD) {
            final float[] defomationLimitsFull = repeatArray(defomationLimitsSingle, facetCount);
            final int[] deformationCountsFull = repeatArray(tc.generateDeformationCounts(defomationLimitsSingle), facetCount);
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

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new LinkedList<>();

        // OpenCL limits performance test
        scenarios.add(new CL_NO_2DInt("CL_NO_2DInt", contextHandler));  // basic variants - full data        
        scenarios.add(new CL_NO_2DImage(contextHandler));
        scenarios.add(new CL_NO_2DImageV(contextHandler));
        scenarios.add(new CL_NO_1DImageLL(contextHandler));
        scenarios.add(new CL_NO_1D_I_V_LL(contextHandler));

        scenarios.add(new CL_NO_2DInt_GPU("CL_NO_2DInt_GPU", contextHandler)); // offline GPU generated data
        scenarios.add(new CL_NO_2DImage_GPU(contextHandler));
        scenarios.add(new CL_NO_2DImageV_GPU(contextHandler));
        scenarios.add(new CL_NO_1DImageLL_GPU(contextHandler));
        scenarios.add(new CL_NO_1D_I_V_LL_GPU(contextHandler));

        scenarios.add(new CL_L_2DInt("CL_L_2DInt", contextHandler)); // variants with both facet and deformation limits
        scenarios.add(new CL_L_2DImage(contextHandler));
        scenarios.add(new CL_L_2DImageV(contextHandler));
        scenarios.add(new CL_L_1DImageLL(contextHandler));
        scenarios.add(new CL_L_1D_I_V_LL(contextHandler));

        scenarios.add(new CL_LD_2DInt("CL_LD_2DInt", contextHandler)); // variants with facet data and deformation limits
        scenarios.add(new CL_LD_2DImage(contextHandler));
        scenarios.add(new CL_LD_2DImageV(contextHandler));
        scenarios.add(new CL_LD_1DImageLL(contextHandler));
        scenarios.add(new CL_LD_1D_I_V_LL(contextHandler));

        scenarios.add(new CL_LD_2DInt_GPU("CL_LD_2DInt_GPU", contextHandler)); // offline GPU generated data
        scenarios.add(new CL_LD_2DImage_GPU(contextHandler));
        scenarios.add(new CL_LD_2DImageV_GPU(contextHandler));
        scenarios.add(new CL_LD_1DImageLL_GPU(contextHandler));
        scenarios.add(new CL_LD_1D_I_V_LL_GPU(contextHandler));

        scenarios.add(new CL_LF_2DInt("CL_LF_2DInt", contextHandler)); // variants with facet limits and deformation data
        scenarios.add(new CL_LF_2DImage(contextHandler));
        scenarios.add(new CL_LF_2DImageV(contextHandler));
        scenarios.add(new CL_LF_1DImageLL(contextHandler));
        scenarios.add(new CL_LF_1D_I_V_LL(contextHandler));

        scenarios.add(new CL_LF_2DInt_GPU("CL_LF_2DInt_GPU", contextHandler)); // offline GPU generated data
        scenarios.add(new CL_LF_2DImage_GPU(contextHandler));
        scenarios.add(new CL_LF_2DImageV_GPU(contextHandler));
        scenarios.add(new CL_LF_1DImageLL_GPU(contextHandler));
        scenarios.add(new CL_LF_1D_I_V_LL_GPU(contextHandler));

        return scenarios;
    }

}
