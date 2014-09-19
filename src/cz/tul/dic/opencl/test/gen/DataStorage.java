package cz.tul.dic.opencl.test.gen;

import cz.tul.dic.opencl.Constants;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;
import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult.State;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

    private static final Parameter[] SQL_UPDATE_SET = new Parameter[]{
        Parameter.FACET_COUNT,
        Parameter.DATASIZE};
    private static final Parameter[] SQL_UPDATE_WHERE = new Parameter[]{
        Parameter.IMAGE_WIDTH, Parameter.IMAGE_HEIGHT, Parameter.FACET_SIZE,
        Parameter.DEFORMATION_COUNT, Parameter.TEST_CASE, Parameter.VARIANT,
        Parameter.LWS0, Parameter.LWS1, Parameter.LWS_SUB};
    private static final int CORRECT_RESULT_POS = 0;
    private static final int ILLEGAL_RESULT = 0;
    private static final File runningOut = new File("D:\\DIC_OpenCL_Data_running.csv");
    private static final String DELIMITER_VALUE = ",";
    private static final String DELIMITER_LINE = "\n";
    private static final String DELIMITER_SQL_AND = " AND ";
    private static final DecimalFormat df;
    private static final Map<ParameterSet, ScenarioResult> data;
    private static final Map<ParameterSet, List<float[]>> resultGroups;
    private static final List<Integer> variantCount;
    private static int lineCount, testCaseCount;
    private static boolean runningInited;
    // DB
    private static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String DB_URL = "jdbc:derby://localhost:1527/d:\\Dropbox\\TUL\\DIC\\DIC_OpenCL\\data\\OpenCLdata\\";
    private static final String USER = "username";
    private static final String PASS = "password";
    private static Connection conn;

    static {
        data = new TreeMap<>();
        variantCount = new LinkedList<>();
        resultGroups = new LinkedHashMap<>();

        df = new DecimalFormat("#.###");
        final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);

        runningInited = false;

        try {
            Class.forName(JDBC_DRIVER);
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Error connecting to DB - " + ex.getLocalizedMessage());
        }
    }

    public static void reset() {
        data.clear();
        resultGroups.clear();
        variantCount.clear();
        runningInited = false;
    }

    public static void storeData(final ParameterSet params, final ScenarioResult result, final String device) throws SQLException {
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

        if (conn != null) {
            writeDataResultLineToDB(params, result, device);
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
                if (dif < Constants.EPS_NORMAL) {
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

    public static void clearVariantCounts() {
        variantCount.clear();
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

    private static void writeDataResultLineToDB(ParameterSet ps, ScenarioResult result, final String device) throws SQLException {
        // UPDATE
        StringBuilder sb = new StringBuilder("UPDATE APP.DATA SET ");
        for (Parameter p : SQL_UPDATE_SET) {
            sb.append(p.toString());
            sb.append("=");
            if (ps.contains(p)) {
                sb.append(Integer.toString(ps.getValue(p)));
            } else {
                sb.append(-1);
            }
            sb.append(DELIMITER_VALUE);
        }
        sb.append("TIME_TOTAL=");
        sb.append(Double.toString(result.getTotalTime() / (double) 1000000));
        sb.append(DELIMITER_VALUE);
        sb.append("TIME_KERNEL=");
        sb.append(Double.toString(result.getKernelExecutionTime() / (double) 1000000));
        sb.append(DELIMITER_VALUE);
        sb.append("STATE=");
        sb.append("\'");
        sb.append(result.getState().toString());
        sb.append("\'");
        sb.append(DELIMITER_VALUE);
        sb.append("RESULT_GROUP=");
        sb.append(Integer.toString(result.getResultGroup()));

        sb.append(" WHERE ");

        sb.append("DEVICE=\'");
        sb.append(device);
        sb.append("\'");
        sb.append(DELIMITER_SQL_AND);
        for (Parameter p : SQL_UPDATE_WHERE) {
            sb.append(p.toString());
            sb.append("=");
            if (ps.contains(p)) {
                sb.append(Integer.toString(ps.getValue(p)));
            } else {
                sb.append(-1);
            }
            sb.append(DELIMITER_SQL_AND);
        }
        sb.setLength(sb.length() - DELIMITER_SQL_AND.length());

        try (Statement stm = conn.createStatement()) {
            if (stm.executeUpdate(sb.toString()) == 0) {
                // INSERT
                sb = new StringBuilder("INSERT INTO APP.DATA VALUES (");
                sb.append("\'");
                sb.append(device);
                sb.append("\'");
                sb.append(DELIMITER_VALUE);
                for (Parameter p : Parameter.values()) {
                    if (ps.contains(p)) {
                        sb.append(Integer.toString(ps.getValue(p)));
                    } else {
                        sb.append(-1);
                    }
                    sb.append(DELIMITER_VALUE);
                }
                sb.append(Double.toString(result.getTotalTime() / (double) 1000000));
                sb.append(DELIMITER_VALUE);
                sb.append(Double.toString(result.getKernelExecutionTime() / (double) 1000000));
                sb.append(DELIMITER_VALUE);
                sb.append("\'");
                sb.append(result.getState().toString());
                sb.append("\'");
                sb.append(DELIMITER_VALUE);
                sb.append(Integer.toString(result.getResultGroup()));
                sb.append(")");

                stm.execute(sb.toString());
            }
        }
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
