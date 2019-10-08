#!/usr/bin/env python

import FeatureVecCreator as fv
import FreqItemsets as od
import os
import sys
import DockerConfig
import re

# python OutlierDetection.py XSLT/policyminer-results/android-7.1.0_r4/acminer

if __name__ == '__main__':
	if len(sys.argv) != 3:
		print "Usage: %s <BASE_DIRECTORY_OF_XML_FILES> <OUTPUT_PREFIX>" % (sys.argv[0],)
		print "E.g., python OutlierDetection.py XSLT/policyminer-results/android-7.1.0_r4/acminer AOSP"
		sys.exit(1)

	ROOT_DIR = sys.argv[1]
	OUT_PREFIX = sys.argv[2]

	if not os.path.exists(ROOT_DIR):
		print "Warning", ROOT_DIR, "does not exist!"
		sys.exit(1)

	for root, dirs, files in os.walk(ROOT_DIR):
		for f in files:
			full_path = os.path.join(root, f)
			if os.path.isfile(full_path) and f.endswith('.csv'):
				rel_root = re.sub(sys.argv[1], '', root) # Remove initial path from output filename
				if rel_root.startswith('/'): # If starts with '/', change to relative
					rel_root = rel_root[1:]

				filename_wo_ext = os.path.splitext(f)[0]
				# Create feature vectors
				fv.OUTPUT_DIR = os.path.join(os.path.join(os.path.join(DockerConfig.OUTPUT_DIRECTORY, '%s_FEATUREVECS_SINGLE' % (OUT_PREFIX,)), rel_root), filename_wo_ext)
				fv.INPUT_FILE_DIR = root
				fv.CONTROL_PRED_CSV = full_path
				fv.OUTPUT_PICKLE = os.path.join(fv.OUTPUT_DIR, 'FV.pickle')
				fv.OUTPUT_CSV = os.path.join(fv.OUTPUT_DIR, 'FV.csv')
#				print fv.OUTPUT_DIR, fv.INPUT_FILE_DIR, fv.CONTROL_PRED_CSV, fv.OUTPUT_PICKLE, fv.OUTPUT_CSV
				print "Creating feature fectors for", full_path
				fv.create(fv.CONTROL_PRED_CSV, fv.OUTPUT_DIR, fv.OUTPUT_PICKLE, fv.OUTPUT_CSV)
			
				print "Running outlier detection for", full_path				
				od.FEATURE_VEC_DIR = fv.OUTPUT_DIR
				od.FV_CSV = fv.OUTPUT_CSV
				od.FV_PICKLE = fv.OUTPUT_PICKLE
				od.XML_OUTPUT_DIR = os.path.join(os.path.join(os.path.join(DockerConfig.OUTPUT_DIRECTORY, '%s_inconsistency_analysis_all_xml' % (OUT_PREFIX,)), rel_root), filename_wo_ext)
				od.OUTPUT_DIR = os.path.join(os.path.join(os.path.join(DockerConfig.OUTPUT_DIRECTORY, '%s_inconsistency_analysis_all' % (OUT_PREFIX,)), rel_root), filename_wo_ext)
				od.runForAll(od.OUTPUT_DIR, od.XML_OUTPUT_DIR, od.FV_CSV, od.FV_PICKLE)
