package com.benandow.policyminer.controlpredicatefilter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.benandow.policyminer.controlpredicatefilter.utils.Field;
import com.benandow.policyminer.controlpredicatefilter.utils.Method;
import com.benandow.policyminer.controlpredicatefilter.utils.TestFileParser;

public class Main {
		
	private static final String PATH = "/Users/benandow/Desktop/PolicyMiner/policyminer/cpredfilter/test_files";
	private static final Path CQ_FILENAME = Paths.get(PATH, "context_queries_db.txt");
	private static final Path FI_FILENAME = Paths.get(PATH, "filter_info.txt");
	private static final Path VT_M_FILENAME = Paths.get(PATH, "vt_methods.txt");
	private static final Path VT_F_FILENAME = Paths.get(PATH, "vt_fields.txt");
	private static final Path VT_STRCONST_FILENAME = Paths.get(PATH, "vt_string_const.txt");
	private static final Path VT_USES_FILENAME = Paths.get(PATH, "vt_uses.txt");
	private static final Path NEW_CQ_FILENAME = Paths.get(PATH, "context_queries_dump.txt");

	
	public static void main(String[] args) throws IOException {
		
		ContextQueryFilter.printFilter();
		
		List<Method> contextQueries = new ArrayList<Method>();
		TestFileParser.readContextQueries(CQ_FILENAME.toFile(), contextQueries);
		
		List<Method> filteredCQs = ContextQueryFilter.filterMethods(contextQueries);
		System.out.printf("Identified %d/%d context queries FROM %s (%d diff)\n"
				+ "---------------------------------------------------------------\n",
				filteredCQs.size(), contextQueries.size(), CQ_FILENAME, Method.dumpMethodDiffs(contextQueries, filteredCQs, true));
		
		
//		List<Method> fiMethods = new ArrayList<Method>();
//		List<Field> fiFields = new ArrayList<Field>();
//		TestFileParser.readFilterInfo(FI_FILENAME.toFile(), fiMethods, fiFields);
//
//		List<Method> filteredMethods = Filter.filterMethods(fiMethods);
//		System.out.printf("Identified %d/%d methods FROM %s\n"
//				+ "---------------------------------------------------------------\n",
//				filteredMethods.size(), fiMethods.size(), FI_FILENAME);
//
//		List<Field> filteredFields = Filter.filterFields(fiFields);
//		System.out.printf("Identified %d/%d fields FROM %s\n"
//				+ "---------------------------------------------------------------\n",
//				filteredFields.size(), fiFields.size(), FI_FILENAME);
//		
//		// Differences between context query methods
//		List<Method> tmethods = new ArrayList<Method>();
//		TestFileParser.readVtMethods(VT_M_FILENAME.toFile(), tmethods);
//		List<Method> vtFilteredMethods = ContextQueryFilter.filterMethods(tmethods);
//		System.out.printf("Identified %d/%d fields FROM %s (%d diff)\n"
//				+ "---------------------------------------------------------------\n",
//				vtFilteredMethods.size(), tmethods.size(), VT_M_FILENAME, Method.dumpMethodDiffs(vtFilteredMethods, filteredCQs, false));
//
//	
//		// Differences between filter methods
//		List<Method> vtFilteredMethods2 = Filter.filterMethods(tmethods);
//		System.out.printf("Identified %d/%d fields FROM %s (%d diff)\n"
//				+ "---------------------------------------------------------------\n",
//				vtFilteredMethods2.size(), tmethods.size(), VT_M_FILENAME, Method.dumpMethodDiffs(vtFilteredMethods2, filteredMethods, false));
//
//		List<Method> vtFilteredMethods3 = new ArrayList<Method>();
//		for (Method m : vtFilteredMethods2) {
//			if (vtFilteredMethods.contains(m)) {
//				continue;
//			}
//			vtFilteredMethods3.add(m);
//		}
//		System.out.printf("Identified %d/%d fields FROM %s (%d diff not flagged by context query filters)\n"
//				+ "---------------------------------------------------------------\n",
//				vtFilteredMethods3.size(), tmethods.size(), VT_M_FILENAME, Method.dumpMethodDiffs(vtFilteredMethods3, filteredMethods, false));
//
//		
//		// Differences between filter fields
//		List<Field> tfields = new ArrayList<Field>();
//		TestFileParser.readVtFields(VT_F_FILENAME.toFile(), tfields);
//		List<Field> vtFilteredFields = Filter.filterFields(tfields);
//		System.out.printf("Identified %d/%d fields FROM %s (%d diff)\n"
//				+ "---------------------------------------------------------------\n",
//				vtFilteredFields.size(), tfields.size(), VT_F_FILENAME, Field.dumpFieldDiffs(vtFilteredFields, filteredFields, false));

		
		List<Field> tstrings = new ArrayList<Field>();
		TestFileParser.readVtFields(VT_STRCONST_FILENAME.toFile(), tstrings);
				
		// Reading new file
		List<Method> methods = new ArrayList<Method>();
		TestFileParser.readMethods(NEW_CQ_FILENAME.toFile(), methods);
		List<Method> filteredCQ2s = ContextQueryFilter.filterMethods(methods);
		System.out.printf("Identified %d/%d fields FROM %s (%d diff)\n"
				+ "---------------------------------------------------------------\n",
				filteredCQ2s.size(), methods.size(), NEW_CQ_FILENAME, Method.dumpMethodDiffs(filteredCQ2s, contextQueries, true));		

		
	}
}
