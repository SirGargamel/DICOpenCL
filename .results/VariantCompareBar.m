clear all;
close all;
clc;
% best times of each variant comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
INSPECTED_TEST_CASE = TEST_CASE_SHIFT;
ANALYZED_TIME = TIME_TOTAL;
% Find best curves (fastest) for each variant
bestCurves = NaN(2,variantCount, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        m = squeeze(allCurves(ANALYZED_TIME,:,:,var,INSPECTED_TEST_CASE,graph));        
        [minVal, index] = min(m(:));        
        [minLws1, minLws0] = ind2sub(size(m), index);

        bestCurves(1, var, graph) = allCurves(TIME_KERNEL,minLws1,minLws0,var,INSPECTED_TEST_CASE,graph);
        bestCurves(2, var, graph) = allCurves(TIME_TOTAL,minLws1,minLws0,var,INSPECTED_TEST_CASE,graph) - bestCurves(1, var, graph);
    end;
end;
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
graphCountX = 2;
graphCountY = 3;
graphsPerWindowCount = graphCountX * graphCountY;
windowCount = ceil(graphCount / graphsPerWindowCount);
for win=1:windowCount
    index = (win-1) * graphsPerWindowCount * pointCount + 1;
    name = 'Kernel running time';
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphX=1:graphCountX
        for graphY=1:graphCountY
            % compute line index
            innerBase = ((win-1) * graphsPerWindowCount) + ((graphY-1) * graphCountX) + graphX;
            if (innerBase > graphCount)
                break;
            end;   
            index = ((innerBase - 1) * pointCount) + 1;
            % create subplot
            subplot(graphCountY, graphCountX, (graphY-1) * graphCountX + graphX);
            ylabel('Time [ms]');                        
            title(cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=' int2str(data(index, INDEX_FACET_SIZE)) ', dc=' int2str(data(index, INDEX_DEFORMATION_COUNT))]));
            % plot both curves to one subfigure       
            hold on;                                    
            bar(bestCurves(:, :, innerBase)','stacked');
            set(gca,'XTick', X(1:numel(bestCurves(1, :, innerBase))), 'XTickLabel',NAMES_VARIANTS);
            fix_xticklabels();
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;