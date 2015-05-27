/**
 * @author Lenam s.r.o.
 */
kernel void generateFacets(
    global float * facetCenters, global int * result,
    const int facetCount, const int facetSize) {
    const int id = get_global_id(0);
    if (id >= facetCount) {
        return;
    }
    
    const int facetSize2 = facetSize * facetSize;
    const int baseIndexResult = id * facetSize2 * 2;
    const int baseIndexFacetCenter = id * 2;
    const int baseX = (int) floor(facetCenters[baseIndexFacetCenter] - (facetSize / 2.0f));
    const int baseY = (int) floor(facetCenters[baseIndexFacetCenter + 1] - (facetSize / 2.0f));
    
    for (int i = 0; i < facetSize2; i++) {
        result[baseIndexResult + i*2] = baseX + i % facetSize;
        result[baseIndexResult + i*2 + 1] = baseY + i / facetSize;
    }
}