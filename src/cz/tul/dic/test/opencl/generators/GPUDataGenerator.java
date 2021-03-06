package cz.tul.dic.test.opencl.generators;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import cz.tul.dic.test.opencl.LimitsTest;
import cz.tul.dic.test.opencl.scenario.ContextHandler;
import cz.tul.dic.test.opencl.scenario.Parameter;
import cz.tul.dic.test.opencl.scenario.ParameterSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Lenam s.r.o.
 */
public class GPUDataGenerator {

    private static final String CL_EXTENSION = ".cl";
    private static final List<CLResource> memoryObjects;

    static {
        memoryObjects = new LinkedList<>();
    }
    
    public static Map<DataType, int[]> runGPUDataGenerationTest(
            final ContextHandler ch, final ParameterSet ps,
            final float[] defomationLimitsSingle, final int[] defomationCountsSingle, final float[] facetCenters) {
        long time;
        final Map<DataType, Map<Double, int[]>> times = new EnumMap<>(DataType.class);
        times.put(DataType.DEFORMATION, new TreeMap<>());
        times.put(DataType.FACET, new TreeMap<>());

        final int maxLws0 = ch.getDevice().getMaxWorkItemSizes()[0];
        final int maxTotal = ch.getDevice().getMaxWorkGroupSize();

        final CLCommandQueue queue = ch.getQueue();

        for (int lws0 = 1; lws0 <= maxLws0; lws0 *= 2) {
            time = System.nanoTime();
            GPUDataGenerator.generateDeformations(ch, defomationLimitsSingle, defomationCountsSingle, ps, lws0);
            queue.finish();
            times.get(DataType.DEFORMATION).put((System.nanoTime() - time) / 1000 / 1000.0, new int[]{lws0});
            GPUDataGenerator.resourceCleanup();
        }
        for (int lws0 = 1; lws0 <= maxLws0; lws0 *= 2) {
            for (int lws1 = 1; lws1 <= maxTotal / lws0; lws1 *= 2) {
                time = System.nanoTime();
                GPUDataGenerator.generateDeformations(ch, defomationLimitsSingle, defomationCountsSingle, ps, lws0, lws1);
                queue.finish();
                times.get(DataType.DEFORMATION).put((System.nanoTime() - time) / 1000 / 1000.0, new int[]{lws0, lws1});
                GPUDataGenerator.resourceCleanup();
            }
        }

        for (int lws0 = 1; lws0 <= maxLws0; lws0 *= 2) {
            time = System.nanoTime();
            GPUDataGenerator.generateFacets(ch, GPUDataGenerator.storeCenters(ch, facetCenters), ps, lws0);
            queue.finish();
            times.get(DataType.FACET).put((System.nanoTime() - time) / 1000 / 1000.0, new int[]{lws0});
            GPUDataGenerator.resourceCleanup();
        }
        for (int lws0 = 1; lws0 <= maxLws0; lws0 *= 2) {
            for (int lws1 = 1; lws1 <= maxTotal / lws0; lws1 *= 2) {
                time = System.nanoTime();
                GPUDataGenerator.generateFacets(ch, GPUDataGenerator.storeCenters(ch, facetCenters), ps, lws0, lws1);
                queue.finish();
                times.get(DataType.FACET).put((System.nanoTime() - time) / 1000 / 1000.0, new int[]{lws0, lws1});
                GPUDataGenerator.resourceCleanup();
            }
        }

        final Map<DataType, int[]> result = new EnumMap<>(DataType.class);
        result.put(DataType.FACET, times.get(DataType.FACET).values().iterator().next());
        result.put(DataType.DEFORMATION, times.get(DataType.DEFORMATION).values().iterator().next());

        return result;
    }

    public static CLBuffer<FloatBuffer> storeCenters(final ContextHandler context, final float[] facetCenters) {
        return createFloatBuffer(context, facetCenters, READ_ONLY);
    }

    public static CLBuffer<FloatBuffer> generateDeformations(final ContextHandler context, final float[] deformationLimitsSingle, final int[] deformationCountsSingle, final ParameterSet params, final int[] lws) {
        if (lws.length == 1) {
            return generateDeformations(context, deformationLimitsSingle, deformationCountsSingle, params, lws[0]);
        } else {
            return generateDeformations(context, deformationLimitsSingle, deformationCountsSingle, params, lws[0], lws[1]);
        }
    }

    public static CLBuffer<FloatBuffer> generateDeformations(final ContextHandler context, final float[] deformationLimitsSingle, final int[] deformationCountsSingle, final ParameterSet params, final int lws0) {
        final CLKernel kernel = readKernel("generateDeformations", context.getContext());
        final CLBuffer<FloatBuffer> bufferDeformationLimits = createFloatBuffer(context, deformationLimitsSingle, READ_ONLY);
        final CLBuffer<IntBuffer> bufferDeformationCounts = createIntBuffer(context, deformationCountsSingle, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(context, params.getValue(Parameter.FACET_COUNT) * params.getValue(Parameter.DEFORMATION_COUNT) * 6);

        kernel.putArgs(bufferDeformationLimits, bufferDeformationCounts, bufferResult)
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(params.getValue(Parameter.FACET_COUNT))
                .rewind();
        // prepare work sizes                
        final int deformationsGlobalWorkSize = roundUp(lws0, params.getValue(Parameter.DEFORMATION_COUNT));
        // execute kernel                
        final CLCommandQueue queue = context.getQueue();
        queue.putWriteBuffer(bufferDeformationLimits, false);
        queue.putWriteBuffer(bufferDeformationCounts, false);
        queue.put1DRangeKernel(kernel, 0, deformationsGlobalWorkSize, lws0);

        return bufferResult;
    }

    public static CLBuffer<FloatBuffer> generateDeformations(final ContextHandler context, final float[] deformationLimitsSingle, final int[] deformationCountsSingle, final ParameterSet params, final int lws0, final int lws1) {
        final CLKernel kernelA = readKernel("deformationGenerate", context.getContext());
        final CLKernel kernelB = readKernel("deformationCopy", context.getContext());
        final CLBuffer<FloatBuffer> bufferDeformationLimits = createFloatBuffer(context, deformationLimitsSingle, READ_ONLY);
        final CLBuffer<IntBuffer> bufferDeformationCounts = createIntBuffer(context, deformationCountsSingle, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(context, params.getValue(Parameter.FACET_COUNT) * params.getValue(Parameter.DEFORMATION_COUNT) * 6);

        kernelA.putArgs(bufferDeformationLimits, bufferDeformationCounts, bufferResult)
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .rewind();
        kernelB.putArgs(bufferResult)
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(params.getValue(Parameter.FACET_COUNT))
                .rewind();
        // execute kernel                
        final CLCommandQueue queue = context.getQueue();
        queue.putWriteBuffer(bufferDeformationLimits, false);
        queue.putWriteBuffer(bufferDeformationCounts, false);
        queue.put1DRangeKernel(kernelA, 0, roundUp(lws0, params.getValue(Parameter.DEFORMATION_COUNT)), lws0);
        queue.put2DRangeKernel(kernelB, 0, 0, roundUp(lws0, params.getValue(Parameter.DEFORMATION_COUNT)), roundUp(lws1, params.getValue(Parameter.FACET_COUNT)), lws0, lws1);


        return bufferResult;
    }

    public static CLBuffer<IntBuffer> generateFacets(final ContextHandler context, final CLBuffer<FloatBuffer> bufferFacetCenters, final ParameterSet params, final int[] lws) {
        if (lws.length == 1) {
            return generateFacets(context, bufferFacetCenters, params, lws[0]);
        } else {
            return generateFacets(context, bufferFacetCenters, params, lws[0], lws[1]);
        }

    }

    public static CLBuffer<IntBuffer> generateFacets(final ContextHandler context, final CLBuffer<FloatBuffer> bufferFacetCenters, final ParameterSet params, final int lws0) {
        final CLKernel kernel = readKernel("generateFacets", context.getContext());

        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLBuffer<IntBuffer> bufferResult = createIntBuffer(context, facetCount * facetSize * facetSize * 2);

        kernel.putArgs(bufferFacetCenters, bufferResult)
                .putArg(facetCount)
                .putArg(facetSize)
                .rewind();
        // prepare work sizes                
        final int facetsGlobalWorkSize = roundUp(lws0, facetCount);
        // execute kernel                
        final CLCommandQueue queue = context.getQueue();
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.put1DRangeKernel(kernel, 0, facetsGlobalWorkSize, lws0);

        return bufferResult;
    }

    public static CLBuffer<IntBuffer> generateFacets(final ContextHandler context, final CLBuffer<FloatBuffer> bufferFacetCenters, final ParameterSet params, final int lws0, final int lws1) {
        final CLKernel kernel = readKernel("generateFacets2D", context.getContext());

        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLBuffer<IntBuffer> bufferResult = createIntBuffer(context, facetCount * facetSize * facetSize * 2);

        kernel.putArgs(bufferFacetCenters, bufferResult)
                .putArg(facetCount)
                .putArg(facetSize)
                .rewind();
        // execute kernel                
        final CLCommandQueue queue = context.getQueue();
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.put2DRangeKernel(kernel, 0, 0, roundUp(lws0, facetCount), roundUp(lws1, facetSize * facetSize), lws0, lws1);

        return bufferResult;
    }

    private static CLBuffer<FloatBuffer> createFloatBuffer(final ContextHandler context, final int bufferLength, CLMemory.Mem... params) {
        final CLBuffer<FloatBuffer> result = context.getContext().createFloatBuffer(bufferLength, params);
        memoryObjects.add(result);
        return result;
    }

    private static CLBuffer<FloatBuffer> createFloatBuffer(final ContextHandler context, final float[] data, CLMemory.Mem... params) {
        final CLBuffer<FloatBuffer> result = context.getContext().createFloatBuffer(data.length, params);
        fillBuffer(result.getBuffer(), data);
        memoryObjects.add(result);
        return result;
    }

    private static void fillBuffer(final FloatBuffer buffer, final float[] data) {
        for (float f : data) {
            buffer.put(f);
        }
        buffer.rewind();
    }

    private static CLBuffer<IntBuffer> createIntBuffer(final ContextHandler context, final int bufferLength, CLMemory.Mem... params) {
        final CLBuffer<IntBuffer> result = context.getContext().createIntBuffer(bufferLength, params);
        memoryObjects.add(result);
        return result;
    }

    private static CLBuffer<IntBuffer> createIntBuffer(final ContextHandler context, final int[] data, CLMemory.Mem... params) {
        final CLBuffer<IntBuffer> result = context.getContext().createIntBuffer(data.length, params);
        fillBuffer(result.getBuffer(), data);
        memoryObjects.add(result);
        return result;
    }

    private static void fillBuffer(final IntBuffer buffer, final int[] data) {
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
    }

    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;

        int result;
        if (r == 0) {
            result = globalSize;
        } else {
            result = globalSize + groupSize - r;
        }

        return result;
    }

    private static CLKernel readKernel(final String kernelName, final CLContext context) {
        try (BufferedReader bin = new BufferedReader(new InputStreamReader(GPUDataGenerator.class.getResourceAsStream(kernelName.concat(CL_EXTENSION))))) {
            final StringBuilder sb = new StringBuilder();
            while (bin.ready()) {
                sb.append(bin.readLine());
                sb.append("\n");
            }
            final CLProgram program = context.createProgram(sb.toString()).build();
            memoryObjects.add(program);
            final CLKernel kernel = program.createCLKernel(kernelName);
            memoryObjects.add(kernel);

            return kernel;
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static void resourceCleanup() {
        for (CLResource m : memoryObjects) {
            if (!m.isReleased()) {
                m.release();
            }
        }
        memoryObjects.clear();
    }

    private GPUDataGenerator() {
    }
    
    public static enum DataType {

        DEFORMATION,
        FACET
    }
}
