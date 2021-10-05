package org.sag.acminer.database.defusegraph.id;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("Part")
public interface Part {
	public abstract String toString();
	public abstract boolean equals(Object o);
	public abstract int hashCode();
	public abstract Part clone();
	public abstract String getCurString();
	public abstract String getOrgString();
	public abstract void setCurString(String curString);
}