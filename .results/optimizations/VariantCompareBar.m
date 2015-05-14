clear all;
% close all;
clc;
% best times of each variant comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
ANALYZED_TIME = TIME_TOTAL;
% Find best curves (fastest) for each variant
bestCurves = NaN(2,variantCount, graphCount);
for graph=1:graphCount
    for v=1:variantCount
        m = squeeze(allCurves(ANALYZED_TIME,:,:,v,ANALYZED_TEST_CASE,graph));        
        m(m == 0) = NaN;
        [minVal, index] = min(m(:));        
        [minLws1, minLws0] = ind2sub(size(m), index);

        bestCurves(1, v, graph) = allCurves(TIME_KERNEL,minLws1,minLws0,v,ANALYZED_TEST_CASE,graph);
        bestCurves(3, v, graph) = allCurves(TIME_TOTAL,minLws1,minLws0,v,ANALYZED_TEST_CASE,graph);
        bestCurves(2, v, graph) = bestCurves(3, v, graph) - bestCurves(1, v, graph);
    end;
end;
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
graphCountX = 1;
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
            bar(bestCurves(1:2, :, innerBase)','stacked');
            set(gca,'XTick', 1:numel(bestCurves(1, :, innerBase)), 'XTickLabel',NAMES_VARIANTS);
            fix_xticklabels();
            % values on top
            text(1:numel(bestCurves(1, :, innerBase)),bestCurves(3, :, innerBase)',num2str(bestCurves(3, :, innerBase)','%0.2f'),...
                'HorizontalAlignment','center',...
                'VerticalAlignment','bottom');
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;