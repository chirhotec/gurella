package com.gurella.engine.pool;

import java.util.Comparator;

import com.badlogic.gdx.utils.Array;
import com.gurella.engine.utils.Values;

public class IntArrayPool {
	public final int max;
	private final Array<int[]> freeObjects;

	public IntArrayPool() {
		this(64, Integer.MAX_VALUE);
	}

	public IntArrayPool(int initialCapacity) {
		this(initialCapacity, Integer.MAX_VALUE);
	}

	public IntArrayPool(int initialCapacity, int max) {
		this.max = max;
		freeObjects = new Array<int[]>(initialCapacity);
	}

	public int[] obtain(int length, int maxLength) {
		int[] array = find(length, maxLength);
		return array == null ? new int[length] : array;
	}

	private int[] find(int length, int maxLength) {
		int low = 0;
		int high = freeObjects.size - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int[] midVal = freeObjects.get(mid);

			if (midVal.length < length) {
				low = mid + 1;
			} else if (midVal.length > length) {
				high = mid - 1;
				if (high >= 0) {
					int[] temp = freeObjects.get(high);
					if (temp.length < length && midVal.length <= maxLength) {
						return freeObjects.removeIndex(high);
					}
				}
			} else {
				return freeObjects.removeIndex(mid);
			}
		}

		return null;
	}

	public void free(int[] object) {
		if (object == null) {
			throw new IllegalArgumentException("object cannot be null.");
		}

		if (freeObjects.size < max) {
			freeObjects.add(object);
			freeObjects.sort(ArrayComparable.instance);
		}
	}

	public void freeAll(Array<int[]> objects) {
		if (objects == null) {
			throw new IllegalArgumentException("object cannot be null.");
		}

		if (freeObjects.size >= max) {
			return;
		}

		for (int i = 0; i < objects.size && freeObjects.size >= max; i++) {
			int[] object = objects.get(i);
			if (object != null) {
				freeObjects.add(object);
			}
		}

		freeObjects.sort(ArrayComparable.instance);
	}

	public void clear() {
		freeObjects.clear();
	}

	public int getFree() {
		return freeObjects.size;
	}

	private static class ArrayComparable implements Comparator<int[]> {
		private static final ArrayComparable instance = new ArrayComparable();

		@Override
		public int compare(int[] o1, int[] o2) {
			return Values.compare(o1.length, o2.length);
		}
	}
}