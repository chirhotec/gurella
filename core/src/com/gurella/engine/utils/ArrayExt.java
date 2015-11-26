package com.gurella.engine.utils;

import com.badlogic.gdx.utils.Array;

public class ArrayExt<T> extends Array<T> {
	private ImmutableArray<T> immutable;

	public ArrayExt() {
		super();
	}

	public ArrayExt(Array<? extends T> array) {
		super(array);
	}

	public ArrayExt(boolean ordered, int capacity, Class arrayType) {
		super(ordered, capacity, arrayType);
	}

	public ArrayExt(boolean ordered, int capacity) {
		super(ordered, capacity);
	}

	public ArrayExt(boolean ordered, T[] array, int start, int count) {
		super(ordered, array, start, count);
	}

	public ArrayExt(Class arrayType) {
		super(arrayType);
	}

	public ArrayExt(int capacity) {
		super(capacity);
	}

	public ArrayExt(T[] array) {
		super(array);
	}

	public ImmutableArray<T> immutable() {
		if (immutable == null) {
			immutable = new ImmutableArray<T>(this);
		}
		return immutable;
	}
}
