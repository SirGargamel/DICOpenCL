COLORS = [[0 0 0];[1 0 0 ];[0 1 0];[0 0 1];[1 1 0];[1 0 1];[0 1 1];[1 1 1];[0 0 0];[1 0 0 ];[0 1 0]];
COUNT_LWS0 = log2(1024) + 1;
COUNT_LWS1 = log2(1024) + 1;
FILE_DELIMITER = ',';
FILE_FORMAT = '%f %f %f %f %f %f %f %f %f %f %f %f %s %f';
% FILE_FORMAT = '%f %f %f %f %f %f %f %f %f %f %f %f %s %f %f %f %f %f %f %f';
FILE_HEADER_LINE_COUNT = 2;
% FILE_NAME = 'DIC_OpenCL_Data.csv';
FILE_NAME = 'D:\\DIC_OpenCL_Data_GPU.csv';
INDEX_DEFORMATION_COUNT = 4;
INDEX_FACET_SIZE = 3;
INDEX_LWS0 = 7;
INDEX_LWS1 = 8;
INDEX_RESX = 1;
INDEX_RESY = 2;
INDEX_TEST_CASE = 5;
INDEX_TIME_KERNEL = 12;
INDEX_TIME_TOTAL = 11;
INDEX_VARIANT = 6;
NAMES_VARIANTS = {'TpF' 'TpD' '1D pF' '1D pD' '1.5D pF' '1.5D pD' '2D Naive' '2D Int[]' '2D I' '2D I FtoA' '2D I MC' '2D I Vec' '2D I C' '1D I LpF' '1D I LpF-L' '2D I V + MC' '2D I V + MC + C'};
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
VARIANT_2D_IMAGE_V_MC = 13;
X = 1 : COUNT_LWS1;
X_LABELS = cell(COUNT_LWS1,1);
for i=1:COUNT_LWS1
    X_LABELS(i) = cellstr(int2str(2^(i-1)));
end;
% which time will be used in graphs
ANALYZED_TIME = TIME_TOTAL;
% which case will be analyzed in graphs
ANALYZED_TEST_CASE = TEST_CASE_RANDOM;