package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import cz.tul.dic.opencl.test.gen.scenario.Compute2DIntGpuDirect;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.File;
import java.io.IOException;
import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final int IMAGE_WIDTH_MIN = 128;
    private static final int IMAGE_WIDTH_MAX = 1024;
    private static final int IMAGE_HEIGHT_MIN = 192;
    private static final int FACET_SIZE_MIN = 10;
    private static final int FACET_SIZE_MAX = 40;
    private static final int DEFORMATION_COUNT_MIN = 200;
    private static final int DEFORMATION_COUNT_MAX = 800;
    private static final int DEFORMATION_ABS_MAX = 5;

    public static void computeImageFillTest() throws IOException {        
        final ContextHandler ch = new ContextHandler();

        final List<Scenario> scenarios = prepareScenarios(ch);

        int[][] images;
        float[] averages;
        int[] facets;
        float[] deformations;
        long time;
        ParameterSet ps;
        ScenarioResult result;
        int scenarioCount = 0;
        Scenario sc;
        try {
            // execute scenarios
            int w, h;
            for (int dim = 1; dim <= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN; dim++) {
                w = dim * IMAGE_WIDTH_MIN;
                h = dim * IMAGE_HEIGHT_MIN;
                images = generateImages(w, h);
                averages = calculateAverages(images);

                for (int s = FACET_SIZE_MIN; s <= FACET_SIZE_MAX; s *= 2) {
                    facets = generateFacets(w, h, s);

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
                                ps.addParameter(Parameter.FACET_COUNT, facets.length / (s * s * 2));
                                ps.addParameter(Parameter.DEFORMATION_COUNT, d);
                                ps.addParameter(Parameter.VARIANT, i);

                                time = nanoTime();
                                result = sc.compute(images[0], averages[0], images[1], averages[1], facets, deformations, ps);
                                time = nanoTime() - time;

                                if (result.getResultData() != null) {
                                    System.out.println("Finished " + sc.getDescription() + " in " + (time / 1000000) + "ms with params " + ps);
                                } else {
                                    System.out.println("Failed   " + sc.getDescription() + " with params " + ps);
                                    ch.reset();
                                }
                                result.setTotalTime(time);
                                DataStorage.storeData(ps, result);
                            }
                            scenarioCount = sc.getVariantCount();
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

        DataStorage.setScenarioCount(scenarioCount);

        int lineCount = scenarios.size();
        lineCount *= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN;
        lineCount *= CustomMath.power2(FACET_SIZE_MAX / FACET_SIZE_MIN) + 1;
        lineCount *= CustomMath.power2(DEFORMATION_COUNT_MAX / DEFORMATION_COUNT_MIN) + 1;
        DataStorage.setLineCount(lineCount);

        DataStorage.exportData(new File("D:\\testData.csv"));
    }

    private static List<Scenario> prepareScenarios(final ContextHandler contextHandler) throws IOException {
        final List<Scenario> scenarios = new ArrayList<>(1);

        scenarios.add(new Compute2DIntGpuDirect(contextHandler));

        return scenarios;
    }

    private static int[][] generateImages(final int width, final int height) {
        final int length = width * height;
        final int[][] result = new int[2][length];

        Random rnd = new Random();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < length; j++) {
                result[i][j] = rnd.nextInt(256);
            }
        }

        return result;
    }

    private static float[] calculateAverages(final int[][] images) {
        final float[] sum = new float[images.length];

        for (int img = 0; img < images.length; img++) {
            for (int i = 0; i < images[img].length; i++) {
                sum[img] += images[img][i];
            }
            sum[img] /= (float) images[img].length;
        }

        return sum;
    }

    private static int[] generateFacets(final int width, final int height, final int size) {
        final int count = (width / size) * (height / size);
        final int facetCordSize = size * size * 2;
        final int[] result = new int[count * facetCordSize];

        Random rnd = new Random();
        int x, y, divX, divY;
        int baseX, baseY, base;
        int offset = DEFORMATION_ABS_MAX;
        for (int i = 0; i < count; i++) {
            base = i * facetCordSize;
            // generate baseX and baseY of facet       
            baseX = rnd.nextInt(width - (2 * offset) - size) + offset;
            baseY = rnd.nextInt(height - (2 * offset) - size) + offset;
            // generate points
            for (int dy = 0; dy < size; dy++) {
                y = baseY + dy;
                divY = dy * size * 2;
                for (int dx = 0; dx < size; dx++) {
                    x = baseX + dx;
                    divX = dx * 2;
                    result[base + divY + divX] = x;
                    result[base + divY + divX + 1] = y;
                }
            }
        }

        return result;
    }

    private static float[] generateDeformations(final int deformationCount) {
        final float[] result = new float[deformationCount * 2];

//        Random rnd = new Random();
//        for (int i = 0; i < deformationCount; i++) {
//            result[i * 2] = rnd.nextInt(DEFORMATION_ABS_MAX) - (DEFORMATION_ABS_MAX / 2);
//            result[(i * 2) + 1] = rnd.nextInt(DEFORMATION_ABS_MAX) - (DEFORMATION_ABS_MAX / 2);
//        }
        return result;
    }

}
