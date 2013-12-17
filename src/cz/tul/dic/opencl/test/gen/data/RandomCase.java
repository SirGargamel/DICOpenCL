package cz.tul.dic.opencl.test.gen.data;

import cz.tul.dic.opencl.test.gen.CustomMath;
import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.util.Random;

/**
 *
 * @author Petr Jecmen
 */
public class RandomCase implements TestCase {
    
    @Override
    public int[][] generateImages(final int width, final int height) {
        final int length = width * height;
        final int[] result = new int[length];

        Random rnd = new Random();
        for (int j = 0; j < length; j++) {
            result[j] = rnd.nextInt(256);
        }

        return new int[][] {result, result};
    }    

    @Override
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

    @Override
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

    @Override
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
        deformations[0] = 0;
        deformations[1] = 0;
        deformations[2] = 0;
        deformations[3] = 0;
        deformations[4] = 0;
        deformations[5] = 0;

        deformations[deformations.length - 6] = 0;
        deformations[deformations.length - 5] = 0;
        deformations[deformations.length - 4] = 0;
        deformations[deformations.length - 3] = 0;
        deformations[deformations.length - 2] = 0;
        deformations[deformations.length - 1] = 0;

        return deformations;
    }

    @Override
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
