clear all;
% import csv data from DIC_OpenCL testing
% Data format - VARIANT, IMAGE_WIDTH [px], IMAGE_HEIGHT [px], FACET_SIZE [px], DEFORMATION_COUNT ,LWS0 ,LWS1 ,Time [ms]

counts = csvread('d:\\testData.csv',0,0,[0,0,0,1]);
fullData = csvread('d:\\testData.csv',2,0);
% data preparation
curves = zeros(counts(1), counts(2)) ;
for curve=1:counts(1)
    for scenario=1:counts(2)
        index = ((curve - 1) * counts(2)) + scenario;
        curves(curve, scenario) = fullData(index, 8);    
    end;
end;
% basic plot
x = 1 : counts(2);
plot(x)
hold on % prikreslime neco dalsiho
% global tiltes and labels
title('Rychlost vykonani programu');
xlabel('LWS0; LWS1 [1]');
ylabel('Time [ms]');
% x-axis value labels
xlabels = cell(counts(2),1);
for i=1:counts(2)
    xlabels(i) = cellstr([int2str(fullData(i, 6)) ';' int2str(fullData(i, 7))]);
end;
set(gca, 'XTick', 1:counts(2), 'XTickLabel', xlabels);
% legend
legendData = cell(1,counts(1));
for i=1:counts(1)
    index = (i-1)*counts(2) + 1;
    legendData(i) = cellstr([int2str(fullData(index, 2)) ',' int2str(fullData(index, 3)) ',' int2str(fullData(index, 4)) ',' int2str(fullData(index, 5))]);
end;
legend(legendData);
% curve colors
colors = rand(counts(1), 3);
% main curve plots
for i=1:counts(1)
    plot(curves(i, :),'Color',colors(i,:))        
end


