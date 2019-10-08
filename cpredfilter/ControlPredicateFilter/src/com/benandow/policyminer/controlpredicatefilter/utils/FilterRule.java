package com.benandow.policyminer.controlpredicatefilter.utils;

import java.util.ArrayList;
import java.util.List;

public class FilterRule {

	public static final String REGEX_RULE = "regex-name-words";
	public static final String START_PKG_RULE = "starts-with-package";
	public static final String CONTAIN_PKG_RULE = "contain-package";
	public static final String EQUAL_PKG_RULE = "equal-package";

	public static final String CONTAIN_CLASS_RULE = "contain-class";
	public static final String EQUAL_CLASS_RULE = "equal-class";
	public static final String SIG_RULE = "signature";

	public static final String OR = "or";
	public static final String AND = "and";
	public static final String NOT = "not";

	
	public String ruleType;
	public String rule;
	
	public String relation;
	public List<FilterRule> children;
	
	//Type 1
	public FilterRule(String ruleType, String rule) {
		this.ruleType = ruleType;
		this.rule = rule;
	}
	
	//Type 2
	public FilterRule(String relation) {
		this.relation = relation;
		this.children = new ArrayList<FilterRule>();
	}
	
	public void addChild(FilterRule r) {
		this.children.add(r);
	}
	
	public static FilterRule genNot(FilterRule r) {
		FilterRule rnot = new FilterRule(FilterRule.NOT);
		rnot.addChild(r);
		return rnot;
	}
	
	public static FilterRule genOR(FilterRule... rules) {
		FilterRule ror = new FilterRule(FilterRule.OR);
		for (FilterRule r : rules) {
			ror.addChild(r);
		}
		return ror;
	}
	
	public static FilterRule genAND(FilterRule... rules) {
		FilterRule rand = new FilterRule(FilterRule.AND);
		for (FilterRule r : rules) {
			rand.addChild(r);
		}
		return rand;
	}
	
}