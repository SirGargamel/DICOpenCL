inline int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

inline int interpolate(const float x, const float y, global read_only int * image, const int imageWidth) {
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
/**
 * @author Petr Jecmen
 */
kernel void CL1DIntPerDeformationSingle(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * facets, global read_only float * facetCenters,
    global read_only float * deformation,
    global write_only float * result,    
    const int imageWidth, const int facetCount,
    const int facetSize) 
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
    // deform facet
    float deformedFacet[-1*-1*2];    
    int index, i2, x, y;
    float dx, dy;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2; 
        
        x = facets[index];
        y = facets[index + 1];

        dx = x - facetCenters[baseIndexFacetCenter];
        dy = y - facetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = x + deformation[0] + deformation[2] * dx + deformation[4] * dy;                    
        deformedFacet[i2 + 1] = y + deformation[1] + deformation[3] * dx + deformation[5] * dy;    
    }
    // compute correlation using ZNCC
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    float val;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;
                
        // facet is just array of int coords        
        val = imageA[computeIndex(facets[index], facets[index + 1], imageWidth)];        
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
        val = facetI[i] - meanF;
        facetI[i] = val;
        deltaF += val * val;
                
        val = deformedI[i] - meanG;
        deformedI[i] = val;
        deltaG += val * val;
    }   
    
    val = 0;                 
    for (int i = 0; i < facetSize2; i++) {                    
        val += facetI[i] * deformedI[i];
    }    
    val /= sqrt(deltaF) * sqrt(deltaG);
    
    //store result    
    result[facetId] = val;
}