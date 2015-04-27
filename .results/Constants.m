COLORS = [[0 0 0];[1 0 0 ];[0 1 0];[0 0 1];[1 1 0];[1 0 1];[0 1 1];[1 1 1];[0 0 0];[1 0 0 ];[0 1 0]];
COUNT_LWS0 = log2(1024) + 1;
COUNT_LWS1 = log2(1024) + 1;
FILE_DELIMITER = ',';
% FILE_FORMAT = '%f %f %f %f %f %f %f %f %f %f %f %f %f %s %f';
FILE_FORMAT = '%f %f %f %f %f %f %f %f %f %f %f %f %f %s %f %f %f %f %f %f %f %f %f %f %f %f %f';
FILE_HEADER_LINE_COUNT = 2;
FILE_NAME_CPU = 'D:\\DIC_OpenCL_Data_CPU.csv';
FILE_NAME_GPU = 'D:\\DIC_OpenCL_Data_GPU.csv';
FILE_NAME_GT650 = 'DIC_OpenCL_Data_GPU_GT650_AT.csv';
FILE_NAME_GTX765 = 'DIC_OpenCL_Data_GPU_GTX765M_AT.csv';
FILE_NAME_i7_3610 = 'DIC_OpenCL_Data_CPU_i7-3610QM_AT.csv';
FILE_NAME_i7_4700 = 'DIC_OpenCL_Data_CPU_i7-4700MQ_AT.csv';
FILE_NAME_HD4000 = 'DIC_OpenCL_Data_iGPU_HD4000_AT.csv';
FILE_NAME_HD4000_FIX = 'DIC_OpenCL_Data_iGPU_HD4000_AT.csv';
FILE_NAME_LIMITS_GPU = 'DIC_OpenCL_Data_GPU_GT650_Limits.csv';
FILE_NAME_LIMITS_CPU = 'DIC_OpenCL_Data_CPU_i7-3610QM_Limits.csv';
FILE_NAME = FILE_NAME_GPU;

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
%     'TpF' 'TpD' ...
%     '1D pF' '1D pD' '1.5D pF' '1.5D pD' ...
%     '2D Naive' '2D Int[]' '2D I' ...
%     '2D I FtoA' '2D I MC' '2D I V' '2D I C' ...
%     '1D I L' '1D I LL' ...
%     '2D I V + MC' '2D I LL + V' '2D I LL + MC' '2D I LL + MC + V' ...
%     '2D Int[] D' '2D I D' '2D I V D' '2D I V MC D' '1D I V LL D' '1D I V LL MC D'};
    '2D Int[]' '2D I' '2D I V' '2D I LL' '2D I V LL' ...
    'FD 1' 'FD 2' 'FD 3' 'FD 4' 'FD 5' ...
    'D 1' 'D 2' 'D 3' 'D 4' 'D 5' ...
    'F 1' 'F 2' 'F 3' 'F 4' 'F 5'};
TEST_CASE_RANDOM = 1;
TEST_CASE_SHIFT = 2;
TIME_KERNEL = 1;
TIME_TOTAL = 2;
VARIANT_JAVA_FACET = 1;
VARIANT_JAVA_DEFORMATION = 2;
VARIANT_1D_FACET = 3;
VARIANT_1D_DEFORMATION = 4;
VARIANT_15D_FACET = 5;
VARIANT_15D_DEFORMATION = 6;
VARIANT_2D_NAIVE = 7;
VARIANT_2D_INT = 8;
VARIANT_2D_IMAGE = 9;
VARIANT_2D_IMAGE_FA = 10;
VARIANT_2D_IMAGE_MC = 11;
VARIANT_2D_IMAGE_VEC = 12;
VARIANT_2D_IMAGE_C = 13;
VARIANT_2D_IMAGE_L = 14;
VARIANT_2D_IMAGE_LL = 15;
VARIANT_2D_IMAGE_V_MC = 16;
VARIANT_2D_IMAGE_V_LL = 17;
VARIANT_2D_IMAGE_LL_MC = 18;
VARIANT_2D_IMAGE_V_LL_MC = 19;
VARIANT_2D_INT_D = 20;
VARIANT_2D_IMAGE_D = 21;
VARIANT_2D_IMAGE_VEC_D = 22;
VARIANT_2D_IMAGE_V_MC_D = 23;
VARIANT_2D_IMAGE_V_LL_D = 24;
VARIANT_2D_IMAGE_V_LL_MC_D = 25;

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