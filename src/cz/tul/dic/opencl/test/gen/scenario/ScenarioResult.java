package cz.tul.dic.opencl.test.gen.scenario;

/**
 *
 * @author Petr Jecmen
 */
public class ScenarioResult {

    private final long kernelExecutionTime;
    private float[] resultData;
    private long totalTime;
    private State state;

    public ScenarioResult(float[] resultData, long kernelExecutionTime) {
        this.resultData = resultData;
        this.kernelExecutionTime = kernelExecutionTime;

        if (resultData == null) {
            state = State.FAIL;
        } else {
            state = State.SUCCESS;
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
    
    public void markResultAsInvalidDynamic() {
        state = State.WRONG_RESULT_DYNAMIC;
    }
    
    public void markResultAsInvalidFixed() {
        state = State.WRONG_RESULT_FIXED;
    }

    public static enum State {

        SUCCESS,
        FAIL,
        WRONG_RESULT_FIXED,
        WRONG_RESULT_DYNAMIC
    }

}
