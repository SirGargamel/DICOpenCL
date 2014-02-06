/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.opencl;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

/**
 *
 * @author Petr Jecmen
 */
public class Main {

    public static void main(final String[] args) throws IOException {
        // init logging
        final Level lvl = Level.WARNING;
        final Logger l = Logger.getGlobal();
        final Handler h = new FileHandler("errorLog.log", true);
        h.setFormatter(new XMLFormatter());
        h.setLevel(lvl);
        l.addHandler(h);

        PerformanceTest.computeImageFillTest();
    }
}
