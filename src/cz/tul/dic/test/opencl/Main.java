package cz.tul.dic.test.opencl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 *
 * @author Petr Jecmen
 */
public final class Main {

    private static final Logger LOG = Logger.getGlobal();
    private static final String SWITCH_LIMITS = "l";
    private static final String SWITCH_OPTIMIZATIONS = "o";
    private static final String SWITCH_GENERAL = "g";

    public static void main(final String[] args) throws IOException, SQLException {
        // init logging
        LogManager.getLogManager().reset();         
        final Handler handler = new HandlerOut();
        handler.setLevel(Level.ALL);
        LOG.addHandler(handler);

        Mode mode = null;
        if (args.length > 0 && args[0].contains(SWITCH_LIMITS)) {
            mode = Mode.LIMITS;
            LOG.info("Testing limits scenarios.");
        } else if (args.length > 0 && args[0].contains(SWITCH_OPTIMIZATIONS)) {
            mode = Mode.OPTIMIZATIONS;
            LOG.info("Testing limits scenarios.");
        } else if (args.length > 0 && args[0].contains(SWITCH_GENERAL)) {
            mode = Mode.GENEREAL;
            LOG.info("Testing general scenarios.");
        } else {
            System.out.println("You need to provide test type -\n\"o\" for optimizations test, \"l\" for limits vs. full data test or \"l\" for general test:");
            try (Scanner reader = new Scanner(System.in)) {
                String input;
                while (mode == null) {
                    input = reader.next();
                    switch (input) {
                        case SWITCH_LIMITS:
                            LOG.info("Testing limits scenarios.");
                            mode = Mode.LIMITS;
                            break;
                        case SWITCH_OPTIMIZATIONS:
                            LOG.info("Testing optimization scenarios.");
                            mode = Mode.OPTIMIZATIONS;
                            break;
                        case SWITCH_GENERAL:
                            LOG.info("Testing general scenarios.");
                            mode = Mode.GENEREAL;
                            break;
                        default:
                            System.out.println("Illegal input, use \"o\" or \"l\" or \"g\"");
                            break;
                    }
                }
            }
        }
        
        switch (mode) {
            case GENEREAL:
                GeneralTest.runTest();
                break;
            case OPTIMIZATIONS:
                OptimizationsTest.runTest();
                break;
            case LIMITS:
                LimitsTest.runTest();
                break;
            default:
                throw new IllegalArgumentException("Unsupported mode of test - " + mode);
        }

        // DB testing
//        ParameterSet ps = new ParameterSet();
//        ScenarioResult sr = new ScenarioResult(new float[]{1}, 1000);
//        String device = "GPU";
//        DataStorage.storeData(ps, sr, device);
//
//        sr = new ScenarioResult(new float[]{1}, 1500);
//        DataStorage.storeData(ps, sr, device);
//        
//        sr = new ScenarioResult(new float[]{2}, 3000);
//        ps.addParameter(Parameter.LWS0, 10);
//        DataStorage.storeData(ps, sr, device);
    }

    private static class HandlerOut extends StreamHandler {

        public HandlerOut() {
            super(System.out, new SimpleFormatter());
            setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {

                    return formatMessage(record).concat("\n");
                }
            });
        }

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }

    }

    private static enum Mode {

        OPTIMIZATIONS,
        LIMITS,
        GENEREAL,;
    }
}
