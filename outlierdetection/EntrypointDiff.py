#!/usr/bin/env python

import pickle
import DockerConfig
import os
import sys
import errno
from lxml import etree
from lxml.etree import tostring


# ./EntrypointDiff.py "<com.android.server.am.ActivityManagerService: void registerTaskStackListener(android.app.ITaskStackListener)>" "<com.android.server.am.ActivityManagerService: boolean shutdown(int)>"

OUTPUT_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'FEATUREVECS_SINGLE')
METHOD_DIFF_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'METHOD_DIFFS')

def createDirIfNotExists(path):
	try:
		os.makedirs(path)
	except OSError as exception:
		if exception.errno != errno.EEXIST:
			raise

def loadValues():
	# Load pickle values
	picklename = 'FV.pickle'
	labels,fvValues,fvidmap,labidmap,featureVector = pickle.load(open(os.path.join(OUTPUT_DIR, picklename), 'rb'))
	#Reverse the dictionaries
	fvinv_map = {v: k for k, v in fvidmap.iteritems()}
	labinv_map = {v: k for k, v in labidmap.iteritems()}
	return (labels,fvValues,fvidmap,labidmap,featureVector,fvinv_map,labinv_map)

def getIndex(ep, labidmap, labels):
	if ep not in labidmap:
		print "Error: %s not a valid entrypoint" % (ep,)
		sys.exit(1)
	encodedEp = labidmap[ep]
	return labels.index(encodedEp)



def dumpControlPredXml(xmlRoot, fvinv_map, ename):
	if len(ename) <= 0:
		return

	cpredval = fvinv_map[ename]
	cpredContainsFlag = 'true'
	if cpredval.startswith('N_'):
		cpredval = cpredval[2:]
		cpredContainsFlag = 'false'

	cpredElement = etree.SubElement(xmlRoot, "controlPred")
	
	argvals = [ v for v in cpredval.split('`') if v != ',' and len(v) >= 1 ]
	for idarg, arg in enumerate(argvals):
		etree.SubElement(cpredElement, "arg%d" % (idarg,), name=arg)


def dumpCpreds(xmlRoot, fvinv_map, cpreds, tagName):
	element = etree.SubElement(xmlRoot, tagName)
	for cp in cpreds:
		dumpControlPredXml(element, fvinv_map, cp)


def writeXml(OUTPUT_XML_FILE, ep1, ep2, diffep1, diffep2, cpredsim, labidmap, fvinv_map):
	XML_ROOT = etree.Element("root")
	rootDoc = etree.ElementTree(XML_ROOT)

	entryPointEl = etree.SubElement(XML_ROOT, "entrypointComparison")
	ep1Element = etree.SubElement(entryPointEl, "entrypoint1", {'name' : ep1, 'identifier' : labidmap[ep1]})
	ep2Element = etree.SubElement(entryPointEl, "entrypoint2", {'name' : ep2, 'identifier' : labidmap[ep2]})

	dumpCpreds(ep1Element, fvinv_map, diffep1, "controlpreddiffs")
	dumpCpreds(ep2Element, fvinv_map, diffep2, "controlpreddiffs")
	dumpCpreds(entryPointEl, fvinv_map, cpredsim, "controlpredsims")
	rootDoc.write(OUTPUT_XML_FILE, pretty_print=True, xml_declaration=True, encoding="utf-8")

if __name__ == '__main__':
	if len(sys.argv) < 3:
		print "Usage: %s <Entrypoint 1> <Entrypoint 2>" % (sys.argv[0],)
		sys.exit(1)	

	ep1 = sys.argv[1]
	ep2 = sys.argv[2]

	labels,fvValues,fvidmap,labidmap,featureVector,fvinv_map,labinv_map = loadValues()

	indexEp1 = getIndex(ep1, labidmap, labels)
	indexEp2 = getIndex(ep2, labidmap, labels)


	createDirIfNotExists(DockerConfig.OUTPUT_DIRECTORY)
	createDirIfNotExists(METHOD_DIFF_DIR)

	xmlfilename = "diff_%s_%s.xml" % (labidmap[ep1], labidmap[ep2])
	OUTPUT_XML_FILE = os.path.join(METHOD_DIFF_DIR, xmlfilename)
	print "Outputting results in %s" % (OUTPUT_XML_FILE,)


	FvEp1 = featureVector[indexEp1]
	FvEp2 = featureVector[indexEp2]
	diffep1 = []
	diffep2 = []
	cpredsim = []

	for idv,v in enumerate(FvEp1):
		if FvEp1[idv] == FvEp2[idv]:
			if not FvEp1[idv].startswith('N_'):
				cpredsim.append(FvEp1[idv])
		else:
			if FvEp1[idv].startswith('N_'):
				diffep2.append(FvEp2[idv])
			else:
				diffep1.append(FvEp1[idv])

	writeXml(OUTPUT_XML_FILE, ep1, ep2, diffep1, diffep2, cpredsim, labidmap, fvinv_map)

