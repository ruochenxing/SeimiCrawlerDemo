package com.download;

public abstract class DLCallback<T> {

	public abstract void callback(T arg);

	public static <T> void trigger(DLCallback<T> callback, T arg) {
		if (callback != null) {
			callback.callback(arg);
		}
	}

}
