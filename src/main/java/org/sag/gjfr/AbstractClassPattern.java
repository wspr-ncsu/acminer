package org.sag.gjfr;

public abstract class AbstractClassPattern extends AbstractPattern {

    private static String check(String patternText) {
        if (patternText.indexOf('/') >= 0)
            throw new IllegalArgumentException("Class patterns cannot contain slashes");
        return patternText.replace('.', '/');
    }

    public AbstractClassPattern(String patternText) {
        super(check(patternText));
    }

}
