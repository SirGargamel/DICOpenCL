package cz.tul.dic.opencl.test.gen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
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
    private static final Map<ParameterSet, Long> data;
    private static final Map<ParameterSet, float[]> correctResults;
    private static int lineCount, scenarioCount;

    static {
        data = new TreeMap<>();
        correctResults = new HashMap<>();

        try {
            initTarget(runningOut);
        } catch (IOException ex) {
            Logger.getLogger(DataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void storeData(final ParameterSet params, final long time, final float[] result) {
        final long checkedTime = checkResult(params, time, result);
        data.put(params, checkedTime);
        try {
            writeDataRunning(params, checkedTime);
        } catch (IOException ex) {
            Logger.getLogger(DataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setLineCount(final int count) {
        lineCount = count;
    }

    public static void setScenarioCount(final int count) {
        scenarioCount = count;
    }

    private static long checkResult(final ParameterSet params, final long time, final float[] result) {
        if (result == null) {
            return -time;
        }

        long res = time;
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
            correctResults.put(params, result);
        } else {
            final float[] correct = correctResults.get(psr);
            if (!areEqual(correct, result, EPSILON)) {
                // TODO mark as invalid result
            }
        }
        return res;
    }

    private static boolean areEqual(final float[] a, final float[] b, final float eps) {
        boolean result = true;

        if (a.length != b.length) {
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
            final Parameter[] params = Parameter.values();
            for (Parameter p : params) {
                bw.write(p.toString());
                bw.write(DELIMITER_VALUE);
            }
            bw.write("Time [ms]");
            bw.write(DELIMITER_LINE);
        }
    }

    private static void writeDataRunning(ParameterSet ps, Long time) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(runningOut, true))) {
            writeDataLine(ps, time, bw);
        }
    }

    public static void exportData(final File out) throws IOException {
        final Parameter[] params = Parameter.values();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            bw.write(Integer.toString(lineCount));
            bw.write(DELIMITER_VALUE);
            bw.write(Integer.toString(scenarioCount));
            bw.write(DELIMITER_LINE);

            for (Parameter p : params) {
                bw.write(p.toString());
                bw.write(DELIMITER_VALUE);
            }
            bw.write("Time [ms]");
            bw.write(DELIMITER_LINE);

            for (Entry<ParameterSet, Long> e : data.entrySet()) {
                writeDataLine(e.getKey(), e.getValue(), bw);
            }
        }
    }

    private static void writeDataLine(ParameterSet ps, Long time, final BufferedWriter bw) throws IOException {
        final Parameter[] params = Parameter.values();
        for (Parameter p : params) {
            if (ps.contains(p)) {
                bw.write(Integer.toString(ps.getValue(p)));
            }
            bw.write(DELIMITER_VALUE);
        }
        bw.write(Long.toString(time / 1000000));

        bw.write(DELIMITER_LINE);
    }
}
