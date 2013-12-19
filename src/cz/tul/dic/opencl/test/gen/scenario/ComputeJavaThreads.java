package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.Utils;
import cz.tul.dic.opencl.test.gen.Parameter;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class ComputeJavaThreads extends Scenario {

    private static final String NAME = "JavaThreads";
    private static final int COUNT_VARIANT = 2;
    private static final int COUNT_THREADS = Runtime.getRuntime().availableProcessors() - 1;
    private ExecutorService exec;
    private int currentVariant;

    public ComputeJavaThreads() throws IOException {
        super(NAME, null);

        currentVariant = 0;
    }

    @Override
    public ScenarioResult compute(int[] imageA, int[] imageB, int[] facetData, int[] facetCenters, float[] deformations, ParameterSet params) {
        // preparation
        exec = Executors.newFixedThreadPool(COUNT_THREADS);
        final int facetCount = params.getValue(Parameter.FACET_COUNT);
        final int deformationCount = params.getValue(Parameter.DEFORMATION_COUNT);
        final float[] results = new float[facetCount * deformationCount];
        // execution
        final int[] counts;
        final List<Worker> workers = new ArrayList<>(COUNT_THREADS);
        switch (currentVariant) {
            case 0:
                // per facet computation
                counts = generateCounts(COUNT_THREADS + 1, facetCount);
                params.addParameter(Parameter.LWS0, 1);
                params.addParameter(Parameter.LWS1, COUNT_THREADS);

                for (int i = 0; i < COUNT_THREADS; i++) {
                    workers.add(new WorkerPerFacet(
                            counts[i], counts[i + 1],
                            imageA, imageB,
                            params.getValue(Parameter.IMAGE_WIDTH),
                            facetData, facetCenters,
                            deformations,
                            params.getValue(Parameter.FACET_SIZE),
                            results));
                }
                break;
            case 1:
                // per deformation computation
                counts = generateCounts(COUNT_THREADS + 1, deformationCount);
                params.addParameter(Parameter.LWS0, COUNT_THREADS);
                params.addParameter(Parameter.LWS1, 1);

                for (int i = 0; i < COUNT_THREADS; i++) {
                    workers.add(new WorkerPerDeformation(
                            counts[i], counts[i + 1],
                            imageA, imageB,
                            params.getValue(Parameter.IMAGE_WIDTH),
                            facetData, facetCenters,
                            deformations,
                            params.getValue(Parameter.FACET_SIZE),
                            results));
                }
                break;
            default:
                System.err.println("Illegal currentVariant - " + currentVariant);
                break;
        }

        long innerTime = System.nanoTime();
        try {
            for (Worker w : workers) {
                exec.execute(w);
            }
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(ComputeJavaThreads.class.getName()).log(Level.SEVERE, null, ex);
        }
        innerTime = System.nanoTime() - innerTime;

        currentVariant++;
        return new ScenarioResult(results, innerTime);
    }

    private static int[] generateCounts(final int count, final int size) {
        final int[] result = new int[count];
        int part = size / count;
        int c = 0;
        for (int i = 0; i < count; i++) {
            result[i] = c;
            c += part;
        }
        result[result.length - 1] = size;

        return result;
    }

    @Override
    public boolean hasNext() {
        return currentVariant < COUNT_VARIANT;
    }

    @Override
    void resetInner() {
        currentVariant = 0;
    }

    @Override
    public int getVariantCount() {
        return COUNT_VARIANT;
    }

    private static abstract class Worker implements Runnable {

        protected final int startIndex, endIndex;
        protected final int[] imageA, imageB;
        protected final int imageWidth;
        protected final int[] facetData, facetCenters;
        protected final float[] deformations;
        protected final int facetSize;
        protected final float[] results;

        public Worker(int startIndex, int endIndex, int[] imageA, int[] imageB, int imageWidth, int[] facetData, int[] facetCenters, float[] deformations, int facetSize, float[] results) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.imageA = imageA;
            this.imageB = imageB;
            this.imageWidth = imageWidth;
            this.facetData = facetData;
            this.facetCenters = facetCenters;
            this.deformations = deformations;
            this.facetSize = facetSize;
            this.results = results;
        }

    }

    private static class WorkerPerFacet extends Worker {

        public WorkerPerFacet(int startIndex, int endIndex, int[] imageA, int[] imageB, int imageWidth, int[] facetData, int[] facetCenters, float[] deformations, int facetSize, float[] results) {
            super(startIndex, endIndex, imageA, imageB, imageWidth, facetData, facetCenters, deformations, facetSize, results);
        }

        @Override
        public void run() {
            final int facetArraySize = Utils.calculateFacetArraySize(facetSize);
            final int facetArea = Utils.calculateFacetArea(facetSize);

            final float[] deformedFacet = new float[facetArraySize];
            final int[] deformedFacetI = new int[facetArea];
            final int[] facetI = new int[facetArea];

            final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;

            for (int fi = startIndex; fi < endIndex; fi++) {
                interpolate(facetData, fi, facetI, imageA, imageWidth);

                for (int di = 0; di < deformationCount; di++) {
                    deform(facetData, facetSize, facetCenters, fi, deformedFacet, deformations, di);
                    interpolate(deformedFacet, deformedFacetI, imageB, imageWidth);
                    results[fi * deformationCount + di] = correlate(facetI, deformedFacetI);
                }
            }
        }
    }

    private static class WorkerPerDeformation extends Worker {

        public WorkerPerDeformation(int startIndex, int endIndex, int[] imageA, int[] imageB, int imageWidth, int[] facetData, int[] facetCenters, float[] deformations, int facetSize, float[] results) {
            super(startIndex, endIndex, imageA, imageB, imageWidth, facetData, facetCenters, deformations, facetSize, results);
        }

        @Override
        public void run() {
            final int facetArraySize = Utils.calculateFacetArraySize(facetSize);
            final int facetArea = Utils.calculateFacetArea(facetSize);

            final int facetCount = facetData.length / facetArraySize;
            final int[][] facetsI = new int[facetCount][];

            final float[] deformedFacet = new float[facetArraySize];
            final int[] deformedFacetI = new int[facetArea];
            final int deformationCount = deformations.length / Utils.DEFORMATION_DIM;

            for (int di = startIndex; di < endIndex; di++) {
                for (int fi = 0; fi < facetCount; fi++) {
                    if (facetsI[fi] == null) {
                        facetsI[fi] = new int[facetArea];
                        interpolate(facetData, fi, facetsI[fi], imageA, imageWidth);
                    }

                    deform(facetData, facetSize, facetCenters, fi, deformedFacet, deformations, di);
                    interpolate(deformedFacet, deformedFacetI, imageB, imageWidth);
                    results[fi * deformationCount + di] = correlate(facetsI[fi], deformedFacetI);
                }
            }
        }
    }

    private static void deform(final int[] facets, final int facetSize, final int[] facetCenters, final int facetIndex, final float[] deformedFacet, final float[] deformations, final int deformationIndex) {
        final int di = deformationIndex * Utils.DEFORMATION_DIM;
        final int facetBase = facetIndex * Utils.calculateFacetArraySize(facetSize);
        final int facetArea = Utils.calculateFacetArea(facetSize);

        final int cx = facetCenters[facetIndex * 2];
        final int cy = facetCenters[facetIndex * 2 + 1];

        int x, y, dx, dy;
        int baseIndex;
        float val;
        for (int i = 0; i < facetArea; i++) {
            baseIndex = facetBase + i * 2;

            x = facets[baseIndex];
            y = facets[baseIndex + 1];

            dx = x - cx;
            dy = y - cy;

            val = x + deformations[di] + deformations[di + 2] * dx + deformations[di + 4] * dy;
            if (val < 0) {
//                System.err.println(val);
                val = 0;
            }
            deformedFacet[i * 2] = val;
            val = y + deformations[di + 1] + deformations[di + 3] * dx + deformations[di + 5] * dy;
            if (val < 0) {
//                System.err.println(val);
                val = 0;
            }
            deformedFacet[i * 2 + 1] = val;
        }
    }

    private static void interpolate(final float[] deformedFacet, final int[] intensities, final int[] image, final int imageWidth) {
        int x, y, intensity, base;
        double dx, dy, val;
        for (int i = 0; i < intensities.length; i++) {
            base = i * 2;

            val = deformedFacet[base];
            x = (int) Math.floor(val);
            dx = val - x;

            val = deformedFacet[base + 1];
            y = (int) Math.floor(val);
            dy = val - y;

            intensity = 0;
            intensity += image[y * imageWidth + x] * (1 - dx) * (1 - dy);
            intensity += image[y * imageWidth + x + 1] * dx * (1 - dy);
            intensity += image[(y + 1) * imageWidth + x] * (1 - dx) * dy;
            intensity += image[(y + 1) * imageWidth + x + 1] * dx * dy;

            intensities[i] = intensity;
        }
    }

    private static void interpolate(final int[] facets, final int facetIndex, final int[] intensities, final int[] image, final int imageWidth) {
        final int facetBase = facetIndex * 2 * intensities.length;

        int x, y, intensity, base;
        double dx, dy, val;
        for (int i = 0; i < intensities.length; i++) {
            base = i * 2;

            val = facets[facetBase + base];
            x = (int) Math.floor(val);
            dx = val - x;

            val = facets[facetBase + base + 1];
            y = (int) Math.floor(val);
            dy = val - y;

            intensity = 0;
            intensity += image[y * imageWidth + x] * (1 - dx) * (1 - dy);
            intensity += image[y * imageWidth + x + 1] * dx * (1 - dy);
            intensity += image[(y + 1) * imageWidth + x] * (1 - dx) * dy;
            intensity += image[(y + 1) * imageWidth + x + 1] * dx * dy;

            intensities[i] = intensity;
        }
    }

    private static float correlate(final int[] a, final int[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Array size mismatch.");
        }

        final float meanA = mean(a);
        final float meanB = mean(b);

        final float deltaA = delta(a, meanA);
        final float deltaB = delta(b, meanB);
        final float lower = deltaA * deltaB;

        float result = 0;

        float upper;
        for (int i = 0; i < a.length; i++) {
            upper = (a[i] - meanA) * (b[i] - meanB);
            result += upper / lower;
        }

        return result;
    }

    private static float mean(int[] l) {
        float result = 0;
        for (int i : l) {
            result += i;
        }

        return result / l.length;
    }

    private static float delta(int[] l, final float mean) {
        float result = 0;

        double tmp;
        for (int i : l) {
            tmp = i - mean;
            result += tmp * tmp;
        }

        return (float) Math.sqrt(result);
    }

}
