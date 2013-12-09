package cz.tul.dic.opencl.test.gen;

/**
 *
 * @author Petr Jecmen
 */
public class CustomMath {

    public static int power2(int a) {
        return 32 - Integer.numberOfLeadingZeros(a - 1);
    }
    
    public static boolean areEqual(final float a, final float b, final float eps) {
        final float dif = Math.abs(a - b);
        return dif <= eps;
    }
}
