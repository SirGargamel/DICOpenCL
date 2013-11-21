int computeIndex(const int x, const int y, const int width) {
    return ((y * width) + x);    
}

kernel void Compute2DIntGpuDirect(
    global read_only int * restrict imageA, global read_only int * restrict imageB, 
    global read_only int * restrict facets, global int * restrict deformedFacets,
    global read_only int * restrict deformations,
    global write_only float * restrict result,
    const float imageAavg, const float imageBavg,
    const int imageWidth, const int facetSize) 
{
    // id checks
    int facetCount = get_global_size(0);    
    int facetId = get_global_id(0);
    if (facetId >= facetCount) {
        return;
    }    
    int deformationCount = get_global_size(1);
    int deformationId = get_global_id(1);
    if ((2*deformationId) >= deformationCount) {
        return;
    }
    // index computation
    int facetSize2 = facetSize * facetSize;
    int facetCoordCount = facetSize2 * 2;
    
    int baseIndexFacet = facetId * facetCoordCount;    
    int baseIndexDeformation = deformationId * 2;
    // deform facet
    int index;
    for (int i = 0; i < facetSize2; i++) {
        index = baseIndexFacet + i * 2;
        deformedFacets[index] = facets[index] + deformations[baseIndexDeformation];
        deformedFacets[index + 1] = facets[index + 1] + deformations[baseIndexDeformation + 1];
    }
    // compute correlation using ZNCC
    float deltaF = 0;
    float deltaG = 0;
    int intensity, val;
    for (int i = 0; i < facetSize2; i++) {
        index = baseIndexFacet + i * 2;
        
        intensity = imageA[computeIndex(facets[index], facets[index + 1], imageWidth)];
        val = intensity - imageAavg;
        deltaF += val * val;
        
        intensity = imageB[computeIndex(deformedFacets[index], deformedFacets[index + 1], imageWidth)];
        val = intensity - imageBavg;
        deltaG += val * val;
    }    
    deltaF = sqrt(deltaF);
    deltaG = sqrt(deltaG);
    
    float resultVal = 0;
    for (int i = 0; i < facetSize2; i++) {
        index = baseIndexFacet + i * 2;
        
        resultVal += (imageA[computeIndex(facets[index], facets[index + 1], imageWidth)] - imageAavg) * (imageB[computeIndex(deformedFacets[index], deformedFacets[index + 1], imageWidth)] - imageBavg) / (deltaF * deltaG);
    }
    // store result
    index = facetId * deformationCount + deformationId;
//    result[index] = resultVal;
    
}