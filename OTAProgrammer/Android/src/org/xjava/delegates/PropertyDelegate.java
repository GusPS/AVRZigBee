package org.xjava.delegates;

import org.xjava.property.Property;

public class PropertyDelegate<T> implements Delegate {
	private Property<T> property;
	
	public PropertyDelegate(Property<T> property) {
		this.property = property;
	}

	@SuppressWarnings("unchecked")
	public Object invoke(Object... args) throws Exception {
		if (args.length == 0) {
			return property.get();
		}
		property = property.set((T)args[0]);
		return null;
	}
}