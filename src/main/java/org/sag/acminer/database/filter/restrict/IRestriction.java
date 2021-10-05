package org.sag.acminer.database.filter.restrict;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.matcher.IMatcher;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface.AbstractXStreamSetup;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("IRestriction")
public interface IRestriction {

	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lw, DefUseGraph vt, Set<INode> in, StringBuilder sb, AtomicInteger c, Object...objects);
	public String getName();
	
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
				}
			}

			@Override
			public Set<Class<?>> getAnnotatedClasses() {
				Set<Class<?>> ret = new HashSet<>();
				ret.add(Restrictions.class);
				ret.add(IRestriction.class);
				ret.add(IsDeclaringClassOfMethodRestriction.class);
				ret.add(IsFieldTypeRestriction.class);
				ret.add(IsFieldUsedDirectlyInRestriction.class);
				ret.add(IsInArithmeticChainRestriction.class);
				ret.add(IsInArithmeticOpRestriction.class);
				ret.add(IsMethodReturnTypeRestriction.class);
				ret.add(IsMethodUsedDirectlyRestriction.class);
				ret.add(IsNumberUsedRestriction.class);
				ret.add(IsValueUsedInMethodCallRestriction.class);
				return ret;
			}
			
			@Override
			public void setXStreamOptions(XStream xstream) {
				defaultOptionsNoRef(xstream);
			}
			
		}
		
	}
	
}
