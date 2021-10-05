package org.sag.common.graphtools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AlEdge extends AlElement implements Comparable<AlEdge> {

	private AlNode from;
	private AlNode to;
	private String label;
	private Map<Long,String> indexToColor;
	private int weight;
	
	public AlEdge(long id, AlNode from, AlNode to, String label, int weight){
		super(id);
		this.from = from;
		this.to = to;
		this.label = label;
		this.indexToColor = Collections.emptyMap();
		this.weight = weight;
	}
	
	public AlEdge(long id, AlNode from, AlNode to, String label){
		this(id,from,to,label,1);
	}
	
	public AlEdge(long id, AlNode from, AlNode to){
		this(id,from,to,"",1);
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			if(!(o instanceof AlEdge))
				return false;
			AlEdge other = (AlEdge)o;
			return Objects.equals(from, other.from) && Objects.equals(to, other.to) && Objects.equals(label, other.label);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + super.hashCode();
		i = i * 31 + Objects.hashCode(from);
		i = i * 31 + Objects.hashCode(to);
		i = i * 31 + Objects.hashCode(label);
		return i;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Objects.toString(from)).append(" ---- ").append(Objects.toString(label)).append(" ---> ").append(Objects.toString(to));
		return sb.toString();
	}
	
	public void incWeight(){
		if(weight != 7){
			weight++;
		}
	}

	public String getId() {
		return "e" + id;
	}
	
	public AlNode getSource(){
		return from;
	}
	
	public AlNode getTarget(){
		return to;
	}
	
	public String getLabel(){
		return label;
	}
	
	/** Sets the color for a given identifier index. This takes in a single
	 * color and so long as it is not Color.NOCOLOR or Color.BLACK, it assigns
	 * the color to the identifier. If it is the two mentioned, nothing happens
	 * and the color defaults to Color.BLACK in getColor.
	 */
	public void setColor(long index, Color color) {
		Objects.requireNonNull(index);
		Objects.requireNonNull(color);
		if(!color.equals(Color.NOCOLOR) && !color.equals(Color.BLACK)){
			if(indexToColor.isEmpty())
				indexToColor = new HashMap<>();
			indexToColor.put(index, color.toString());
		}
	}
	
	public String getColor(long index){
		String ret = indexToColor.get(index);
		if(ret == null) {
			return Color.BLACK.toString();
		}
		return ret;
	}
	
	public int getWeight(){
		return weight;
	}

	@Override
	public int compareTo(AlEdge e2) {
		int ret = (this.getSource().getLabel() + this.getTarget().getLabel()).compareTo(e2.getSource().getLabel() + e2.getTarget().getLabel());
		if(ret == 0) {
			ret = Long.compare(id, e2.id);
		}
		return ret;
	}
	
}
