/**
 * Created: May 3, 2008
 */
package org.xjava.delegates;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.xjava.util.ClassUtilities;

/**
 * MethodDelegates reference a specific instance and method name that receives
 * arguments either at invocation time or if pre-set before execution.
 * 
 * @author Matt Hicks
 */
public class MethodDelegate implements Delegate
{
	private Object obj;
	private WeakReference<Object> ref;
	private Method method;
	private Object[] arguments;

	public MethodDelegate(Object obj, Method method, boolean weak)
	{
		if (weak)
		{
			ref = new WeakReference<Object>(obj);
		} else
		{
			this.obj = obj;
		}
		this.method = method;

		if (method == null)
		{
			throw new NullPointerException("Method cannot be null");
		} else if (((method.getModifiers() & Modifier.STATIC) != Modifier.STATIC) && (obj == null))
		{
			throw new NullPointerException("Cannot pass null Object for non-static method: " + method.getName());
		}
	}

	public MethodDelegate(Object obj, String methodName, Class<?>... args)
	{
		this.obj = obj;
		try
		{
			method = obj.getClass().getMethod(methodName, args);
		} catch (SecurityException e)
		{
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void setArguments(Object... arguments)
	{
		this.arguments = arguments;
	}

	public void addArguments(Object... arguments)
	{
		int length = this.arguments != null ? this.arguments.length : 0;
		length += arguments.length;
		Object[] tmp = new Object[length];
		int position = 0;
		if (this.arguments != null)
		{
			for (Object o : this.arguments)
			{
				tmp[position++] = o;
			}
		}
		for (Object o : arguments)
		{
			tmp[position++] = o;
		}
		this.arguments = tmp;
	}

	public Object invoke(Object... args) throws Exception
	{
		try
		{
			Object[] tmp = new Object[method.getParameterTypes().length];
			int pos = 0;
			if ((arguments != null) && (pos < tmp.length))
			{
				for (int i = 0; i < arguments.length; i++)
				{
					if (pos >= tmp.length)
					{
						break;
					}
					tmp[pos++] = arguments[i];
				}
			}
			if (pos < tmp.length)
			{
				for (int i = 0; i < args.length; i++)
				{
					if (pos >= tmp.length)
					{
						break;
					}
					tmp[pos++] = args[i];
				}
			}
			Object obj = this.obj;
			if (ref != null)
			{
				obj = ref.get();
			}
			return method.invoke(obj, tmp);
		} catch (IllegalArgumentException exc)
		{
			throw new RuntimeException("Wrong number or type of arguments, Received: " + args.length + ", Expected: "
					+ args.length + " - " + exc.getMessage());
		}
	}

	public boolean isValid()
	{
		if (ref != null)
		{
			return ref.get() != null;
		}
		return true;
	}

	public Object getObject()
	{
		if (ref != null)
		{
			return ref.get();
		}
		return obj;
	}

	public Method getMethod()
	{
		return method;
	}

	private static final MethodDelegate create(Class<?> c, Object obj, String methodName, boolean matchReverse,
			boolean weak, Class<?>... args)
	{
		boolean includeStatic = obj == null;
		Method[] methods = ClassUtilities.getMethods(c, includeStatic, true);

		// Check for method that matches signature
		methodLoop: for (Method m : methods)
		{
			if (m.getName().equals(methodName))
			{
				if (m.getParameterTypes().length == args.length)
				{
					for (int i = 0; i < args.length; i++)
					{
						Class<?> type = ClassUtilities.updateClass(m.getParameterTypes()[i]);
						if ((args[i] != null) && (!type.isAssignableFrom(args[i])))
						{
							if (matchReverse)
							{
								if (!args[i].isAssignableFrom(type))
								{
									continue methodLoop;
								}
							} else
							{
								continue methodLoop;
							}
						}
					}
					return new MethodDelegate(obj, m, weak);
				}
			}
		}

		// Check for method that matches signature - ignoring case
		methodLoop: for (Method m : methods)
		{
			if (m.getName().equalsIgnoreCase(methodName))
			{
				if (m.getParameterTypes().length == args.length)
				{
					for (int i = 0; i < args.length; i++)
					{
						Class<?> type = ClassUtilities.updateClass(m.getParameterTypes()[i]);
						if (!type.isAssignableFrom(args[i]))
						{
							if (matchReverse)
							{
								if (!args[i].isAssignableFrom(type))
								{
									continue methodLoop;
								}
							} else
							{
								continue methodLoop;
							}
						}
					}
					return new MethodDelegate(obj, m, weak);
				}
			}
		}

		// Check again if none are found and the args.length is 0 ignoring arg
		// count
		// if (args.length == 0) {
		for (Method m : methods)
		{
			if (m.getName().equals(methodName))
			{
				return new MethodDelegate(obj, m, weak);
			}
		}
		// }

		// Check again if none are found and the args.length is 0 ignoring arg
		// count - ignoring case
		// if (args.length == 0) {
		for (Method m : methods)
		{
			if (m.getName().equalsIgnoreCase(methodName))
			{
				return new MethodDelegate(obj, m, weak);
			}
		}
		// }

		StringBuilder builder = new StringBuilder();
		{
			for (Class<?> cl : args)
			{
				builder.append(cl + " ");
			}
		}
		throw new RuntimeException("Unable to find Method: " + c.getCanonicalName() + "." + methodName + " - "
				+ builder);
	}

	private static final MethodDelegate createWithArgs(Class<?> c, Object obj, String methodName, boolean matchReverse,
			boolean weak, Object... args)
	{
		Class<?>[] classes = new Class<?>[args.length];
		for (int i = 0; i < classes.length; i++)
		{
			if (args[i] != null)
			{
				classes[i] = args[i].getClass();
			}
		}
		MethodDelegate delegate = create(c, obj, methodName, matchReverse, weak, classes);

		if (delegate != null)
		{
			delegate.setArguments(args);
			return delegate;
		}
		throw new RuntimeException("Unable to find Method: " + c.getCanonicalName() + "." + methodName);
	}

	public static final MethodDelegate create(Object obj, String methodName, boolean matchReverse, boolean weak,
			Class<?>... args)
	{
		return create(obj.getClass(), obj, methodName, matchReverse, weak, args);
	}

	public static final MethodDelegate create(Object obj, String methodName, Class<?>... args)
	{
		return create(obj, methodName, false, false, args);
	}

	public static final MethodDelegate createWithArgs(Object obj, String methodName, Object... args)
	{
		return createWithArgs(obj.getClass(), obj, methodName, false, false, args);
	}

	public static final MethodDelegate create(Class<?> c, String methodName, boolean matchReverse, boolean weak,
			Class<?>... args)
	{
		return create(c, null, methodName, matchReverse, weak, args);
	}

	public static final MethodDelegate create(Class<?> c, String methodName, Class<?>... args)
	{
		return create(c, methodName, false, false, args);
	}

	public static final MethodDelegate createWithArgs(Class<?> c, String methodName, Object... args)
	{
		return createWithArgs(c, null, methodName, false, false, args);
	}
}