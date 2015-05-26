int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

int interpolate(const float2 coords, read_only image2d_t image) {
    const float ix = floor(coords.x);
    const float dx = coords.x - ix;
    
    const float iy = floor(coords.y);
    const float dy = coords.y - iy;

    float intensity = 0;    
    intensity += read_imageui(image, sampler, (float2)(ix, iy)).x * (1 - dx) * (1 - dy);
    intensity += read_imageui(image, sampler, (float2)(ix+1, iy)).x * dx * (1 - dy);
    intensity += read_imageui(image, sampler, (float2)(ix, iy+1)).x * (1 - dx) * dy;
    intensity += read_imageui(image, sampler, (float2)(ix+1, iy+1)).x * dx * dy;            

    return intensity;    
}

kernel void CL_LF_1D_I_V_LL(
    read_only image2d_t imageA, read_only image2d_t imageB, 
    global float2 * facetCenters,
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
    const int baseIndexFacet = facetId * facetSize2;   
    const int baseIndexFacetCenter = facetId * 2;
    const int baseIndexDeformation = facetId * deformationCount * 6 + deformationId * 6;
    // top left facet coord
    float2 base = facetCenters[facetId];
    base -= (float2)(floor(facetSize / 2.0f), floor(facetSize / 2.0f));
    // load facet to local memory    
    local float2 facetLocal[-1*-1];    
    if (groupSize >= facetSize2) {
        if (localId < facetSize2) {
            facetLocal[localId] = base + (float2)(localId % facetSize, localId / facetSize);            
        }    
    } else {
        const int runCount = facetSize2 / groupSize;
        int index;
        for (int i = 0; i < runCount; i++) {
            index = i*groupSize + localId;                    
            facetLocal[index] = base + (float2)(index % facetSize, index / facetSize);
        }
        const int rest = facetSize2 % groupSize;
        if (localId < rest) {
            index = groupSize * runCount + localId;
            facetLocal[index] = base + (float2)(index % facetSize, index / facetSize);
        }
    }        
    barrier(CLK_LOCAL_MEM_FENCE);
    if (deformationId >= deformationCount) {
        return;
    }
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