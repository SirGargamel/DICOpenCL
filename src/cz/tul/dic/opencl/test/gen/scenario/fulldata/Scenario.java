package cz.tul.dic.opencl.test.gen.scenario.fulldata;

import cz.tul.dic.opencl.test.gen.ParameterSet;

/**
 *
 * @author Petr Ječmen
 */
public abstract class Scenario {

    public void prepare(final ParameterSet params) {
        // nothing to prepare here
    }

    public abstract boolean hasNext();

    public void reset() {
        resetInner();
    }

    protected abstract void resetInner();

    public String getKernelName() {
        return getClass().getSimpleName();
    }

    public abstract int getVariantCount();
    
}
