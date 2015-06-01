/**
 * @author Lenam s.r.o.
 */
kernel void deformationGenerate(
    global float * deformationLimits, global int * deformationCounts, 
    global float * result, 
    const int deformationCount) {    
    const int id = get_global_id(0);    
    if (id >= deformationCount ) {
        return;
    }    
    
    float deformation[6];             
    const int resultBase = id * 6;
    
    int counter = id; 
    deformation[0] = counter % deformationCounts[0]; 
    counter = counter / deformationCounts[0]; 
    deformation[1] = counter % deformationCounts[1]; 
    counter = counter / deformationCounts[1]; 
    deformation[2] = counter % deformationCounts[2]; 
    counter = counter / deformationCounts[2]; 
    deformation[3] = counter % deformationCounts[3]; 
    counter = counter / deformationCounts[3]; 
    deformation[4] = counter % deformationCounts[4]; 
    counter = counter / deformationCounts[4]; 
    deformation[5] = counter % deformationCounts[5];         
        
    result[resultBase + 0] = deformationLimits[0] + deformation[0] * deformationLimits[2]; 
    result[resultBase + 1] = deformationLimits[3] + deformation[1] * deformationLimits[5]; 
    result[resultBase + 2] = deformationLimits[6] + deformation[2] * deformationLimits[8]; 
    result[resultBase + 3] = deformationLimits[9] + deformation[3] * deformationLimits[11]; 
    result[resultBase + 4] = deformationLimits[12] + deformation[4] * deformationLimits[14]; 
    result[resultBase + 5] = deformationLimits[15] + deformation[5] * deformationLimits[17];        
}