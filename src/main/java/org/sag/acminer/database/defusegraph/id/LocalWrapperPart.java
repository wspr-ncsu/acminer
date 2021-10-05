package org.sag.acminer.database.defusegraph.id;

import java.util.Objects;

import org.sag.acminer.database.defusegraph.LocalWrapper;

import soot.Local;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("LocalWrapperPart")
public class LocalWrapperPart extends ValuePart {
	
	@XStreamAlias("LocalWrapper")
	private final LocalWrapper lw;
	
	public LocalWrapperPart(LocalWrapper lw, String curString) {
		super(lw.toString(), curString);
		this.lw = lw;
	}
	
	private LocalWrapperPart(LocalWrapperPart p) {
		super(p.getOrgString(), p.getCurString());
		this.setIndex(p.getIndex());
		this.lw = p.lw;
	}
	
	public LocalWrapper getLocalWrapper() { 
		return lw; 
	}
	
	public Local getValue() {
		return lw.getLocal();
	}
	
	public int hashCode() {
		return super.hashCode() * 31 + Objects.hashCode(lw);
	}
	
	//hashcode is the same as super already takes care of the value in both equals and hash code
	public boolean equals(Object o) {
		if(super.equals(o))
			return o instanceof LocalWrapperPart && Objects.equals(lw, ((LocalWrapperPart)o).lw);
		return false;
	}
	
	public LocalWrapperPart clone() {
		return new LocalWrapperPart(this);
	}
	
}