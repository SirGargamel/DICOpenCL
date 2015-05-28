/**
 * @author Lenam s.r.o.
 */
kernel void generateFacets2D(
    global float * facetCenters, global int * result,
    const int facetCount, const int facetSize) {
    const int idF = get_global_id(0);
    const int idC = get_global_id(1);
    const int facetSize2 = facetSize * facetSize;
    
    if (idF >= facetCount || idC >= facetSize2) {
        return;
    }
    
    
    const int baseIndexResult = idF * facetSize2 * 2;
    const int baseIndexFacetCenter = idF * 2;
    const int baseX = (int) floor(facetCenters[baseIndexFacetCenter] - (facetSize / 2.0f));
    const int baseY = (int) floor(facetCenters[baseIndexFacetCenter + 1] - (facetSize / 2.0f));
        
    result[baseIndexResult + idC*2] = baseX + idC % facetSize;
    result[baseIndexResult + idC*2 + 1] = baseY + idC / facetSize;    
}