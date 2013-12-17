package cz.tul.dic.opencl.test.gen.data;

import cz.tul.dic.opencl.test.gen.scenario.ScenarioResult;

/**
 *
 * @author Petr Jecmen
 */
public interface TestCase {
    
     int[][] generateImages(final int width, final int height);
     
     int[] generateFacetCenters(final int width, final int height, final int size);
     
     int[] generateFacetData(final int[] facetCenters, final int size);
     
     float[] generateDeformations(final int deformationCount);
     
     void checkResult(final ScenarioResult result, final int facetCount);
    
}
