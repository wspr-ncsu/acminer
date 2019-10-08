package org.sag.common.graphtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.google.common.collect.Lists;

public class AlNode extends AlElement implements Comparable<AlNode> {

	private String label;
	private Map<Long,List<String>> indexToColors;
	private Map<Long,String> indexToShape;
	private Map<Long,String> indexToExtraData;
	
	//only supports two colors so a set size larger than 2 will get random results
	public AlNode(long id, String label){
		super(id);
		this.label = label;
		this.indexToColors = Collections.emptyMap();
		this.indexToShape = Collections.emptyMap();
		this.indexToExtraData = Collections.emptyMap();
	}
	
	public AlNode(long id){
		this(id,"");
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			if(!(o instanceof AlNode))
				return false;
			return Objects.equals(label, ((AlNode)o).label);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + super.hashCode();
		i = i * 31 + Objects.hashCode(label);
		return i;
	}
	
	public String toString() {
		return Objects.toString(label);
	}
	
	public String getId(){
		return "n" + id;
	}
	
	public String getLabel(){
		return label;
	}
	
	/** Sets the colors for a given identifier index. Since a node in graphml
	 * can have two colors this accepts a list of colors to be set. If the
	 * list is empty or contains a Color.NOCOLOR entry then nothing is modified
	 * and both colors will default to COLOR.NOCOLOR when getColor or getColor2
	 * is called. Otherwise, this method lets the first color it encounters be
	 * color1 and the second color not equal to color1 be color2. Note any null
	 * entries in the list are ignored.
	 */
	public void setColors(long index, List<Color> colors) {
		Objects.requireNonNull(colors);
		colors = new ArrayList<>(colors);//make sure this is mutable
		colors.removeAll(Collections.singleton(null));//remove all null entries from colors
		if(!colors.isEmpty()) {
			Color color1 = colors.get(0);
			Color color2 = null;
			//Set color2 to be the first color not equal to color1
			for(int i = 1; i < colors.size(); i++) {
				Color temp = colors.get(i);
				if(!color1.equals(temp)) {
					color2 = temp;
					break;
				}
			}
			if(indexToColors.isEmpty())
				indexToColors = new HashMap<>();
			if(color2 == null) {
				indexToColors.put(index, Collections.singletonList(color1.toString()));
			} else if(color1.equals(Color.NOCOLOR) || color2.equals(Color.NOCOLOR)) {
				indexToColors.put(index, Collections.singletonList(Color.NOCOLOR.toString()));
			} else {
				indexToColors.put(index, Lists.newArrayList(color1.toString(), color2.toString()));
			}
		}
	}
	
	public String getColor(long index){
		List<String> ret = indexToColors.get(index);
		if(ret == null || ret.isEmpty())
			return Color.NOCOLOR.toString();
		return ret.get(0);
	}
	
	public String getColor2(long index){
		List<String> ret = indexToColors.get(index);
		if(ret == null || ret.size() < 2)
			return Color.NOCOLOR.toString();
		return ret.get(1);
	}
	
	public void setShape(long index, Shape shape) {
		Objects.requireNonNull(shape);
		if(indexToShape.isEmpty())
			indexToShape = new HashMap<>();
		indexToShape.put(index, shape.toString());
	}
	
	public String getShape(long index){
		String ret = indexToShape.get(index);
		if(ret == null) {
			return Shape.RECTANGLE.toString();
		}
		return ret;
	}
	
	public boolean hasColor(long index){
		return indexToColors.containsKey(index);
	}
	
	public boolean hasColor2(long index){
		List<String> r = indexToColors.get(index);
		return r != null && r.size() > 1;
	}
	
	public enum Shape{
		ELLIPSE("ellipse"),
		RECTANGLE("rectangle"),
		ROUNDRECTANGLE("roundrectangle"),
		TRIANGLE("triangle"),
		DIAMOND("diamond"),
		OCTAGON("octagon");	
		
		private final String shape;
		
		private Shape(String shape){
			this.shape = shape;
		}
		
		public String toString(){
			return shape;
		}
	}

	@Override
	public int compareTo(AlNode s2) {
		int ret = this.getLabel().compareTo(s2.getLabel());
		if(ret == 0)
			ret = Long.compare(this.id, s2.id);
		return ret;
	}
	
	public void setExtraData(long index, String extraData) {
		Objects.requireNonNull(extraData);
		if(indexToExtraData.isEmpty())
			indexToExtraData = new HashMap<>();
		indexToExtraData.put(index, extraData);
	}
	
	public boolean hasExtraData(long index) {
		return indexToExtraData.containsKey(index);
	}
	
	public String getExtraData(long index) {
		String extraData = indexToExtraData.get(index);
		if(extraData == null)
			return "";
		return extraData;
	}
	
}
