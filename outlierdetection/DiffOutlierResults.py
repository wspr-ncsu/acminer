#!/usr/bin/env python

# Script to diff
import FeatureVecCreator as fv
import FreqItemsets as od
import os
import sys
import DockerConfig
import re
from lxml import etree

def fetchItems(root):
	items = []
	for item in root.iter("item"):
		iname = item.get("name")
		# Get all items...
		all_items = item.find("all_items")
		for full_item in all_items.iter("full_item"):
			items.append(full_item.get("name"))
	return items

def loadXml(xml_filename):
	tree = etree.parse(xml_filename)
	# Read entrypoint info
	root = tree.getroot()
	# Get entrypoint name
	entrypointName = root.get('name')
	# Iterate through rules
	rules = []
	idcounter = 0
	for rule in root.iter("rule"):
		antecedent = rule.find("antecedent")
		aitems = fetchItems(antecedent)

		consequent = rule.find("consequent")
		citems = fetchItems(consequent)

		missing_antecedent = rule.find("missing_antecedent")
		maitems = fetchItems(missing_antecedent)

		alternative_checks = rule.find("alternative_checks")
		acitems = fetchItems(alternative_checks)

		epoints = []
		other_entrypoints = rule.find("other_entrypoints")
		for ep in other_entrypoints.iter("epoint"):
			epoints.append(ep.get("name"))
		rules.append((aitems,citems,maitems,acitems,epoints,idcounter))
		idcounter += 1

	return (entrypointName, rules)

def loadAllXmlFiles(root):
	epoints = {}
	for root, dirs, files in os.walk(root):
		for f in files:
			full_path = os.path.join(root, f)
			if os.path.isfile(full_path) and f.endswith('.xml'):
				ename,erules = loadXml(full_path)
				epoints[ename] = erules
	return epoints

def getDiff(rhs_vendor, rhs_aosp):
	vplus = [ vi for vi in rhs_vendor if vi not in rhs_aosp ]
	vminus = [ vi for vi in rhs_aosp if vi not in rhs_vendor ]
	vsame = [ vi for vi in rhs_vendor if vi in rhs_aosp ]
	return (vplus, vminus, vsame)

def ediff(vendor_xml_filename, aosp_rules):
	print "Parsing", vendor_xml_filename
	tree = etree.parse(vendor_xml_filename)
	# Read entrypoint info
	root = tree.getroot()
	# Get entrypoint name
	entrypointName = root.get('name')
	print "Checking", entrypointName
	# Iterate through rules
	idcounter = 0
	for rule in root.iter("rule"):
		vantecedent = rule.find("antecedent")
		vantecedent_items = fetchItems(vantecedent)

		vconsequent = rule.find("consequent")
		vconsequent_items = fetchItems(vconsequent)

		diffs = None
		if entrypointName not in aosp_rules:
			continue

		for aantecedent_items,aconsequent_items,amissing_antecendent_items,aalternative_checks_items,aepoints,aidcounter in aosp_rules[entrypointName]:
			vaitems = vantecedent_items #[ vantecedent_items[k] for k in vantecedent_items ]
			aaitems = aantecedent_items #[ aantecedent_items[k] for k in aantecedent_items ]
			vcitems = vconsequent_items #[ vconsequent_items[k] for k in vconsequent_items ]
			acitems = aconsequent_items #[ aconsequent_items[k] for k in aconsequent_items ]

			if set(vaitems) == set(aaitems):
				if set(acitems) == set(vcitems):
					diffs = None
					break

				# Output DIFF with AOSP
				vplus,vminus,vsame = getDiff(vcitems, acitems)
				if len(vplus) > 0 or len(vminus) > 0 or len(vsame) > 0:
					if diffs is None:
						diffs = []
					diffs.append((vplus,vminus,vsame,aidcounter))

		if diffs is not None:
			diffElement = etree.SubElement(rule, "aosp_diff")
			for vplus,vminus,vsame,aidcounter in diffs:
				diffRule = etree.SubElement(diffElement, "rule_diff", {"rule_id" : aidcounter})
				diffSame = etree.SubElement(diffRule, "diff_same")
				diffSameItems = etree.SubElement(diffSame, "all_items")
				for i in vsame:
					fitem = etree.SubElement(diffSameItems, "full_item", {"name" : i})
					etree.SubElement(fitem, "item", {"name" : i})
					

				diffNeg = etree.SubElement(diffRule, "diff_neg")
				diffNegItems = etree.SubElement(diffNeg, "all_items")
				for i in vminus:
					fitem = etree.SubElement(diffNegItems, "full_item", {"name" : i})
					etree.SubElement(fitem, "item", {"name" : i})

				diffPos = etree.SubElement(diffRule, "diff_pos")
				diffPosItems = etree.SubElement(diffPos, "all_items")
				for i in vplus:
					fitem = etree.SubElement(diffPosItems, "full_item", {"name" : i})
					etree.SubElement(fitem, "item", {"name" : i})

	rootDoc = etree.ElementTree(root)
	rootDoc.write(vendor_xml_filename, pretty_print=True, xml_declaration=True, encoding="utf-8")


if __name__ == '__main__':
	if len(sys.argv) != 3:
		print "Usage: %s <BASE_DIRECTORY_OF_XML_FILES_AOSP> <BASE_DIRECTORY_OF_XML_FILES_VENDOR>" % (sys.argv[0],)
		print "E.g., python OutlierDetection.py XSLT/policyminer-results/android-7.1.0_r4/acminer"
		sys.exit(1)

	ROOT_DIR1 = sys.argv[1]
	ROOT_DIR2 = sys.argv[2]

	if not os.path.exists(ROOT_DIR1):
		print "Warning", ROOT_DIR1, "does not exist!"
		sys.exit(1)
	if not os.path.exists(ROOT_DIR2):
		print "Warning", ROOT_DIR2, "does not exist!"
		sys.exit(1)

	# TODO load entire directory into memory, then loop through ROOT_DIR2
	for root, dirs, files in os.walk(ROOT_DIR2):
		foundXml = False
		for f in files:
			full_path = os.path.join(root, f)
			if os.path.isfile(full_path) and f.endswith('.xml'):
				foundXml = True
		if not foundXml:
			continue

#		epoints_vendor = loadAllXmlFiles(root)

		# Parse same directory in ROOT_DIR1
		rel_root = re.sub(ROOT_DIR2, '', root) # Remove initial path from output filename
		if rel_root.startswith('/'): # If starts with '/', change to relative
			rel_root = rel_root[1:]
		full_path2 = os.path.join(ROOT_DIR1, rel_root)
		epoints_aosp = loadAllXmlFiles(full_path2)


		for f in files:
			ediff(os.path.join(root, f), epoints_aosp)
				
