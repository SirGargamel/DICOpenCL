package cz.tul.dic.opencl.test.gen.data;

import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;

/**
 *
 * @author Petr Jecmen
 */
public class ShiftedImageCase extends TestCase {

    private static final int ITEM_WIDTH = 10;
    private static final int ITEM_HEIGHT = 10;
    private static final int ITEM_INTENSITY = 255;
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
        final int centerX = (width / 2) - (ITEM_WIDTH / 2);
        final int centerY = (height / 2) - (ITEM_HEIGHT / 2);
        int index;
        for (int x = 0; x < ITEM_WIDTH; x++) {
            for (int y = 0; y < ITEM_HEIGHT; y++) {
                index = Utils.compute1DIndex(centerX + x, centerY + y, width);
                if (index >= 0 && index < imageA.length) {
                    imageA[index] = ITEM_INTENSITY;
                }
                index = Utils.compute1DIndex(centerX + x + shiftX, centerY + y + shiftY, width);
                if (index >= 0 && index < imageB.length) {
                    imageB[index] = ITEM_INTENSITY;
                }
            }
        }

        return result;
    }

    @Override
    public float[] generateDeformations(int deformationCount) {
        final float[] result = super.generateDeformations(deformationCount);

        // write shifts
        writeKnownDeformations(result, shiftX, shiftY, 0, 0, 0, 0);
        
        return result;
    }

    @Override
    public void checkResult(ScenarioResult result, int facetCount) {
        // check shifts
    }

}
