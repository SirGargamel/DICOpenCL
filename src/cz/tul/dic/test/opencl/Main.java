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
    private static final String SWITCH_OPT = "o";

    public static void main(final String[] args) throws IOException, SQLException {
        // init logging
        LogManager.getLogManager().reset();
        Handler handler;

//        final Level lvl = Level.WARNING;        
//        Handler h = new FileHandler("errorLog.log", true);
//        h.setFormatter(new SimpleFormatter());
//        h.setLevel(lvl);
//        l.addHandler(h);
        handler = new HandlerOut();
        handler.setLevel(Level.ALL);
        LOG.addHandler(handler);

        Boolean testLimits = null;
        if (args.length > 0 && args[0].contains(SWITCH_LIMITS)) {
            testLimits = true;
            LOG.info("Testing limits scenarios.");
        } else if (args.length > 0 && args[0].contains(SWITCH_OPT)) {
            testLimits = false;
            LOG.info("Testing limits scenarios.");
        } else {
            System.out.println("You need to provide test type -\n\"o\" for optimizations test, \"l\" for limits vs. full data test:");
            final Scanner reader = new Scanner(System.in);
            String input;
            while (testLimits == null) {
                input = reader.next();
                if (input.equals(SWITCH_LIMITS)) {
                    LOG.info("Testing limits scenarios.");
                    testLimits = true;
                } else if (input.equals(SWITCH_OPT)) {
                    LOG.info("Testing optimization scenarios.");
                    testLimits = false;
                } else {
                    System.out.println("Illegal input, use \"o\" or \"l\"");
                }
            }
            reader.close();
        }

        if (testLimits) {
            LimitsTest.runTest();
        } else {
            OptimizationsTest.runTest();
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
}
