int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

int interpolate(const float x, const float y, global read_only int * image, const int imageWidth) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    float intensity = 0;    
    intensity += image[computeIndex(ix, iy, imageWidth)] * (1 - dx) * (1 - dy);
    intensity += image[computeIndex(ix+1, iy, imageWidth)] * dx * (1 - dy);
    intensity += image[computeIndex(ix, iy+1, imageWidth)] * (1 - dx) * dy;
    intensity += image[computeIndex(ix+1, iy+1, imageWidth)] * dx * dy;

    return intensity;    
}

kernel void CL15DIntPerDeformation(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * facets, global read_only int * facetCenters,
    global read_only float * deformations,
    global write_only float * result,
    global read_only int * deformationIndex,
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount) 
{        
    // id checks         
    const size_t facetId = get_global_id(0);
    if (facetId >= facetCount) {
        return;
    }
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int facetCoordCount = facetSize2 * 2;
    const int baseIndexFacet = facetId * facetCoordCount;         
    const int baseIndexFacetCenter = facetId * 2;
    const int baseIndexDeformation = deformationIndex[0] * 6;
    // deform facet
    float deformedFacet[-1*-1*2];    
    int indexFacet, i2, x, y, dx, dy;   
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        indexFacet = baseIndexFacet + i2;        
        
        x = facets[indexFacet];
        y = facets[indexFacet+1];

        dx = x - facetCenters[baseIndexFacetCenter];
        dy = y - facetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;                    
        deformedFacet[i2 + 1] = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy;    
    }
    // compute correlation using ZNCC
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    float val;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        indexFacet = baseIndexFacet + i2;
                
        // facet is just array of int coords        
        val = imageA[computeIndex(facets[indexFacet], facets[indexFacet + 1], imageWidth)];        
        facetI[i] = val;
        meanF += val;
        
        val = interpolate(deformedFacet[i2], deformedFacet[i2 + 1], imageB, imageWidth);                        
        deformedI[i] = val;
        meanG += val;
    } 
    meanF /= (float) facetSize2;
    meanG /= (float) facetSize2;
    
    float deltaF = 0;
    float deltaG = 0;    
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        indexFacet = baseIndexFacet + i2;
                                     
        val = facetI[i] - meanF;
        facetI[i] = val;
        deltaF += val * val;
                
        val = deformedI[i] - meanG;
        deformedI[i] = val;
        deltaG += val * val;
    }    
    const float deltaFs = sqrt(deltaF);
    const float deltaGs = sqrt(deltaG);    
    
    float resultVal = 0;                  
    for (int i = 0; i < facetSize2; i++) {        
        indexFacet = baseIndexFacet + i*2;        
        resultVal += facetI[i] * deformedI[i];
    }
    resultVal /= deltaFs * deltaGs;    
    
    //store result
    indexFacet = facetId * deformationCount + deformationIndex[0];
    result[indexFacet] = resultVal;    
}