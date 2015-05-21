package cz.tul.dic.test.opencl;

import cz.tul.dic.test.opencl.test.gen.ContextHandler;

/**
 *
 * @author Petr Jecmen
 */
public final class Constants {

    public static final float EPS_PRECISE = 0.0001F;
    public static final float EPS_NORMAL = 0.05F;

    //  Devices for computation
    public static final ContextHandler.DeviceType[] HW
            //            = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.CPU};
//            = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU};
//            = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU, ContextHandler.DeviceType.CPU};
                        = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU, ContextHandler.DeviceType.CPU, ContextHandler.DeviceType.iGPU};
    // item counts
    public static final int[][] IMAGE_SIZES
            //            = new int[][]{{88, 240}};
            = new int[][]{{88, 240}, {240, 240}};     // Standart test
    public static final int[] DEFORMATION_COUNTS
            //            = new int[]{200, 5000};
            //            = new int[]{10, 200};
            = new int[]{10, 50, 200, 1000};     // Standart test
    public static final int[] FACET_SIZES
            //            = new int[]{41, 21};
            = new int[]{61, 41, 21, 7};     // Standart test
    public static final int[] FACET_MULTI
            //            = new int[]{1, 2};
            = new int[]{1, 2, 4};     // Standart test

    private Constants() {
    }

}
