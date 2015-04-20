clear all;
% close all;
clc;
% Find best variant for each data config
% Display time and lws0 / lws1
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
VAL_VARIANT = 1;
VAL_TIME_KERNEL = 2;
VAL_TIME_OVERHEAD = 3;
VAL_TIME_TOTAL = 4;
VAL_LWS0 = 5;
VAL_LWS1 = 6;
% Find best value (fastest) for each variant
bestVariantData = repmat(intmax, 6, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        m = squeeze(allCurves(TIME_TOTAL,:,:,var,ANALYZED_TEST_CASE,graph));
        m(m == 0) = NaN;
        [minVal, index] = min(m(:));
        [minLws1, minLws0] = ind2sub(size(m), index);

        if (minVal < bestVariantData(VAL_TIME_KERNEL, graph))
            bestVariantData(VAL_TIME_TOTAL, graph) = minVal;
            bestVariantData(VAL_TIME_KERNEL, graph) = allCurves(TIME_KERNEL,minLws1,minLws0,var,ANALYZED_TEST_CASE,graph);
            bestVariantData(VAL_TIME_OVERHEAD, graph) = bestVariantData(VAL_TIME_TOTAL, graph) - bestVariantData(VAL_TIME_KERNEL, graph);
            bestVariantData(VAL_VARIANT, graph) = var;
            bestVariantData(VAL_LWS0, graph) = minLws0;
            bestVariantData(VAL_LWS1, graph) = minLws1;
        end;        
    end;
end;
plotData = bestVariantData(2:3, :);
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
columnsPerGraph = 9;
graphCountX = 1;
graphCountY = 1;
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
                innerBaseI = innerBase - 1 + i;
                if (innerBaseI > size(bestVariantData, 2))
                    break;
                end;
                
                index = innerBaseI * pointCount;                
                variant = NAMES_VARIANTS(bestVariantData(VAL_VARIANT, innerBase+i-1));
                titles(i) = cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=', int2str(data(index, INDEX_FACET_SIZE)) ', dc=', int2str(data(index, INDEX_DEFORMATION_COUNT)) ', var=' variant{:} ', time=' int2str(bestVariantData(VAL_TIME_KERNEL, innerBaseI)) '+' int2str(bestVariantData(VAL_TIME_OVERHEAD, innerBaseI))]);
            end;            
            % plot data to subplot
            subplot(graphCountY, graphCountX, (graphY-1) * graphCountX + graphX);
            bar(bestVariantData(VAL_TIME_KERNEL:VAL_TIME_OVERHEAD, innerBase:min(innerBase+columnsPerGraph-1, size(bestVariantData, 2)))','stacked');
            set(gca,'xticklabel',titles);
            fix_xticklabels();
            
            ylabel('Time [ms]');
        end;
    end;
end;