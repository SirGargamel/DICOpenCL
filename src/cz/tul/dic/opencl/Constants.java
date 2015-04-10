package cz.tul.dic.opencl;

import cz.tul.dic.opencl.test.gen.ContextHandler;

/**
 *
 * @author Petr Jecmen
 */
public interface Constants {

    float EPS_PRECISE = 0.0001F;
    float EPS_NORMAL = 0.05F;

    //  Devices for computation
    ContextHandler.DeviceType[] HW
//            = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.CPU};
            = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU};
//    ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.CPU, ContextHandler.DeviceType.iGPU};
//    ContextHandler.DeviceType[] HW = new ContextHandler.DeviceType[]{ContextHandler.DeviceType.GPU, ContextHandler.DeviceType.CPU, ContextHandler.DeviceType.iGPU};
//  Full task
//    int[][] IMAGE_SIZES = new int[][]{{128, 96}, {384, 256}, {768, 576}, {1280, 960}};
//    int[] DEFORMATION_COUNTS = new int[]{100, 200, 500, 1000};
//    int[] FACET_SIZES = new int[]{51, 35, 21, 9};
//  Large task
//    int[][] IMAGE_SIZES = new int[][]{{128, 96}, {384, 256}, {768, 576}};
//    int[] DEFORMATION_COUNTS = new int[]{100, 400, 1000};
//    int[] FACET_SIZES = new int[]{51, 21, 9};
//  Medium task
//    int[][] IMAGE_SIZES = new int[][]{{128, 96}, {384, 256}};
//    int[] DEFORMATION_COUNTS = new int[]{100, 400};
//    int[] FACET_SIZES = new int[]{35, 9};
//  Small task
//    int[][] IMAGE_SIZES = new int[][]{{128, 96}};
//    int[] DEFORMATION_COUNTS = new int[]{100};
//    int[] FACET_SIZES = new int[]{21};
//  Test task
//    int[][] IMAGE_SIZES = new int[][]{{70, 240}, {88, 240}};
    int[][] IMAGE_SIZES = new int[][]{{88, 240}};
//    int[] DEFORMATION_COUNTS = new int[]{200, 5000, 100000};
//    int[] DEFORMATION_COUNTS = new int[]{200, 5000};
//    int[] FACET_SIZES = new int[]{41, 21, 7};
    int[] DEFORMATION_COUNTS = new int[]{200};
    int[] FACET_SIZES = new int[]{21};
//  Real task 0 order
//     int[][] IMAGE_SIZES = new int[][]{{52, 52}, {143,143}};
//     int[] DEFORMATION_COUNTS = new int[]{200, 10000};
//     int[] FACET_SIZES = new int[]{17, 25};
//  Generic task
//     int[][] IMAGE_SIZES = new int[][]{{384, 256}};
//     int[] DEFORMATION_COUNTS = new int[]{100};
//     int[] FACET_SIZES = new int[]{9};
//  Tiny task
//    int[][] IMAGE_SIZES = new int[][]{{88, 240}};
//    int[] DEFORMATION_COUNTS = new int[]{5000};
//    int[] FACET_SIZES = new int[]{20};

}
