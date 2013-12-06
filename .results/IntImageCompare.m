clear all;
close all;
clc;
% 2D kernel int[] vs image2d comparison
% Data format - VARIANT, IMAGE_WIDTH [px], IMAGE_HEIGHT [px], FACET_SIZE
% [px], DEFORMATION_COUNT ,LWS0 ,LWS1 ,Total Time [ms], Kernel time [ms]

graphCount = csvread('IntImageCompare.csv',0,0,[0,0,0,0]);
variantCount = csvread('IntImageCompare.csv',0,1,[0,1,0,1]);
scenarioCount = csvread('IntImageCompare.csv',0,2,[0,2,0,2]);
data = csvread('IntImageCompare.csv',2,0);
% Data preparation
lws1count = 11;
lws0count = 7;
% Lines extraction
allCurves = NaN(lws1count, lws0count, variantCount, graphCount);
for var=1:variantCount
    for graph=1:graphCount
        for scenario=1:scenarioCount
            index = ((graph - 1) * scenarioCount) + (graphCount * scenarioCount * (var-1)) + scenario;
            
            lws0 = data(index, 7);
            lws1 = data(index, 8);
                        
            % multiple variants
            % one graph per data  combination (w, h, fs, dc)
            % one line for each LWS0
            allCurves(log2(lws1) + 1, log2(lws0) + 1, data(index, 1) + 1, graph) = data(index, 11);
        end;
    end;
end;
% Find best curves (fastest) for each variant
bestCurves = NaN(lws1count, variantCount, graphCount);
bestCurvesParams = NaN(variantCount, graphCount);
for graph=1:graphCount
    for var=1:variantCount
        min = intmax;
        minIndex = 1; 
        minLws0 = 1;
        for lws1=1:lws1count
            for lws0=1:lws0count
                val = allCurves(lws1, lws0, var, graph);
                if (val < min)
                    min = val;
                    minIndex = lws0;   
                    minLws0 = lws0;
                end;
            end;
        end;
        bestCurves(:, var, graph) = allCurves(:, minIndex, var, graph);        
        bestCurvesParams(var, graph) = minLws0;        
    end;
end;

% Plot graphs
% Line colors
colors = [[0 0 0];[1 0 0 ]];
% x-axis value labels
x = 1 : lws1count;
xlabels = cell(lws1count,1);
for i=1:lws1count
    xlabels(i) = cellstr(int2str(2^(i-1)));
end;

splitCount = 3;
dSplitCount = splitCount * splitCount;
windowCount = graphCount / dSplitCount;

for win=1:windowCount
    index = (win-1) * dSplitCount * scenarioCount + 1;
    name = ['Kernel running time, Resolution  ' int2str(data(index, 2)) 'x' int2str(data(index, 3))];
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphX=1:splitCount
        for graphY=1:splitCount
            % create subplot
            subplot(splitCount, splitCount, (graphX-1) * 3 + graphY);                     
            xlabel('LWS1');
            ylabel('Time [ms]');
            % compute line index
            innerBase = ((win-1) * dSplitCount) + ((graphX-1) * splitCount) + graphY;
            index = ((innerBase - 1) * scenarioCount) + 1;
            title(cellstr(['Facet size ' int2str(data(index, 4)) ', Deformation count ' int2str(data(index, 6))]));            
            % plot both curves to one subfigure       
            hold on;                        
            plot(x,bestCurves(:, 1 ,innerBase),'-o','Color',colors(1, :), 'LineSmoothing','on')
            plot(x,bestCurves(:, 2, innerBase),'-x','Color',colors(2, :), 'LineSmoothing','on')
            set(gca, 'XTick', 1:lws1count, 'XTickLabel', xlabels);
            h = legend(['int[], LWS0 ' int2str(bestCurvesParams(1,innerBase))], ['image2d, LWS0 ' int2str(bestCurvesParams(2,innerBase))]);                    
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;


