clear all;
close all;
clc;
% 1D (and 1.5D) kernel LWS comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
ANALYZED_VARIANT = VARIANT_15D_FACET;
LWS1 = 1;
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
graphCountX = 1;
graphCountY = 1;
graphsPerWindowCount = graphCountX * graphCountY;
windowCount = ceil(graphCount / graphsPerWindowCount);
for win=1:windowCount
    if (~(win == 1 || win == 3 || win == 5 || win == 7 || win == 9))
        continue;
    end;
    
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
            xlabel('LWS0');
            ylabel('Time [ms]');            
            title(cellstr([int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY)) ', fs=' int2str(data(index, INDEX_FACET_SIZE)) ', fc=' int2str(data(index, INDEX_FACET_COUNT)) ', dc=' int2str(data(index, INDEX_DEFORMATION_COUNT))]));
            % plot curves for all LWS0 into one subfigure
            hold on;
            values(1, :) = allCurves(TIME_KERNEL, LWS1,  1:size(X_LABELS, 1), ANALYZED_VARIANT, ANALYZED_TEST_CASE, innerBase);
            values(2, :) = allCurves(TIME_TOTAL, LWS1, 1:size(X_LABELS, 1), ANALYZED_VARIANT, ANALYZED_TEST_CASE, innerBase) - allCurves(TIME_KERNEL, LWS1, 1:size(X_LABELS, 1), ANALYZED_VARIANT, ANALYZED_TEST_CASE, innerBase);
            values(3, :) = allCurves(TIME_TOTAL, LWS1, 1:size(X_LABELS, 1), ANALYZED_VARIANT, ANALYZED_TEST_CASE, innerBase);
            values = values(:, 1:size(X_LABELS, 1));
            bar(values(1:2,:)', 'stacked');
            set(gca, 'XTick', X, 'XTickLabel', X_LABELS);
%             text(1:numel(values(3, :)), values(3, :)',num2str(values(3, :)','%0.2f'),...
%                 'HorizontalAlignment','center',...
%                 'VerticalAlignment','bottom');
            set(findall(gcf, '-property', 'FontSize'), 'FontSize', 25);
            hold off;
        end;
    end;
    
    hgexport(gcf, sprintf('export%d.jpg', win), hgexport('factorystyle'), 'Format', 'jpeg');
    close(gcf);    
end;