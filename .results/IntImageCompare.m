clear all;
close all;
clc;
% 2D kernel int[] vs image2d comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
INSPECTED_TEST_CASE = TEST_CASE_SHIFT;
% Find best curves (fastest) for each variant
bestCurves = NaN(COUNT_LWS1, variantCount, graphCount);
bestCurvesParams = NaN(variantCount, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        min = intmax;
        minIndex = 1; 
        minLws0 = 1;
        for lws1=1:COUNT_LWS1
            for lws0=1:COUNT_LWS0
                val = allCurves(lws1, lws0, var, graph);
                if (val < min)
                    min = val;
                    minIndex = lws0;   
                    minLws0 = lws0;
                end;
            end;
        end;
        bestCurves(:, var, graph) = allCurves(:, minIndex, var, INSPECTED_TEST_CASE, graph);        
        bestCurvesParams(var, graph) = minLws0;        
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
            xlabel('LWS1');
            ylabel('Time [ms]');                        
            title(cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=' int2str(data(index, INDEX_FACET_SIZE)) ', dc=' int2str(data(index, INDEX_DEFORMATION_COUNT))]));            
            % plot both curves to one subfigure       
            hold on;                                    
            plot(x,bestCurves(:, VARIANT_NAIVE, innerBase),'-+','Color',colors(1, :), 'LineSmoothing','on')
            plot(x,bestCurves(:, VARIANT_INT, innerBase),'-x','Color',colors(2, :), 'LineSmoothing','on')
            plot(x,bestCurves(:, VARIANT_IMAGE, innerBase),'-o','Color',colors(3, :), 'LineSmoothing','on')            

            legend(h,['naive, LWS0 = ' int2str(bestCurvesParams(VARIANT_NAIVE,innerBase))], ['int[], LWS0 = ' int2str(bestCurvesParams(VARIANT_INT,innerBase))], ['image2d, LWS0 = ' int2str(bestCurvesParams(VARIANT_IMAGE,innerBase))]);
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;