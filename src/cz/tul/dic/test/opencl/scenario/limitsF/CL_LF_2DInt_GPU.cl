inline int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

inline int interpolate(const float x, const float y, global int * image, const int imageWidth) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    const float intensity = 
        image[computeIndex(ix, iy, imageWidth)] * (1 - dx) * (1 - dy)
        + image[computeIndex(ix+1, iy, imageWidth)] * dx * (1 - dy)
        + image[computeIndex(ix, iy+1, imageWidth)] * (1 - dx) * dy
        + image[computeIndex(ix+1, iy+1, imageWidth)] * dx * dy;

    return intensity;    
}

kernel void CL_LF_2DInt_GPU(
    global int * imageA, global int * imageB, 
    global float * facetCenters,
    global float * deformations,
    global  float * result,    
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
    const int baseIndexFacetCenter = facetId * 2;
    const int baseIndexDeformation = facetId * deformationCount * 6 + deformationId * 6;
    // top left facet coord
    const int baseX = (int) floor(facetCenters[baseIndexFacetCenter] - floor(facetSize / 2.0f));
    const int baseY = (int) floor(facetCenters[baseIndexFacetCenter + 1] - floor(facetSize / 2.0f));    
    // deform facet
     // compute correlation using ZNCC
    float deformedFacet[-1*-1*2];    
    int index, i2, x, y;
    float dx, dy;
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;        
        
        x = baseX + i % facetSize;
        y = baseY + i / facetSize;

        dx = x - facetCenters[baseIndexFacetCenter];
        dy = y - facetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;
        deformedFacet[i2 + 1] = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy;
        
        facetI[i] = imageA[computeIndex(x, y, imageWidth)];        
        meanF += facetI[i];
        
        deformedI[i] = interpolate(deformedFacet[i2], deformedFacet[i2 + 1], imageB, imageWidth);                        
        meanG += deformedI[i];
    }
    meanF /= (float) facetSize2;
    meanG /= (float) facetSize2;
    
    float deltaF = 0;
    float deltaG = 0;
    float val;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;
                                     
        val = facetI[i] - meanF;        
        deltaF += val * val;
                
        val = deformedI[i] - meanG;
        deltaG += val * val;
    }    
    const float deltaFs = sqrt(deltaF);
    const float deltaGs = sqrt(deltaG);
    const float delta = deltaFs * deltaGs;
    
    float resultVal = 0;                  
    for (int i = 0; i < facetSize2; i++) {        
        index = baseIndexFacet + i*2;
        
        val = (facetI[i] - meanF) * (deformedI[i] - meanG);
        val /= delta;
        resultVal += val;
    }   
    
    //store result
    index = facetId * deformationCount + deformationId;
    result[index] = resultVal;    
}