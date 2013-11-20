package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.opencl.CustomMath;
import cz.tul.dic.opencl.test.gen.scenario.ComputeJavaIntDirect;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import java.io.File;
import java.io.IOException;
import static java.lang.System.nanoTime;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Petr Jecmen
 */
public class PerformanceTest {

    private static final int IMAGE_WIDTH_MIN = 256;
    private static final int IMAGE_WIDTH_MAX = 2048;
    private static final int IMAGE_HEIGHT_MIN = 192;
    private static final int FACET_SIZE_MIN = 10;
    private static final int FACET_SIZE_MAX = 160;
    private static final int DEFORMATION_COUNT_MIN = 100;
    private static final int DEFORMATION_COUNT_MAX = 800;

    public static void computeImageFillTest() throws IOException {
        // select best GPU (non-integrated one for laptops)
        CLPlatform.initialize();
        final Filter<CLPlatform> filter = new Filter<CLPlatform>() {
            @Override
            public boolean accept(CLPlatform item) {
                return item.listCLDevices(Type.CPU).length == 0;
            }
        };
        CLPlatform platform = CLPlatform.getDefault(filter);

        if (platform == null) {
            platform = CLPlatform.getDefault();
        }

        CLContext context = CLContext.create(platform, Type.GPU);
        out.println("created " + context);

        CLDevice device = context.getMaxFlopsDevice(Type.GPU);
        out.println("Using " + device);

        final List<Scenario> scenarios = prepareScenarios(device);

        int[][] images;
        int[] facets, deformations;
        long time;
        ParameterSet ps;
        boolean result;
        int scenarioCount = 0;
        try {
            // execute scenarios
            int w, h;
            for (int dim = 1; dim <= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN; dim++) {
                w = dim * IMAGE_WIDTH_MIN;
                h = dim * IMAGE_HEIGHT_MIN;
                images = generateImages(w, h);

                for (int s = FACET_SIZE_MIN; s <= FACET_SIZE_MAX; s *= 2) {
                    facets = generateFacets(w, h, s);

                    for (int d = DEFORMATION_COUNT_MIN; d <= DEFORMATION_COUNT_MAX; d *= 2) {
                        deformations = generateDeformations(d);

                        for (Scenario sc : scenarios) {
                            sc.reset();
                            while (sc.hasNext()) {
                                ps = new ParameterSet();
                                ps.addParameter(Parameter.IMAGE_WIDTH, w);
                                ps.addParameter(Parameter.IMAGE_HEIGHT, h);
                                ps.addParameter(Parameter.FACET_SIZE, s);
                                ps.addParameter(Parameter.DEFORMATION_COUNT, d);

                                time = nanoTime();
                                result = sc.computeScenario(images[0], images[1], facets, deformations, ps, device);
                                time = nanoTime() - time;

                                if (result) {
                                    System.out.println("Finished " + sc.getDescription() + " in " + (time / 1000) + "ms with params " + ps);
                                    DataStorage.storeData(ps, time);
                                } else {
                                    System.out.println("Failed " + sc.getDescription() + " with params " + ps);
                                }
                            }
                            scenarioCount = sc.getVariantCount();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        } finally {
            // cleanup all resources associated with this context.
            context.release();
        }

        DataStorage.setScenarioCount(scenarioCount);
        
        int lineCount = scenarios.size();
        lineCount *= IMAGE_WIDTH_MAX / IMAGE_WIDTH_MIN;        
        lineCount *= CustomMath.power2(FACET_SIZE_MAX / FACET_SIZE_MIN) + 1;
        lineCount *= CustomMath.power2(DEFORMATION_COUNT_MAX / DEFORMATION_COUNT_MIN) + 1;
        DataStorage.setLineCount(lineCount);
        
        DataStorage.exportData(new File("D:\\testData.csv"));
    }

    private static List<Scenario> prepareScenarios(final CLDevice device) throws IOException {
        final List<Scenario> scenarios = new ArrayList<>(1);

        scenarios.add(new ComputeJavaIntDirect(device));

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

    private static int[] generateFacets(final int width, final int height, final int size) {
        final int count = (width / size) * (height / size);
        final int[] result = new int[count * size * size];

        Random rnd = new Random();
        int baseX, baseY, base;
        int offset = 5;
        for (int i = 0; i < count; i++) {
            base = i * size * size;
            // generate baseX and baseY            
            baseX = rnd.nextInt(width - (2 * offset)) + offset;
            baseY = rnd.nextInt(height - (2 * offset)) + offset;
            // generate points
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    result[base + (x * size) + y] = ((baseX + x) * size) + (baseY + y);
                }
            }
        }

        return result;
    }

    private static int[] generateDeformations(final int deformationCount) {
        final int[] result = new int[deformationCount * 2];

        Random rnd = new Random();
        for (int i = 0; i < deformationCount; i++) {
            result[i * 2] = rnd.nextInt(11) - 5;
            result[(i * 2) + 1] = rnd.nextInt(11) - 5;
        }

        return result;
    }

}
