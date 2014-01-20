package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class ContextHandler {
    
    private static final String CL_EXTENSION = ".cl";
    private static final int MAX_RESET_COUNT_IN_TIME = 3;
    private static final long RESET_TIME = 60000;
    private CLPlatform platform;
    private Scenario scenario;
    private CLContext context;
    private CLDevice device;
    private CLKernel kernel;
    private int resetCounter, facetSize;
    private long firstResetTime;
    
    public ContextHandler() {
        reset();
        resetCounter--;
    }
    
    public CLContext getContext() {
        return context;
    }
    
    public CLDevice getDevice() {
        return device;
    }
    
    public CLKernel getKernel() {
        return kernel;
    }
    
    public void setFacetSize(int facetSize) {
        if (this.facetSize != facetSize) {
            this.facetSize = facetSize;
            assignScenario(scenario);
        }
    }
    
    public void assignScenario(final Scenario sc) {
        scenario = sc;
        if (context != null && scenario != null) {
            final String name = scenario.getDescription();
            try {
                Class cls = sc.getClass().getSuperclass();
                InputStream in = cls.getResourceAsStream(name.concat(CL_EXTENSION));
                BufferedReader bin = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                while (bin.ready()) {
                    sb.append(bin.readLine());
                    sb.append("\n");
                }
                String source = sb.toString().replaceAll("-1", Integer.toString(facetSize));
                kernel = context.createProgram(source).build().createCLKernel(name);
            } catch (IOException ex) {
                // should not happen
                ex.printStackTrace(System.err);
            }
        }
    }
    
    public final void reset() {
        resetCounter++;
        if (resetCounter == 1) {
            firstResetTime = System.currentTimeMillis();
        } else if (resetCounter == MAX_RESET_COUNT_IN_TIME) {
            long time = System.currentTimeMillis();
            time -= firstResetTime;
            time = RESET_TIME - time;
            if (time > 0) {
                synchronized (this) {
                    try {
                        System.out.println("Waiting " + time + "ms before another computation.");
                        this.wait(time);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ContextHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            resetCounter = 0;
        }
        
        if (context != null && !context.isReleased()) {
            for (CLMemory mem : context.getMemoryObjects()) {
                mem.release();
            }
            context.release();
        }

        // select best GPU (non-integrated one for laptops)        
        final Filter<CLPlatform> filter = new Filter<CLPlatform>() {
            @Override
            public boolean accept(CLPlatform item) {
                return item.listCLDevices(CLDevice.Type.CPU).length == 0;
            }
        };
        platform = CLPlatform.getDefault(filter);
        
        if (platform == null) {
            platform = CLPlatform.getDefault();
        }
        
        device = platform.getMaxFlopsDevice(CLDevice.Type.GPU);
        context = CLContext.create(device);
        System.out.println("Using " + device + " on " + context);
        
        assignScenario(scenario);
    }
}
