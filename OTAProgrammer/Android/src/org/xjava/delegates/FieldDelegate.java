/**
 * Created: Jul 1, 2008
 */
package org.xjava.delegates;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import org.xjava.util.ClassUtilities;

/**
 * FieldDelegates reference a specific field on a specific Object that can
 * be referenced for getting and setting.
 * 
 * @author Matt Hicks
 */
public class FieldDelegate implements Delegate {
	private Object obj;
	private WeakReference<Object> ref;
	private Field field;
	
	private FieldDelegate(Object obj, Field field, boolean weak) {
		if (weak) {
			ref = new WeakReference<Object>(obj);
		} else {
			this.obj = obj;
		}
		this.field = field;
	}
	
	public Object getObject() {
		if (ref != null) {
			return ref.get();
		}
		return obj;
	}
	
	public Field getField() {
		return field;
	}
	
	public static final FieldDelegate get(Object obj, String name) {
		return get(obj, name, false);
	}
	
	public static final FieldDelegate get(Object obj, String name, boolean weak) {
		Field field = ClassUtilities.getField(obj.getClass(), name);
		if (field == null) {
			throw new RuntimeException("Cannot find field: " + name + " in Class " + obj.getClass().getName());
		}
		return new FieldDelegate(obj, field, weak);
	}
	
	public static final FieldDelegate get(Class<?> c, String name) {
		return get(c, name, false);
	}
	
	public static final FieldDelegate get(Class<?> c, String name, boolean weak) {
		Field field = ClassUtilities.getField(c, name, true, true);
		if (field == null) {
			throw new RuntimeException("Cannot find field: " + name + " in Class " + c.getName());
		}
		return new FieldDelegate(null, field, weak);
	}

	public Object invoke(Object... args) {
		Object obj = this.obj;
		if (ref != null) {
			obj = ref.get();
		}
		
		if (args.length > 0) {
			try {
				field.set(obj, args[0]);
			} catch(Exception exc) {
				// TODO: add better handling
				exc.printStackTrace();
			}
		}
		try {
			return field.get(obj);
		} catch(Exception exc) {
			// TODO: add better handling
			exc.printStackTrace();
		}
		return null;
	}

	public boolean isValid() {
		if (ref != null) {
			return ref.get() != null;
		}
		return true;
	}
}
