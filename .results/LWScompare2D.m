clear all;
close all;
clc;
% 2D kernel LWS comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
INSPECTED_VARIANT = VARIANT_2D_IMAGE;
% Plot graphs
% Main plot, create multiple windows
graphCountX = 2;
graphCountY = 3;
graphsPerWindowCount = graphCountX * graphCountY;
windowCount = ceil(graphCount / graphsPerWindowCount);
for win=1:windowCount
    index = (win-1) * graphsPerWindowCount * pointCount + 1;
    name = ['Kernel running time, Resolution  ' int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY))];
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphX=1:graphCountX
        for graphY=1:graphCountY 
            % compute line index
            innerBase = ((win-1) * graphsPerWindowCount) + ((graphY-1) * graphCountX) + graphX;
            if (innerBase > graphCount)
                break;
            end;            
            index = ((innerBase - 1) * pointCount) + 1;
            % create subfigure
            subplot(graphCountY, graphCountX, (graphY-1) * graphCountX + graphX);
            xlabel('LWS1');
            ylabel('Time [ms]');            
            title(cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=' int2str(data(index, INDEX_FACET_SIZE)) ', dc=' int2str(data(index, INDEX_DEFORMATION_COUNT))]));            
            % plot curves for all LWS0 into one subfigure
            hold on;                                    
            for lws0i=1:COUNT_LWS0
                plot(allCurves(ANALYZED_TIME, :, lws0i, INSPECTED_VARIANT, INSPECTED_TEST_CASE, innerBase),'-o','Color',COLORS(lws0i, :), 'LineSmoothing','on')                
            end;            
            set(gca, 'XTick', X, 'XTickLabel', X_LABELS);
            h = legend('1', '2', '4', '8', '16', '32', '64');
            v = get(h,'title');
            set(v,'string','LWS0');
            hold off;
        end;
    end;
end;