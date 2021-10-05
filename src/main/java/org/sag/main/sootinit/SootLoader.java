package org.sag.main.sootinit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sag.common.logging.ILogger;
import soot.Body;
import soot.DoubleType;
import soot.FloatType;
import soot.Local;
import soot.LongType;
import soot.NullType;
import soot.Pack;
import soot.PackManager;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.dexpler.typing.UntypedConstant;
import soot.dexpler.typing.UntypedIntOrFloatConstant;
import soot.dexpler.typing.UntypedLongOrDoubleConstant;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.MethodHandle;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.scalar.LocalNameStandardizer;
import soot.options.Options;
import soot.shimple.toolkits.scalar.SEvaluator.MetaConstant;
import soot.util.Chain;

public abstract class SootLoader {

	/* The static initializers of SootInstanceWrapper must be run before any Soot loading
	 * begins because they replace the default GlobalObjectGetter with the instance of the
	 * SootInstanceWrapper which in turn replaces the current Soot instance. So if this were
	 * done after Soot were loaded, Soot would be returned to an unloaded state. To ensure
	 * we always perform this task before any Soot loading begins, we grab the instance of
	 * the singleton of SootInstanceWrapper using its getter v(). This triggers the running of 
	 * any static initializers in SootInstanceWrapper if they have not already run since static
	 * Initializers in Java are run when a class is first accessed. Moreover, since the grabbing
	 * of the singleton instance of SootInstanceWrapper is performed as part of the static 
	 * initializers of this class, the super class of all SootLoaders, we guarantee it is always
	 * performed before any soot loading can occur. Note it is not possible for the SootInstanceWrapper
	 * instance grabbed here to change as it is a singleton that does not allow for the singleton
	 * object being returned by v() to change. In other words, this instance will always be the
	 * same as the one returned by v().
	 */
	private static final SootInstanceWrapper sootInstanceWrapper = SootInstanceWrapper.v();
	
	public static SootInstanceWrapper getSootInstanceWrapper(){
		return sootInstanceWrapper;
	}
	
	private final int sootLoadKey;
	protected final String cn;
	
	public SootLoader(int sootLoadKey){
		this.sootLoadKey = sootLoadKey;
		this.cn = getClass().getSimpleName();
	}
	
	public int getSootLoadKey(){
		return sootLoadKey;
	}
	
	public boolean isSootLoaded(){
		return getSootInstanceWrapper().isSootInitSetTo(sootLoadKey);
	}
	
	/* Because of a bug when performing backwards analysis using Heros, we need to add a "nop" as
	 * the first statement of every method.
	 * (TODO: remove this when the bug is fixed in heros)*/
	protected boolean addNopAsFirstStatment(ILogger logger) {
		for(SootClass sc : Scene.v().getClasses()){
			for(SootMethod sm : sc.getMethods()){
				if (!sm.isConcrete())
					continue;
				Unit target = null;
				try{
					Chain<Unit> units = sm.retrieveActiveBody().getUnits();
					for(Unit u : units){
						if (!(u instanceof IdentityStmt)) {
							target = u;
							break;
						}
					}
					if(target != null){
						units.insertBefore(Jimple.v().newNopStmt(), target);
					}else{
						logger.fatal("{}: Failed to add nop as the first statement of method '{}'.",cn,sm);
						return false;
					}
				}catch(Throwable t){
					logger.fatal("{}: Failed to add nop as the first statement of method '{}'.",t,cn,sm);
					return false;
				}
			}
		}
		return true;
	}
	
	/* Make sure the jb pack runs because it will be run later when jimple is reloaded
	 * and it is not run when dex is loaded. Also apply a fix for NullType and make
	 * sure the LocalNameStandardizer is the last thing run always.
	 */
	protected boolean runJBPackAndFixTypes(boolean runjb, ILogger logger) {
		Pack jbPack = PackManager.v().getPack("jb");
		Set<SootClass> curClasses = new HashSet<>();
		Set<SootClass> prevClasses = null;
		while(!curClasses.equals(prevClasses)) {
			prevClasses = curClasses;
			curClasses = new HashSet<>();
			for(Iterator<SootClass> it = Scene.v().getClasses().snapshotIterator(); it.hasNext();) {
				SootClass sc = it.next();
				curClasses.add(sc);
				for(SootMethod sm : sc.getMethods()) {
					try {
						if(sm.isConcrete()) {
							Body b = sm.retrieveActiveBody();
							if(runjb)
								jbPack.apply(b);
							//TypeAssigner  -> TypeResolver -> AugEvalFunction -> eval_ may introduce NullType
							//for locals that are only every assigned the value null
							//This gives them a proper type that can be output
							for (Local l : b.getLocals()) {
								if (l.getType() instanceof NullType)
									l.setType(RefType.v("java.lang.Object"));
							}
							LocalNameStandardizer.v().transform(b);
						}
					} catch(Throwable t) {
						logger.fatal("{}: Failed to fix types for method '{}'",t,cn,sm);
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/* This function gets rid of cast expressions in the code when the things being cast is a constant.
	 * This is a hack because I made the assumption that cast expressions are only ever done on locals
	 * when it is in fact locals and constants. Here I leave casts for locals alone because they are handled
	 * and then try to convert constants to the correct type if possible and if not I just remove the cast
	 * to get rid of the issue since there is no real need to know that a constant has been cast for our 
	 * purposes. 
	 */
	protected boolean eleminateNonLocalCasts(ILogger logger) {
		for(SootClass sc : Scene.v().getClasses()) {
			for(SootMethod sm : sc.getMethods()) {
				try {
					if(sm.isConcrete()) {
						Body b = sm.retrieveActiveBody();
						
						for(Unit u : b.getUnits()) {
							if(u instanceof AssignStmt) {
								AssignStmt stmt = (AssignStmt)u;
								Value right = stmt.getRightOp();
								if(right instanceof CastExpr) {
									Value op = ((CastExpr)right).getOp(); //Can only be Local or Constant
									Type cast = ((CastExpr)right).getCastType();
									
									//Keep Local casts as these are properly handled later and remove all constant casts
									if(op instanceof Constant) {
										if(op instanceof NullConstant || op instanceof ClassConstant || op instanceof MetaConstant 
												|| op instanceof MethodHandle || op instanceof StringConstant) {
											stmt.setRightOp(op);
										} else if(op instanceof NumericConstant) {
											if(!(cast instanceof PrimType)) {
												throw new RuntimeException("Error: Casting numeric constant '" 
														+ op + "' to non primitive type '" + cast + "' for unit '" 
														+ u + "' of method '" + sm +"' ???");
											}
											if(op instanceof IntConstant) {
												int val = ((IntConstant)op).value;
												if(cast instanceof LongType) {
													stmt.setRightOp(LongConstant.v(val));
												} else if(cast instanceof FloatType) {
													stmt.setRightOp(FloatConstant.v(val));
												} else if(cast instanceof DoubleType) {
													stmt.setRightOp(DoubleConstant.v(val));
												} else {
													stmt.setRightOp(op);
												}
											} else if(op instanceof LongConstant) {
												long val = ((LongConstant)op).value;
												if(cast instanceof LongType) {
													stmt.setRightOp(op);
												} else if(cast instanceof FloatType) {
													stmt.setRightOp(FloatConstant.v(val));
												} else if(cast instanceof DoubleType) {
													stmt.setRightOp(DoubleConstant.v(val));
												} else {
													stmt.setRightOp(IntConstant.v((int)val));
												}
											} else if(op instanceof FloatConstant) {
												float val = ((FloatConstant)op).value;
												if(cast instanceof LongType) {
													stmt.setRightOp(LongConstant.v((long)val));
												} else if(cast instanceof FloatType) {
													stmt.setRightOp(op);
												} else if(cast instanceof DoubleType) {
													stmt.setRightOp(DoubleConstant.v(val));
												} else {
													stmt.setRightOp(IntConstant.v((int)val));
												}
											} else if(op instanceof DoubleConstant) {
												double val = ((DoubleConstant)op).value;
												if(cast instanceof LongType) {
													stmt.setRightOp(LongConstant.v((long)val));
												} else if(cast instanceof FloatType) {
													stmt.setRightOp(FloatConstant.v((float)val));
												} else if(cast instanceof DoubleType) {
													stmt.setRightOp(op);
												} else {
													stmt.setRightOp(IntConstant.v((int)val));
												}
											}
										} else if(op instanceof UntypedConstant) {
											if(!(cast instanceof PrimType)) {
												throw new RuntimeException("Error: Casting untyped numeric constant '" 
														+ op + "' to non primitive type '" + cast + "' for unit '" 
														+ u + "' of method '" + sm +"' ???");
											}
											if(op instanceof UntypedIntOrFloatConstant) {
												FloatConstant valFloat = ((UntypedIntOrFloatConstant)op).toFloatConstant();
												IntConstant val = ((UntypedIntOrFloatConstant)op).toIntConstant();
												if(cast instanceof LongType) {
													stmt.setRightOp(LongConstant.v(val.value));
												} else if(cast instanceof FloatType) {
													stmt.setRightOp(valFloat);
												} else if(cast instanceof DoubleType) {
													stmt.setRightOp(DoubleConstant.v(valFloat.value));
												} else {
													stmt.setRightOp(val);
												}
											} else if(op instanceof UntypedLongOrDoubleConstant) {
												DoubleConstant valD = ((UntypedLongOrDoubleConstant)op).toDoubleConstant();
												LongConstant valL = ((UntypedLongOrDoubleConstant)op).toLongConstant();
												if(cast instanceof LongType) {
													stmt.setRightOp(valL);
												} else if(cast instanceof FloatType) {
													stmt.setRightOp(FloatConstant.v((float)valD.value));
												} else if(cast instanceof DoubleType) {
													stmt.setRightOp(valD);
												} else {
													stmt.setRightOp(IntConstant.v((int)valL.value));
												}
											}
										} else {
											throw new RuntimeException("Error: Unhandled constant type '" + op.getClass().getName() 
													+ "' for unit '" + u + "' of method '" + sm + "'.");
										}
									}
								}
							}
						}
					}
				} catch(Throwable t) {
					logger.fatal("{}: Failed to remove casts in method '{}'",t,cn,sm);
					return false;
				}
			}
		}
		return true;
	}
	
	public static int javaVersionConvert(int javaVersion) {
		switch(javaVersion) {
			case 1:
				return Options.java_version_1;
			case 2:
				return Options.java_version_2;
			case 3:
				return Options.java_version_3;
			case 4:
				return Options.java_version_4;
			case 5:
				return Options.java_version_5;
			case 6:
				return Options.java_version_6;
			case 7:
				return Options.java_version_7;
			case 8:
				return Options.java_version_8;
			default:
				return Options.java_version_default;
		}
	}
	
}
