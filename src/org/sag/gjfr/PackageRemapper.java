package org.sag.gjfr;

import java.util.Arrays;
import java.util.List;

public class PackageRemapper {

    private static final String RESOURCE_SUFFIX = "RESOURCE";

    private final List<ClassRename> patterns;

    public PackageRemapper(Iterable<? extends ClassRename> patterns) {
        this.patterns = PatternUtils.toList(patterns);
    }

    public PackageRemapper(ClassRename... patterns) {
        this(Arrays.asList(patterns));
    }

    public void addRule(ClassRename pattern) {
        this.patterns.add(pattern);
    }

    public String map(String key) {
        String s = replaceHelper(key);
        if (key.equals(s))
            s = null;
        return s;
    }

    public String mapPath(String path) {
    	String s = path;
        int slash = s.lastIndexOf('/');
        String end;
        if (slash < 0) {
            end = s;
            s = RESOURCE_SUFFIX;
        } else {
            end = s.substring(slash + 1);
            s = s.substring(0, slash + 1) + RESOURCE_SUFFIX;
        }
        boolean absolute = s.startsWith("/");
        if (absolute)
            s = s.substring(1);

        s = replaceHelper(s);

        if (absolute)
            s = "/" + s;
        if (s.indexOf(RESOURCE_SUFFIX) < 0)
            return path;
        s = s.substring(0, s.length() - RESOURCE_SUFFIX.length()) + end;
        return s;
    }

    public String mapValue(String value) {
        String s = mapPath(value);
        if (s.equals(value)) {
            boolean hasDot = s.indexOf('.') >= 0;
            boolean hasSlash = s.indexOf('/') >= 0;
            if (!hasDot || !hasSlash) {
                if (hasDot) {
                    s = replaceHelper(s.replace('.', '/')).replace('/', '.');
                } else {
                    s = replaceHelper(s);
                }
            }
        }
        return s;
    }

    private String replaceHelper(String value) {
        for (ClassRename pattern : patterns) {
            String result = pattern.replace(value);
            if (result != null)
                return result;
        }
        return value;
    }
}
