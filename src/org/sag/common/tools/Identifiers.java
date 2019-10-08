package org.sag.common.tools;

public class Identifiers {

	public static final int PUBLIC = 	0x0001;
	public static final int PROTECTED = 0x0002;
	public static final int PRIVATE = 	0x0004;
	public static final int FINAL = 	0x0008;
	public static final int STATIC = 	0x0010;
	public static final int NATIVE = 	0x0020;
	public static final int ABSTRACT = 	0x0040;
	public static final int SYNC = 		0x0080;
	public static final int WRITE = 	0x0100;
	
	public static int setPublic(int n){
		return n | PUBLIC;
	}
	
	public static int setProtected(int n){
		return n | PROTECTED;
	}
	
	public static int setPrivate(int n){
		return n | PRIVATE;
	}
	
	public static int setFinal(int n){
		return n | FINAL;
	}
	
	public static int setStatic(int n){
		return n | STATIC;
	}
	
	public static int setWrite(int n){
		return n | WRITE;
	}
	
	public static int setNative(int n){
		return n | NATIVE;
	}
	
	public static int setAbstract(int n){
		return n | ABSTRACT;
	}
	
	public static int setSynchronized(int n){
		return n | SYNC;
	}
	
	public static boolean isPublic(int n){
		return (n & PUBLIC) != 0;
	}
	
	public static boolean isProtected(int n){
		return (n & PROTECTED) != 0;
	}
	
	public static boolean isPrivate(int n){
		return (n & PRIVATE) != 0;
	}
	
	public static boolean isFinal(int n){
		return (n & FINAL) != 0;
	}
	
	public static boolean isStatic(int n){
		return (n & STATIC) != 0;
	}
	
	public static boolean isWrite(int n){
		return (n & WRITE) != 0;
	}
	
	public static boolean isNative(int n){
		return (n & NATIVE) != 0;
	}
	
	public static boolean isAbstract(int n){
		return (n & ABSTRACT) != 0;
	}
	
	public static boolean isSynchronized(int n){
		return (n & SYNC) != 0;
	}
	
	public static String generateHeader(int n){
		StringBuffer b = new StringBuffer();
		if(isPublic(n)){
			b.append("public ");
		}else if(isPrivate(n)){
			b.append("private ");
		}else if(isProtected(n)){
			b.append("protected ");
		}
		
		if(isAbstract(n)){
			b.append("abstract ");
		}
		
		if(isStatic(n)){
			b.append("static ");
		}
		
		if(isFinal(n)){
			b.append("final ");
		}
		
		if(isSynchronized(n)){
			b.append("synchronized ");
		}
		
		if(isNative(n)){
			b.append("native ");
		}
		
		return (b.toString()).trim();
	}
	
	public static String generateWrite(int n){
		if(isWrite(n)){
			return "write";
		}else{
			return "read";
		}
	}
}
