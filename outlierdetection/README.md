# How to run

Python 2.7 is required to run outlier detection. You will also need the following modules installed: (1) numpy; (2) scikit-learn; (3) lxml; and (4) pyfim


## Running outlier detection:

To run outlier detection execute the following command:

$ python OutlierDetection.py INPUT\_RES/android-7.1.0\_r4/acminer/ AOSP

To format the output of consistency analyis, execute the following commands:

$ cd XSLT

$ python genHtml.py /home/benandow/outlierdetection/output/AOSP\_inconsistency\_analysis\_all\_xml/ AOSP\_HTML


The results will be in XSLT/AOSP\_HTML

