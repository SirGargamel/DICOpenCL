/**
 * @author Lenam s.r.o.
 */
kernel void deformationCopy(
    global float * result, 
    const int deformationCount, const int facetCount) {    
    const int idDef = get_global_id(0);    
    const int idFac = get_global_id(1);    
    if (idDef >= deformationCount || idFac >= facetCount ) {
        return;
    }    
                   
    const int resultBase = idDef * 6;    
    const int facetOffset = deformationCount * 6;
    
    result[(idFac * facetOffset) + resultBase + 0] = result[resultBase + 0];
    result[(idFac * facetOffset) + resultBase + 1] = result[resultBase + 1];
    result[(idFac * facetOffset) + resultBase + 2] = result[resultBase + 2];
    result[(idFac * facetOffset) + resultBase + 3] = result[resultBase + 3];
    result[(idFac * facetOffset) + resultBase + 4] = result[resultBase + 4];
    result[(idFac * facetOffset) + resultBase + 5] = result[resultBase + 5];    
}