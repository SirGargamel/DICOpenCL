package cz.tul.dic.opencl.test.gen;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Utils {

    public static final int DEFORMATION_DIM = 6;
    public static final float EPS_NORMAL = 0.01f;
    public static final float EPS_PRECISE = 0.001f;
    public static final float DEFORMATION_ABS_MAX_0 = 5;
    public static final float DEFORMATION_ABS_MAX_1 = 0.1f; 

    public static int calculateFacetArraySize(final int facetSize) {
        return (facetSize * facetSize) * 2;
    }

    public static int calculateFacetArea(final int facetSize) {
        return facetSize * facetSize;
    }

}
