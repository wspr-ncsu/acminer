package org.sag.acminer.database.accesscontrol;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.sag.acminer.phases.entrypoints.EntryPoint;

import soot.SootMethod;

public interface IContextQueryDatabase extends IAccessControlDatabase {

	/** Returns all context query methods for all entry points.
	 */
	Set<SootMethod> getContextQueries();
	
	/** Returns all context query methods for all entry points sorted by the entry points.
	 */
	Map<EntryPoint, Set<SootMethod>> getContextQueriesByEntryPoint();

	/** Returns all context query methods for a given entry point.
	 */
	Set<SootMethod> getContextQueries(EntryPoint ep);

	/** Returns all the context query methods for all entry points
	 * with all possible subgraph methods for each context query. (i.e. all subgraph methods for every
	 * entry point).
	 */
	Map<SootMethod, Set<SootMethod>> getContextQueriesWithSubGraphMethods();

	/** Returns all context query methods for a given entry point with all subgraph methods
	 * for each context query.
	*/
	Map<SootMethod, Set<SootMethod>> getContextQueriesWithSubGraphMethods(EntryPoint ep);

	/** Returns all entry points with all context queries with all subgraph methods.
	 */
	Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> getAllContextQueriesWithSubGraphMethods();

	/** Returns true if the given context query method has the given method as a member of its subgraph
	 * for any entry point and false otherwise.
	 */
	boolean isSubGraphMethodOf(SootMethod cq, SootMethod sm);

	/** Returns true if the given context query method has the given method as a member of its subgraph
	 * for the given entry point and false otherwise.
	 */
	boolean isSubGraphMethodOf(EntryPoint ep, SootMethod cq, SootMethod sm);

	/** Returns true if the given method is a member of the subgraph for any context query for any entry 
	 * point and false otherwise.
	 */
	boolean isSubGraphMethod(SootMethod sm);

	/** Returns true if the given method is a member of the subgraph for any context query of the given
	 * entry point and false otherwise.
	 */
	boolean isSubGraphMethod(EntryPoint ep, SootMethod sm);
	
	boolean isContextQuery(SootMethod sm);
	
	boolean isContextQuery(EntryPoint ep, SootMethod sm);

	/** Returns all the subgraph methods for all context queries of all entry point methods.
	 */
	Set<SootMethod> getSubGraphMethods();

	/** Returns all the subgraph methods for all the context queries of a given entry point.
	 */
	Set<SootMethod> getSubGraphMethods(EntryPoint ep);
	
	public abstract IContextQueryDatabase readXML(String filePath, Path path) throws Exception;

	void addContextQuerySubGraphs(EntryPoint ep, Map<SootMethod, Set<SootMethod>> dataToAdd);

	void addAllContextQuerySubGraphs(Map<EntryPoint, Map<SootMethod, Set<SootMethod>>> dataToAdd);

}
