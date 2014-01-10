% Data reading
graphCount = csvread(FILE_NAME,0,0,[0,0,0,0]);
testCaseCount = csvread(FILE_NAME,0,1,[0,1,0,1]);
variantCount = csvread(FILE_NAME,0,2,[0,2,0,2]);
pointCounts = csvread(FILE_NAME,0,3,[0,3,0,1+ variantCount + 1]);
pointCount = sum(pointCounts) * testCaseCount;
fid = fopen(FILE_NAME);
allData = textscan(fid, FILE_FORMAT, 'headerlines', FILE_HEADER_LINE_COUNT, 'Delimiter', FILE_DELIMITER);
data = cell2mat(allData(:, 1:INDEX_TIME_KERNEL));
% Lines extraction
allCurves = NaN(COUNT_LWS1, COUNT_LWS0, variantCount, testCaseCount, graphCount);
for graph=1:graphCount
    for point=1:pointCount
        index = ((graph - 1) * pointCount) + point;
        
        variant = data(index, INDEX_VARIANT) + 1;
        testCase = data(index, INDEX_TEST_CASE) + 1;
        lws0 = data(index, INDEX_LWS0);
        lws1 = data(index, INDEX_LWS1);
        
        if (variant == VARIANT_JAVA)        
            if (lws0 == 1)
                lws0 = 2;
                lws1 = 8;
            elseif (lws0 == 7)
                lws0 = 8;
                lws1 = 2;
            end;                              
        end;
        
        % multiple variants, multiple test cases
        % one graph per data combination (w, h, fs, dc)
        % one line for each LWS0, variant and test case
        allCurves(log2(lws1) + 1, log2(lws0) + 1, variant, testCase, graph) = data(index, ANALYZED_TIME);
    end;
end;