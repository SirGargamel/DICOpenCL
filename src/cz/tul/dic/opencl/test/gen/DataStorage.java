package cz.tul.dic.opencl.test.gen;

import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
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

    private static final int CORRECT_RESULT_POS = 0;
    private static final int ILLEGAL_RESULT = 0;
    private static final File runningOut = new File("D:\\DIC_OpenCL_Data_running.csv");
    private static final String DELIMITER_VALUE = ",";
    private static final String DELIMITER_LINE = "\n";
    private static final DecimalFormat df;
    private static final Map<ParameterSet, ScenarioResult> data;
    private static final Map<ParameterSet, List<float[]>> resultGroups;
    private static final List<Integer> variantCount;
    private static int lineCount, testCaseCount;
    private static boolean runningInited;

    static {
        data = new TreeMap<>();
        variantCount = new LinkedList<>();
        resultGroups = new LinkedHashMap<>();

        df = new DecimalFormat("#.###");
        final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);

        runningInited = false;
    }

    public static void reset() {
        data.clear();
        resultGroups.clear();
        variantCount.clear();
        runningInited = false;
    }

    public static void storeData(final ParameterSet params, final ScenarioResult result) {
        data.put(params, result);

        if (State.WRONG_RESULT_FIXED.equals(result.getState())) {
            result.setResultGroup(ILLEGAL_RESULT);
        } else {
            final int resultGroup = validateResult(result, params);
            result.setResultGroup(resultGroup);
            if (result.getState().equals(State.SUCCESS) && resultGroup == ILLEGAL_RESULT) {
                result.markAsInvalidDynamicPart();
            }
        }

        try {
            writeDataRunning(params, result);
        } catch (IOException ex) {
            Logger.getLogger(DataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }

        result.markAsStored();
    }

    private static int validateResult(final ScenarioResult result, final ParameterSet rps) {
        final float[] coeffs = result.getResultData();
        if (coeffs == null) {
            return ILLEGAL_RESULT;
        }

        int resultIndex = ILLEGAL_RESULT;

        List<float[]> results = null;
        for (ParameterSet ps : resultGroups.keySet()) {
            if (ps.equals(rps, Parameter.IMAGE_WIDTH, Parameter.IMAGE_HEIGHT, Parameter.FACET_SIZE, Parameter.DEFORMATION_COUNT, Parameter.TEST_CASE)) {
                results = resultGroups.get(ps);
                break;
            }
        }
        if (results == null) {
            results = new LinkedList<>();
            results.add(coeffs);
            resultGroups.put(rps, results);
            resultIndex = 1;
        } else {
            float[] res;
            float dif;
            for (int i = 0; i < results.size(); i++) {
                res = results.get(i);

                if (coeffs.length != res.length) {
                    continue;
                }

                dif = CustomMath.maxDifferece(coeffs, res);
                if (dif < Utils.EPS_NORMAL) {
                    resultIndex = -(i + 1);
                    break;
                }
            }

            result.setMaxDifference(CustomMath.maxDifferece(coeffs, results.get(CORRECT_RESULT_POS)));

            if (resultIndex == ILLEGAL_RESULT) {
                results.add(coeffs);
                resultIndex = results.size();
            }
        }

        return resultIndex;
    }

    public static void setCounts(final int lineCount, final int testCaseCount) {
        DataStorage.lineCount = lineCount;
        DataStorage.testCaseCount = testCaseCount;
    }

    public static void addVariantCount(final int count) {
        variantCount.add(count);
    }

    private static void initTarget(final File out) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, false))) {
            writeDataParameterLine(bw);
            writeDataHeaderLine(bw);
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
            writeDataResultLine(ps, result, bw);
        }
    }

    public static void exportData(final File out) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            writeDataParameterLine(bw);
            writeDataHeaderLine(bw);

            for (Entry<ParameterSet, ScenarioResult> e : data.entrySet()) {
                writeDataResultLine(e.getKey(), e.getValue(), bw);
            }
        }
    }

    private static void writeDataParameterLine(final BufferedWriter bw) throws IOException {
        bw.write(Integer.toString(lineCount));
        bw.write(DELIMITER_VALUE);
        bw.write(Integer.toString(testCaseCount));
        bw.write(DELIMITER_VALUE);
        bw.write(Integer.toString(variantCount.size()));
        for (Integer i : variantCount) {
            bw.write(DELIMITER_VALUE);
            bw.write(i.toString());
        }
        bw.write(DELIMITER_LINE);
    }

    private static void writeDataHeaderLine(final BufferedWriter bw) throws IOException {
        final Parameter[] params = Parameter.values();
        for (Parameter p : params) {
            bw.write(p.toString());
            bw.write(DELIMITER_VALUE);
        }
        bw.write("Total time [ms]");
        bw.write(DELIMITER_VALUE);
        bw.write("Kernel time [ms]");
        bw.write(DELIMITER_VALUE);
        bw.write("Status");
        bw.write(DELIMITER_VALUE);
        bw.write("Result Group");

        bw.write(DELIMITER_LINE);
    }

    private static void writeDataResultLine(ParameterSet ps, ScenarioResult result, final BufferedWriter bw) throws IOException {
        final Parameter[] params = Parameter.values();
        for (Parameter p : params) {
            if (ps.contains(p)) {
                bw.write(Integer.toString(ps.getValue(p)));
            }
            bw.write(DELIMITER_VALUE);
        }
        bw.write(df.format(result.getTotalTime() / (double) 1000000));
        bw.write(DELIMITER_VALUE);
        bw.write(df.format(result.getKernelExecutionTime() / (double) 1000000));
        bw.write(DELIMITER_VALUE);
        bw.write(result.getState().toString());
        bw.write(DELIMITER_VALUE);
        bw.write(Integer.toString(result.getResultGroup()));

        bw.write(DELIMITER_LINE);
    }

    public static void exportResultGroups(final File out) throws IOException {
        // check result groups

        // export data
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            final List<float[]> resultsData = new LinkedList<>();
            int allLineCount = 0;

            for (Entry<ParameterSet, List<float[]>> e : resultGroups.entrySet()) {
                bw.write("\"");
                bw.write(e.getKey().toStringSmall());
                bw.write("\"");
                for (int i = 0; i < e.getValue().size(); i++) {
                    bw.write(DELIMITER_VALUE);
                }

                for (float[] fa : e.getValue()) {
                    resultsData.add(fa);
                    if (fa.length > allLineCount) {
                        allLineCount = fa.length;
                    }
                }
            }
            bw.write(DELIMITER_LINE);

            float val;
            for (int i = 0; i < allLineCount; i++) {
                for (float[] fa : resultsData) {
                    if (i < fa.length) {
                        val = fa[i];
                        if (Float.isNaN(val)) {
                            bw.write("NaN");
                        } else {
                            bw.write(df.format(val));
                        }
                    }
                    bw.write(DELIMITER_VALUE);
                }
                bw.write(DELIMITER_LINE);
            }
        }
    }
}
