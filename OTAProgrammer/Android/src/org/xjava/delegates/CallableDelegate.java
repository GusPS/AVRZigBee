package org.xjava.delegates;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

public class CallableDelegate implements Delegate {
	private Callable<?> callable;
	private WeakReference<Callable<?>> ref;
	
	public CallableDelegate(Callable<?> callable) {
		this(callable, false);
	}
	
	public CallableDelegate(Callable<?> callable, boolean weak) {
		if (weak) {
			ref = new WeakReference<Callable<?>>(callable);
		} else {
			this.callable = callable;
		}
	}
	
	public Object invoke(Object... args) {
		Callable<?> callable = this.callable;
		if (ref != null) {
			callable = ref.get();
		}
		try {
			return callable.call();
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