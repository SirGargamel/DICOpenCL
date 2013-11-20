package cz.tul.dic.opencl.test.gen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author Petr Jecmen
 */
public class DataStorage {

    private static final String DELIMITER_VALUE = ",";
    private static final String DELIMITER_LINE = "\n";
    private static final Map<ParameterSet, Long> data;
    private static int lineCount, scenarioCount;

    static {
        data = new TreeMap<>();
    }

    public static void storeData(final ParameterSet params, final long time) {
        data.put(params, time);
    }
    
    public static void setLineCount(final int count) {
        lineCount = count;
    }
    
    public static void setScenarioCount(final int count) {
        scenarioCount = count;
    }

    public static void exportData(final File out) throws IOException {
        ParameterSet ps;
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
                ps = e.getKey();
                for (Parameter p : params) {
                    if (ps.contains(p)) {
                        bw.write(Integer.toString(ps.getValue(p)));
                    }
                    bw.write(DELIMITER_VALUE);
                }
                bw.write(Long.toString(e.getValue() / 1000));
                bw.write(DELIMITER_LINE);
            }
        }
    }
}
