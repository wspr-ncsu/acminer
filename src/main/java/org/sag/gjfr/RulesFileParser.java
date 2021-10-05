package org.sag.gjfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class RulesFileParser {
	
	public static final class Remapper {
		private PackageRemapper pkgRemapper;
		public Remapper(PackageRemapper pkgRemapper) {
			this.pkgRemapper = pkgRemapper;
		}
		public String remap(String classPath) {
			return pkgRemapper.mapValue(classPath.replace('.', '/')).replace('/', '.');
		}
		public String remapPackage(String classPath) {
			return pkgRemapper.mapValue(classPath.replace('.', '/') + "/$$").replace("/$$", "").replace('/', '.');
		}
	}

    private RulesFileParser() {}
    
    public static String remap(String classPath, Path file) {
    	return getRemapper(file).remap(classPath);
    }
    
    public static Remapper getRemapper(Path file) {
    	PackageRemapper pkgRemapper = new PackageRemapper();
    	try {
    		parse(pkgRemapper, file);
    	} catch(Throwable t) {
    		throw new RuntimeException("Error: Failed to parse '" + file + "'.",t);
    	}
    	return new Remapper(pkgRemapper);
    }

    private static List<String> split(String line) {
        StringTokenizer tok = new StringTokenizer(line);
        List<String> out = new ArrayList<String>();
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (token.startsWith("#"))
                break;
            out.add(token);
        }
        return out;
    }

    private static void parse(PackageRemapper pkgRemapper, Path p) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(p)) {
            int lineNumber = 1;
            String line;
            while ((line = br.readLine()) != null) {
                List<String> words = split(line);
                if (words.isEmpty())
                    continue;
                if (words.size() < 2)
                    throw error(lineNumber, words, "not enough words on line.");
                String type = words.get(0);
                if (type.equals("rule")) {
                    if (words.size() < 3)
                        throw error(lineNumber, words, "'rule' requires 2 arguments.");
                    pkgRemapper.addRule(new ClassRename(words.get(1), words.get(2)));
                } else if (type.equals("zap")) {
                   //Do Nothing
                } else if (type.equals("keep")) {
                    //Do Nothing
                } else {
                    throw error(lineNumber, words, "Unrecognized keyword " + type);
                }
                lineNumber++;
            }
        }
    }

    private static IllegalArgumentException error(int lineNumber, List<String> words, String reason) {
        throw new IllegalArgumentException("Error on line " + lineNumber + ": " + words + ": " + reason);
    }
}
