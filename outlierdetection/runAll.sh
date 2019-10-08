#!/bin/bash

rm -rf output/AOSP_*
rm -rf output/HTC_*
rm -rf output/ HUAWEI_*
rm -rf output/MOTO_*
rm -rf output/ONEPLUS_*
rm -rf output/PIXEL_*

mkdir output

python OutlierDetection.py INPUT_RES/android-7.1.0_r4/acminer/ AOSP
python OutlierDetection.py INPUT_RES/google-pixel-xl-7.1.2/acminer/ PIXEL
python OutlierDetection.py INPUT_RES/htc-u11-7.1.1/acminer/ HTC
python OutlierDetection.py INPUT_RES/huawei-mate-9-7.0/acminer/ HUAWEI
python OutlierDetection.py INPUT_RES/moto-z2-force-7.1.1/acminer/ MOTO
python OutlierDetection.py INPUT_RES/oneplus-5-7.1.1/acminer/ ONEPLUS
python OutlierDetection.py INPUT_RES/sony-xperia-xz-premium-7.1.1/acminer/ SONY

python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/HTC_inconsistency_analysis_all_xml/
python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/HTC_inconsistency_analysis_all_xml/
python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/HUAWEI_inconsistency_analysis_all_xml/
python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/MOTO_inconsistency_analysis_all_xml/
python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/ONEPLUS_inconsistency_analysis_all_xml/
python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/PIXEL_inconsistency_analysis_all_xml/
python DiffOutlierResults.py output/AOSP_inconsistency_analysis_all_xml/ output/SONY_inconsistency_analysis_all_xml/

cd XSLT
rm -rf policyminer-results
mkdir policyminer-results
python genHtml.py /home/benandow/outlierdetection/output/AOSP_inconsistency_analysis_all_xml/ AOSP_HTML
python genHtml.py /home/benandow/outlierdetection/output/HTC_inconsistency_analysis_all_xml/ HTC_HTML
python genHtml.py /home/benandow/outlierdetection/output/HUAWEI_inconsistency_analysis_all_xml/ HUAWEI_HTML
python genHtml.py /home/benandow/outlierdetection/output/MOTO_inconsistency_analysis_all_xml/ MOTO_HTML
python genHtml.py /home/benandow/outlierdetection/output/ONEPLUS_inconsistency_analysis_all_xml/ ONEPLUS_HTML
python genHtml.py /home/benandow/outlierdetection/output/PIXEL_inconsistency_analysis_all_xml/ PIXEL_HTML
python genHtml.py /home/benandow/outlierdetection/output/SONY_inconsistency_analysis_all_xml/ SONY_HTML

mv AOSP_HTML policyminer-results
mv HTC_HTML policyminer-results
mv HUAWEI_HTML policyminer-results
mv MOTO_HTML policyminer-results
mv ONEPLUS_HTML policyminer-results
mv PIXEL_HTML policyminer-results 
mv SONY_HTML policyminer-results
