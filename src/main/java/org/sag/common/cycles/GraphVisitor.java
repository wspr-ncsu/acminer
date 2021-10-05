package org.sag.common.cycles;

/** 
* Interface for visitors of {@link Graph}.
* 
* @param <V> The vertex type of the graph.
*/
public interface GraphVisitor<V> {
	
	/**
	 * Called before the start of visit.
	 */
	public void startVisit();
	
	/**
	 * Called before a vertex is traversed. 
	 * 
	 * @param vertex The vertex.
	 */
	public void preVisit(V vertex);
	
	/**
	 * Called after a vertex is traversed.
	 * 
	 * @param vertex The vertex.
	 */
	public void postVisit(V vertex);
	
	/**
	 * Called after each visiting operation to check whether the visitor has
	 * done its work. If so then the traversal of the graph is terminated.
	 * 
	 * @return A boolean indicating whether the visitor is done.
	 */
	public boolean isDone();
	
	/** 
	 * Called after the end of visit.
	 */
	public void endVisit();

}

