package com.gurella.engine.utils;

import static com.gurella.engine.utils.Values.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.StringBuilder;

//TODO poolable
public class IdentityObjectIntMap<K> implements Iterable<IdentityObjectIntMap.Entry<K>>, Container {
	private static final int PRIME1 = 0xb4b82e39;
	private static final int PRIME2 = 0xced1c241;

	public int size;

	K[] keyTable;
	int[] valueTable;
	int capacity, stashSize;

	float loadFactor;
	int hashShift, mask, threshold;
	int stashCapacity;
	int pushIterations;

	private Entries<K> entries1, entries2;
	private Values<K> values1, values2;
	private Keys<K> keys1, keys2;

	/**
	 * Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before
	 * growing the backing table.
	 */
	public IdentityObjectIntMap() {
		this(32, 0.8f);
	}

	/**
	 * Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the
	 * backing table.
	 */
	public IdentityObjectIntMap(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity *
	 * loadFactor items before growing the backing table.
	 */
	public IdentityObjectIntMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		}
		if (initialCapacity > 1 << 30) {
			throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
		}
		capacity = MathUtils.nextPowerOfTwo(initialCapacity);

		if (loadFactor <= 0) {
			throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
		}
		this.loadFactor = loadFactor;

		threshold = (int) (capacity * loadFactor);
		mask = capacity - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(capacity);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(capacity)) * 2);
		pushIterations = Math.max(Math.min(capacity, 8), (int) Math.sqrt(capacity) / 8);

		keyTable = cast(new Object[capacity + stashCapacity]);
		valueTable = new int[keyTable.length];
	}

	/** Creates a new map identical to the specified map. */
	public IdentityObjectIntMap(IdentityObjectIntMap<? extends K> map) {
		this(map.capacity, map.loadFactor);
		stashSize = map.stashSize;
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
	}

	public void put(K key, int value) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null.");
		}
		K[] keyTable = this.keyTable;

		// Check for existing keys.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mask;
		K key1 = keyTable[index1];
		if (key == key1) {
			valueTable[index1] = value;
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key == key2) {
			valueTable[index2] = value;
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key == key3) {
			valueTable[index3] = value;
			return;
		}

		// Update key in the stash.
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				valueTable[i] = value;
				return;
			}
		}

		// Check for empty buckets.
		if (key1 == null) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		if (key2 == null) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		if (key3 == null) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3);
	}

	public void putAll(IdentityObjectIntMap<K> map) {
		for (Entry<K> entry : map.entries()) {
			put(entry.key, entry.value);
		}
	}

	/** Skips checks for existing keys. */
	private void putResize(K key, int value) {
		// Check for empty buckets.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mask;
		K key1 = keyTable[index1];
		if (key1 == null) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key2 == null) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key3 == null) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) {
				resize(capacity << 1);
			}
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3);
	}

	private void push(K insertKey, int insertValue, int index1, K key1, int index2, K key2, int index3, K key3) {
		K tempInsertKey = insertKey;
		int tempInsertValue = insertValue;
		int tempIndex1 = index1;
		K tempKey1 = key1;
		int tempIndex2 = index2;
		K tempKey2 = key2;
		int tempIndex3 = index3;
		K tempKey3 = key3;

		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		int mask = this.mask;

		// Push keys until an empty bucket is found.
		K evictedKey;
		int evictedValue;
		int i = 0, pushIterations = this.pushIterations;
		do {
			// Replace the key and value for one of the hashes.
			switch (MathUtils.random(2)) {
			case 0:
				evictedKey = tempKey1;
				evictedValue = valueTable[tempIndex1];
				keyTable[tempIndex1] = tempInsertKey;
				valueTable[tempIndex1] = tempInsertValue;
				break;
			case 1:
				evictedKey = tempKey2;
				evictedValue = valueTable[tempIndex2];
				keyTable[tempIndex2] = tempInsertKey;
				valueTable[tempIndex2] = tempInsertValue;
				break;
			default:
				evictedKey = tempKey3;
				evictedValue = valueTable[tempIndex3];
				keyTable[tempIndex3] = tempInsertKey;
				valueTable[tempIndex3] = tempInsertValue;
				break;
			}

			// If the evicted key hashes to an empty bucket, put it there and stop.
			int hashCode = System.identityHashCode(evictedKey);
			tempIndex1 = hashCode & mask;
			tempKey1 = keyTable[tempIndex1];
			if (tempKey1 == null) {
				keyTable[tempIndex1] = evictedKey;
				valueTable[tempIndex1] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			tempIndex2 = hash2(hashCode);
			tempKey2 = keyTable[tempIndex2];
			if (tempKey2 == null) {
				keyTable[tempIndex2] = evictedKey;
				valueTable[tempIndex2] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			tempIndex3 = hash3(hashCode);
			tempKey3 = keyTable[tempIndex3];
			if (tempKey3 == null) {
				keyTable[tempIndex3] = evictedKey;
				valueTable[tempIndex3] = evictedValue;
				if (size++ >= threshold) {
					resize(capacity << 1);
				}
				return;
			}

			if (++i == pushIterations) {
				break;
			}

			tempInsertKey = evictedKey;
			tempInsertValue = evictedValue;
		} while (true);

		putStash(evictedKey, evictedValue);
	}

	private void putStash(K key, int value) {
		if (stashSize == stashCapacity) {
			// Too many pushes occurred and the stash is full, increase the table size.
			resize(capacity << 1);
			put(key, value);
			return;
		}
		// Store key in the stash.
		int index = capacity + stashSize;
		keyTable[index] = key;
		valueTable[index] = value;
		stashSize++;
		size++;
	}

	/**
	 * @param defaultValue
	 *            Returned if the key was not associated with a value.
	 */
	public int get(K key, int defaultValue) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key != keyTable[index]) {
			index = hash2(hashCode);
			if (key != keyTable[index]) {
				index = hash3(hashCode);
				if (key != keyTable[index]) {
					return getStash(key, defaultValue);
				}
			}
		}
		return valueTable[index];
	}

	private int getStash(K key, int defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				return valueTable[i];
			}
		}
		return defaultValue;
	}

	/**
	 * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue +
	 * increment is put into the map.
	 */
	public int getAndIncrement(K key, int defaultValue, int increment) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key != keyTable[index]) {
			index = hash2(hashCode);
			if (key != keyTable[index]) {
				index = hash3(hashCode);
				if (key != keyTable[index]) {
					return getAndIncrementStash(key, defaultValue, increment);
				}
			}
		}
		int value = valueTable[index];
		valueTable[index] = value + increment;
		return value;
	}

	private int getAndIncrementStash(K key, int defaultValue, int increment) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key == keyTable[i]) {
				int value = valueTable[i];
				valueTable[i] = value + increment;
				return value;
			}
		put(key, defaultValue + increment);
		return defaultValue;
	}

	public int remove(K key, int defaultValue) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mask;
		if (key == keyTable[index]) {
			keyTable[index] = null;
			int oldValue = valueTable[index];
			size--;
			return oldValue;
		}

		index = hash2(hashCode);
		if (key == keyTable[index]) {
			keyTable[index] = null;
			int oldValue = valueTable[index];
			size--;
			return oldValue;
		}

		index = hash3(hashCode);
		if (key == keyTable[index]) {
			keyTable[index] = null;
			int oldValue = valueTable[index];
			size--;
			return oldValue;
		}

		return removeStash(key, defaultValue);
	}

	int removeStash(K key, int defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				int oldValue = valueTable[i];
				removeStashIndex(i);
				size--;
				return oldValue;
			}
		}
		return defaultValue;
	}

	void removeStashIndex(int index) {
		// If the removed location was not last, move the last tuple to the removed location.
		stashSize--;
		int lastIndex = capacity + stashSize;
		if (index < lastIndex) {
			keyTable[index] = keyTable[lastIndex];
			valueTable[index] = valueTable[lastIndex];
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
		resize(tempMaximumCapacity);
	}

	/** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
	public void clear(int maximumCapacity) {
		if (capacity <= maximumCapacity) {
			clear();
			return;
		}
		size = 0;
		resize(maximumCapacity);
	}

	@Override
	public void clear() {
		if (size == 0) {
			return;
		}
		K[] keyTable = this.keyTable;
		for (int i = capacity + stashSize; i-- > 0;)
			keyTable[i] = null;
		size = 0;
		stashSize = 0;
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value,
	 * which may be an expensive operation.
	 */
	public boolean containsValue(int value) {
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = capacity + stashSize; i-- > 0;) {
			if (keyTable[i] != null && valueTable[i] == value) {
				return true;
			}
		}
		return false;

	}

	public boolean containsKey(K key) {
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

	private boolean containsKeyStash(K key) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and
	 * compares every value, which may be an expensive operation.
	 */
	public K findKey(int value) {
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = capacity + stashSize; i-- > 0;) {
			if (keyTable[i] != null && valueTable[i] == value) {
				return keyTable[i];
			}
		}
		return null;
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
		int oldEndIndex = capacity + stashSize;

		capacity = newSize;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(newSize)) * 2);
		pushIterations = Math.max(Math.min(newSize, 8), (int) Math.sqrt(newSize) / 8);

		K[] oldKeyTable = keyTable;
		int[] oldValueTable = valueTable;

		@SuppressWarnings("unchecked")
		K[] casted = (K[]) new Object[newSize + stashCapacity];
		keyTable = casted;
		valueTable = new int[newSize + stashCapacity];

		int oldSize = size;
		size = 0;
		stashSize = 0;
		if (oldSize > 0) {
			for (int i = 0; i < oldEndIndex; i++) {
				K key = oldKeyTable[i];
				if (key != null) {
					putResize(key, oldValueTable[i]);
				}
			}
		}
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
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h += System.identityHashCode(key) * 31;
				int value = valueTable[i];
				h += value;
			}
		}
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IdentityObjectIntMap)) {
			return false;
		}

		IdentityObjectIntMap<K> other = com.gurella.engine.utils.Values.cast(obj);
		if (other.size != size) {
			return false;
		}
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = 0, n = capacity + stashSize; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				int otherValue = other.get(key, 0);
				if (otherValue == 0 && !other.containsKey(key)) {
					return false;
				}
				int value = valueTable[i];
				if (otherValue != value) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		if (size == 0) {
			return "{}";
		}

		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) {
				continue;
			}
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) {
				continue;
			}
			buffer.append(", ");
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		buffer.append('}');
		return buffer.toString();
	}

	@Override
	public Entries<K> iterator() {
		return entries();
	}

	/**
	 * Returns an iterator for the entries in the map. Remove is supported. Note that the same iterator instance is
	 * returned each time this method is called. Use the {@link Entries} constructor for nested or multithreaded
	 * iteration.
	 */
	public Entries<K> entries() {
		if (entries1 == null) {
			entries1 = new Entries<K>(this);
			entries2 = new Entries<K>(this);
		}
		if (!entries1.valid) {
			entries1.reset();
			entries1.valid = true;
			entries2.valid = false;
			return entries1;
		}
		entries2.reset();
		entries2.valid = true;
		entries1.valid = false;
		return entries2;
	}

	/**
	 * Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is
	 * returned each time this method is called. Use the {@link Entries} constructor for nested or multithreaded
	 * iteration.
	 */
	public Values<K> values() {
		if (values1 == null) {
			values1 = new Values<K>(this);
			values2 = new Values<K>(this);
		}
		if (!values1.valid) {
			values1.reset();
			values1.valid = true;
			values2.valid = false;
			return values1;
		}
		values2.reset();
		values2.valid = true;
		values1.valid = false;
		return values2;
	}

	/**
	 * Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is
	 * returned each time this method is called. Use the {@link Entries} constructor for nested or multithreaded
	 * iteration.
	 */
	public Keys<K> keys() {
		if (keys1 == null) {
			keys1 = new Keys<K>(this);
			keys2 = new Keys<K>(this);
		}
		if (!keys1.valid) {
			keys1.reset();
			keys1.valid = true;
			keys2.valid = false;
			return keys1;
		}
		keys2.reset();
		keys2.valid = true;
		keys1.valid = false;
		return keys2;
	}

	static public class Entry<K> {
		public K key;
		public int value;

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}

	static private class MapIterator<K> {
		public boolean hasNext;

		final IdentityObjectIntMap<K> map;
		int nextIndex, currentIndex;
		boolean valid = true;

		public MapIterator(IdentityObjectIntMap<K> map) {
			this.map = map;
			reset();
		}

		public void reset() {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		void findNextIndex() {
			hasNext = false;
			K[] keyTable = map.keyTable;
			for (int n = map.capacity + map.stashSize; ++nextIndex < n;) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					break;
				}
			}
		}

		public void remove() {
			if (currentIndex < 0)
				throw new IllegalStateException("next must be called before remove.");
			if (currentIndex >= map.capacity) {
				map.removeStashIndex(currentIndex);
				nextIndex = currentIndex - 1;
				findNextIndex();
			} else {
				map.keyTable[currentIndex] = null;
			}
			currentIndex = -1;
			map.size--;
		}
	}

	static public class Entries<K> extends MapIterator<K> implements Iterable<Entry<K>>, Iterator<Entry<K>> {
		private Entry<K> entry = new Entry<K>();

		public Entries(IdentityObjectIntMap<K> map) {
			super(map);
		}

		/** Note the same entry instance is returned each time this method is called. */
		@Override
		public Entry<K> next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new GdxRuntimeException("#iterator() cannot be used nested.");
			}
			K[] keyTable = map.keyTable;
			entry.key = keyTable[nextIndex];
			entry.value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new GdxRuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		@Override
		public Entries<K> iterator() {
			return this;
		}

		@Override
		public void remove() {
			super.remove();
		}
	}

	static public class Values<V> extends MapIterator<V> {
		public Values(IdentityObjectIntMap<V> map) {
			super(map);
		}

		public boolean hasNext() {
			if (!valid) {
				throw new GdxRuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		public int next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new GdxRuntimeException("#iterator() cannot be used nested.");
			}
			int value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		/** Returns a new array containing the remaining values. */
		public IntArray toArray() {
			IntArray array = new IntArray(true, map.size);
			while (hasNext) {
				array.add(next());
			}
			return array;
		}
	}

	static public class Keys<K> extends MapIterator<K> implements Iterable<K>, Iterator<K> {
		public Keys(IdentityObjectIntMap<K> map) {
			super(map);
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
			K key = map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		@Override
		public Keys<K> iterator() {
			return this;
		}

		/** Returns a new array containing the remaining keys. */
		public Array<K> toArray() {
			Array<K> array = new Array<K>(true, map.size);
			while (hasNext) {
				array.add(next());
			}
			return array;
		}

		/** Adds the remaining keys to the array. */
		public Array<K> toArray(Array<K> array) {
			while (hasNext) {
				array.add(next());
			}
			return array;
		}

		@Override
		public void remove() {
			super.remove();
		}
	}
}
