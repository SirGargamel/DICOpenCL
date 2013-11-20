kernel void ComputeJavaIntDirect(
    global read_only int* imageA, global read_only int* imageB, 
    global read_only int* facets, global read_only int* deformations,
    global write_only float* result, 
    int imageWidth, int facetSize) 
{
    int facetCount = get_global_size(0);    
    int facetId = get_global_id(0);
    if (facetId >= facetCount) {
        return;
    }
    
    int deformationCount = get_global_size(1);
    int deformationId = get_global_id(1);
    if (deformationId >= deformationCount) {
        return;
    }
    
    
}