package org.sag.main.phase;

import java.nio.file.Path;
import java.util.Objects;

import org.sag.common.io.FileHelpers;
import org.sag.main.config.Config;

public interface IPhaseOption<T> {
	
	int hashCode();
	boolean equals(Object o);
	String toString();
	String getName();
	T getValue();
	String getDescription();
	boolean isEnabled();
	void toggleIsEnabled();
	boolean setValueFromInput(String value);
	void setConfig(Config config);
	
	public static class BaseOption<T> implements IPhaseOption<T> {
		
		protected final String name;
		protected T value;
		protected final String description;
		protected boolean isEnabled;
		protected Config config;
		
		public BaseOption(String name, T value, String description, boolean isEnabled) {
			this.name = name;
			this.value = value;
			this.description = description;
			this.isEnabled = isEnabled;
		}
		
		@Override
		public int hashCode() {
			int i = 17;
			i = i * 31 + Objects.hashCode(getName());
			i = i * 31 + Objects.hashCode(getValue());
			i = i * 31 + Objects.hashCode(getDescription());
			i = i * 31 + (isEnabled() ? 1 : 0);
			return i;
		}
		
		@Override
		public boolean equals(Object o) {
			if(this == o)
				return true;
			if(o == null || !(o instanceof BaseOption))
				return false;
			BaseOption<?> other = (BaseOption<?>)o;
			return Objects.equals(getName(), other.getName()) && Objects.equals(getValue(), other.getValue()) 
					&& Objects.equals(getDescription(), other.getDescription()) && isEnabled() == other.isEnabled();
		}
		
		@Override
		public String toString() {
			return getName() + " : " + (isEnabled() ? (getValue() == null ? "true" : getValue()) : "false");
		}
		
		public String getName() {
			return name;
		}
		
		public T getValue() {
			return value;
		}
		
		public String getDescription() {
			return description;
		}
		
		public boolean isEnabled() {
			return isEnabled;
		}
		
		public void toggleIsEnabled() {
			isEnabled = isEnabled ? false : true;
		}
		
		public final boolean setValueFromInput(String value) {
			if(value.equals("false"))
				isEnabled = false;
			else if(value.equals("true"))
				isEnabled = true;
			else
				return setValueFromInputInner(value);
			return true;
		}
		
		protected boolean setValueFromInputInner(String value) {
			return false;
		}
		
		public void setConfig(Config config) {
			this.config = config;
		}
		
	}
	
	public static class BooleanOption extends BaseOption<Boolean> {
		
		public BooleanOption(String name, String description) {
			this(name, description, false);
		}
		
		public BooleanOption(String name, String description, boolean isEnabled) {
			super(name, null, description, isEnabled);
		}
		
		@Override
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof BooleanOption;
		}
		
		@Override
		public BooleanOption clone() {
			return new BooleanOption(this.name,this.description,this.isEnabled);
		}
	}
	
	public static class IntOption extends BaseOption<Integer> {
		
		public IntOption(String name, String description) {
			this(name, null, description, false);
		}
		
		public IntOption(String name, Integer value, String description) {
			this(name, value, description, false);
		}
		
		public IntOption(String name, Integer value, String description, boolean isEnabled) {
			super(name, value, description, isEnabled);
		}
		
		@Override
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof IntOption;
		}
		
		@Override
		public IntOption clone() {
			return new IntOption(this.name,this.value,this.description,this.isEnabled);
		}
		
		protected boolean setValueFromInputInner(String value) {
			if(value == null || value.isEmpty())
				return false;
			int ret = Integer.parseInt(value);
			if(ret < 0)
				return false;
			this.value = ret;
			isEnabled = true;
			return true;
		}
		
	}
	
	public static class PathOption extends BaseOption<Path> {
		
		private String pathKey;
		
		public PathOption(String name, String description) {
			this(name, null, description, false);
		}
		
		public PathOption(String name, String pathKey, String description) {
			this(name, pathKey, description, false);
		}
		
		public PathOption(String name, String pathKey, String description, boolean isEnabled) {
			super(name, null, description, isEnabled);
			this.pathKey = pathKey;
		}
		
		@Override
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof PathOption;
		}
		
		@Override
		public Path getValue() {
			if(value == null && pathKey == null) {
				return null;
			} else if(value == null) {
				return config.getFilePath(pathKey);
			} else {
				return value;
			}
		}
		
		protected boolean setValueFromInputInner(String value) {
			if(value == null || value.isEmpty())
				return false;
			this.value = FileHelpers.getPath(value);
			isEnabled = true;
			return true;
		}
		
	}
	
	public static class ListOption extends BaseOption<String[]> {
		
		public ListOption(String name, String description) {
			this(name, null, description, false);
		}
		
		public ListOption(String name, String[] value, String description) {
			this(name, value, description, false);
		}
		
		public ListOption(String name, String[] value, String description, boolean isEnabled) {
			super(name, value, description, isEnabled);
		}
		
		@Override
		public boolean equals(Object o) {
			return super.equals(o) && o instanceof ListOption;
		}
		
		@Override
		public ListOption clone() {
			return new ListOption(this.name,this.value.clone(),this.description,this.isEnabled);
		}
		
		protected boolean setValueFromInputInner(String value) {
			if(value == null || value.isEmpty())
				return false;
			this.value = value.split(";");
			isEnabled = true;
			return true;
		}
		
	}
	
}