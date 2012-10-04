/**
 * Created: May 9, 2008
 */
package org.xjava.delegates;

/**
 * Delegate implementations are utilized in various places
 * to be abstractly invoked asynchronously.
 * 
 * @author Matt Hicks
 */
public interface Delegate {
	public Object invoke(Object ... args) throws Exception;
}