package org.sag.gjfr;

import java.util.List;

public class ClassRename extends AbstractClassPattern {

    // private final String replaceText;
    private final List<Object> replace;

    public ClassRename(String patternText, String replaceText) {
        super(patternText);
        if (replaceText == null)
            throw new IllegalArgumentException("Result may not be null.");
        // this.replaceText = replaceText;
        this.replace = PatternUtils.newReplace(getPattern(), replaceText);
    }

    public String replace(String value) {
        return PatternUtils.replace(this, replace, value);
    }
}
