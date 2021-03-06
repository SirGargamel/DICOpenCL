package cz.tul.dic.test.opencl;

import cz.tul.dic.test.opencl.utils.CustomMath;
import cz.tul.dic.test.opencl.utils.Utils;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class TestCase {

    private static final Logger LOG = Logger.getGlobal();
    public static final float DEFORMATION_ABS_MAX_0 = 1;
    public static final float DEFORMATION_ABS_MAX_1 = 0.1f;

    public int[][] generateImages(final int width, final int height) {
        final int length = width * height;
        final int[] result = new int[length];

        Random rnd = new Random();
        int val;
        for (int j = 0; j < length; j++) {
            val = rnd.nextInt(256);
            result[j] = val;
        }

        return new int[][]{result, result};
    }

    public float[] generateFacetCenters(final int width, final int height, final int size, final int countMultiplier) {
        final int count = (width / size) * (height / size) * countMultiplier;
        final float[] result = new float[count * 2];

        final Random rnd = new Random();
        int centerX, centerY, baseIndex;
        final double sizeHalf = Math.ceil(size / 2.0);
        final int offset = (int) (sizeHalf + DEFORMATION_ABS_MAX_0 + (2 * DEFORMATION_ABS_MAX_1 * sizeHalf));
        for (int i = 0; i < count; i++) {
            baseIndex = i * 2;
            centerX = rnd.nextInt(width - (2 * offset)) + offset;
            centerY = rnd.nextInt(height - (2 * offset)) + offset;
            result[baseIndex] = centerX;
            result[baseIndex + 1] = centerY;
        }

        return result;
    }

    public int[] generateFacetData(final float[] facetCenters, final int size) {
        final int facetArraySize = Utils.calculateFacetArraySize(size);
        final int count = facetCenters.length / 2;
        final int[] result = new int[count * facetArraySize];

        int centerX, centerY, baseIndex;
        int halfSize = size / 2;
        int index;
        for (int i = 0; i < count; i++) {
            baseIndex = i * facetArraySize;

            centerX = (int) Math.round(facetCenters[i * 2]);
            centerY = (int) Math.round(facetCenters[i * 2 + 1]);

            // generate points
            index = 0;
            for (int dy = 0; dy < size; dy++) {
                for (int dx = 0; dx < size; dx++) {
                    result[baseIndex + index] = centerX + dx - halfSize;
                    result[baseIndex + index + 1] = centerY + dy - halfSize;
                    index += 2;
                }
            }
        }

        return result;
    }

    public float[] generateDeformations(final float[] deformationLimits, final int[] deformationCounts) {
        final int defCount = deformationCounts[deformationCounts.length - 1];
        final float[] result = new float[6 * defCount];
        final float[] deformation = new float[6];

        int resultsBase, counter;
        for (int i = 0; i < defCount; i++) {
            resultsBase = i * 6;

            counter = i;
            deformation[0] = counter % deformationCounts[0];
            counter = counter / deformationCounts[0];
            deformation[1] = counter % deformationCounts[1];
            counter = counter / deformationCounts[1];
            deformation[2] = counter % deformationCounts[2];
            counter = counter / deformationCounts[2];
            deformation[3] = counter % deformationCounts[3];
            counter = counter / deformationCounts[3];
            deformation[4] = counter % deformationCounts[4];
            counter = counter / deformationCounts[4];
            deformation[5] = counter % deformationCounts[5];
            counter = counter / deformationCounts[5];
            deformation[0] = deformationLimits[0] + deformation[0] * deformationLimits[2];
            deformation[1] = deformationLimits[3] + deformation[1] * deformationLimits[5];
            deformation[2] = deformationLimits[6] + deformation[2] * deformationLimits[8];
            deformation[3] = deformationLimits[9] + deformation[3] * deformationLimits[11];
            deformation[4] = deformationLimits[12] + deformation[4] * deformationLimits[14];
            deformation[5] = deformationLimits[15] + deformation[5] * deformationLimits[17];

            System.arraycopy(deformation, 0, result, resultsBase, 6);
        }

        return result;
    }

    public float[] generateDeformationLimits(final int deformationCount) {
        final float[] deformationLimits = new float[Utils.DEFORMATION_DIM * 3];

        for (int dim = 0; dim < Utils.DEFORMATION_DIM; dim++) {
            deformationLimits[dim * 3] = 0;
            deformationLimits[dim * 3 + 1] = DEFORMATION_ABS_MAX_1;
        }
        deformationLimits[1] = DEFORMATION_ABS_MAX_0;
        deformationLimits[4] = DEFORMATION_ABS_MAX_0;

        int rest = deformationCount;
        int div, optimal;
        boolean found;
        for (int dim = 0; dim < Utils.DEFORMATION_DIM - 1; dim++) {
            if (rest <= 1) {
                deformationLimits[dim * 3 + 1] = deformationLimits[dim * 3];
                deformationLimits[dim * 3 + 2] = 0;
            } else {
                found = false;
                optimal = (int) Math.pow(rest, 1 / (double) (Utils.DEFORMATION_DIM - dim));
                for (int n = optimal; n > 1; n--) {
                    div = rest / n;
                    if (n * div == rest) {
                        deformationLimits[dim * 3 + 2] = (float) ((deformationLimits[dim * 3 + 1] - deformationLimits[dim * 3]) / (double) (n - 1));
                        rest /= n;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    for (int n = optimal; n <= rest; n++) {
                        div = rest / n;
                        if (n * div == rest) {
                            if (n > 1) {
                                deformationLimits[dim * 3 + 2] = (float) ((deformationLimits[dim * 3 + 1] - deformationLimits[dim * 3]) / (double) (n - 1));
                            } else {
                                deformationLimits[dim * 3 + 1] = deformationLimits[dim * 3];
                                deformationLimits[dim * 3 + 2] = 0;
                            }
                            rest /= n;
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        LOG.warning("Could not find the proper divider.");
                    }
                }
            }
        }

        final int base = (Utils.DEFORMATION_DIM - 1) * 3;
        if (rest > 1) {
            deformationLimits[base + 2] = (float) ((deformationLimits[base + 1] - deformationLimits[base]) / (double) (rest - 1));
        } else {
            deformationLimits[base + 1] = deformationLimits[base];
            deformationLimits[base + 2] = 0;
        }

        return deformationLimits;
    }

    public int[] generateDeformationCounts(final float[] deformationLimits) {
        final int l = deformationLimits.length / 3;
        final int[] counts = new int[l + 1];

        int total = 1;
        for (int i = 0; i < l; i++) {
            counts[i] = (int) Math.round((deformationLimits[i * 3 + 1] - deformationLimits[i * 3]) / deformationLimits[i * 3 + 2]) + 1;
            total *= counts[i];
        }
        counts[l] = total;
        return counts;
    }

    public void checkResult(final ScenarioResult result, final int facetCount) {
        final float[] coeffs = result.getResultData();

        if (coeffs == null) {
            result.markAsInvalidFixedPart();
        } else {
            int oneCount = 0;
            for (int i = 0; i < coeffs.length; i++) {
                if (CustomMath.areEqual(coeffs[i], 1.0f, Constants.EPS_PRECISE)) {
                    oneCount++;
//                    System.out.print(i + "; ");
                }
            }
//            System.out.println();
            if (oneCount != (facetCount)) {
                result.markAsInvalidFixedPart();
            }
        }
    }

}
