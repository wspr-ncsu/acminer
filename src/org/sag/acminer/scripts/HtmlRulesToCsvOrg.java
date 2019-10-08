package org.sag.acminer.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sag.common.io.FileHelpers;
import org.sag.common.tools.SortingMethods;
import org.sag.soot.SootSort;

import com.google.common.collect.ImmutableList;

public class HtmlRulesToCsvOrg {
	
	private static final Pattern pat = Pattern.compile("^<([^\\s:]+):\\s+([^\\s]+)\\s+([^\\s\\(]+)\\(([^\\)]*)\\)>$");
	
	public static void main(String[] args) {
		Map<String,List<List<String>>> stubsToEps = new HashMap<>();
		try(BufferedReader br = Files.newBufferedReader(FileHelpers.getPath(args[0]))) {
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				String[] spl = line.split(";");
				if(spl.length == 2) {
					String filePath = spl[0];
					String key = Paths.get(filePath).getParent().toString().replace(java.io.File.separatorChar, '.');
					List<List<String>> list = stubsToEps.get(key);
					if(list == null) {
						list = new ArrayList<>();
						stubsToEps.put(key, list);
					}
					Matcher m = pat.matcher(spl[1]);
					String service;
					String ret;
					String name;
					String parms;
					if(m.matches()) {
						service = m.group(1);
						ret = m.group(2);
						name = m.group(3);
						parms = m.group(4);
					} else {
						throw new RuntimeException("Error: Failed to match '" + spl[1] + "' to the pattern.");
					}
					list.add(ImmutableList.<String>of(filePath,spl[1],service,ret,name,parms));
				} else {
					throw new RuntimeException("Error: Something is wrong with line '" + line + "'");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<List<String>> singles = new ArrayList<>();
		for(Iterator<String> it = stubsToEps.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			List<List<String>> vals = stubsToEps.get(key);
			if(vals.size() == 1) {
				it.remove();
				List<String> temp = new ArrayList<>(vals.get(0));
				temp.add(key);
				singles.add(temp);
			} else if(vals.size() == 0) {
				throw new RuntimeException("Error: How did we get here");
			} else {
				Collections.sort(vals,new Comparator<List<String>>() {
					@Override
					public int compare(List<String> o1, List<String> o2) {
						int ret = SortingMethods.sComp.compare(o1.get(2),o2.get(2));
						if(ret == 0) {
							ret = SortingMethods.sComp.compare(o1.get(4), o2.get(4));
							if(ret == 0) {
								ret = SootSort.smStringComp.compare(o1.get(1), o2.get(1));
								if(ret == 0)
									ret = SortingMethods.sComp.compare(o1.get(0), o2.get(0));
							}
						}
						return ret;
					}
				});
			}
		}
		Collections.sort(singles, new Comparator<List<String>>() {
			@Override
			public int compare(List<String> o1, List<String> o2) {
				return SortingMethods.sComp.compare(o1.get(6), o2.get(6));
			}
		});
		
		Path dir = FileHelpers.getPath(args[0]).getParent();
		for(String key : stubsToEps.keySet()) {
			List<List<String>> pairs = stubsToEps.get(key);
			Path out = FileHelpers.getPath(dir, key + ".txt");
			try(BufferedWriter bw = Files.newBufferedWriter(out)) {
				bw.write("Name;Class;Args;Signature;File\n");
				for(List<String> pair : pairs) {
					String parms = pair.get(5);
					int pc = 0;
					if(parms != null && !parms.isEmpty()) {
						pc = parms.split(",").length;
					}
					bw.write(pair.get(4) + ";" + pair.get(2) + ";" + pc + ";" + pair.get(1) + ";" + pair.get(0) + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Path out = FileHelpers.getPath(dir, "Singles.txt");
		try(BufferedWriter bw = Files.newBufferedWriter(out)) {
			bw.write("Stub;Name;Class;Args;Signature;File\n");
			for(List<String> pair : singles) {
				String parms = pair.get(5);
				int pc = 0;
				if(parms != null && !parms.isEmpty()) {
					pc = parms.split(",").length;
				}
				bw.write(pair.get(6) + ";" + pair.get(4) + ";" + pair.get(2) + ";" + pc + ";" + pair.get(1) + ";" + pair.get(0) + "\n");
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}
