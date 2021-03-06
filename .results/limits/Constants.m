COLORS = [[0 0 0];[1 0 0 ];[0 1 0];[0 0 1];[1 1 0];[1 0 1];[0 1 1];[1 1 1];[0 0 0];[1 0 0 ];[0 1 0]];
COUNT_LWS0 = log2(1024) + 1;
COUNT_LWS1 = log2(1024) + 1;
FILE_DELIMITER = ',';
FILE_FORMAT = '%f %f %f %f %f %f %f %f %f %f %f %f %f %s %f';
FILE_HEADER_LINE_COUNT = 2;
FILE_NAME_GT650 = 'DIC_OpenCL_Data_GPU_GT650_Limits.csv';
FILE_NAME_GTX765 = 'DIC_OpenCL_Data_GPU_GTX765M_Limits.csv';
FILE_NAME_i7_3610 = 'DIC_OpenCL_Data_CPU_i7-3610QM_Limits_FM=1.csv';
FILE_NAME = FILE_NAME_i7_3610;

INDEX_DEFORMATION_COUNT = 5;
INDEX_FACET_SIZE = 3;
INDEX_FACET_COUNT = 4;
INDEX_LWS0 = 8;
INDEX_LWS1 = 9;
INDEX_LWS_SUB = 10;
INDEX_RESX = 1;
INDEX_RESY = 2;
INDEX_TEST_CASE = 6;
INDEX_TIME_KERNEL = 13;
INDEX_TIME_TOTAL = 12;
INDEX_VARIANT = 7;
NAMES_VARIANTS = { ...
%     '2DInt NO' '2DIm NO' '2DIm V NO' '1DIm L NO' '1D I V L NO' ...
%     '2DInt NO GPU' '2DIm NO GPU' '2DIm V NO GPU' '1DIm L NO GPU' '1D I V L NO GPU' ...
%     '2DInt L ' '2DIm L ' '2DIm V L' '1DIm L L' '1D I V L L' ...    
%     '2DInt LD' '2DIm LD' '2DIm V LD' '1DIm L LD' '1D I V L LD' ...
%     '2DInt LD GPU' '2DIm LD GPU' '2DIm V LD GPU' '1DIm L LD GPU' '1D I V L LD GPU' ...    
%     '2DInt LF' '2DIm LF' '2DIm V LF' '1DIm L LF' '1D I V L LF' ...
%     '2DInt LF GPU' '2DIm LF GPU' '2DIm V LF GPU' '1DIm L LF GPU' '1D I V L LF GPU' ...
    
    '2DInt NO' '2DIm NO' '2DIm V NO' '1D I V L NO' ...
    '2DInt NO GPU' '2DIm NO GPU' '2DIm V NO GPU' '1D I V L NO GPU' ...
    '2DInt L ' '2DIm L ' '2DIm V L' '1D I V L L' ...    
    '2DInt LD' '2DIm LD' '2DIm V LD' '1D I V L LD' ...
    '2DInt LD GPU' '2DIm LD GPU' '2DIm V LD GPU' '1D I V L LD GPU' ...    
    '2DInt LF' '2DIm LF' '2DIm V LF' '1D I V L LF' ...
    '2DInt LF GPU' '2DIm LF GPU' '2DIm V LF GPU' '1D I V L LF GPU' ...
};
TEST_CASE_RANDOM = 1;
TEST_CASE_SHIFT = 2;
TIME_KERNEL = 1;
TIME_TOTAL = 2;

X = 1 : COUNT_LWS1;
X_LABELS = cell(COUNT_LWS1,1);
for i=1:COUNT_LWS1
    X_LABELS(i) = cellstr(int2str(2^(i-1)));   
end;
ANALYZED_TIME = TIME_TOTAL;
% which case will be analyzed in graphs
ANALYZED_TEST_CASE = TEST_CASE_RANDOM;