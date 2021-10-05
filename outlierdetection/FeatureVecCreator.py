#!/usr/bin/env python
import sys
import os
import errno
import numpy as np
import re
from sklearn.metrics import silhouette_score
from sklearn.metrics import silhouette_samples
import pickle
import math
import DockerConfig
import parsers.ControlPredicateParser as cpredparser
	
OUTPUT_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'FEATUREVECS_SINGLE')
INPUT_FILE_DIR = os.path.join(DockerConfig.SOURCE_DIRECTORY, 'TEST_INPUT')
CONTROL_PRED_CSV = os.path.join(INPUT_FILE_DIR, 'ActivityManagerNative.csv')
OUTPUT_PICKLE = os.path.join(OUTPUT_DIR, 'FV.pickle')
OUTPUT_CSV = os.path.join(OUTPUT_DIR, 'FV.csv')

IGNORE_ARGS = False

def createDirIfNotExists(path):
	try:
		os.makedirs(path)
	except OSError as exception:
		if exception.errno != errno.EEXIST:
			raise

def detectMethod(name): #FIXME this is dumb, but does it seems to work out....
	return name.startswith('<') and '>' in name and '(' in name

def getFirstArg(argStr):
	# Extract permission check
	beginSplit = 0
	endSplit = 0
	for idc in range(0 , len(argStr)):
		if argStr[idc] == '"': # Begin string constant
			beginSplit = idc + 1
			for jdc in range(idc + 1, len(argStr)):
				if argStr[jdc] == '"': # End string constant
					endSplit = jdc
					break
#			print "\tSTRVAL  = ", argStr[beginSplit:endSplit]
			return argStr[beginSplit:endSplit]
		elif argStr[idc] == '<':
			beginSplit = idc
			mdepth = 1
			for jdc in range(idc + 1, len(argStr)):
				if argStr[jdc] == '>': # End string constant
					mdepth -= 1
				if mdepth <= 0:
					endSplit = jdc + 1
					break
#			print "\tSTRVAL  = ", argStr[beginSplit:endSplit]
			return argStr[beginSplit:endSplit]
		elif argStr[idc:idc+3] == 'ALL':
			return 'ALL'
	return None

def parseMethod(name):
	startIndex = name.index('>') + 1
	endIndex = 0
	parenDepth = 0
	for i in range(startIndex, len(name)):
		if name[i] == '(':
			parenDepth += 1
		elif name[i] == ')':
			parenDepth -= 1
		if parenDepth == 0:
			endIndex = i + 1
			break

	return name[:startIndex]  + name[endIndex:]
#	return (name[:startIndex] + name[endIndex:], name[startIndex:endIndex])



def getSimplifiedValStr(cp):
	return cp if not detectMethod(cp) else parseMethod(cp)

def getSimplifiedVal(cp):
	if not IGNORE_ARGS:
		return cp

	if type(cp) == str:
		return getSimplifiedValStr(cp)
	return (getSimplifiedValStr(cp[0]), getSimplifiedValStr(cp[1]))

def getFeatVecValues(entryPoints):
	fvValues = set()
	for entryPoint in entryPoints:
		cpreds = entryPoints[entryPoint]
		for cp in cpreds:
			if type(cp) != str:	#FILTER: Skip do not include cpreds tupules (a, b) where a==b
				if cp[0] == cp[1]:
					continue
			# Ignoring ARGS
			
			fvValues.add(getSimplifiedVal(cp))
#			fvValues.add(cp)
	return list(fvValues)

# Just comma seperate tuples for now...
def getFvValString(v):
	return '`' +  v + '`' if type(v) == str else ','.join( [ '`' + i + '`' for i in v ] )

# We do this to simplify writing CSV for Kmodes
def createIdMap(fvValues):
	idmap = {}
	for idfv, fv in enumerate(fvValues):
		val = getFvValString(fv)
		idmap[val] = 'cpred-%d' % (idfv,)
		idmap['N_%s' % (val,)] = 'N_cpred-%d' % (idfv,)
	return idmap

def createLabIdMap(entryPoints):
	idmap = {}
	idindex = 0
	for entryPoint in entryPoints:
		cpreds = entryPoints[entryPoint]
		idmap[entryPoint] = 'funct_%d' % (idindex,)
		idindex += 1
	return idmap

# TODO may want to create an extremely nasty feature vector with EVERYTHING so that we can predict later
def createFeatureVector(entryPoints):
	fvValues = getFeatVecValues(entryPoints) 
	fvidmap = createIdMap(fvValues)
	labidmap = createLabIdMap(entryPoints)

	featureVector = []
	labels = []
	for entryPoint in entryPoints:
#		cpreds = entryPoints[entryPoint] 
		cpreds = [ getSimplifiedVal(cp) for cp in entryPoints[entryPoint] ]

		fv = [] 
		for vi,v in enumerate(fvValues):
			val = getFvValString(v)
			fv.append(fvidmap[val if v in cpreds else 'N_%s' % (val,)])
		featureVector.append(fv)
		labels.append(labidmap[entryPoint])
	return (labels, fvValues, fvidmap, labidmap, featureVector)

################################################################################

def dumpCSV(filename, labels, featureVector):
	with open(filename, 'wb') as outputCSV:
		for idlab,lab in enumerate(labels):
			outputCSV.write('%s,%s\n' % (lab, ','.join(featureVector[idlab])))

def createFV(methods, FV_OUTPUT_DIR, OUTPUT_PICKLE, OUTPUT_CSV):
	labels,fvValues,fvidmap,labidmap,featureVector = createFeatureVector(methods)

	# Let's pickle everything
	pickle.dump((labels,fvValues,fvidmap,labidmap,featureVector), open(OUTPUT_PICKLE, 'wb'))

	# Now write the feature vector to a CSV
	dumpCSV(OUTPUT_CSV, labels, featureVector)
			
def create(CONTROL_PRED_CSV, OUTPUT_DIR, OUTPUT_PICKLE, OUTPUT_CSV):
	# Create dirs if they do not exist!
	createDirIfNotExists(DockerConfig.OUTPUT_DIRECTORY)
	createDirIfNotExists(OUTPUT_DIR)

	methods = cpredparser.parseControlPredicates(CONTROL_PRED_CSV)
	createFV(methods, OUTPUT_DIR, OUTPUT_PICKLE, OUTPUT_CSV)

if __name__ == '__main__':
	if len(sys.argv) == 4:
		CONTROL_PRED_CSV = os.path.join(INPUT_FILE_DIR, sys.argv[1])
		OUTPUT_PICKLE = os.path.join(OUTPUT_DIR, sys.argv[2])
		OUTPUT_CSV = os.path.join(FV_OUTPUT_DIR, sys.argv[3])
	else:
		print "Usage %s <CONTROL_PRED_CSV> <OUTPUT_PICKLE> <OUTPUT_CSV>\nRunning default (Activity Manager Service)" % (sys.argv[0],)


	create(CONTROL_PRED_CSV, OUTPUT_DIR, OUTPUT_PICKLE, OUTPUT_CSV)

