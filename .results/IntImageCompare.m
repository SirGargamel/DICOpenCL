clear all;
close all;
clc;
% 2D kernel int[] vs image2d comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
% Find best curves (fastest) for each variant
bestCurves = NaN(COUNT_LWS1, variantCount, graphCount);
bestCurvesParams = NaN(variantCount, graphCount);
for graph=1:graphCount
    for var=1:variantCount        
        m = allCurves(ANALYZED_TIME,:,:,var,ANALYZED_TEST_CASE,graph);
        [minVal, index] = min(m(:));
        [minLws1, minLws0] = ind2sub(size(m(:)), index);

        % extract line with lowest value found
        bestCurves(:, var, graph) = allCurves(ANALYZED_TIME, :, minLws0, var, ANALYZED_TEST_CASE, graph);        
        bestCurvesParams(var, graph) = minLws0;
    end;
end;
% Plot graphs
% split graphs to multiple windows
graphCountX = 3;
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
            h = subplot(graphCountY, graphCountX, (graphY-1) * graphCountX + graphX);
            xlabel('LWS1');
            ylabel('Time [ms]');                        
            title(cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=' int2str(data(index, INDEX_FACET_SIZE)) ', dc=' int2str(data(index, INDEX_DEFORMATION_COUNT))]));            
            % plot both curves to one subfigure       
            hold on;                                    
            plot(X,bestCurves(:, VARIANT_2D_NAIVE, innerBase),'-+','Color',COLORS(1, :), 'LineSmoothing','on')
            plot(X,bestCurves(:, VARIANT_2D_INT, innerBase),'-x','Color',COLORS(2, :), 'LineSmoothing','on')
            plot(X,bestCurves(:, VARIANT_2D_IMAGE, innerBase),'-o','Color',COLORS(3, :), 'LineSmoothing','on')           

            legend(h,['naive, LWS0 = ' int2str(bestCurvesParams(VARIANT_2D_NAIVE,innerBase))], ['int[], LWS0 = ' int2str(bestCurvesParams(VARIANT_2D_INT,innerBase))], ['image2d, LWS0 = ' int2str(bestCurvesParams(VARIANT_2D_IMAGE,innerBase))]);
            set(gca, 'XTick', X, 'XTickLabel', X_LABELS);
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;