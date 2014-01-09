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
/**
 * @author Petr Jecmen
 */
kernel void Compute1DIntPerFacetSingle(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * facet, global read_only int * facetCenter,
    global read_only float * deformations,
    global write_only float * result,    
    const int imageWidth, const int deformationCount,
    const int facetSize) 
{
    // id checks       
    const size_t deformationId = get_global_id(0);
    if (deformationId >= deformationCount) {
        return;
    }    
    
    // index computation
    const int facetSize2 = facetSize * facetSize;
    const int facetCoordCount = facetSize2 * 2;    
    const int baseIndexDeformation = deformationId * 6;
    // deform facet
    float deformedFacet[50*50*2];    
    int indexFacet, i2, x, y, dx, dy;   
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;          
        
        x = facet[i2];
        y = facet[i2+1];

        dx = x - facetCenter[0];
        dy = y - facetCenter[1];
        
        deformedFacet[i2] = x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * dx + deformations[baseIndexDeformation + 4] * dy;                    
        deformedFacet[i2 + 1] = y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * dx + deformations[baseIndexDeformation + 5] * dy;    
    }
    // compute correlation using ZNCC
    float deformedI[50*50];
    float facetI[50*50];
    float meanF = 0;
    float meanG = 0; 
    float val;
    for (int i = 0; i < facetSize2; i++) {
        i2 = i*2;        
                
        // facet is just array of int coords        
        val = imageA[computeIndex(facet[i2], facet[i2 + 1], imageWidth)];        
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
    const float deltaFs = sqrt(deltaF);
    const float deltaGs = sqrt(deltaG);    
    
    float resultVal = 0;                  
    for (int i = 0; i < facetSize2; i++) {                    
        resultVal += facetI[i] * deformedI[i];
    }    
    
    //store result    
    result[deformationId] = resultVal / (deltaFs * deltaGs);
}