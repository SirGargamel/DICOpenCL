constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

inline int interpolate(const float2 coords, read_only image2d_t image) {
    const float ix = floor(coords.x);
    const float dx = coords.x - ix;
    
    const float iy = floor(coords.y);
    const float dy = coords.y - iy;

    const float intensity = 
        read_imageui(image, sampler, (float2)(ix, iy)).x * (1 - dx) * (1 - dy)
        + read_imageui(image, sampler, (float2)(ix+1, iy)).x * dx * (1 - dy)
        + read_imageui(image, sampler, (float2)(ix, iy+1)).x * (1 - dx) * dy
        + read_imageui(image, sampler, (float2)(ix+1, iy+1)).x * dx * dy;               

    return intensity;    
}

kernel void CL_LF_2DImageV(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global float2 * facetCenters,
    global float * deformations,
    global float * result,    
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
    const int baseIndexFacet = facetId * facetSize2;    
    const int baseIndexFacetCenter = facetId * 2;
    const int baseIndexDeformation = deformationId * 6;
    // top left facet coord
    float2 base = facetCenters[facetId];
    base -= (float2)(floor(facetSize / 2.0f), floor(facetSize / 2.0f));    
    // deform facet
    // compute correlation using ZNCC
    float2 deformedFacet[-1*-1];
    int index;
    float2 coords, def;
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < facetSize2; i++) {                
        index = baseIndexFacet + i;
        
        coords = base + (float2)(i % facetSize, i / facetSize);
        
        def = coords - facetCenters[facetId];        
        
        deformedFacet[i] = (float2)(
            coords.x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * def.x + deformations[baseIndexDeformation + 4] * def.y, 
            coords.y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * def.x + deformations[baseIndexDeformation + 5] * def.y);
            
        facetI[i] = read_imageui(imageA, sampler, coords).x;
        meanF += facetI[i];
        
        deformedI[i] = interpolate(deformedFacet[i], imageB);        
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
    
    float resultVal = 0;           
    for (int i = 0; i < facetSize2; i++) {              
        resultVal += facetI[i] * deformedI[i];
    }
    resultVal /= sqrt(deltaF) * sqrt(deltaG);
    
    //store result
    index = facetId * deformationCount + deformationId;
    result[index] = resultVal;    
}