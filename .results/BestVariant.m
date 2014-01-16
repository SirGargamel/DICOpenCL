clear all;
close all;
clc;
% Find best variant for each data config
% Display time and lws0 / lws1
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
VAL_VARIANT = 1;
VAL_TIME = 2;
VAL_LWS0 = 3;
VAL_LWS1 = 4;
ANALYZED_TIME = TIME_TOTAL;
% Find best value (fastest) for each variant
bestVariantData = repmat(intmax, 4, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        m = allCurves(ANALYZED_TIME,:,:,var,INSPECTED_TEST_CASE,graph);
        [minVal, index] = min(m(:));
        [minLws1, minLws0] = ind2sub(size(m(:)), index);

        if (minVal < bestVariantData(VAL_TIME, graph))
            bestVariantData(VAL_TIME, graph) = minVal;
            bestVariantData(VAL_VARIANT, graph) = var;
            bestVariantData(VAL_LWS0, graph) = minLws0;
            bestVariantData(VAL_LWS1, graph) = minLws1;
        end;        
    end;
end;
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
columnsPerGraph = 4;
graphCountX = 2;
graphCountY = 3;
graphsPerWindowCount = graphCountX * graphCountY * columnsPerGraph;
windowCount = ceil(graphCount / graphsPerWindowCount);
titles = cell(columnsPerGraph,1);
for win=1:windowCount
    index = (win-1) * graphsPerWindowCount * pointCount + 1;
    name = 'Kernel running time';
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphY=1:graphCountY
        for graphX=1:graphCountX        
            % plot multiple columns to one figure
            % compute line index
            innerBase = ((win-1) * graphsPerWindowCount) + ((((graphY-1) * graphCountX) + (graphX-1)) * columnsPerGraph) + 1;
            if (innerBase > graphCount)
                break;
            end;
            for i=1:columnsPerGraph
                index = ((innerBase - 1 + i) * pointCount);
                variant = NAMES_VARIANTS(bestVariantData(VAL_VARIANT, innerBase+i-1));
                titles(i) = cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=', int2str(data(index, INDEX_FACET_SIZE)) ', dc=', int2str(data(index, INDEX_DEFORMATION_COUNT)) ', var=' variant{:}]);
            end;            
            % plot data to subplot
            subplot(graphCountY, graphCountX, (graphY-1) * graphCountX + graphX);
            bar(bestVariantData(VAL_TIME, innerBase:innerBase+columnsPerGraph-1));
            set(gca,'xticklabel',titles);
            fix_xticklabels();
            ylabel('Time [ms]');
        end;
    end;
end;