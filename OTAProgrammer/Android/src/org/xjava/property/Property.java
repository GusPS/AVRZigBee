package org.xjava.property;

/**
 * Property provides an abstraction and hierarchical control
 * over objects and a complete replacement to bean getter/setter
 * methodology.
 * 
 * @author Matt Hicks
 *
 * @param <T>
 */
public interface Property<T> {
	/**
	 * Gets the current value associated with this property.
	 * 
	 * @return
	 * 		T
	 */
	public T get();
	
	/**
	 * Sets the passed value <code>t</code> to this property if mutable. If the
	 * property is immutable a new Property<T> will be created and returned
	 * containing the new value. If this Property is mutable "<code>this</code>"
	 * will be returned.
	 * 
	 * @param t
	 * @return
	 * 		Property<T>
	 */
	public Property<T> set(T t);

	/**
	 * The mutability state of this Property. If true, invocations of <code>set(T t)</code>
	 * will modify the Property directly. If false, a new instance of the Property is created
	 * for each invocation of <code>set(T t)</code>.
	 * 
	 * @return
	 * 		mutable
	 */
	public boolean isMutable();
}