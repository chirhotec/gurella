package com.gurella.engine.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.StringBuilder;
import com.gurella.engine.pool.PoolService;

/**
 * @author Nathan Sweet
 */
public class IdentitySet<T> implements Iterable<T>, Poolable, Container {
	private static final int PRIME1 = 0xb4b82e39;
	private static final int PRIME2 = 0xced1c241;

	public int size;

	T[] keyTable;
	int capacity, stashSize;

	float loadFactor;
	int hashShift, mask, threshold;
	int stashCapacity;
	int pushIterations;

	private IdentitySetIterator<T> iterator1, iterator2;

	/**
	 * Creates a new set with an initial capacity of 32 and a load factor of 0.8. This set will hold 25 items before
	 * growing the backing table.
	 */
	public IdentitySet() {
		this(32, 0.8f);
	}

	/**
	 * Creates a new set with a load factor of 0.8. This set will hold initialCapacity * 0.8 items before growing the
	 * backing table.
	 */
	public IdentitySet(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity *
	 * loadFactor items before growing the backing table.
	 */
	public IdentitySet(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		}
		if (initialCapacity > 1 << 30) {
			throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
		}
		if (loadFactor <= 0) {
			throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
		}
		init(initialCapacity, loadFactor);
	}

	private void init(int initialCapacity, float loadFactor) {
		this.loadFactor = loadFactor;
		capacity = MathUtils.nextPowerOfTwo(initialCapacity);
		threshold = (int) (capacity * loadFactor);
		mask = capacity - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(capacity);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(capacity)) * 2);
		pushIterations = Math.max(Math.min(capacity, 8), (int) Math.sqrt(capacity) / 8);
		keyTable = Values.cast(new Object[capacity + stashCapacity]);
	}

	/** Creates a new set identical to the specified set. */
	public IdentitySet(IdentitySet<T> set) {
		this(set.capacity, set.loadFactor);
		stashSize = set.stashSize;
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
	}

	/**
	 * Returns true if the key was not already in the set. If this set already contains the key, the call leaves the set
	 * unchanged and returns false.
	 */
	public boolean add(T key) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null.");
		}

		T[] keyTable = this.keyTable;

		// Check for existing keys.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mask;
		T key1 = keyTable[index1];
		if (key == key1) {
			return false;
		}

		int index2 = hash2(hashCode);
		T key2 = keyTable[index2];
		if (key == key2) {
			return false;
		}

		int index3 = hash3(hashCode);
		T key3 = keyTable[index3];
		if (key == key3) {
			return false;
		}

		// Find key in the stash.
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key == keyTable[i]) {
				return false;
			}

		// Check for empty buckets.
		if (key1 == null) {
			keyTable[index1] = key;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return true;
		}

		if (key2 == null) {
			keyTable[index2] = key;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return true;
		}

		if (key3 == null) {
			keyTable[index3] = key;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return true;
		}

		push(key, index1, key1, index2, key2, index3, key3);
		return true;
	}

	public void addAll(Array<? extends T> array) {
		addAll(array, 0, array.size);
	}

	public void addAll(Array<? extends T> array, int offset, int length) {
		if (offset + length > array.size) {
			throw new IllegalArgumentException(
					"offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
		}
		addAll(Values.<T[]> cast(array.items), offset, length);
	}

	public void addAll(T... array) {
		addAll(array, 0, array.length);
	}

	public void addAll(T[] array, int offset, int length) {
		ensureCapacity(length);
		for (int i = offset, n = i + length; i < n; i++) {
			add(array[i]);
		}
	}

	public void addAll(IdentitySet<T> set) {
		ensureCapacity(set.size);
		for (T key : set) {
			add(key);
		}
	}

	/** Skips checks for existing keys. */
	private void addResize(T key) {
		// Check for empty buckets.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mask;
		T key1 = keyTable[index1];
		if (key1 == null) {
			keyTable[index1] = key;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		int index2 = hash2(hashCode);
		T key2 = keyTable[index2];
		if (key2 == null) {
			keyTable[index2] = key;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		int index3 = hash3(hashCode);
		T key3 = keyTable[index3];
		if (key3 == null) {
			keyTable[index3] = key;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		push(key, index1, key1, index2, key2, index3, key3);
	}

	private void push(T insertKey, int index1, T key1, int index2, T key2, int index3, T key3) {
		T tempInsertKey = insertKey;
		int tempIndex1 = index1;
		T tempKey1 = key1;
		int tempIndex2 = index2;
		T tempKey2 = key2;
		int tempIndex3 = index3;
		T tempKey3 = key3;
		T[] keyTable = this.keyTable;
		int mask = this.mask;

		// Push keys until an empty bucket is found.
		T evictedKey;
		int i = 0, pushIterations = this.pushIterations;
		do {
			// Replace the key and value for one of the hashes.
			switch (MathUtils.random(2)) {
			case 0:
				evictedKey = tempKey1;
				keyTable[tempIndex1] = tempInsertKey;
				break;
			case 1:
				evictedKey = tempKey2;
				keyTable[tempIndex2] = tempInsertKey;
				break;
			default:
				evictedKey = tempKey3;
				keyTable[tempIndex3] = tempInsertKey;
				break;
			}

			// If the evicted key hashes to an empty bucket, put it there and stop.
			int hashCode = System.identityHashCode(evictedKey);
			tempIndex1 = hashCode & mask;
			tempKey1 = keyTable[tempIndex1];
			if (tempKey1 == null) {
				keyTable[tempIndex1] = evictedKey;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			tempIndex2 = hash2(hashCode);
			tempKey2 = keyTable[tempIndex2];
			if (tempKey2 == null) {
				keyTable[tempIndex2] = evictedKey;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			tempIndex3 = hash3(hashCode);
			tempKey3 = keyTable[tempIndex3];
			if (tempKey3 == null) {
				keyTable[tempIndex3] = evictedKey;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			if (++i == pushIterations) {
				break;
			}

			tempInsertKey = evictedKey;
		} while (true);

		addStash(evictedKey);
	}

	private void addStash(T key) {
		if (stashSize == stashCapacity) {
			// Too many pushes occurred and the stash is full, increase the table size.
			resize(capacity << 1);
			add(key);
			return;
		}
		// Store key in the stash.
		int index = capacity + stashSize;
		keyTable[index] = key;
		stashSize++;
		size++;
	}

	/** Returns true if the key was removed. */
	public boolean remove(T key) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key == keyTable[index]) {
			keyTable[index] = null;
			size--;
			return true;
		}

		index = hash2(hashCode);
		if (key == keyTable[index]) {
			keyTable[index] = null;
			size--;
			return true;
		}

		index = hash3(hashCode);
		if (key == keyTable[index]) {
			keyTable[index] = null;
			size--;
			return true;
		}

		return removeStash(key);
	}

	boolean removeStash(T key) {
		T[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				removeStashIndex(i);
				size--;
				return true;
			}
		}
		return false;
	}

	void removeStashIndex(int index) {
		// If the removed location was not last, move the last tuple to the removed location.
		stashSize--;
		int lastIndex = capacity + stashSize;
		if (index < lastIndex) {
			keyTable[index] = keyTable[lastIndex];
		}
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less,
	 * nothing is done. If the map contains more items than the specified capacity, the next highest power of two
	 * capacity is used instead.
	 */
	public void shrink(int maximumCapacity) {
		int tempMaximumCapacity = maximumCapacity;
		if (tempMaximumCapacity < 0) {
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + tempMaximumCapacity);
		}
		if (size > tempMaximumCapacity) {
			tempMaximumCapacity = size;
		}
		if (capacity <= tempMaximumCapacity) {
			return;
		}
		tempMaximumCapacity = MathUtils.nextPowerOfTwo(tempMaximumCapacity);
		resize(tempMaximumCapacity, 0);
	}

	/** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
	public void clear(int maximumCapacity) {
		if (capacity <= maximumCapacity) {
			clear();
			return;
		}
		size = 0;
		resize(maximumCapacity, 0);
	}

	@Override
	public void clear() {
		if (size == 0)
			return;
		T[] keyTable = this.keyTable;
		for (int i = capacity + stashSize; i-- > 0;) {
			keyTable[i] = null;
		}
		size = 0;
		stashSize = 0;
	}

	public boolean contains(T key) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key != keyTable[index]) {
			index = hash2(hashCode);
			if (key != keyTable[index]) {
				index = hash3(hashCode);
				if (key != keyTable[index]) {
					return containsKeyStash(key);
				}
			}
		}
		return true;
	}

	private boolean containsKeyStash(T key) {
		T[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				return true;
			}
		}
		return false;
	}

	public T first() {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			if (keyTable[i] != null) {
				return keyTable[i];
			}
		}
		throw new IllegalStateException("ObjectSet is empty.");
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity(int additionalCapacity) {
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded >= threshold) {
			resize(MathUtils.nextPowerOfTwo((int) (sizeNeeded / loadFactor)));
		}
	}

	private void resize(int newSize) {
		resize(newSize, 0.3f);
	}

	private void resize(int newSize, float maxDeviation) {
		int oldEndIndex = capacity + stashSize;

		capacity = newSize;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(newSize)) * 2);
		pushIterations = Math.max(Math.min(newSize, 8), (int) Math.sqrt(newSize) / 8);

		T[] oldKeyTable = keyTable;
		keyTable = Values.cast(PoolService.obtainArray(Object.class, newSize + stashCapacity, maxDeviation));

		int oldSize = size;
		size = 0;
		stashSize = 0;
		if (oldSize > 0) {
			for (int i = 0; i < oldEndIndex; i++) {
				T key = oldKeyTable[i];
				if (key != null) {
					addResize(key);
				}
			}
		}

		PoolService.free(oldKeyTable);
	}

	private int hash2(int h) {
		int temp = h * PRIME1;
		return (temp ^ temp >>> hashShift) & mask;
	}

	private int hash3(int h) {
		int temp = h * PRIME2;
		return (temp ^ temp >>> hashShift) & mask;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int hashCode() {
		int h = 0;
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			if (keyTable[i] != null) {
				h += System.identityHashCode(keyTable[i]);
			}
		}
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IdentitySet)) {
			return false;
		}
		IdentitySet<T> other = Values.cast(obj);
		if (other.size != size) {
			return false;
		}
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			if (keyTable[i] != null && !other.contains(keyTable[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return '{' + toString(", ") + '}';
	}

	public String toString(String separator) {
		if (size == 0) {
			return "";
		}
		StringBuilder buffer = new StringBuilder(32);
		T[] keyTable = this.keyTable;
		int i = keyTable.length;
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) {
				continue;
			}
			buffer.append(key);
			break;
		}
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) {
				continue;
			}
			buffer.append(separator);
			buffer.append(key);
		}
		return buffer.toString();
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported. Note that the same iterator instance is
	 * returned each time this method is called. Use the {@link IdentitySetIterator} constructor for nested or
	 * multithreaded iteration.
	 */
	@Override
	public IdentitySetIterator<T> iterator() {
		if (iterator1 == null) {
			iterator1 = new IdentitySetIterator<T>(this);
			iterator2 = new IdentitySetIterator<T>(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	@Override
	public void reset() {
		stashSize = 0;
		size = 0;
		if (keyTable.length > 64) {
			T[] oldKeyTable = keyTable;
			init(32, 0.8f);
			PoolService.free(oldKeyTable);
		}
	}

	static public <T> IdentitySet<T> with(T... array) {
		IdentitySet<T> set = new IdentitySet<T>();
		set.addAll(array);
		return set;
	}

	static public class IdentitySetIterator<K> implements Iterable<K>, Iterator<K> {
		public boolean hasNext;

		final IdentitySet<K> set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public IdentitySetIterator(IdentitySet<K> set) {
			this.set = set;
			reset();
		}

		public void reset() {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		void findNextIndex() {
			hasNext = false;
			K[] keyTable = set.keyTable;
			for (int n = set.capacity + set.stashSize; ++nextIndex < n;) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					break;
				}
			}
		}

		@Override
		public void remove() {
			if (currentIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			if (currentIndex >= set.capacity) {
				set.removeStashIndex(currentIndex);
				nextIndex = currentIndex - 1;
				findNextIndex();
			} else {
				set.keyTable[currentIndex] = null;
			}
			currentIndex = -1;
			set.size--;
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new GdxRuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		@Override
		public K next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new GdxRuntimeException("#iterator() cannot be used nested.");
			}
			K key = set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		@Override
		public IdentitySetIterator<K> iterator() {
			return this;
		}

		/** Adds the remaining values to the array. */
		public Array<K> toArray(Array<K> array) {
			while (hasNext) {
				array.add(next());
			}
			return array;
		}

		/** Returns a new array containing the remaining values. */
		public Array<K> toArray() {
			return toArray(new Array<K>(true, set.size));
		}
	}
}
