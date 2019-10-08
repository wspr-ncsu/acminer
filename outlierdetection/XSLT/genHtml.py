#!/usr/bin/env python
import os
import errno
import sys
import multiprocessing as mp
from subprocess import call

ROOT_OUTPUT_DIR = 'policyminer-results/android-7.1.0_r4/html'
XML_BASE_DIR = '../output/inconsistency_analysis_all_xml'

SAXON_JAR = 'saxon-mod.jar'
XSL_FILE = 'ConsistencyAnalysis.xsl'

def createDirIfNotExists(path):
	try:
		os.makedirs(path)
	except OSError as exception:
		if exception.errno != errno.EEXIST:
			raise

def getHtmlFilename(html_output_path, f):
	return os.path.join(html_output_path, os.path.splitext(os.path.basename(f))[0] + '.html')

def getAllFiles(root_output_dir, xml_base_dir):
	xml_files = []
	for (root, dirs, files) in os.walk(xml_base_dir):
		for f in files:
			if f.endswith('.xml'):
				xml_file = os.path.join(root, f)
				html_output_dir = os.path.join(root_output_dir, os.path.relpath(root, xml_base_dir))
				#Create the output directory
				createDirIfNotExists(html_output_dir)
				html_file = getHtmlFilename(html_output_dir, f)
				xml_files.append((xml_file, html_file))
	return xml_files


def generateHtml((xml_file, html_file)):
	print "Dumping HTML for ", xml_file
	call(['java', '-jar', SAXON_JAR, '-s:%s' % (xml_file,), '-xsl:%s' % (XSL_FILE,), '-o:%s' % (html_file,)])

if __name__ == '__main__':
	if len(sys.argv) == 3:
		XML_BASE_DIR = sys.argv[1]
		ROOT_OUTPUT_DIR = sys.argv[2]
	else:
		print "Running with default config..."
		print "Usage: %s <XML_BASE_DIRECTORY> <ROOT_OUTPUT_DIRECTORY>" % (sys.argv[0],)
#		sys.exit(0)


	createDirIfNotExists(ROOT_OUTPUT_DIR)

	# Get list of files - (xml_file, html_output_dir, html_file)
	files = getAllFiles(ROOT_OUTPUT_DIR, XML_BASE_DIR)

	p = mp.Pool(mp.cpu_count())
	p.map(generateHtml, files)

