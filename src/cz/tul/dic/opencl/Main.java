/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.opencl;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

/**
 *
 * @author Petr Jecmen
 */
public class Main {

    public static void main(final String[] args) throws IOException {
        // init logging
        LogManager.getLogManager().reset();
        final Level lvl = Level.WARNING;
        final Logger l = Logger.getGlobal();

        Handler h = new FileHandler("errorLog.log", true);
        h.setFormatter(new XMLFormatter());
        h.setLevel(lvl);
        l.addHandler(h);

        h = new HandlerOut();
        h.setLevel(Level.ALL);
        l.addHandler(h);

        PerformanceTest.computeImageFillTest();
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
