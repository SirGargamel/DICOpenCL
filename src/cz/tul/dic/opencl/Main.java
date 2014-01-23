/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.opencl;

import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public class Main {

    public static void main(final String[] args) throws IOException {                
        PerformanceTest.computeImageFillTest();
    }
}
