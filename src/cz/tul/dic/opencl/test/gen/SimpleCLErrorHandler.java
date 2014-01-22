package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLErrorHandler;
import java.nio.ByteBuffer;

/**
 *
 * @author Petr Jecmen
 */
public class SimpleCLErrorHandler implements CLErrorHandler {

    @Override
    public void onError(String string, ByteBuffer bb, long l) {
        StringBuilder sb = new StringBuilder(string);
        sb.append(" :: ");
        if (bb != null) {
            sb.append(bb.toString());
        }
        sb.append(" :: ");
        sb.append(l);
        System.err.println(sb.toString());
    }

}
