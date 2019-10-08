package org.sag.common.graphtools;

public abstract class AlElement {
	
	protected long id;
	
	public AlElement(long id){
		this.id = id;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o) 
			return true;
		if(o == null || !(o instanceof AlElement))
			return false;
		return id == ((AlElement)o).id;
	}
	
	@Override
	public int hashCode() {
		return (int)id;
	}
	
	@Override public abstract String toString();
	public abstract String getId();
	
	public enum Color{
		WHITE("#FFFFFF"),
		RED("#FF6666"),
		BLUE("#33CCFF"),
		GREEN("#00CC66"),
		YELLOW("#FFFF66"),
		ORANGE("#FF9900"),
		PINK("#FF99FF"),
		PURPLE("#CC99FF"),
		GRAY("#CCCCCC"),
		BLACK("#000000"),
		NOCOLOR("");		
		
		private final String color;
		
		private Color(String color){
			this.color = color;
		}
		
		public String toString(){
			return color;
		}
	}
	
}
