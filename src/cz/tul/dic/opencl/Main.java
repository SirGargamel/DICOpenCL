package cz.tul.dic.opencl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;

/**
 *
 * @author Petr Jecmen
 */
public class Main {

    public static void main(final String[] args) {
        // main entry point
        CLDevice device = CLContext.create(CLDevice.Type.GPU).getMaxFlopsDevice();
    }
    
}
