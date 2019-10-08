package org.sag.acminer.database.filter.restrict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.acminer.database.defusegraph.DefUseGraph;
import org.sag.acminer.database.defusegraph.INode;
import org.sag.acminer.database.defusegraph.LocalWrapper;
import org.sag.acminer.database.defusegraph.StartNode;
import org.sag.acminer.database.filter.entry.IEntry;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("Restrictions")
public class Restrictions implements IRestriction {
	
	public static final String name = "Restrictions";
	
	@XStreamAlias("UseUnion")
	@XStreamAsAttribute
	private final boolean useUnion;
	
	@XStreamImplicit
	private final List<IRestriction> restrictions;
	
	
	/** Creates a container around a single IRestriction object. The restriction
	 * application policy is always intersection but that does not really matter
	 * since there is only one restriction.
	 */
	public Restrictions(IRestriction restriction) {
		this(Collections.singleton(restriction));
	}
	
	/** Creates a container of IRestriction objects and specifies that the
	 * restrictions should be applied using set intersection during the 
	 * applyRestriction phase.
	 */
	public Restrictions(IRestriction... res) {
		this(res == null ? null : res.length == 1 ? Collections.singleton(res[0]) : Arrays.asList(res));
	}
	
	/** Creates a container of IRestriction objects and specifies that the
	 * restrictions should be applied using set intersection during the 
	 * applyRestriction phase.
	 */
	public Restrictions(Collection<IRestriction> restrictions) {
		this(false, restrictions);
	}
	
	/** Creates a container of IRestriction objects and specifies how the restrictions
	 * should be applied during the applyRestriction function call. The two options are
	 * set intersection (false) and union (true).
	 */
	public Restrictions(boolean useUnion, IRestriction... res) {
		this(useUnion, res == null ? null : res.length == 1 ? Collections.singleton(res[0]) : Arrays.asList(res));
	}
	
	/** Creates a container of IRestriction objects and specifies how the restrictions
	 * should be applied during the applyRestriction function call. The two options are
	 * set intersection (false) and union (true).
	 */
	public Restrictions(boolean useUnion, Collection<IRestriction> restrictions) {
		Objects.requireNonNull(restrictions);
		for(IRestriction r : restrictions) {
			Objects.requireNonNull(r);
		}
		if(restrictions.isEmpty()) throw new IllegalArgumentException();
		this.useUnion = useUnion;
		this.restrictions = new ArrayList<>(restrictions);
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<INode> applyRestriction(StartNode sn, LocalWrapper lw, DefUseGraph vt, Set<INode> in, StringBuilder sb, 
			AtomicInteger c, Object... objects) {
		int curC = 0;
		if(sb != null) {
			curC = c.get();
			sb.append("Start Restriction ").append(name).append(" ").append(curC).append(" UseUnion=").append(useUnion).append("\n");
			sb.append("  Incomming Nodes:\n");
			for(INode n : in) {
				sb.append("    ").append(n).append("\n");
			}
		}
		
		Set<INode> ret;
		if(useUnion) {
			ret = new HashSet<>();
			for(IRestriction r : restrictions) {
				if(c != null) c.incrementAndGet();
				ret.addAll(r.applyRestriction(sn, lw, vt, in, sb, c, objects));
			}
			if(ret.isEmpty())
				ret = Collections.emptySet();
		} else {
			/* Set Intersection - Only apply the next restriction to the ones returned by the previous restriction
			 * since the intersecting sets will not contain those in the next restriction if they are not in the 
			 * previous.
			 */
			for(IRestriction r : restrictions) {
				if(c != null) c.incrementAndGet();
				in = r.applyRestriction(sn, lw, vt, in, sb, c, objects);
				if(in.isEmpty())
					break;
			}
			if(in.isEmpty())
				ret = Collections.emptySet();
			ret = in;
		}
		
		if(sb != null) {
			sb.append("End Restriction ").append(name).append(" ").append(curC).append(" UseUnion=").append(useUnion).append("\n");
			sb.append("  Outgoing Nodes:\n");
			for(INode n : ret) {
				sb.append("    ").append(n).append("\n");
			}
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || !(o instanceof Restrictions))
			return false;
		Restrictions other = (Restrictions)o;
		return other.useUnion == useUnion && Objects.equals(other.restrictions, restrictions);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + (useUnion ? 1 : 0);
		i = i * 31 + Objects.hashCode(restrictions);
		return i;
	}
	
	@Override
	public String toString() {
		List<String> r = new ArrayList<>();
		r.add("UseUnion="+useUnion);
		if(restrictions != null) {
			for(IRestriction res : restrictions) {
				r.add(res.toString());
			}
		}
		return IEntry.Factory.genSig(name, r);
	}

}
