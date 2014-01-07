package cz.tul.dic.opencl.test.gen.scenario;

import cz.tul.dic.opencl.test.gen.ContextHandler;
import cz.tul.dic.opencl.test.gen.ParameterSet;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Scenario {

    private final String name;
    protected final ContextHandler contextHandler;

    public Scenario(final String scenarioName, final ContextHandler contextHandler) throws IOException {
        this.contextHandler = contextHandler;
        this.name = scenarioName;
    }

    public abstract ScenarioResult compute(
            final int[] imageA, final int[] imageB,
            final int[] facetData, final int[] facetCenters,
            final float[] deformations,
            final ParameterSet params);

    public abstract boolean hasNext();

    public void reset() {
        if (contextHandler != null) {
            contextHandler.assignScenario(this);
        }

        resetInner();
    }

    protected abstract void resetInner();

    public String getDescription() {
        return name;
    }

    public abstract int getVariantCount();

}
