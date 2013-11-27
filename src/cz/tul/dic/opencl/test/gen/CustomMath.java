package cz.tul.dic.opencl.test.gen;

/**
 *
 * @author Petr Jecmen
 */
public class CustomMath {

    public static int power2(int a) {
        return 32 - Integer.numberOfLeadingZeros(a - 1);
    }
}
