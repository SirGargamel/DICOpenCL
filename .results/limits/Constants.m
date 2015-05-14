COLORS = [[0 0 0];[1 0 0 ];[0 1 0];[0 0 1];[1 1 0];[1 0 1];[0 1 1];[1 1 1];[0 0 0];[1 0 0 ];[0 1 0]];
COUNT_LWS0 = log2(1024) + 1;
COUNT_LWS1 = log2(1024) + 1;
FILE_DELIMITER = ',';
FILE_FORMAT = '%f %f %f %f %f %f %f %f %f %f %f %f %f %s %f';
FILE_HEADER_LINE_COUNT = 2;
FILE_NAME_GT650 = 'DIC_OpenCL_Data_GPU_GT650_Limits.csv';
FILE_NAME_GTX765 = 'DIC_OpenCL_Data_GPU_GTX765M_Limits.csv';
FILE_NAME_i7_3610 = 'DIC_OpenCL_Data_CPU_i7-3610QM_Limits.csv';
FILE_NAME = FILE_NAME_GT650;

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
    '2D Int[]' '2D I' '2D I V' '2D I LL' '2D I V LL' ...
    'FD 1' 'FD 2' 'FD 3' 'FD 4' 'FD 5' ...
    'D 1' 'D 2' 'D 3' 'D 4' 'D 5' ...
    'F 1' 'F 2' 'F 3' 'F 4' 'F 5'};
TEST_CASE_RANDOM = 1;
TEST_CASE_SHIFT = 2;
TIME_KERNEL = 1;
TIME_TOTAL = 2;

X = 1 : COUNT_LWS1;
X_LABELS = cell(COUNT_LWS1,1);
for i=1:COUNT_LWS1
    X_LABELS(i) = cellstr(int2str(2^(i-1)));   
end;

% X = 1 : 8;
% X_LABELS = cell(8,1);
% for i=1:8
%     X_LABELS(i) = cellstr(int2str(i));
% end;

% which time will be used in graphs
ANALYZED_TIME = TIME_TOTAL;
% which case will be analyzed in graphs
ANALYZED_TEST_CASE = TEST_CASE_RANDOM;