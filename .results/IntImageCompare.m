clear all;
close all;
clc;
% 2D kernel int[] vs image2d comparison
% Data format - VARIANT, IMAGE_WIDTH [px], IMAGE_HEIGHT [px], FACET_SIZE
% [px], DEFORMATION_COUNT ,LWS0 ,LWS1 ,Total Time [ms], Kernel time [ms]
INDEX_DEFORMATION_COUNT = 5;
INDEX_FACET_SIZE = 3;
INDEX_LWS0 = 8;
INDEX_LWS1 = 9;
INDEX_RESX = 1;
INDEX_RESY = 2;
INDEX_TIME = 11;
INDEX_VARIANT = 7;
COUNT_LWS0 = log2(64) + 1;
COUNT_LWS1 = log2(1024) + 1;
% Data reading
graphCount = csvread('DIC_OpenCL_Data.csv',0,0,[0,0,0,0]);
variantCount = csvread('DIC_OpenCL_Data.csv',0,1,[0,1,0,1]);
pointCounts = csvread('DIC_OpenCL_Data.csv',0,2,[0,2,0,variantCount + 1]);
pointCount = sum(pointCounts);
fid = fopen('DIC_OpenCL_Data.csv');
allData = textscan(fid,'%f %f %f %f %f %f %f %f %f %f %f %s %f', 'headerlines', 2, 'Delimiter', ',');
data = cell2mat(allData(:, 1:11));
% Lines extraction
allCurves = NaN(COUNT_LWS1, COUNT_LWS0, variantCount, graphCount);
for graph=1:graphCount
    for point=1:pointCount
        index = ((graph - 1) * pointCount) + point;
        
        variant = data(index, INDEX_VARIANT) + 1;
        lws0 = data(index, INDEX_LWS0);
        lws1 = data(index, INDEX_LWS1);
        
        if (variant == 1)        
            if (lws0 == 1)
                lws0 = 2;
                lws1 = 8;
            end;
            if (lws0 == 7)
                lws0 = 8;
                lws1 = 2;
            end;                              
        end;
        
        % multiple variants
        % one graph per data combination (w, h, fs, dc)
        % one line for each LWS0
        allCurves(log2(lws1) + 1, log2(lws0) + 1, variant, graph) = data(index, INDEX_TIME);
    end;
end;
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
        bestCurves(:, var, graph) = allCurves(:, minIndex, var, graph);        
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
windowCount = graphCount / dSplitCount;
for win=1:windowCount
    index = (win-1) * dSplitCount * pointCount + 1;
    name = ['Kernel running time, Resolution  ' int2str(data(index, INDEX_RESX)) 'x' int2str(data(index, INDEX_RESY))];
    figure('units','normalized','outerposition',[0 0.05 1 0.95],'name',name)    
    
    for graphX=1:splitCount
        for graphY=1:splitCount
            % create subplot
            h = subplot(splitCount, splitCount, (graphX-1) * 3 + graphY);                     
            xlabel('LWS1');
            ylabel('Time [ms]');            
            % compute line index
            innerBase = ((win-1) * dSplitCount) + ((graphX-1) * splitCount) + graphY;
            index = ((innerBase - 1) * pointCount) + 1;
            title(cellstr(['Facet size ' int2str(data(index, INDEX_FACET_SIZE)) ', Deformation count ' int2str(data(index, INDEX_DEFORMATION_COUNT))]));            
            % plot both curves to one subfigure       
            hold on;                                    
            plot(x,bestCurves(:, 2, innerBase),'-x','Color',colors(2, :), 'LineSmoothing','on')
            plot(x,bestCurves(:, 3, innerBase),'-o','Color',colors(3, :), 'LineSmoothing','on')            

            legend(h,['int[], LWS0 = ' int2str(bestCurvesParams(2,innerBase))], ['image2d, LWS0 = ' int2str(bestCurvesParams(3,innerBase))]);
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;