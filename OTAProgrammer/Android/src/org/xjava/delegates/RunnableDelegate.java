/**
 * Created: May 9, 2008
 */
package org.xjava.delegates;

import java.lang.ref.WeakReference;

/**
 * RunnableDelegate invokes a Runnable when called.
 * 
 * @author Matt Hicks
 */
public class RunnableDelegate implements Delegate {
	private Runnable r;
	private WeakReference<Runnable> ref;
	
	public RunnableDelegate() {
	}
	
	public RunnableDelegate(Runnable r) {
		this(r, false);
	}
	
	public RunnableDelegate(Runnable r, boolean weak) {
		if (weak) {
			ref = new WeakReference<Runnable>(r);
		} else {
			this.r = r;
		}
	}
	
	public Object invoke(Object... args) {
		run();
		return null;
	}
	
	public void run() {
		Runnable r = this.r;
		if (ref != null) {
			r = ref.get();
		}
		r.run();
	}

	public boolean isValid() {
		if (ref != null) {
			return ref.get() != null;
		}
		return true;
	}
}
