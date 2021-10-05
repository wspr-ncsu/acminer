package org.sag.acminer.database.filter.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.filter.IData;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("Not")
public class NotEntry implements IBooleanEntry {
	
	public static final String name = "Not";

	//Hack to not have this field show up only the data within
	@XStreamImplicit
	private final List<IEntry> entry;
	
	public NotEntry() {
		entry = new ArrayList<>(1);
		entry.add(null);
	}
	
	public NotEntry(IEntry entry) {
		this();
		addEntry(entry);
	}
	
	public void addEntry(IEntry entry) {
		if(entry != null)
			this.entry.set(0, entry);
	}
	
	public void addAllEntries(Collection<IEntry> entries) {
		if(entries != null && !entries.isEmpty()) {
			IEntry e = entries.iterator().next();
			addEntry(e);
		}
	}
	
	public List<IEntry> getEntries() {
		return Collections.singletonList(entry.get(0));
	}
	
	public String getName() {
		return name;
	}
	
	/** Evaluates like a normal java not statement. Returns the opposite of whatever
	 * the internal entry returns.
	 */
	@Override
	public boolean eval(IData data) {
		return evalDebug(data, null, null);
	}
	
	@Override
	public boolean evalDebug(IData data, StringBuilder sb, AtomicInteger c) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Eval ").append(name).append(" ").append(curC).append("\n");
		}
		if(c != null) c.incrementAndGet();
		boolean ret = !entry.get(0).evalDebug(data, sb, c);
		if(sb != null)
			sb.append("End Eval ").append(name).append(" ").append(curC).append("\nResult: ").append(ret).append("\n");
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof NotEntry))
			return false;
		NotEntry other = (NotEntry)o;
		return Objects.equals(other.entry, entry);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(entry);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		if(entry != null) {
			for(IEntry res : entry) {
				r.add(res.toString());
			}
		}
		return Factory.genSig(name, r);
	}
	
}
