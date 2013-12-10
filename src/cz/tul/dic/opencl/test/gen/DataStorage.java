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

    private static final File runningOut = new File("D:\\DIC_OpenCL_Data_running.csv");
    private static final String DELIMITER_VALUE = ",";
    private static final String DELIMITER_LINE = "\n";
    private static final float EPS = 0.0001f;
    private static final Map<ParameterSet, ScenarioResult> data;
    private static final Map<ParameterSet, List<float[]>> resultGroups;
    private static final List<Integer> variantCount;
    private static int lineCount;
    private static boolean runningInited;

    static {
        data = new TreeMap<>();
        variantCount = new LinkedList<>();
        resultGroups = new HashMap<>();

        runningInited = false;
    }

    public static void storeData(final ParameterSet params, final ScenarioResult result) {
        data.put(params, result);        
        final int resultGroup = validateResult(result, params);
        result.setResultGroup(resultGroup);
        if (resultGroup != 0) {
            result.markResultAsInvalidDynamic();
        }
        
        try {
            writeDataRunning(params, result);
        } catch (IOException ex) {
            Logger.getLogger(DataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        result.markAsStored();
    }

    public static void setLineCount(final int count) {
        lineCount = count;
    }

    public static void addVariantCount(final int count) {
        variantCount.add(count);
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
        bw.write(DELIMITER_VALUE);
        bw.write("Status");
        bw.write(DELIMITER_VALUE);
        bw.write("Result Group");

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
        bw.write(DELIMITER_VALUE);
        bw.write(result.getState().toString());
        bw.write(DELIMITER_VALUE);
        bw.write(Integer.toString(result.getResultGroup()));

        bw.write(DELIMITER_LINE);
    }
    
    private static int validateResult(final ScenarioResult result, final ParameterSet rps) {
        final float[] coeffs = result.getResultData();        
        int resultIndex = -1;  
        
        List<float[]> results = null;        
        for (ParameterSet ps : resultGroups.keySet()) {
            if (ps.equals(rps, Parameter.IMAGE_WIDTH, Parameter.IMAGE_HEIGHT, Parameter.FACET_SIZE, Parameter.DEFORMATION_COUNT)) {
                results = resultGroups.get(ps);
            }
        }
        if (results == null) {
            results = new LinkedList<>();
            results.add(coeffs);
            resultGroups.put(rps, results);
        }
        
        float[] res;
        boolean same;
        for (int i = 0; i < results.size(); i++) {
            res = results.get(i);
            same = true;
            
            if (coeffs.length != res.length) {
                continue;
            }
            
            for (int j = 0; j < coeffs.length; j++) {                
                if (!CustomMath.areEqual(coeffs[j], res[j], EPS)) {
                    same = false;
                    break;
                }
            }
            
            if (same) {
                resultIndex = i;
                break;
            }
        }
        
        if (resultIndex == -1) {
            resultIndex = resultGroups.size();
            results.add(coeffs);
        }
        
        return resultIndex;
    }
}
