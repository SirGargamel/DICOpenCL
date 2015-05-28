package cz.tul.dic.test.opencl.testcase;

import cz.tul.dic.test.opencl.scenario.Utils;
import cz.tul.dic.test.opencl.scenario.ScenarioResult;

/**
 *
 * @author Petr Jecmen
 */
public class ShiftedImageCase extends TestCase {

    private final int shiftX, shiftY;

    public ShiftedImageCase(int shiftX, int shiftY) {
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }

    @Override
    public int[][] generateImages(int width, int height) {
        final int[][] result = super.generateImages(width, height);

        // shift second image
        final int length = width * height;
        final int[] imageA = result[0];
        final int[] imageB = result[1];
        int indexA, indexB;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                indexA = Utils.compute1DIndex(x - shiftX, y - shiftY, width);
                indexB = Utils.compute1DIndex(x, y, width);

                if (isValidIndex(indexA, length)) {
                    imageB[indexB] = imageA[indexA];
                } else {
                    imageB[indexB] = -1;
                }
            }
        }

        return result;
    }

    private static boolean isValidIndex(final int index, final int length) {
        return index >= 0 && index < length;
    }

    @Override
    public float[] generateDeformations(final float[] deformationLimits, final int[] deformationCounts) {
        final float[] result = super.generateDeformations(deformationLimits, deformationCounts);

        // write shifts        
        result[0] = shiftX;
        result[1] = shiftY;
        result[2] = 0;
        result[3] = 0;
        result[4] = 0;
        result[5] = 0;

        return result;
    }

    @Override
    public void checkResult(ScenarioResult result, int facetCount) {
        // check shifts
    }

}
