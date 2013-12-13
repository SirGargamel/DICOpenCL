int computeIndex(const int x, const int y, const int width) {
    return (y * width) + x;    
}

int interpolate(const float x, const float y, global read_only int * image, const int imageWidth) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    float intensity = 0;    
    intensity += image[computeIndex(x, y, imageWidth)] * (1 - dx) * (1 - dy);
    intensity += image[computeIndex(x+1, y, imageWidth)] * dx * (1 - dy);
    intensity += image[computeIndex(x, y+1, imageWidth)] * (1 - dx) * dy;
    intensity += image[computeIndex(x+1, y+1, imageWidth)] * dx * dy;

    return intensity;    
}

kernel void Compute2DIntGpuDirect(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * facets, global read_only int * facetCenters,
    global read_only float * deformations,
    global write_only float * result,
    const float imageAavg, const float imageBavg,
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount) 
{        
    // id checks    
    const size_t facetId = get_global_id(0);
    if (facetId >= facetCount) {
        return;
    }        
    const size_t deformationId = get_global_id(1);
    if (deformationId >= deformationCount) {
        return;
    }
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int facetCoordCount = facetSize2 * 2;
    const int baseIndexFacet = facetId * facetCoordCount;         
    const int baseIndexDeformation = deformationId * 6;
    // deform facet
    float deformedFacet[100*100*2];    
    int indexFacet, i2;    
    int x, y, dx, dy, val;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        indexFacet = baseIndexFacet + i2;        
        
        x = facets[indexFacet];
        y = facets[indexFacet+1];

        dx = x - facetCenters[facetId * 2];
        dy = y - facetCenters[facetId * 2 + 1];

        val = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;        
        deformedFacet[i2] = val;
            
        val = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy;            
        deformedFacet[i2 + 1] = val;    
    }
    // compute correlation using ZNCC
    int deformedI[100*100];
    int facetI[100*100];
    float deltaF = 0;
    float deltaG = 0;  
    float value;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        indexFacet = baseIndexFacet + i2;
                
        // facet is just array of int coords        
        facetI[i] = imageA[computeIndex(facets[indexFacet], facets[indexFacet + 1], imageWidth)];
        value = facetI[i] - imageAavg;
        deltaF += value * value;
        
        deformedI[i] = interpolate(deformedFacet[i2], deformedFacet[i2 + 1], imageB, imageWidth);                
        value = deformedI[i] - imageBavg;
        deltaG += value * value;
    }    
    const float deltaFs = sqrt(deltaF);
    const float deltaGs = sqrt(deltaG);
    const float delta = deltaFs * deltaGs;
    
    float resultVal = 0;                  
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;        
        indexFacet = baseIndexFacet + i2;
        
        resultVal += (facetI[i] - imageAavg)
         * (deformedI[i] - imageBavg);                    
    }
    resultVal /= delta;    
    
    //store result
    indexFacet = facetId * deformationCount + deformationId;
    result[indexFacet] = resultVal;    
}