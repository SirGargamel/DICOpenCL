package cz.tul.dic.opencl.test.gen;

import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class DataStorage {

    private static final File runningOut = new File("D:\\runningExport.csv");
    private static final float EPSILON = 0.0001f;
    private static final String DELIMITER_VALUE = ",";
    private static final String DELIMITER_LINE = "\n";
    private static final Map<ParameterSet, ScenarioResult> data;
    private static final Map<ParameterSet, float[]> correctResults;
    private static final List<Integer> variantCount;
    private static int lineCount;
    private static boolean runningInited;

    static {
        data = new TreeMap<>();
        correctResults = new HashMap<>();
        variantCount = new LinkedList<>();

        runningInited = false;
    }

    public static void storeData(final ParameterSet params, final ScenarioResult result) {
        checkResult(params, result);
        data.put(params, result);
        try {
            writeDataRunning(params, result);
        } catch (IOException ex) {
            Logger.getLogger(DataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setLineCount(final int count) {
        lineCount = count;
    }

    public static void addVariantCount(final int count) {
        variantCount.add(count);
    }

    private static void checkResult(final ParameterSet params, final ScenarioResult result) {
        final float[] resultData = result.getResultData();
        if (resultData == null) {
            result.setTotalTime(-result.getTotalTime());
        } else {
            boolean found = false;
            ParameterSet psr = null;
            for (ParameterSet ps : correctResults.keySet()) {
                if (ps.equals(params, Parameter.DEFORMATION_COUNT, Parameter.FACET_SIZE, Parameter.IMAGE_HEIGHT, Parameter.IMAGE_WIDTH)) {
                    found = true;
                    psr = ps;
                    break;
                }
            }

            if (!found) {
                correctResults.put(params, resultData);
            } else {
                final float[] correct = correctResults.get(psr);
                if (!areEqual(correct, resultData, EPSILON)) {
                    // TODO mark as invalid result
                    System.out.println("Invalid result.");
                }
            }
            result.markAsStored();
        }
    }

    private static boolean areEqual(final float[] a, final float[] b, final float eps) {
        boolean result = true;

        if (a == null || b == null || a.length != b.length) {
            result = false;
        } else {

            float dif;
            for (int i = 0; i < a.length; i++) {
                dif = Math.abs(a[i] - b[i]);
                if (dif > eps) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    private static void initTarget(final File out) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, false))) {
            writeParameterLine(bw);
            writeHeaderLine(bw);
        }
    }

    private static void writeDataRunning(ParameterSet ps, ScenarioResult result) throws IOException {
        if (!runningInited) {
            try {
                initTarget(runningOut);
                runningInited = true;
            } catch (IOException ex) {
                Logger.getLogger(DataStorage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(runningOut, true))) {
            writeDataLine(ps, result, bw);
        }
    }

    public static void exportData(final File out) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            writeParameterLine(bw);
            writeHeaderLine(bw);

            for (Entry<ParameterSet, ScenarioResult> e : data.entrySet()) {
                writeDataLine(e.getKey(), e.getValue(), bw);
            }
        }
    }

    private static void writeParameterLine(final BufferedWriter bw) throws IOException {
        bw.write(Integer.toString(lineCount));
        bw.write(DELIMITER_VALUE);
        bw.write(Integer.toString(variantCount.size()));
        for (Integer i : variantCount) {
            bw.write(DELIMITER_VALUE);
            bw.write(i.toString());
        }
        bw.write(DELIMITER_LINE);
    }

    private static void writeHeaderLine(final BufferedWriter bw) throws IOException {
        final Parameter[] params = Parameter.values();
        for (Parameter p : params) {
            bw.write(p.toString());
            bw.write(DELIMITER_VALUE);
        }
        bw.write("Total time [ms]");
        bw.write(DELIMITER_VALUE);
        bw.write("Kernel time [ms]");
        bw.write(DELIMITER_LINE);
    }

    private static void writeDataLine(ParameterSet ps, ScenarioResult result, final BufferedWriter bw) throws IOException {
        final Parameter[] params = Parameter.values();
        for (Parameter p : params) {
            if (ps.contains(p)) {
                bw.write(Integer.toString(ps.getValue(p)));
            }
            bw.write(DELIMITER_VALUE);
        }
        bw.write(Long.toString(result.getTotalTime() / 1000000));
        bw.write(DELIMITER_VALUE);
        bw.write(Long.toString(result.getKernelExecutionTime() / 1000000));

        bw.write(DELIMITER_LINE);
    }
}
