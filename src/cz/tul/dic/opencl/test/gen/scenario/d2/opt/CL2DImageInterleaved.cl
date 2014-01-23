int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

int interpolate(const float x, const float y, read_only image2d_t image) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    float intensity = 0;    
    intensity += read_imageui(image, sampler, (float2)(ix, iy)).x * (1 - dx) * (1 - dy);
    intensity += read_imageui(image, sampler, (float2)(ix+1, iy)).x * dx * (1 - dy);
    intensity += read_imageui(image, sampler, (float2)(ix, iy+1)).x * (1 - dx) * dy;
    intensity += read_imageui(image, sampler, (float2)(ix+1, iy+1)).x * dx * dy;            

    return intensity;    
}

kernel void CL2DImageInterleaved(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global read_only int * facets, global read_only int * facetCenters,
    global read_only float * deformations,
    global write_only float * result,    
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
    const int baseIndexDeformation = deformationId * 6;
    // deform facet
    float deformedFacet[-1*-1*2];
    int facet[-1*-1*2];
    int index, i2, dx, dy;    
    for (int i = 0; i < facetSize2; i++) {        
        i2 = i*2;
        index = i*facetCoordCount + 2*facetId;
        
        facet[i2] = facets[index];
        facet[i2 + 1] = facets[index + 1];

        dx = facet[i2] - facetCenters[baseIndexFacetCenter];
        dy = facet[i2+1] - facetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = facet[i2] + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;                    
        deformedFacet[i2 + 1] = facet[i2+1] + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy; 
    }
    // compute correlation using ZNCC
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;        
                
        // facet is just array of int coords        
        facetI[i] = read_imageui(imageA, sampler, (int2)(facet[i2], facet[i2 + 1])).x;        
        meanF += facetI[i];
        
        deformedI[i] = interpolate(deformedFacet[i2], deformedFacet[i2+1], imageB);        
        meanG += deformedI[i];
    } 
    meanF /= (float) facetSize2;
    meanG /= (float) facetSize2;    
    
    float deltaF = 0;
    float deltaG = 0;   
    for (int i = 0; i < facetSize2; i++) {                                            
        facetI[i] -= meanF;
        deltaF += facetI[i] * facetI[i];
                        
        deformedI[i] -= meanG;
        deltaG += deformedI[i] * deformedI[i];
    }    
    const float deltaFs = sqrt(deltaF);
    const float deltaGs = sqrt(deltaG);    
    
    float resultVal = 0;           
    for (int i = 0; i < facetSize2; i++) {            
        resultVal += facetI[i] * deformedI[i];
    }
    resultVal /= deltaFs * deltaGs;    
    
    //store result
    index = facetId * deformationCount + deformationId;
    result[index] = resultVal;    
}