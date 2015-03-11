constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

inline int interpolate(const float x, const float y, read_only image2d_t image) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    const float intensity = 
        read_imageui(image, sampler, (float2)(ix, iy)).x * (1 - dx) * (1 - dy)
        + read_imageui(image, sampler, (float2)(ix+1, iy)).x * dx * (1 - dy)
        + read_imageui(image, sampler, (float2)(ix, iy+1)).x * (1 - dx) * dy
        + read_imageui(image, sampler, (float2)(ix+1, iy+1)).x * dx * dy;               

    return intensity;    
}

kernel void CL_LF_1DImageLL(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global float * facetCenters,
    global float * deformations,
    global float * result,        
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
    // top left facet coord
    const int baseX = (int) floor(facetCenters[baseIndexFacetCenter] - (facetSize / 2.0f));
    const int baseY = (int) floor(facetCenters[baseIndexFacetCenter + 1] - (facetSize / 2.0f));    
    // load facet to local memory    
    local int facetLocal[-1*-1*2];    
    if (groupSize >= facetSize2) {
        if (localId < facetSize2) {
            facetLocal[localId * 2] = baseX + localId % facetSize;
            facetLocal[localId * 2 + 1] = baseY + localId / facetSize;
        }    
    } else {
        const int runCount = facetSize2 / groupSize;
        int index;
        for (int i = 0; i < runCount; i++) {
            index = i*groupSize + localId;                    
            facetLocal[index * 2] = baseX + index % facetSize;
            facetLocal[index * 2 + 1] = baseY + index / facetSize;
        }
        const int rest = facetSize2 % groupSize;
        if (localId < rest) {
            index = groupSize * runCount + localId;
            facetLocal[index * 2] = baseX + index % facetSize;
            facetLocal[index * 2 + 1] = baseY + index / facetSize;
        }
    }        
    barrier(CLK_LOCAL_MEM_FENCE);
    if (deformationId >= deformationCount) {
        return;
    }
    // deform facet
    // compute correlation using ZNCC
    float deformedFacet[-1*-1*2];
    int i2, x, y;
    float dx, dy;
    float deformedI[-1*-1];
    float facetI[-1*-1];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;             
        
        x = facetLocal[i2];
        y = facetLocal[i2+1];

        dx = x - facetCenters[baseIndexFacetCenter];
        dy = y - facetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;
        deformedFacet[i2 + 1] = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy;
        
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
    
    float resultVal = 0;           
    for (int i = 0; i < facetSize2; i++) {              
        resultVal += facetI[i] * deformedI[i];
    }
    resultVal /= sqrt(deltaF) * sqrt(deltaG);   
    
    //store result    
    result[facetId * deformationCount + deformationId] = resultVal;    
}