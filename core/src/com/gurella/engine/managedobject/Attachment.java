package com.gurella.engine.managedobject;

public abstract class Attachment<T> {
	protected T value;

	public T getValue() {
		return value;
	}

	protected abstract void attach();

	protected abstract void detach();
}
