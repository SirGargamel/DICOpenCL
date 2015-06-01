package cz.tul.dic.test.opencl.utils;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Utils {

    public static final int DEFORMATION_DIM = 6;
    public static final float DEFORMATION_ABS_MAX_0 = 1;
    public static final float DEFORMATION_ABS_MAX_1 = 0.1f;

    public static int calculateFacetArraySize(final int facetSize) {
        return (facetSize * facetSize) * 2;
    }

    public static int calculateFacetArea(final int facetSize) {
        return facetSize * facetSize;
    }

    public static int compute1DIndex(final int x, final int y, final int width) {
        return (y * width) + x;
    }

    public static float[] repeatArray(final float[] input, final int repetitionCount) {
        final int l = input.length;
        final float[] result = new float[l * repetitionCount];
        for (int i = 0; i < repetitionCount; i++) {
            System.arraycopy(input, 0, result, i * l, l);
        }
        return result;
    }

    public static int[] repeatArray(final int[] input, final int repetitionCount) {
        final int l = input.length;
        final int[] result = new int[l * repetitionCount];
        for (int i = 0; i < repetitionCount; i++) {
            System.arraycopy(input, 0, result, i * l, l);
        }
        return result;
    }

}
