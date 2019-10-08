package com.benandow.policyminer.controlpredicatefilter.utils;

public abstract class IFieldOrMethod {

	private String mPackage, mType, mName;
	
	public String getmPackage() {
		return mPackage;
	}

	public void setPackage(String mPackage) {
		this.mPackage = mPackage;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		this.mType = type;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mName == null) ? 0 : mName.hashCode());
		result = prime * result + ((mPackage == null) ? 0 : mPackage.hashCode());
		result = prime * result + ((mType == null) ? 0 : mType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IFieldOrMethod other = (IFieldOrMethod) obj;
		if (mName == null) {
			if (other.mName != null)
				return false;
		} else if (!mName.equals(other.mName))
			return false;
		if (mPackage == null) {
			if (other.mPackage != null)
				return false;
		} else if (!mPackage.equals(other.mPackage))
			return false;
		if (mType == null) {
			if (other.mType != null)
				return false;
		} else if (!mType.equals(other.mType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IFieldOrMethod [mPackage=" + mPackage + ", mType=" + mType + ", mName=" + mName + "]";
	}
	
}
