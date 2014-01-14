clear all;
close all;
clc;
% 2D kernel LWS comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
INSPECTED_TEST_CASE = TEST_CASE_SHIFT;
INSPECTED_VARIANT = VARIANT_2D_IMAGE;
% Plot graphs
% line colors
colors = [[0 0 0];[1 0 0 ];[0 1 0];[0 0 1];[1 1 0];[1 0 1];[0 1 1];[1 1 1];[0 0 0];[1 0 0 ];[0 1 0]];
% x-axis value labels
x = 1 : COUNT_LWS1;
xlabels = cell(COUNT_LWS1,1);
for i=1:COUNT_LWS1
     xlabels(i) = cellstr(int2str(2^(i-1)));    
end;
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
            subplot(graphCountX, graphCountY, (graphY-1) * graphCountX + graphX);
            xlabel('LWS1');
            ylabel('Time [ms]');            
            title(cellstr(['Facet size ' int2str(data(index, INDEX_FACET_SIZE)) ', Deformation count ' int2str(data(index, INDEX_DEFORMATION_COUNT))]));
            
            % plot curves for all LWS0 into one subfigure
            hold on;                                    
            for lws0i=1:COUNT_LWS0
                plot(x, allCurves(:, lws0i, INSPECTED_VARIANT, INSPECTED_TEST_CASE, innerBase),'-o','Color',colors(lws0i, :), 'LineSmoothing','on')                
            end;            
            set(gca, 'XTick', 1:COUNT_LWS1, 'XTickLabel', xlabels);
            h = legend('1', '2', '4', '8', '16', '32', '64');
            v = get(h,'title');
            set(v,'string','LWS0');
            hold off;
        end;
    end;
end;