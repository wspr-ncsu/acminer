import sys
import os

def parseControlPredicates(filename):
	if not os.path.isfile(filename):
		print "Error: File (%s) does not exist!" % (filename,)
		sys.exit(1)

	out = {}
	for line in open(filename, 'rb'):
		line = line.strip()
		if len(line) <= 0:
			continue

		inSymbol = False
		firstSymbol = True
		isEntryPoint = True	

		sb = ""
		sb2 = ""
		entryPoint = None

		for i,c in enumerate(line):
			if c == "`":
				inSymbol = not inSymbol
				if i == len(line) -1:
					if entryPoint is not None:
						out[entryPoint].append(sb)
					else:
						out[sb] = []
			elif not inSymbol and (c == ";" or i == len(line) - 1):
				if isEntryPoint:
					isEntryPoint = False
					entryPoint = sb
					if entryPoint not in out:
						out[entryPoint] = []
					else:
						print "KeyError", sb
				elif len(sb2) == 0:
					out[entryPoint].append(sb)
				else:
					tup = tuple(sorted([sb, sb2]))
					out[entryPoint].append(tup)					
					sb2 = ""
				sb = ""
			elif not inSymbol and c == ',':
				firstSymbol = False
			elif not inSymbol and c =='{':
				firstSymbol = True
			elif not inSymbol and c =='}':
				firstSymbol = True
			elif inSymbol and firstSymbol:
				sb += c
			elif inSymbol and not firstSymbol:
				sb2 += c
	return out
