constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

inline int interpolate(const float x, const float y, read_only image2d_t image) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    const float intensity = 
        read_imageui(image, sampler, (float2)(ix, iy)).x * (1 - dx) * (1 - dy);
        + read_imageui(image, sampler, (float2)(ix+1, iy)).x * dx * (1 - dy);
        + read_imageui(image, sampler, (float2)(ix, iy+1)).x * (1 - dx) * dy;
        + read_imageui(image, sampler, (float2)(ix+1, iy+1)).x * dx * dy;               

    return intensity;    
}

kernel void CL1DImageLL(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global read_only int * facets, global read_only int * facetCenters,
    global read_only float * deformations,
    global write_only float * result,        
    const int imageWidth, const int deformationCount,
    const int facetSize, const int facetCount,
    const int groupCountPerFacet) 
{        
    //// ID checks    
    // facet
    const size_t groupId = get_group_id(0);
    const size_t facetId = groupId / groupCountPerFacet;
    if (facetId >= facetCount) {
        return;
    }       
    // deformation    
    const int groupSubId = groupId % groupCountPerFacet;
    const size_t localId = get_local_id(0);
    const size_t groupSize = get_local_size(0);
    const int deformationId = groupSubId * groupSize + localId;    
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int facetCoordCount = facetSize2 * 2;    
    const int baseIndexFacet = facetId * facetCoordCount; 
    const int baseIndexFacetCenter = facetId * 2;
    const int baseIndexDeformation = deformationId * 6;        
    // load facet to local memory    
    local int facetLocal[-1*-1*2];    
    if (groupSize >= facetCoordCount) {
        if (localId < facetCoordCount) {
            facetLocal[localId] = facets[baseIndexFacet + localId];
        }    
    } else {
        const int runCount = facetCoordCount / groupSize;
        int index;
        for (int i = 0; i < runCount; i++) {
            index = i*groupSize + localId;
            facetLocal[index] = facets[baseIndexFacet + index];
        }
        const int rest = facetCoordCount % groupSize;
        if (localId < rest) {
            index = groupSize * runCount + localId;
            facetLocal[index] = facets[baseIndexFacet + index];
        }
    }        
    barrier(CLK_LOCAL_MEM_FENCE);
    if (deformationId >= deformationCount) {
        return;
    }
    // deform facet
    float deformedFacet[-1*-1*2];
    int i2, x, y, dx, dy;    
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;             
        
        x = facetLocal[i2];
        y = facetLocal[i2+1];

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
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;       
                
        // facet is just array of int coords        
        facetI[i] = read_imageui(imageA, sampler, (int2)(facetLocal[i2], facetLocal[i2 + 1])).x;        
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
    result[facetId * deformationCount + deformationId] = resultVal;    
}