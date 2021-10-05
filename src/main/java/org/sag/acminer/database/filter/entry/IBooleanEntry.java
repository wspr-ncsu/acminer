package org.sag.acminer.database.filter.entry;

import java.util.Collection;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IBooleanEntry")
public interface IBooleanEntry extends IEntry {

	public void addEntry(IEntry entry);
	public void addAllEntries(Collection<IEntry> entries);
	public List<IEntry> getEntries();
	
}
