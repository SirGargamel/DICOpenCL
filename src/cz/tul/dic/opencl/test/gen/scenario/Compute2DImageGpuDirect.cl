int computeIndex(const int x, const int y, const int width) {
    return (y * width) + x;    
}

constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

kernel void Compute2DImageGpuDirect(
    global read_only image2d_t imageA, global read_only image2d_t imageB, 
    global read_only int * facets,
    global read_only float * deformations,
    global write_only float * result,
    const float imageAavg, const float imageBavg,
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount) 
{
    float temp[100*100*2];
    // id checks    
    size_t facetId = get_global_id(0);
    if (facetId >= facetCount) {
        return;
    }        
    size_t deformationId = get_global_id(1);
    if (deformationId >= deformationCount) {
        return;
    }
    // index computation
    int facetSize2 = facetSize * facetSize;
    int facetCoordCount = facetSize2 * 2;
    
    int baseIndexFacet = facetId * facetCoordCount;         
    int baseIndexDeformation = deformationId * 2;
    // deform facet
    int indexFacet;    
    for (int i = 0; i < facetSize2; i++) {
        indexFacet = baseIndexFacet + (i * 2);        
        
        temp[2*i] = facets[indexFacet] + deformations[baseIndexDeformation];
        temp[2*i + 1] = facets[indexFacet+1] + deformations[baseIndexDeformation+1];        
    }
    // compute correlation using ZNCC
    float deltaF = 0;
    float deltaG = 0;
    int intensity;
    int index;
    float val;
    for (int i = 0; i < facetSize2; i++) {
        indexFacet = baseIndexFacet + (i * 2);
                
        intensity = read_imagei(imageA, sampler, (int2)(facets[indexFacet], facets[indexFacet + 1])).x;        
        val = intensity - imageAavg;
        deltaF += val * val;
                
        intensity = read_imagei(imageB, sampler, (int2)(temp[2*i], temp[2*i + 1])).x;
        val = intensity - imageBavg;
        deltaG += val * val;
    }    
    float deltaFs = sqrt((float) deltaF);
    float deltaGs = sqrt((float) deltaG);
    float delta = deltaFs * deltaGs;
    
    float resultVal = 0;
    if (delta != 0) {                
        for (int i = 0; i < facetSize2; i++) {
            indexFacet = baseIndexFacet + (i * 2);
        
            resultVal += (read_imagei(imageA, sampler, (int2)(facets[indexFacet], facets[indexFacet + 1])).x - imageAavg)
             * (read_imagei(imageB, sampler, (int2)(temp[2*i], temp[2*i + 1])).x - imageBavg);                    
        }
        resultVal /= delta;
    }
    
    //store result
    indexFacet = facetId * deformationCount + deformationId;
    result[indexFacet] = resultVal;    
}