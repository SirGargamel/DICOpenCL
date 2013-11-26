package cz.tul.dic.opencl.test.gen;

/**
 *
 * @author Petr Jecmen
 */
public class CustomMath {

    public static int power2(int a) {
        return 32 - Integer.numberOfLeadingZeros(a - 1);
    }
    
    public static int subfact(int f, int c) {
        if (c > f) {
            throw new IllegalArgumentException("Factorized number must be greater than count. [" + f + "," + c + "]");
        }
        
        int result = 1;
        for (int i = 0; i < c; i++) {
            result *= f - i;
        }
        return result;
    }
}
