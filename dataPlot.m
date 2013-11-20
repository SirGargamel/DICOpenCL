% import csv data from DIC_OpenCL testing
% Data format - VARIANT,IMAGE_WIDTH,IMAGE_HEIGHT,FACET_SIZE,DEFORMATION_COUNT,LWS0,LWS1,Time [ms]

counts = csvread('d:\\testData.csv',0,0,[0,0,0,1]);
fullData = csvread('d:\\testData.csv',2,0);

