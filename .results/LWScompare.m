clear all;
close all;
clc;
% 2D kernel LWS comparison
% Data format - IMAGE_WIDTH [px], IMAGE_HEIGHT [px], FACET_SIZE
% [px], DEFORMATION_COUNT ,LWS0 ,LWS1 ,Total Time [ms], Kernel time [ms]

graphCount = csvread('LWScompare.csv',0,0,[0,0,0,0]);
scenarioCount = csvread('LWScompare.csv',0,2,[0,2,0,2]);
data = csvread('LWScompare.csv',2,0);
% Data preparation
lws1count = 11;
lws0count = 7;
curves = NaN(lws1count, lws0count, graphCount);

currentLws0 = 0;
currentLwsIndex = 0;
pointIndex = 1;
for graphX=1:graphCount
    currentLws0 = 0;
    currentLwsIndex = 0;
    
    for scenario=1:scenarioCount
        index = ((graphX - 1) * scenarioCount) + scenario;
        
        lws0 = data(index, 6);
        lws1 = data(index, 7);
        
        % one variant
        % one graph per data  combination (w, h, fs, dc)
        % one line for each LWS0
        curves(log2(lws1) + 1, log2(lws0) + 1, graphX) = data(index, 10);
    end;
end;
% Plot graphs
% line colors
colors = [[0 0 0];[1 0 0 ];[0 1 0];[0 0 1];[1 1 0];[1 0 1];[0 1 1];[1 1 1]];
% x-axis value labels
x = 1 : lws1count;
xlabels = cell(lws1count,1);
for i=1:lws1count
    xlabels(i) = cellstr(int2str(2^(i-1)));
end;
% Main plot, create multiple windows
splitCount = 3;
dSplitCount = splitCount * splitCount;
windowCount = graphCount / dSplitCount;
for win=1:windowCount
    index = (win-1) * dSplitCount * scenarioCount + 1;
    name = ['Kernel running time, Resolution  ' int2str(data(index, 1)) 'x' int2str(data(index, 2))];
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphX=1:splitCount
        for graphY=1:splitCount
            % crate subfigure
            subplot(splitCount, splitCount, (graphX-1) * 3 + graphY);                        
            xlabel('LWS1');
            ylabel('Time [ms]');
            % compute line index
            innerBase = ((win-1) * dSplitCount) + ((graphX-1) * splitCount) + graphY;
            index = ((innerBase - 1) * scenarioCount) + 1;
            title(cellstr(['Facet size ' int2str(data(index, 3)) ', Deformation count ' int2str(data(index, 5))]));
            
            % plot curves for all LWS0 into one subfigure
            hold on;                        
            for lws0i=1:lws0count
                plot(x, curves(:, lws0i, innerBase),'-o','Color',colors(lws0i, :), 'LineSmoothing','on')
                set(gca, 'XTick', 1:lws1count, 'XTickLabel', xlabels);
                h = legend('1', '2', '4', '8', '16', '32', '64');
                v = get(h,'title');
                set(v,'string','LWS0');
            end;            
            hold off;
        end;
    end;
end;


