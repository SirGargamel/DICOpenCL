clear all;
close all;
clc;
% best times of each variant comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
INSPECTED_TEST_CASE = TEST_CASE_SHIFT;
% Find best curves (fastest) for each variant
bestCurves = NaN(variantCount, graphCount);
bestCurvesParams = NaN(2, variantCount, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        m = allCurves(:,:,var,INSPECTED_TEST_CASE,graph);
        [minVal, index] = min(m(:));
        [minLws1, minLws0] = ind2sub(size(m(:)), index);

        bestCurves(var, graph) = minVal;        
    end;
end;
% Plot graphs
% line colors
colors = [[0 0 0];[1 0 0 ];[0 1 0]];
% x-axis value labels
x = 1 : COUNT_LWS1;
xlabels = cell(COUNT_LWS1,1);
for i=1:COUNT_LWS1
    xlabels(i) = cellstr(int2str(2^(i-1)));
end;
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
            h = subplot(graphCountX, graphCountY, (graphY-1) * graphCountX + graphX);                                 
            ylabel('Time [ms]');                        
            title(cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=' int2str(data(index, INDEX_FACET_SIZE)) ', dc=' int2str(data(index, INDEX_DEFORMATION_COUNT))]));            
            % plot both curves to one subfigure       
            hold on;                                    
            bar(bestCurves(:, innerBase));
            set(gca, 'XTickLabel',NAMES_VARIANTS, 'XTick',1:numel(NAMES_VARIANTS));

            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;