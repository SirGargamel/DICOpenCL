package cz.tul.dic.test.opencl.test.gen;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.util.Filter;
import cz.tul.dic.test.opencl.test.gen.scenario.fulldata.Scenario;
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

    private static final Logger LOG = Logger.getGlobal();
    private static final String CL_EXTENSION = ".cl";
    private static final int MAX_RESET_COUNT_IN_TIME = 3;
    private static final long RESET_TIME = 60000;
    private final DeviceType type;
    private final CLErrorHandler errorHandler;
    private CLPlatform platform;
    private Scenario scenario;
    private CLContext context;
    private CLDevice device;
    private CLKernel kernel;
    private int resetCounter, facetSize;
    private long firstResetTime;

    public ContextHandler(final DeviceType type) {
        this.type = type;
        errorHandler = new SimpleCLErrorHandler();
        facetSize = 1;
        resetCounter = -1;
        reset();
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
        if (facetSize > 0 && this.facetSize != facetSize) {
            this.facetSize = facetSize;
            assignScenario(scenario);
        }
    }

    public void assignScenario(final Scenario sc) {
        scenario = sc;
        if (context != null && scenario != null) {
            final String name = scenario.getKernelName();
            CLProgram program = null;
            try {
                Class cls = sc.getClass();
                InputStream in = cls.getResourceAsStream(name.concat(CL_EXTENSION));
                BufferedReader bin = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                while (bin.ready()) {
                    sb.append(bin.readLine());
                    sb.append("\n");
                }
                final String source = sb.toString().replaceAll("-1", Integer.toString(facetSize));
                program = context.createProgram(source);
                program.build();
                kernel = program.createCLKernel(name);
            } catch (IOException ex) {
                // should not happen
                ex.printStackTrace(System.err);
            } catch (CLException ex) {
                LOG.log(Level.SEVERE, "CLException: ", ex);
                if (program != null) {
                    LOG.log(Level.SEVERE, program.getBuildLog());
                }
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
                        LOG.log(Level.WARNING, "Waiting {0}ms before another computation.", time);
                        this.wait(time);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ContextHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            resetCounter = 0;
        }

        initContext();
    }

    public final void initContext() {
        try {
            if (context != null) {
                LOG.log(Level.WARNING, "Reseting context memory.");
                for (CLMemory mem : context.getMemoryObjects()) {
                    if (mem != null && !mem.isReleased()) {
                        mem.release();
                    }
                }
                if (!context.isReleased()) {
                    LOG.log(Level.WARNING, "Releasing context.");
                    context.release();
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error reseting context.", ex);
        }

        final Filter<CLPlatform> filter;
        final Type deviceType;

        switch (type) {
            case CPU:
                filter = (CLPlatform item) -> item.listCLDevices(CLDevice.Type.CPU).length > 0;
                deviceType = Type.CPU;
                break;
            case GPU:
                // select best GPU (prefer non-integrated)
                filter = (CLPlatform item) -> item.listCLDevices(CLDevice.Type.CPU).length == 0;
                deviceType = Type.GPU;
                break;
            case iGPU:
                // select integrated GPU
                filter = (CLPlatform item) -> item.listCLDevices(CLDevice.Type.CPU).length > 0;
                deviceType = Type.GPU;
                break;
            default:
                filter = (CLPlatform i) -> true;
                deviceType = Type.ALL;
        }

        platform = CLPlatform.getDefault(filter);
        if (platform == null) {
            platform = CLPlatform.getDefault();
        }

        device = platform.getMaxFlopsDevice(deviceType);
        if (device == null) {
            device = platform.getMaxFlopsDevice();
        }

        context = CLContext.create(device);
        context.addCLErrorHandler(errorHandler);
        LOG.log(Level.INFO, "Using {0} on {1}", new Object[]{device, context});

        assignScenario(scenario);
    }

    public final String getDeviceName() {
        return device.getName();
    }

    public enum DeviceType {

        CPU,
        GPU,
        iGPU
    }
}
