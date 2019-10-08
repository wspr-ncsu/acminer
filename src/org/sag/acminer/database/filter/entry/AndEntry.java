package org.sag.acminer.database.filter.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.filter.IData;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("And")
public class AndEntry implements IBooleanEntry {
	
	public static final String name = "And";
	
	@XStreamImplicit
	private final List<IEntry> entries;

	public AndEntry() {
		this.entries = new ArrayList<>();
	}
	
	public AndEntry(Collection<IEntry> entries) {
		this();
		addAllEntries(entries);
	}
	
	public void addEntry(IEntry entry) {
		if(entry != null)
			this.entries.add(entry);
	}
	
	public void addAllEntries(Collection<IEntry> entries) {
		if(entries != null) {
			for(IEntry e : entries) {
				addEntry(e);
			}
		}
	}
	
	public List<IEntry> getEntries() {
		return new ArrayList<>(entries);
	}
	
	public String getName() {
		return name;
	}
	
	/** Evaluates like a normal java and statement. Left to right, if a false is encountered
	 * then false is returned immediately without evaluation the rest of the entries. The only
	 * way to get a true is by evaluation all the entries and having them return true.
	 */
	@Override
	public boolean eval(IData data) {
		return evalInner(data, null, null);
	}
	
	@Override
	public boolean evalDebug(IData data, StringBuilder sb, AtomicInteger c) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Eval ").append(name).append(" ").append(curC).append("\n");
		}
		boolean ret = evalInner(data, sb, c);
		if(sb != null)
			sb.append("End Eval ").append(name).append(" ").append(curC).append("\nResult: ").append(ret).append("\n");
		return ret;
	}
	
	private boolean evalInner(IData data, StringBuilder sb, AtomicInteger c) {
		for(IEntry entry : entries) {
			if(c != null) c.incrementAndGet();
			if(!entry.evalDebug(data, sb, c))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof AndEntry))
			return false;
		AndEntry other = (AndEntry)o;
		return Objects.equals(other.entries, entries);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(entries);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		if(entries != null) {
			for(IEntry res : entries) {
				r.add(res.toString());
			}
		}
		return Factory.genSig(name, r);
	}
	
}
