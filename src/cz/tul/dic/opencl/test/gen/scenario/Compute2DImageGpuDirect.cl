int computeIndex(const int x, const int y, const int width) {
    return (y * width) + x;    
}

constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

int interpolate(const float x, const float y, global read_only image2d_t image) {
    const float ix = floor(x);
    const float dx = x - ix;
    
    const float iy = floor(y);
    const float dy = y - iy;

    float intensity = 0;    
    intensity += read_imagei(image, sampler, (int2)(x, y)).x * (1 - dx) * (1 - dy);
    intensity += read_imagei(image, sampler, (int2)(x+1, y)).x * dx * (1 - dy);
    intensity += read_imagei(image, sampler, (int2)(x, y+1)).x * (1 - dx) * dy;
    intensity += read_imagei(image, sampler, (int2)(x+1, y+1)).x * dx * dy;        

    return intensity;    
}

kernel void Compute2DImageGpuDirect(
    global read_only image2d_t imageA, global read_only image2d_t imageB, 
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
    int indexFacet, i2, x, y, dx, dy, val;    
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
    int intensity;
    int index;
    float value;
    for (int i = 0; i < facetSize2; i++) {
        indexFacet = baseIndexFacet + (i * 2);
                
        facetI[i] = read_imagei(imageA, sampler, (int2)(facets[indexFacet], facets[indexFacet + 1])).x;        
        value = facetI[i] - imageAavg;
        deltaF += value * value;
                
        deformedI[i] = interpolate(deformedFacet[2*i], deformedFacet[2*i + 1], imageB);
        value = deformedI[i] - imageBavg;
        deltaG += value * value;
    }    
    float deltaFs = sqrt((float) deltaF);
    float deltaGs = sqrt((float) deltaG);
    float delta = deltaFs * deltaGs;
    
    float resultVal = 0;
    if (delta != 0) {                
        for (int i = 0; i < facetSize2; i++) {
            indexFacet = baseIndexFacet + (i * 2);
        
            resultVal += (facetI[i] - imageAavg)
             * (deformedI[i] - imageBavg);                    
        }
        resultVal /= delta;
    }
    
    //store result
    indexFacet = facetId * deformationCount + deformationId;
    result[indexFacet] = resultVal;    
}