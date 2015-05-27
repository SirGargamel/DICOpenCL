package cz.tul.dic.test.opencl.generator;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import cz.tul.dic.test.opencl.test.gen.ContextHandler;
import cz.tul.dic.test.opencl.test.gen.Parameter;
import cz.tul.dic.test.opencl.test.gen.ParameterSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

    public static CLBuffer<FloatBuffer> storeCenters(final ContextHandler context, final float[] facetCenters) {
        return createFloatBuffer(context, facetCenters, READ_ONLY);
    }

    public static CLBuffer<FloatBuffer> generateDeformations(final ContextHandler context, final float[] deformationLimitsSingle, final int[] deformationCountsSingle, final ParameterSet params) {
        final CLKernel kernel = readKernel("generateDeformations", context.getContext());
        final CLBuffer<FloatBuffer> bufferDeformationLimits = createFloatBuffer(context, deformationLimitsSingle, READ_ONLY);
        final CLBuffer<IntBuffer> bufferDeformationCounts = createIntBuffer(context, deformationCountsSingle, READ_ONLY);
        final CLBuffer<FloatBuffer> bufferResult = createFloatBuffer(context, params.getValue(Parameter.FACET_COUNT) * params.getValue(Parameter.DEFORMATION_COUNT) * 6);

        kernel.putArgs(bufferDeformationLimits, bufferDeformationCounts, bufferResult)
                .putArg(params.getValue(Parameter.DEFORMATION_COUNT))
                .putArg(params.getValue(Parameter.FACET_COUNT))
                .rewind();
        // prepare work sizes        
        final int lws0 = 128;
        final int deformationsGlobalWorkSize = roundUp(lws0, params.getValue(Parameter.DEFORMATION_COUNT));
        // execute kernel                
        final CLCommandQueue queue = context.getQueue();
        queue.putWriteBuffer(bufferDeformationLimits, false);
        queue.putWriteBuffer(bufferDeformationCounts, false);
        queue.put1DRangeKernel(kernel, 0, deformationsGlobalWorkSize, lws0);

        return bufferResult;
    }

    public static CLBuffer<IntBuffer> generateFacets(final ContextHandler context, final CLBuffer<FloatBuffer> bufferFacetCenters, final ParameterSet params) {
        final CLKernel kernel = readKernel("generateFacets", context.getContext());        

        final int facetSize = params.getValue(Parameter.FACET_SIZE);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final CLBuffer<IntBuffer> bufferResult = createIntBuffer(context, facetCount * facetSize * facetSize * 2);

        kernel.putArgs(bufferFacetCenters, bufferResult)
                .putArg(facetCount)
                .putArg(facetSize)
                .rewind();
        // prepare work sizes        
        final int lws0 = 8;
        final int facetsGlobalWorkSize = roundUp(lws0, facetCount);
        // execute kernel                
        final CLCommandQueue queue = context.getQueue();
        queue.putWriteBuffer(bufferFacetCenters, false);
        queue.put1DRangeKernel(kernel, 0, facetsGlobalWorkSize, lws0);

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
            final CLKernel kernelReduce = program.createCLKernel(kernelName);
            memoryObjects.add(kernelReduce);

            return kernelReduce;
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static float[] readBuffer(final FloatBuffer buffer) {
        buffer.rewind();
        float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }
    
    private static int[] readBuffer(final IntBuffer buffer) {
        buffer.rewind();
        int[] result = new int[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
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
}
