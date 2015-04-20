clear all;
close all;
clc;
% best times of each variant comparison
% Data format specification can be found in Constants.m and LoadData.m
Constants;
LoadData;
ANALYZED_TIME = TIME_TOTAL;
% ANALYZED_VARIANTS = [VARIANT_2D_IMAGE VARIANT_2D_IMAGE_VEC VARIANT_2D_IMAGE_LL VARIANT_2D_IMAGE_MC VARIANT_2D_IMAGE_V_LL_MC];
% ANALYZED_VARIANTS = [VARIANT_2D_INT VARIANT_2D_INT_D VARIANT_2D_IMAGE VARIANT_2D_IMAGE_D VARIANT_2D_IMAGE_VEC VARIANT_2D_IMAGE_VEC_D VARIANT_2D_IMAGE_V_MC VARIANT_2D_IMAGE_V_MC_D VARIANT_2D_IMAGE_V_LL VARIANT_2D_IMAGE_V_LL_D VARIANT_2D_IMAGE_V_LL_MC VARIANT_2D_IMAGE_V_LL_MC_D];
% ANALYZED_VARIANTS = [VARIANT_2D_INT VARIANT_2D_INT_D VARIANT_2D_IMAGE VARIANT_2D_IMAGE_D VARIANT_2D_IMAGE_LL_MC VARIANT_2D_IMAGE_V_MC VARIANT_2D_IMAGE_V_LL VARIANT_2D_IMAGE_V_LL_MC];

% ANALYZED_VARIANTS = [8 9 11:13 15:25];
% ANALYZED_VARIANTS = [1:3 5 7:13 15:25];
% ANALYZED_VARIANTS = [1 2];
ANALYZED_VARIANTS = [1:5:16 2:5:17 3:5:18 4:5:19 5:5:20];
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
        bestCurves(3, v, graph) = allCurves(TIME_TOTAL,minLws1,minLws0,var,ANALYZED_TEST_CASE,graph);
        bestCurves(2, v, graph) = bestCurves(3, v, graph) - bestCurves(1, v, graph);        
    end;
end;
% Plot graphs
% Main plot, create multiple windows
% split graphs to multiple windows
graphCountX = 1;
graphCountY = 1;
graphsPerWindowCount = graphCountX * graphCountY;
windowCount = ceil(graphCount / graphsPerWindowCount);
for win=1:windowCount
%     if (~(win == 1 || win == 3 || win == 5 || win == 7 || win == 9))
%         continue;
%     end;
    
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
            % values on top
            text(1:numel(bestCurves(1, :, innerBase)),bestCurves(3, :, innerBase)',num2str(bestCurves(3, :, innerBase)','%0.2f'),...
                'HorizontalAlignment','center',...
                'VerticalAlignment','bottom');
            % Finish plotting to subfigure
            set(findall(gcf, '-property', 'FontSize'), 'FontSize', 25);
            set(gca,'XTick', 1:numel(bestCurves(1, :, innerBase)), 'XTickLabel', NAMES_VARIANTS_INNER);
            fix_xticklabels();            
            hold off;
        end;
    end;
        
%     hgexport(gcf, sprintf('export%d.jpg', win), hgexport('factorystyle'), 'Format', 'jpeg');
%     close(gcf);
end;