package org.sag.gjfr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPattern {

    private final String patternText;
    private final Pattern pattern;

    public AbstractPattern(String patternText) {
        if (patternText == null)
            throw new IllegalArgumentException("Pattern text may not be null.");
        this.patternText = patternText;
        this.pattern = PatternUtils.newPattern(patternText);
    }

    public String getPatternText() {
        return patternText;
    }

    public Pattern getPattern() {
        return pattern;
    }

    protected Matcher getMatcher(String value) {
        if (!PatternUtils.isPossibleQualifiedName(value, "/"))
            return null;
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches())
            return matcher;
        return null;
    }

    public boolean matches(String value) {
        return getMatcher(value) != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + pattern + ")";
    }
}
