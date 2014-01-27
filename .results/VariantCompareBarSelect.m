clear all;
close all;
clc;
% best times of each variant comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
ANALYZED_TIME = TIME_TOTAL;
% ANALYZED_VARIANTS = [VARIANT_2D_IMAGE VARIANT_2D_IMAGE_MC VARIANT_2D_IMAGE_VEC VARIANT_2D_IMAGE_V_MC];
ANALYZED_VARIANTS = [1 2];
varCount = size(ANALYZED_VARIANTS, 2);
NAMES_VARIANTS_INNER = cell(varCount);
for i=1:varCount
    NAMES_VARIANTS_INNER(i) = NAMES_VARIANTS(ANALYZED_VARIANTS(i));
end;
% Find best curves (fastest) for each variant
bestCurves = NaN(2,varCount, graphCount);
for graph=1:graphCount
    for v=1:varCount
        var = ANALYZED_VARIANTS(v);
        m = squeeze(allCurves(ANALYZED_TIME,:,:,var,ANALYZED_TEST_CASE,graph));
        m(m == 0) = NaN;
        [minVal, index] = min(m(:));
        [minLws1, minLws0] = ind2sub(size(m), index);

        bestCurves(1, v, graph) = allCurves(TIME_KERNEL,minLws1,minLws0,var,ANALYZED_TEST_CASE,graph);
        bestCurves(2, v, graph) = allCurves(TIME_TOTAL,minLws1,minLws0,var,ANALYZED_TEST_CASE,graph) - bestCurves(1, v, graph);
    end;
end;
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
graphCountX = 2;
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
            bar(bestCurves(:, :, innerBase)','stacked');
            set(gca,'XTick', 1:numel(bestCurves(1, :, innerBase)), 'XTickLabel',NAMES_VARIANTS_INNER);
            fix_xticklabels();
            % Finish plotting to subfigure
            hold off;
        end;
    end;
end;