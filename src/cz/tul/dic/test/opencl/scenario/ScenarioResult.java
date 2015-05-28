package cz.tul.dic.test.opencl.scenario;

/**
 *
 * @author Petr Jecmen
 */
public class ScenarioResult {

    private final long kernelExecutionTime;
    private float[] resultData;
    private long totalTime;
    private State state;
    private int resultGroup;
    private double maxDifference;

    public ScenarioResult(final long totalTime, final boolean exceptionOccured) {
        if (exceptionOccured) {
            state = State.FAIL;
        } else {
            state = State.INVALID_PARAMS;
        }
        this.totalTime = totalTime;
        this.kernelExecutionTime = -1;
        maxDifference = 0;
    }

    public ScenarioResult(float[] resultData, long kernelExecutionTime) {
        this.resultData = resultData;
        this.kernelExecutionTime = kernelExecutionTime;

        if (resultData != null) {
            state = State.SUCCESS;
        } else {
            state = State.INVALID_PARAMS;
        }
    }

    public float[] getResultData() {
        return resultData;
    }

    public void markAsStored() {
        resultData = null;
    }

    public long getKernelExecutionTime() {
        return kernelExecutionTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public State getState() {
        return state;
    }

    public void markAsInvalidDynamicPart() {
        state = State.WRONG_RESULT_DYNAMIC;
    }

    public void markAsInvalidFixedPart() {
        state = State.WRONG_RESULT_FIXED;
    }

    public int getResultGroup() {
        return resultGroup;
    }

    public void setResultGroup(int resultGroup) {
        this.resultGroup = resultGroup;
    }

    public double getMaxDifference() {
        return maxDifference;
    }

    public void setMaxDifference(double maxDifference) {
        this.maxDifference = maxDifference;
    }

    public static enum State {

        SUCCESS,
        FAIL,
        INVALID_PARAMS,
        WRONG_RESULT_FIXED,
        WRONG_RESULT_DYNAMIC
    }

}
