package cz.tul.dic.opencl.test.gen.scenario;

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
    public ScenarioResult compute(int[] imageA, float imageAavg, int[] imageB, float imageBavg, int[] facets, float[] deformations, ParameterSet params) {
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
                            imageAavg, imageBavg,
                            params.getValue(Parameter.IMAGE_WIDTH),
                            facets, deformations,
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
                            imageAavg, imageBavg,
                            params.getValue(Parameter.IMAGE_WIDTH),
                            facets, deformations,
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
        protected final float imageAavg, imageBavg;
        protected final int imageWidth;
        protected final int[] facets;
        protected final float[] deformations;
        protected final int facetSize;
        protected final float[] results;

        public Worker(int startIndex, int endIndex, int[] imageA, int[] imageB, float imageAavg, float imageBavg, final int imageWidth, int[] facets, float[] deformations, final int facetSize, final float[] results) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.imageA = imageA;
            this.imageB = imageB;
            this.imageAavg = imageAavg;
            this.imageBavg = imageBavg;
            this.facets = facets;
            this.deformations = deformations;
            this.facetSize = facetSize;
            this.results = results;
            this.imageWidth = imageWidth;
        }

    }

    private static class WorkerPerFacet extends Worker {

        public WorkerPerFacet(int startIndex, int endIndex, int[] imageA, int[] imageB, float imageAavg, float imageBavg, final int imageWidth, int[] facets, float[] deformations, final int facetSize, final float[] results) {
            super(startIndex, endIndex, imageA, imageB, imageAavg, imageBavg, imageWidth, facets, deformations, facetSize, results);
        }

        @Override
        public void run() {
            final int facetSize2 = facetSize * facetSize;
            final int[] facetI = new int[facetSize2];
            final float[] deformedFacet = new float[facetSize2 * 2];
            final int[] deformedFacetI = new int[facetSize2];
            final int deformationCount = deformations.length / 2;

            int facetBaseIndex;
            for (int fi = startIndex; fi < endIndex; fi++) {
                facetBaseIndex = fi * facetSize2 * 2;
                interpolate(facets, fi, facetI, imageA, imageWidth);

                for (int di = 0; di < deformationCount; di++) {
                    deform(facets, deformedFacet, facetBaseIndex, facetSize2, deformations[di * 2], deformations[di * 2 + 1]);
                    interpolate(deformedFacet, deformedFacetI, imageB, imageWidth);
                    results[fi * deformationCount + di] = correlate(facetI, deformedFacetI);
                }
            }
        }
    }

    private static class WorkerPerDeformation extends Worker {

        public WorkerPerDeformation(int startIndex, int endIndex, int[] imageA, int[] imageB, float imageAavg, float imageBavg, final int imageWidth, int[] facets, float[] deformations, final int facetSize, final float[] results) {
            super(startIndex, endIndex, imageA, imageB, imageAavg, imageBavg, imageWidth, facets, deformations, facetSize, results);
        }

        @Override
        public void run() {
            final int facetSize2 = facetSize * facetSize;
            final int facetCount = facets.length / (facetSize2 * 2);
            final int[][] facetsI = new int[facetCount][];

            final float[] deformedFacet = new float[facetSize2 * 2];
            final int[] deformedFacetI = new int[facetSize2];
            final int deformationCount = deformations.length / 2;

            int facetBaseIndex;
            float dx, dy;
            for (int di = startIndex; di < endIndex; di++) {
                dx = deformations[di * 2];
                dy = deformations[di * 2 + 1];

                for (int fi = 0; fi < facetCount; fi++) {
                    if (facetsI[fi] == null) {
                        facetsI[fi] = new int[facetSize2];
                        interpolate(facets, fi, facetsI[fi], imageA, imageWidth);
                    }

                    facetBaseIndex = fi * facetSize2 * 2;
                    deform(facets, deformedFacet, facetBaseIndex, facetSize2, dx, dy);
                    interpolate(deformedFacet, deformedFacetI, imageB, imageWidth);
                    results[fi * deformationCount + di] = correlate(facetsI[fi], deformedFacetI);
                }
            }
        }
    }

    private static void deform(final int[] facets, final float[] deformedFacet, final int startIndex, final int size, final float dx, final float dy) {
        int baseIndex;
        for (int i = 0; i < size; i++) {
            baseIndex = startIndex + i * 2;

            deformedFacet[i * 2] = facets[baseIndex] + dx;
            deformedFacet[i * 2 + 1] = facets[baseIndex + 1] + dy;
        }
    }

    private static void interpolate(final float[] deformedFacet, final int[] intensities, final int[] image, final int imageWidth) {
        int x, y;
        for (int i = 0; i < intensities.length; i++) {
            x = Math.round(deformedFacet[i * 2]);
            y = Math.round(deformedFacet[i * 2 + 1]);
            intensities[i] = image[y * imageWidth + x];
        }
    }

    private static void interpolate(final int[] facets, final int facetIndex, final int[] intensities, final int[] image, final int imageWidth) {
        final int facetBase = facetIndex * 2 * intensities.length;

        int x, y;
        for (int i = 0; i < intensities.length; i++) {
            x = Math.round(facets[facetBase + i * 2]);
            y = Math.round(facets[facetBase + i * 2 + 1]);
            intensities[i] = image[y * imageWidth + x];
        }
    }

    private static float correlate(final int[] a, final int[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Array size mismatch.");
        }

        final float aAvg = mean(a);
        final float bAvg = mean(b);

        final float deltaA = delta(a, aAvg);
        final float deltaB = delta(b, bAvg);

        float result = 0;

        double upper, lower;
        for (int i = 0; i < a.length; i++) {
            upper = (a[i] - aAvg) * (b[i] - bAvg);
            lower = deltaA * deltaB;
            result += upper / lower;
        }

        return result;
    }

    private static float mean(int[] l) {
        float result = 0;
        for (int i : l) {
            result += i;
        }

        final int s = l.length;
        result *= 1 / (s * s);

        return result;
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
