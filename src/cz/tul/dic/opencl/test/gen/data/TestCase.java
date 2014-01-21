package cz.tul.dic.opencl.test.gen.data;

import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.util.Random;

/**
 *
 * @author Petr Jecmen
 */
public class TestCase {

    public int[][] generateImages(final int width, final int height) {
        final int length = width * height;
        final int[] result = new int[length];

        Random rnd = new Random();
        int val;
        for (int j = 0; j < length; j++) {
            val = rnd.nextInt(256);
            result[j] = val + (val << 8) + (val << 16) + (val << 24);
        }

        return new int[][]{result, result};
    }

    public int[] generateFacetCenters(final int width, final int height, final int size) {
        final int count = (width / size) * (height / size);
        final int[] result = new int[count * 2];

        Random rnd = new Random();
        int centerX, centerY, baseIndex;
        int offset = (int) (Utils.DEFORMATION_ABS_MAX_0 * 4);
        for (int i = 0; i < count; i++) {
            baseIndex = i * 2;
            centerX = rnd.nextInt(width - (2 * offset) - size) + offset;
            centerY = rnd.nextInt(height - (2 * offset) - size) + offset;
            result[baseIndex] = centerX;
            result[baseIndex + 1] = centerY;
        }

        return result;
    }

    public int[] generateFacetData(final int[] facetCenters, final int size) {
        final int facetArraySize = Utils.calculateFacetArraySize(size);
        final int count = facetCenters.length / 2;
        final int[] result = new int[count * facetArraySize];

        int centerX, centerY, baseIndex;
        int halfSize = size / 2;
        int index;
        for (int i = 0; i < count; i++) {
            baseIndex = i * facetArraySize;

            centerX = facetCenters[i * 2];
            centerY = facetCenters[i * 2 + 1];

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

    public float[] generateDeformations(final int deformationCount) {
        final float[] deformations = new float[deformationCount * Utils.DEFORMATION_DIM];

        Random rnd = new Random();
        float val;
        int base;
        for (int i = 0; i < deformationCount; i++) {
            base = i * Utils.DEFORMATION_DIM;
            for (int j = 0; j < 2; j++) {
                val = rnd.nextFloat() * Utils.DEFORMATION_ABS_MAX_0 - (Utils.DEFORMATION_ABS_MAX_0 / 2);
                if (val == 0) {
                    val++;
                }
                deformations[base + j] = val;
            }
            for (int j = 2; j < Utils.DEFORMATION_DIM; j++) {
                val = (rnd.nextFloat() * Utils.DEFORMATION_ABS_MAX_1) - (Utils.DEFORMATION_ABS_MAX_1 * 0.5f);
                if (val == 0) {
                    val = Utils.DEFORMATION_ABS_MAX_1 * 0.01f;
                }
                deformations[base + j] = val;
            }
        }

        // known results
        writeKnownDeformations(deformations, 0, 0, 0, 0, 0, 0);

        return deformations;
    }

    protected void writeKnownDeformations(final float[] result, float... deformations) {
        final int lr = result.length;
        final int ld = deformations.length;
        for (int i = 0; i < ld; i++) {
            result[i] = deformations[i];
            result[lr - ld + i] = deformations[i];
        }
    }

    public void checkResult(final ScenarioResult result, final int facetCount) {
        final float[] coeffs = result.getResultData();

        if (coeffs == null) {
            result.markResultAsInvalidFixed();
        } else {
            int oneCount = 0;
            for (int i = 0; i < coeffs.length; i++) {
                if (CustomMath.areEqual(coeffs[i], 1.0f, Utils.EPS_PRECISE)) {
                    oneCount++;
                }
            }
            if (oneCount != (facetCount * 2)) {
                result.markResultAsInvalidFixed();
            }
        }
    }

}
