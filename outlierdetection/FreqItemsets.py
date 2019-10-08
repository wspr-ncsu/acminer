#!/usr/bin/env python
import sys
import os
import errno
import pickle
import math
import multiprocessing as mp

import ACMinerQuery as qsm

from fim import eclat
import parsers.MethodFilter as mf

from lxml import etree
from lxml.etree import tostring


import DockerConfig

OUTPUT_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'inconsistency_analysis_all')
FEATURE_VEC_DIR =  os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'FEATUREVECS_SINGLE')
XML_OUTPUT_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'inconsistency_analysis_all_xml')


EPG_FV_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'FEATUREVECS_EPOINT_GROUPS')
EPG_OUTPUT_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'inconsistency_analysis_epg')
EPG_XML_OUTPUT_DIR = os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'inconsistency_analysis_epg_xml')


FV_CSV = os.path.join(FEATURE_VEC_DIR, "FV.csv")
FV_PICKLE = os.path.join(FEATURE_VEC_DIR, "FV.pickle")

DB_FILENAME = 'acminer_db/_acminer_db_.xml'

MIN_SUPPORT = 2
MIN_ITEMSET_SIZE = 2
MIN_CONF = 0.85

PARALLELIZE = False

def createDirIfNotExists(path):
	try:
		os.makedirs(path)
	except OSError as exception:
		if exception.errno != errno.EEXIST:
			raise

def loadAndStrip(elements, idMap):
	commonPatterns = {}
	for ide,e in enumerate(elements):
		for i in e:
			if i not in commonPatterns:
				commonPatterns[i] = []
			commonPatterns[i].append(ide)

	# Now let's inverse the dictionary
	inverseCommonPatterns = {}
	for k,v in commonPatterns.iteritems():
		newk = tuple(v)
		if newk not in inverseCommonPatterns:
			inverseCommonPatterns[newk] = []
		inverseCommonPatterns[newk].append(k)

	# Remove values from dictionary that only have one entry
	removeKeys = [ k for k,v in inverseCommonPatterns.iteritems() if len(v) <= 1 ]
	for k in removeKeys:
		inverseCommonPatterns.pop(k, None)

	for k,v in inverseCommonPatterns.iteritems():
		if tuple(v) not in idMap:
			idMap.append(tuple(v))
		indexval = idMap.index(tuple(v))
		for index in k:
			for val in v:
				elements[index].remove(val)
			elements[index].append(indexval)

	return (elements, idMap)

def loadData(FV_FILE):
	transactions = []
	idMap = []
	for line in open(FV_FILE, 'rb'):
		vals = []
		for v in line.strip().split(',')[1:]:
			if len(v) <= 0:
				continue
			if not v.startswith('N_'):
				if v not in idMap:
					idMap.append(v)
				vals.append(idMap.index(v))
		transactions.append(sorted(vals))

	return loadAndStrip(transactions, idMap)
#	return (transactions, idMap)

def getIntersection(a, b):
	s,l = (a, b) if len(a) <= len(b) else (a, b)
	return [ i for i in s if i in l ]

def setAContainsB(a, b):
	for i in b:
		if i not in a:
			return False
	return True

def calcSupportFrac(s, transactions, target):
	nmax = 0.0
	cnt = 0.0
	skipFlag = True if target is not None else False

	for iset in transactions:
		if skipFlag and iset == target: # Skip over first instance of target set in transactions, as it will skew our confidence measure
			skipFlag = False
			continue

		# Contains lhs
		if setAContainsB(iset, s):
			nmax += 1.0
		cnt += 1.0
	return nmax/cnt


def calcConfidence(lhs, rhs, transactions, target):
	s = sorted(lhs + rhs)
	nmax = 0.0
	dmax = 0.0
	skipFlag = True if target is not None else False

	for iset in transactions:
		if skipFlag and iset == target: # Skip over first instance of target set in transactions, as it will skew our confidence measure
			skipFlag = False
			continue

		# Contains lhs + rhs
		if setAContainsB(iset, s):
			nmax += 1.0
		# Contains lhs
		if setAContainsB(iset, lhs):
			dmax += 1.0

	# Target will be in the transactions, skip over it as it will skew our confidence
	return nmax/dmax # Target 

# TODO what we really want here is percentage of rule matched... # of elements in X in LHS / len(X)
def jaccard(s1, s2):
	#M11
	m11 = float(len([ c for c in s1 if c in s2 ]))
	# M01
	m01 = float(len([ c for c in s1 if c not in s2 ]))
	# M10
	m10 = float(len([ c for c in s2 if c not in s1 ]))
	if (m01 + m10 + m11) == 0:
		return 0.0
	return m11/(m01 + m10 + m11)

def getS1MinusS2(s1, s2):
	return [ i for i in s1 if i not in s2 ]

# Assume closed itemsets?
def getAssocRules(target, itemsets, minconf, transactions):
	# Presort itemsets...
	itemsets = [ (sorted(iset), isup) for iset,isup in itemsets ]
	res = []
	# Get itemsets with largest Jaccard
	for iset,isup in itemsets: # Bucket by Jaccard!
		if target == iset: # target is a closed frequent itemset
			return [] # TODO should we check support or confidence?

		lhs = getIntersection(target, iset)
		rhs = [ i for i in iset if i not in lhs ] # possible RHS
		if len(rhs) <= 0: # No rhs, continue...
			continue

		# Calculate confidence
		jcoeff = jaccard(target, lhs)
		if jcoeff <= 0.0:
			continue
	
		# Build up RHS 1-by-1
		# Look for highest confidence, if rule found A->B, A->C, and A->BC equal confidence then merge!

		conf = calcConfidence(lhs, rhs, transactions, target)
		if conf < minconf:
			continue

		lift = conf/calcSupportFrac(rhs, transactions, target)
		# If lhs INTERSECTION target != EMPTY_SET, calculate the confidence
		sdiff = getS1MinusS2(target, lhs)
		dconf = calcConfidence(lhs, sdiff, transactions, target) if len(sdiff) >= 1 else -1.0


		conviction = (1.0 - calcSupportFrac(rhs, transactions, target))/(1.0-conf) if conf != 1.0 else 1.0

		res.append((jcoeff, conf, (lhs, rhs), isup, dconf, lift, conviction))

	res.sort(key=lambda x: (x[0], x[1], x[3], x[2], x[4], x[5]), reverse=True)
	return res

# Get frequent closed itemsets that have val

def decodeVals(outputfile, oprefix, val, fvinv_map, idMap):
	if type(val) == str:
		outputfile.write(oprefix % (fvinv_map[val],))
	else:
		for v in val:
			v2 = idMap[v]
			if v2.startswith("N_"):
				continue
			outputfile.write(oprefix % (fvinv_map[v2],))

def getExpandedVals(vlist, val, idMap, fvinv_map):
	if type(val) == str:
		vlist.append(fvinv_map[val])
	else:
		for v in val:
			v2 = idMap[v]
			if v2.startswith("N_"):
				continue
			vlist.append(fvinv_map[v2])

def expandCpreds(cpreds, idMap, fvinv_map):
	cpred_extended = []
	for pred in cpreds:
		getExpandedVals(cpred_extended, idMap[pred], idMap, fvinv_map)
	return cpred_extended

def decodeAssocRule(lhs, rhs, idMap, fvinv_map, conf, isup, target):
	lhs_expanded = sorted(list(set(expandCpreds(lhs, idMap, fvinv_map))))
	target_expanded = sorted(list(set(expandCpreds(target, idMap, fvinv_map))))
	rhs_expanded = sorted(list(set(expandCpreds(rhs, idMap, fvinv_map))))

	# Let's do some filtering now to simplify the output for the analyst:
	# Filter 1: If an item in the RHS (args stripped) is contained in the target (args stripped), we dump it above...

	target_simplified = set([ mf.simplifyPredicate(t) for t in target_expanded ])

	rhs_simplified = {}
	alt_checks = {}
	for cpred in rhs_expanded:
		spred = mf.simplifyPredicate(cpred)
		if spred not in target_simplified:
			if spred not in rhs_simplified:
				rhs_simplified[spred] = []
			rhs_simplified[spred].append(cpred)
		else:
			if spred not in alt_checks:
				alt_checks[spred] = []
			alt_checks[spred].append(cpred)

	# Filter 2: Remove args from RHS when dumping output and only output UNIQUE values (minus certain special cases -- e.g., checkPermission)
	#rhs_simplified = sorted(list(set(rhs_simplified)))
	if len(rhs_simplified.keys()) <= 0 and len(alt_checks.keys()) <= 0:
		return None


	lhs_simplified = {}
	for cpred in lhs_expanded:
		spred = mf.simplifyPredicate(cpred)
		if spred not in lhs_simplified:
			lhs_simplified[spred] = []
		lhs_simplified[spred].append(cpred)

	missing_antecedent = {}
	for cpred in target_expanded:
		if cpred not in lhs_expanded:
			spred = mf.simplifyPredicate(cpred)
			if spred not in missing_antecedent:
				missing_antecedent[spred] = []
			missing_antecedent[spred].append(cpred)


	# Recalculate the LHS and RHS - the stats are wrong earlier due to how we collapsed common co-occurring control predicates...	
	jacc = jaccard(target_expanded, lhs_expanded)
	lhsLen = len(lhs_expanded)
	rhsLen = len(rhs_simplified.keys())

	# Filter 4: Remove rules there length of RHS is grather than 5 times of the length of the RHS
	if float(rhsLen)/float(lhsLen) >= 5.0:
		return None

	return (conf, jacc, isup, lhsLen, rhsLen, lhs_expanded, target_expanded, rhs_expanded, lhs_simplified, target_simplified, rhs_simplified, alt_checks, missing_antecedent)
#	return (conf, jacc, isup, lhsLen, rhsLen, lhs_expanded, target_expanded, rhs_expanded, rhs_simplified, target_simplified)


def writeOutput(outputfile, (conf, jacc, isup, lhsLen, rhsLen, lhs_expanded, target_expanded, rhs_expanded, lhs_simplified, target_simplified, rhs_simplified, alt_checks, missing_antecedent)):

	outputfile.write("Confidence = %f, LHS Jaccard = %f, Sup=%d\n" % (conf, jacc, isup))

	lhsDivRhs = float(lhsLen)/float(rhsLen) if rhsLen > 0 else 0.0

	outputfile.write("len(LHS) = %d, len(RHS) = %d, LHS/RHS=%f\n" % (lhsLen, rhsLen, lhsDivRhs))
	
	for i in lhs_expanded:
		outputfile.write("\t---%s\n" % (i,))


	for i in rhs_simplified:
		for j in rhs_simplified[i]:
			outputfile.write("\t\t\t+++%s\n" % (j,))
#		outputfile.write("\t\t\t+++%s\n" % (i,))

def getTransactionsContainingItemset(decoded_transactions, itemset, labels, labinv_map):
	return [ decodeLabel(labid, labels, labinv_map) for labid,t in enumerate(decoded_transactions) if set(itemset).issubset(set(t)) ]

def dumpXml(xmlFileName, rules, labid, lab, ntrans, decoded_transactions, labels, labinv_map):

	XML_ROOT = etree.Element("entrypoint", {'name' : lab, 'id' : str(labid), 'href' : 'http://androidxref.com/7.1.1_r6/xref/'})
	rootDoc = etree.ElementTree(XML_ROOT)

	for args in rules:
		conf, jacc, isup, lhsLen, rhsLen, lhs_expanded, target_expanded, rhs_expanded, lhs_simplified, target_simplified, rhs_simplified, alt_checks, missing_antecedent = args

		ruleElement = etree.SubElement(XML_ROOT, "rule", {'confidence' : str(conf), 'jaccard' : str(jacc), 'support' : str(isup), 'psupport' : str(float(isup)/float(ntrans))})
		antecedentElement = etree.SubElement(ruleElement, "antecedent")
		consequentElement = etree.SubElement(ruleElement, "consequent")
		missingAntecedentElement = etree.SubElement(ruleElement, "missing_antecedent")
		altChecksElement = etree.SubElement(ruleElement, "alternative_checks")
		otherEntrypointsElement = etree.SubElement(ruleElement, "other_entrypoints")
		
		itemset = set([i for i in lhs_expanded])
		itemset.update(set(rhs_expanded))
		entrypoint_names = []
		for tname in getTransactionsContainingItemset(decoded_transactions, itemset, labels, labinv_map):
			entrypoint_names.append(tname)

		for i in lhs_simplified:
			for j in lhs_simplified[i]:
				itemElement = etree.SubElement(antecedentElement, "item", {'name' : j})
				# TODO for each entrypoint, dump out location...
				sourceMethods = etree.SubElement(itemElement, "source_methods")
				# For myself and each entrypoint, look up
				epoints = [ e for e in entrypoint_names ]
				epoints.append(lab)
				
				res = qsm.query(';'.join(epoints), j, DB_FILENAME) 	
				if res is not None and len(res) > 0:
					for m in res:
						etree.SubElement(sourceMethods, "method", {'name' : m})
				else:
					with open('DEBUG_ERROR.LOG', 'a') as outlog:
						outlog.write('%s - %s\n' % (';'.join(epoints), j))

		for i in missing_antecedent:
			for j in missing_antecedent[i]:
				itemElement = etree.SubElement(missingAntecedentElement, "item", {'name' : j})
				# TODO for each entrypoint, dump out location...
				sourceMethods = etree.SubElement(itemElement, "source_methods")
				# For myself, look up
				epoints = [ lab ]

				res = qsm.query(';'.join(epoints), j, DB_FILENAME)
				if res is not None and len(res) > 0:
					for m in res:
						etree.SubElement(sourceMethods, "method", {'name' : m})
				else:
					with open('DEBUG_ERROR.LOG', 'a') as outlog:
						outlog.write('%s - %s\n' % (';'.join(epoints), j))


		for i in rhs_simplified:
			for j in rhs_simplified[i]:
				itemElement = etree.SubElement(consequentElement, "item", {'name' : j})
				# TODO for each entrypoint, dump out location...
				sourceMethods = etree.SubElement(itemElement, "source_methods")
				epoints = [ e for e in entrypoint_names ]
				res = qsm.query(';'.join(epoints), j, DB_FILENAME) 	
				if res is not None and len(res) > 0:
					for m in res:
						etree.SubElement(sourceMethods, "method", {'name' : m})
				else:
					with open('DEBUG_ERROR.LOG', 'a') as outlog:
						outlog.write('%s - %s\n' % (';'.join(epoints), j))

		for i in alt_checks:
			for j in alt_checks[i]:
				itemElement = etree.SubElement(consequentElement, "item", {'name' : j})
				# TODO for each entrypoint, dump out location...
				sourceMethods = etree.SubElement(itemElement, "source_methods")

				epoints = [ e for e in entrypoint_names ]
				res = qsm.query(';'.join(epoints), j, DB_FILENAME) 	
				if res is not None and len(res) > 0:
					for m in res:
						etree.SubElement(sourceMethods, "method", {'name' : m})
				else:
					with open('DEBUG_ERROR.LOG', 'a') as outlog:
						outlog.write('%s - %s\n' % (';'.join(epoints), j))

		#Write all other entrypoints
		for e in entrypoint_names:
			etree.SubElement(otherEntrypointsElement, "epoint", {'name' : e})
			
	rootDoc.write(xmlFileName, pretty_print=True, xml_declaration=True, encoding="utf-8")

def writeRegularFile(output_dir, rules, labid, lab):
	if len(rules) <= 0:
		return

	with open(os.path.join(output_dir, "%d.txt" % (labid,)), 'wb') as outputfile:
		outputfile.write("Entrypoint: %s\n" % (lab,))
		for args in rules:
			outputfile.write("------------------------------------------------------\n")
			writeOutput(outputfile, args)
			outputfile.write("------------------------------------------------------\n")

def decodeLabel(labid, labels, labinv_map):
	return labinv_map[labels[labid]]

# TODO remove redundant rules! -- If r1.LHS == r2.LHS AND (((r2.RHS isSubsetOf r1.RHS) --> Remove r2) OR (r1.RHS isSubsetOf r2.RHS) --> Remove r1)
def checkConsistency(args):
	t, itemsets, confidence, transactions, decoded_transactions, lab, labid, fvinv_map, idMap, output_directory, xml_output_directory, labels, labinv_map = args

	print "Starting consistency analysis for", lab

	res = getAssocRules(t, itemsets, confidence, transactions)
	if len(res) <= 0:
		print "Not writing output for",lab
		print "Done with consistency analysis for ", lab
		return

	rules = []
	for jcoeff, conf, arule, sup, dconf, lift, conviction in res:
		args = decodeAssocRule(arule[0], arule[1], idMap, fvinv_map, conf, sup, t)
		if args is None:
			continue

		# Filter: Remove redundant rules -- i.e., multiple rules that have the same RHS
		addRule = True
		for r in rules:
			old_rhs = set(r[7])	# consequent of previously added rule
			new_rhs = set(args[7]) #consequent of new rule

			if new_rhs <= old_rhs: # If it adds no new information (i.e., new_rhs subset old_rhs)
				addRule = False
				break


		if addRule:
			rules.append(args)

	if len(rules) > 0:
		writeRegularFile(output_directory, rules, labid, lab)
		dumpXml(os.path.join(xml_output_directory, '%d.xml') % (labid,), rules, labid, lab, len(transactions), decoded_transactions, labels, labinv_map)

	print "Done with consistency analysis for ", lab


################################################################################################################################################
# Running for all


def runForAll(OUTPUT_DIR, XML_OUTPUT_DIR, FV_CSV, FV_PICKLE):
	createDirIfNotExists(OUTPUT_DIR)
	createDirIfNotExists(XML_OUTPUT_DIR)

	transactions,idMap = loadData(FV_CSV)

	print "Mining itemsets"
	itemsets = eclat(tracts=transactions, target='c', supp=-MIN_SUPPORT, zmin=MIN_ITEMSET_SIZE)
	print "Done mining itemsets"
	if itemsets is None:
		print "No itemsets found"
		sys.exit(0)
	print "Found",len(itemsets),"itemsets"
	labels,fvValues,fvidmap,labidmap,featureVector = pickle.load(open(FV_PICKLE, 'rb'))
	fvinv_map = {v: k for k, v in fvidmap.iteritems()}
	labinv_map = {v: k for k, v in labidmap.iteritems()}


	decoded_transactions = [ set(expandCpreds(t, idMap, fvinv_map)) for t in transactions ]
	

	args = [ (t, itemsets, MIN_CONF, transactions, decoded_transactions, decodeLabel(idt, labels, labinv_map), idt, fvinv_map, idMap, OUTPUT_DIR, XML_OUTPUT_DIR, labels, labinv_map) for idt,t in enumerate(transactions) if len(t) > 0 ]

	if PARALLELIZE:
		# Let's make this parallel now for speed...
		p = mp.Pool(mp.cpu_count())
		p.map(checkConsistency, args)
	else:
		for arg in args:
			checkConsistency(arg)


################################################################################################################################################
# Running for epoint groups

def getFilename(f):
	return  os.path.splitext(os.path.basename(f))[0]

def isFile(PATH, f):
	return os.path.isfile(os.path.join(PATH, f))

def dumpXmlItemset(xmlFileName, rules, labid, lab, idMap, fvinv_map, ntrans, decoded_transactions, labels, labinv_map):
	if len(rules) <= 0:
		return

	XML_ROOT = etree.Element("entrypoint", {'name' : lab, 'id' : str(labid), 'href' : 'http://androidxref.com/7.1.1_r6/xref/'})
	rootDoc = etree.ElementTree(XML_ROOT)

	outputXml = False
	for isets,isup in rules:
		if float(isup)/float(ntrans) < 0.50:
			continue

		outputXml = True

		iset = sorted(list(set(expandCpreds(isets, idMap, fvinv_map))))
		iset_simplified = {}
		for cpred in iset:
			spred = mf.simplifyPredicate(cpred)
			if spred not in iset_simplified:
				iset_simplified[spred] = []
			iset_simplified[spred].append(cpred)


		ruleElement = etree.SubElement(XML_ROOT, "rule", {'confidence' : '-1', 'jaccard' : '-1', 'support' : str(isup), 'psupport' : str(float(isup)/float(ntrans))})
		consequentElement = etree.SubElement(ruleElement, "consequent")
		otherEntrypointsElement = etree.SubElement(ruleElement, "other_entrypoints")

		for i in iset_simplified:
			itemElement = etree.SubElement(consequentElement, "item", {'name' : i})
			allItemsElement = etree.SubElement(itemElement, "all_items")
			for j in iset_simplified[i]:
				fullItemElement = etree.SubElement(allItemsElement, "full_item", {'name' : j})


		#Write all other entrypoints
		itemset = set([i for i in iset])
		for tname in getTransactionsContainingItemset(decoded_transactions, itemset, labels, labinv_map):
			etree.SubElement(otherEntrypointsElement, "epoint", {'name' : tname})

	if outputXml:
		rootDoc.write(xmlFileName, pretty_print=True, xml_declaration=True, encoding="utf-8")


def checkGroupConsistency(args):
	t, itemsets, confidence, transactions, decoded_transactions, lab, labid, fvinv_map, idMap, output_directory, xml_output_directory, labels, labinv_map = args

	if len(t) <= 0: # If no authorization logic, this is a special case...
		# Let's predict frequent itemsets...
		# TODO narrow this down farther, by assuming that we need a X\%support


		dumpXmlItemset(os.path.join(xml_output_directory, '%d.xml') % (labid,), itemsets, labid, lab, idMap, fvinv_map, len(transactions), decoded_transactions, labels, labinv_map)
#		dumpXml(os.path.join(xml_output_directory, '%d.xml') % (labid,), rules, labid, lab)
		
	else:
		# Use the general appraoch
		checkConsistency(args)


def runForEPointGroups():
	createDirIfNotExists(EPG_OUTPUT_DIR)
	createDirIfNotExists(EPG_XML_OUTPUT_DIR)

	# Read in input directory
	csvFiles = [ getFilename(f) for f in os.listdir(EPG_FV_DIR) if isFile(EPG_FV_DIR, f) and f.endswith('.csv')]
	for fname in csvFiles:
		EPG_FV_CSV = os.path.join(EPG_FV_DIR, fname + '.csv')
		EPG_FV_PICKLE = os.path.join(EPG_FV_DIR, fname + '.pickle')
		EPG_i_OD = os.path.join(EPG_OUTPUT_DIR, fname)
		EPG_i_OD_XML = os.path.join(EPG_XML_OUTPUT_DIR, fname)

		createDirIfNotExists(EPG_i_OD)
		createDirIfNotExists(EPG_i_OD_XML)

		
		transactions,idMap = loadData(EPG_FV_CSV)
		
		#Make sure transactions are not all empty
		if len(idMap) <= 0:
			continue

		print "Mining itemsets for",fname
		itemsets = eclat(tracts=transactions, target='c', supp=-MIN_SUPPORT, zmin=MIN_ITEMSET_SIZE)
		print "Done mining itemsets for",fname
		if itemsets is None:
			print "No itemsets found"
			sys.exit(0)
		print "Found",len(itemsets),"itemsets"
		labels,fvValues,fvidmap,labidmap,featureVector = pickle.load(open(EPG_FV_PICKLE, 'rb'))
		fvinv_map = {v: k for k, v in fvidmap.iteritems()}
		labinv_map = {v: k for k, v in labidmap.iteritems()}

		decoded_transactions = [ set(expandCpreds(t, idMap, fvinv_map)) for t in transactions ]


		itemsets = sorted([(isup, iset) for iset,isup in itemsets], reverse=True)
		itemsets = [(iset, isup) for isup,iset in itemsets]

		for idt,t in enumerate(transactions):
			checkGroupConsistency((t, itemsets, MIN_CONF, transactions, decoded_transactions, decodeLabel(idt, labels, labinv_map), idt, fvinv_map, idMap, EPG_i_OD, EPG_i_OD_XML, labels, labinv_map))


if __name__ == '__main__':
	if len(sys.argv) == 5:
		FV_CSV = os.path.join(FEATURE_VEC_DIR, sys.argv[1])
		FV_PICKLE = os.path.join(FEATURE_VEC_DIR, sys.argv[2])
		XML_OUTPUT_DIR = os.path.join(os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'inconsistency_analysis_all_xml'), sys.argv[3])
		OUTPUT_DIR = os.path.join(os.path.join(DockerConfig.OUTPUT_DIRECTORY, 'inconsistency_analysis_all'), sys.argv[4])
	else:
		print "Usage: %s <FV_CSV> <FV_PICKLE> <XML_OUTPUT_SUB_DIRECTORY_NAME> <OUTPUT_SUB_DIRECTORY_NAME>\nRunning for ActivityManagerService" % (sys.argv[0],)

	runForAll(OUTPUT_DIR, XML_OUTPUT_DIR, FV_CSV, FV_PICKLE)
#	runForEPointGroups()
