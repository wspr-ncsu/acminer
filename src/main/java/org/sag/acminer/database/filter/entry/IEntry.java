package org.sag.acminer.database.filter.entry;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.filter.IData;
import org.sag.acminer.database.filter.matcher.IMatcher;
import org.sag.acminer.database.filter.restrict.IRestriction;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface.AbstractXStreamSetup;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IEntry")
public interface IEntry {
	
	public String getName();
	public boolean eval(IData data);
	public boolean evalDebug(IData data, StringBuilder sb, AtomicInteger c);
	
	public static final class Factory {
		
		private static AbstractXStreamSetup xstreamSetup = null;
		
		public static AbstractXStreamSetup getXStreamSetupStatic(){
			if(xstreamSetup == null)
				xstreamSetup = new XStreamSetup();
			return xstreamSetup;
		}
		
		private static class XStreamSetup extends AbstractXStreamSetup {
			
			@Override
			public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
				if(!in.contains(this)) {
					in.add(this);
					IMatcher.Factory.getXStreamSetupStatic().getOutputGraph(in);
					IRestriction.Factory.getXStreamSetupStatic().getOutputGraph(in);
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(OrEntry.class);
				ret.add(AndEntry.class);
				ret.add(IBooleanEntry.class);
				ret.add(IEntry.class);
				ret.add(KeepFieldValueUseEntry.class);
				ret.add(KeepLoopHeaderEntry.class);
				ret.add(KeepMethodReturnValueUseEntry.class);
				ret.add(KeepNumberConstantUseEntry.class);
				ret.add(KeepSourceMethodIsEntry.class);
				ret.add(KeepSourceMethodIsInContextQuerySubGraphEntry.class);
				ret.add(KeepContextQueryMethodReturnValueUseEntry.class);
				ret.add(NotEntry.class);
				ret.add(KeepMethodIsEntry.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsNoRef(xstream);
			}
			
		}

		public static final String genSig(String name, List<String> values) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(values);
			if(values.isEmpty()) {
				return "<" + name + "/>";
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("<").append(name).append(">\n");
				for(String s : values) {
					sb.append("\t").append(s).append("\n");
				}
				sb.append("</").append(name).append(">");
				return sb.toString();
			}
		}
		
	}
	
}
