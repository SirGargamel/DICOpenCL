clear all;
close all;
clc;
% best times of each variant comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
% Find best curves (fastest) for each variant
bestCurves = NaN(variantCount, graphCount);
bestCurvesParams = NaN(2, variantCount, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        min = intmax;        
        minLws0 = 1;
        minLws1 = 1;
        for lws1=1:COUNT_LWS1
            for lws0=1:COUNT_LWS0
                val = allCurves(lws1, lws0, var, graph);
                if (val < min)
                    min = val;                    
                    minLws0 = lws0;
                    minLws1 = lws1;
                end;
            end;
        end;
        bestCurves(var, graph) = min;        
        bestCurvesParams(1, var, graph) = minLws0;
        bestCurvesParams(2, var, graph) = minLws1;
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
splitCount = 3;
dSplitCount = splitCount * splitCount;
windowCount = ceil(graphCount / dSplitCount);
for win=1:windowCount
    index = (win-1) * dSplitCount * pointCount + 1;
    name = 'Kernel running time';
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphX=1:splitCount
        for graphY=1:splitCount
            % compute line index
            innerBase = ((win-1) * dSplitCount) + ((graphX-1) * splitCount) + graphY;
            if (innerBase > graphCount)
                break;
            end;   
            index = ((innerBase - 1) * pointCount) + 1;
            % create subplot
            h = subplot(splitCount, splitCount, (graphX-1) * 3 + graphY);                                 
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