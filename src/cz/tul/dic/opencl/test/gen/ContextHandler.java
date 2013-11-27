package cz.tul.dic.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.opencl.test.gen.scenario.Scenario;
import java.io.IOException;
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
    private int resetCounter;
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

    public void assignScenario(final Scenario sc) {
        scenario = sc;
        if (context != null && scenario != null) {
            final String name = scenario.getDescription();
            try {
                kernel = context.createProgram(Scenario.class.getResourceAsStream(name.concat(CL_EXTENSION))).build().createCLKernel(name);
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
        } else {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ContextHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (context != null && !context.isReleased()) {
            context.release();
        }

        // select best GPU (non-integrated one for laptops)
        CLPlatform.initialize();
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

        context = CLContext.create(platform, CLDevice.Type.GPU);
        device = context.getMaxFlopsDevice(CLDevice.Type.GPU);
        System.out.println("Using " + device + " on " + context);

        assignScenario(scenario);
    }
}
