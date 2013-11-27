package cz.tul.dic.opencl.test.gen.scenario;

/**
 *
 * @author Petr Jecmen
 */
public class ScenarioResult {
    
    private float[] resultData;
    private long totalTime;
    private final long kernelExecutionTime;

    public ScenarioResult(float[] resultData, long kernelExecutionTime) {
        this.resultData = resultData;
        this.kernelExecutionTime = kernelExecutionTime;
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
    
}
