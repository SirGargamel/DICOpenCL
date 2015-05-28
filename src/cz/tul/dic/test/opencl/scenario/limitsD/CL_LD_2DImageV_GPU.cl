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

kernel void CL_LD_2DImageV_GPU(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global int2 * facets, global float2 * facetCenters,
    global float * deformationLimits, global int * deformationCounts,
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
    // generate deformation
    float deformation[6];     
    const int limitsBase = facetId * 18; 
    const int countsBase = facetId * 7; 
    if (deformationId >= deformationCounts[countsBase + 6]) { return; } 	
    int counter = deformationId; 
    deformation[0] = counter % deformationCounts[countsBase + 0]; 
    counter = counter / deformationCounts[countsBase + 0]; 
    deformation[1] = counter % deformationCounts[countsBase + 1]; 
    counter = counter / deformationCounts[countsBase + 1]; 
    deformation[2] = counter % deformationCounts[countsBase + 2]; 
    counter = counter / deformationCounts[countsBase + 2]; 
    deformation[3] = counter % deformationCounts[countsBase + 3]; 
    counter = counter / deformationCounts[countsBase + 3]; 
    deformation[4] = counter % deformationCounts[countsBase + 4]; 
    counter = counter / deformationCounts[countsBase + 4]; 
    deformation[5] = counter % deformationCounts[countsBase + 5]; 
    counter = counter / deformationCounts[countsBase + 5]; 
    deformation[0] = deformationLimits[limitsBase + 0] + deformation[0] * deformationLimits[limitsBase + 2]; 
    deformation[1] = deformationLimits[limitsBase + 3] + deformation[1] * deformationLimits[limitsBase + 5]; 
    deformation[2] = deformationLimits[limitsBase + 6] + deformation[2] * deformationLimits[limitsBase + 8]; 
    deformation[3] = deformationLimits[limitsBase + 9] + deformation[3] * deformationLimits[limitsBase + 11]; 
    deformation[4] = deformationLimits[limitsBase + 12] + deformation[4] * deformationLimits[limitsBase + 14]; 
    deformation[5] = deformationLimits[limitsBase + 15] + deformation[5] * deformationLimits[limitsBase + 17]; 
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
        
        coords = convert_float2(facets[index]);
        
        def = coords - facetCenters[facetId];        
        
        deformedFacet[i] = (float2)(
            coords.x + deformation[0] + deformation[2] * def.x + deformation[4] * def.y, 
            coords.y + deformation[1] + deformation[3] * def.x + deformation[5] * def.y);
            
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