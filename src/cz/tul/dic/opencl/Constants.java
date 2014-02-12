package cz.tul.dic.opencl;

import cz.tul.dic.opencl.test.gen.ContextHandler;

/**
 *
 * @author Petr Jecmen
 */
public interface Constants {

    public static final float EPS_PRECISE = 0.001F;
    public static final float EPS_NORMAL = 0.05F;

    //  Devices for computation
    public final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU};
//    public final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.CPU, ContextHandler.DeviceType.iGPU};
//    public final ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU, ContextHandler.DeviceType.iGPU, ContextHandler.DeviceType.CPU};
//  Full task
//    public final int[][] IMAGE_SIZES = new int[][]{{128, 96}, {384, 256}, {768, 576}, {1280, 960}};
//    public final int[] DEFORMATION_COUNTS = new int[]{100, 200, 500, 1000};
//    public final int[] FACET_SIZES = new int[]{51, 35, 21, 9};
//  Large task
    public final int[][] IMAGE_SIZES = new int[][]{{128, 96}, {384, 256}, {768, 576}};
    public final int[] DEFORMATION_COUNTS = new int[]{100, 400, 1000};
    public final int[] FACET_SIZES = new int[]{51, 21, 9};
//  Medium task
//    public final int[][] IMAGE_SIZES = new int[][]{{128, 96}, {384, 256}};
//    public final int[] DEFORMATION_COUNTS = new int[]{100, 400};
//    public final int[] FACET_SIZES = new int[]{35, 9};
//  Small task
//    public final int[][] IMAGE_SIZES = new int[][]{{128, 96}};
//    public final int[] DEFORMATION_COUNTS = new int[]{100};
//    public final int[] FACET_SIZES = new int[]{21};
//  Real task 1st order
//    public final int[][] IMAGE_SIZES = new int[][]{{44, 240}, {110, 712}};
//    public final int[] DEFORMATION_COUNTS = new int[]{500, 1000};
//    public final int[] FACET_SIZES = new int[]{5, 11, 21};
//  Real task 0 order
//    public final int[][] IMAGE_SIZES = new int[][]{{52, 52}, {143,143}};
//    public final int[] DEFORMATION_COUNTS = new int[]{200};
//    public final int[] FACET_SIZES = new int[]{17, 25};
//  Generic task
//    public final int[][] IMAGE_SIZES = new int[][]{{128, 96}};
//    public final int[] DEFORMATION_COUNTS = new int[]{500};
//    public final int[] FACET_SIZES = new int[]{5};

}
