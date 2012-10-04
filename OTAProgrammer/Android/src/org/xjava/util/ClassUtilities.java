package org.xjava.util;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author Matt Hicks
 */
public class ClassUtilities {
	private static final Map<String, Field[]> fieldCache = new HashMap<String, Field[]>();
	private static final Map<String, Field> internalFields = new HashMap<String, Field>();
	
	public static List<Class<?>> getClassHierarchy(Class<?> c, boolean includeInterfaces) {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		while (c != null) {
			classes.add(c);
			if (includeInterfaces) {
				Class<?>[] interfaces = c.getInterfaces();
				for (Class<?> i : interfaces) {
					classes.add(i);
				}
			}
			c = c.getSuperclass();
		}

		return classes;
	}
	
	public static Method[] getMethods(Class<?> c, boolean includeStatic) {
		return getMethods(c, includeStatic, true);
	}
	
	public static Method[] getMethods(Class<?> c, boolean includeStatic, boolean recursive) {
		ArrayList<Method> methods = new ArrayList<Method>();

		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (recursive) {
    		while (c != Object.class) {
    			classes.add(c);
    			c = c.getSuperclass();
    		}
    
    		// Reverse order so we make sure we maintain consistent order
    		Collections.reverse(classes);
		} else {		// Not recursive...only process this Class
			classes.add(c);
		}

		for (Class<?> clazz : classes) {
			Method[] array = clazz.getDeclaredMethods();
			for (Method m : array) {
				if ((m.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT)
					continue;
				boolean isStatic = (m.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
				if ((isStatic) && (!includeStatic)) {
					continue;
				}
				if (!m.isAccessible()) {
					try {
						m.setAccessible(true);
					} catch(Throwable t) {}
				}
				methods.add(m);
			}
		}

		return methods.toArray(new Method[methods.size()]);
	}
	
	public static Field[] getFields(Object object, boolean includeStatic) {
		return getFields(object, includeStatic, true);
	}
	
	public static Field[] getFields(Object object, boolean includeStatic, boolean includeTransient) {
		Class<?> c = object.getClass();
		return getFields(c, includeStatic, includeTransient);
	}
	
	public static Field[] getFields(Class<?> c, boolean includeStatic) {
		return getFields(c, includeStatic, true);
	}
	
	public static Field[] getFields(Class<?> c, boolean includeStatic, boolean includeTransient) {
		String cacheKey = c.getCanonicalName() + ":" + includeStatic;
		Field[] array = fieldCache.get(cacheKey);
		
		if (array == null) {
    		ArrayList<Field> fields = new ArrayList<Field>();
    
    		List<Class<?>> classes = getClassHierarchy(c, false);
    		
    		// Reverse order so we make sure we maintain consistent order
    		Collections.reverse(classes);
    
    		for (Class<?> clazz : classes) {
    			Field[] allFields = clazz.getDeclaredFields();
    			for (Field f : allFields) {
    				if ((!includeTransient) && ((f.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT)) {
    					continue;
    				} else if (f.isSynthetic()) {
    					// Synthetic fields are bad!!!
    					continue;
    				}
    				boolean isStatic = (f.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
    				if ((isStatic) && (!includeStatic)) {
    					continue;
    				}
    				if (f.getName().equalsIgnoreCase("serialVersionUID")) {
    					continue;
    				}
    				f.setAccessible(true);
    				fields.add(f);
    			}
    		}
    
    		array = fields.toArray(new Field[fields.size()]);
    		fieldCache.put(cacheKey, array);
		}
		return array;
	}

	public static final Field internalField(Object object, String fieldName) {
		if (object == null) {
			System.out.println("Internal Field: " + object + ", " + fieldName);
			return null;
		}
		
		String key = object.getClass().getCanonicalName() + "." + fieldName;
		Field field = internalFields.get(key);
		if (field == null) {
    		Field[] fields = getFields(object.getClass(), false);
    		
    		for (Field f : fields) {
				String name = f.getName();
				if (name.equals(fieldName)) {
					field = f;
					internalFields.put(key, field);
					break;
				}
    		}
		}
		return field;
	}

	/**
	 * Gets the internal value for 'fieldName' without calling the getter
	 * method. This is to keep from invoking the aspect intercepter.
	 * 
	 * @param object
	 * @param fieldName
	 * @return value of 'fieldName'
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static final Object internalValue(Object object, String fieldName) throws IllegalArgumentException, IllegalAccessException {
		try {
    		Field f = internalField(object, fieldName);
    		if (f != null) {
    			return f.get(object);
    		}
		} catch(Exception exc) {
			exc.printStackTrace();
			throw new RuntimeException("Error trying to get internalValue of " + object.getClass() + "." + fieldName, exc);
		}
		return null;
	}
	
	public static final Object internalValueIgnoreExceptions(Object object, String fieldName) {
		try {
			return internalValue(object, fieldName);
		} catch(Exception exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/**
	 * Allows you to change 'fieldName's value in 'object' to 'value' without
	 * calling the setter method. This is to keep from hitting the aspect
	 * intercepter.
	 * 
	 * @param object
	 * @param fieldName
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	public static final void changeValue(Object object, String fieldName, Object value) throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		if (object == null)
			return;
		Class<?> c = object.getClass();
		while (c != Object.class) {
			try {
				Field f = c.getDeclaredField(fieldName);
				f.setAccessible(true);
				f.set(object, value);
				return;
			} catch (NoSuchFieldException exc) {
				// Ignore, we continue
			}
			c = c.getSuperclass();
		}
		throw new NoSuchFieldException(object.getClass().getName() + "." + fieldName);
	}
	
	public static final void changeValueIgnoreExceptions(Object object, String fieldName, Object value) {
    	try {
    		changeValue(object, fieldName, value);
    	} catch(Exception exc) {
    		exc.printStackTrace();
    	}
	}

    public static boolean doesClassExist(String name) {
        try {
            Class<?> c = Class.forName(name);
            if (c != null) return true;
        } catch(ClassNotFoundException e) {
            // Class not found
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static final Object getStaticFieldValue(Class<?> c, String fieldName) {
    	try {
	    	Field f = c.getField(fieldName);
	    	if (f != null) {
	    		return f.get(null);
	    	}
	    	return null;
    	} catch(NoSuchFieldException exc) {
    		return null;
    	} catch(Throwable t) {
    		throw new RuntimeException(t);
    	}
    }
    
    public static final Field getField(Class<?> c, String fieldName) {
    	return getField(c, fieldName, true);
    }
    
    public static final Field getField(Class<?> c, String fieldName, boolean matchTransient) {
    	return getField(c, fieldName, true, matchTransient);
    }
    
    public static final Field getField(Class<?> c, String fieldName, boolean matchStatic, boolean matchTransient) {
    	if (c == null) {
    		throw new RuntimeException("Class cannot be null for field: " + fieldName);
    	}
		while (c != Object.class) {
			try {
				Field f = c.getDeclaredField(fieldName);
				if ((f.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT) {
					if (!matchTransient) {
						c = c.getSuperclass();
						continue;
					}
				} else if ((f.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
					if (!matchStatic) {
						c = c.getSuperclass();
						continue;
					}
				}
				f.setAccessible(true);
				return f;
			} catch(NoSuchFieldException exc) {
				// Ignore, we continue
			}
			c = c.getSuperclass();
		}
		//throw new NoSuchFieldException(fieldName);
//		System.err.println("No such field on " + c.getName() + " - " + fieldName + " (JSLUtil.internalField)");
		return null;
	}
    
    public static final Method getMethod(Class<?> c, String methodName) {
    	while (c != Object.class) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equalsIgnoreCase(methodName)) {
					try {
						m.setAccessible(true);
					} catch(Throwable t) {}
					return m;
				}
			}
			c = c.getSuperclass();
		}
    	return null;
    }
    
    public static final Method getMethod(Class<?> c, String methodName, Class<?> ... paramTypes) {
    	while (c != Object.class) {
			try {
				Method m = c.getDeclaredMethod(methodName, paramTypes);
				m.setAccessible(true);
				return m;
			} catch(NoSuchMethodException exc) {
				// Ignore, we continue
			}
			c = c.getSuperclass();
		}
		//throw new NoSuchFieldException(fieldName);
//		System.err.println("No such field on " + c.getName() + " - " + fieldName + " (JSLUtil.internalField)");
		return null;
    }

    public static final byte[] serialize(Object o) throws IOException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ObjectOutputStream oos = new ObjectOutputStream(baos);
    	oos.writeObject(o);
    	oos.flush();
    	oos.close();
    	return baos.toByteArray();
    }
    
    public static final Object restore(byte[] b) throws IOException, ClassNotFoundException {
    	ByteArrayInputStream bais = new ByteArrayInputStream(b);
    	ObjectInputStream ois = new ObjectInputStream(bais);
    	Object o = ois.readObject();
    	ois.close();
    	return o;
    }

    public static final boolean hasRecursiveReference(Object object) {
    	return checkRecursive(object, object, new ArrayList<Object>());
    }
    
    private static final boolean checkRecursive(Object object, Object sub, List<Object> cache) {
    	if (sub == null) return false;
    	
    	Field[] fields = getFields(sub.getClass(), false);
    	for (Field f : fields) {
    		try {
    			Object child = f.get(sub);
    			if (!cache.contains(child)) {
    				cache.add(child);
            		if (object == child) {
            			return true;
            		} else if (checkRecursive(object, child, cache)) {
            			return true;
            		}
    			}
    		} catch(IllegalAccessException exc) {
    		}
    	}
    	
    	return false;
    }

    /**
	 * Updates primitive classes to wrapper classes.
	 * 
	 * @param c
	 * @return
	 * 		updated class
	 */
	public static final Class<?> updateClass(Class<?> c) {
        if (c == boolean.class) c = Boolean.class;
        else if (c == byte.class) c = Byte.class;
        else if (c == short.class) c = Short.class;
    	else if (c == int.class) c = Integer.class;
    	else if (c == float.class) c = Float.class;
    	else if (c == long.class) c = Long.class;
    	else if (c == double.class) c = Double.class;
    	else if (c == char.class) c = Character.class;
    	return c;
	}

	public static final Field[] getAllFieldsExcept(Field[] fields, String ... fieldNames) {
		List<Field> list = new ArrayList<Field>();
		fieldLoop: for (Field f : fields) {
			for (String s : fieldNames) {
				if (f.getName().equalsIgnoreCase(s)) {
					// Exclude
					continue fieldLoop;
				}
			}
			list.add(f);
		}
		return list.toArray(new Field[list.size()]);
	}
	
	public static final String[] getFieldNames(Field ... fields) {
		String[] names = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			names[i] = fields[i].getName();
		}
		return names;
	}
}