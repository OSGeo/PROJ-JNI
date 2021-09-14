/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021 Open Source Geospatial Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.osgeo.proj;

import java.util.Arrays;
import java.lang.ref.SoftReference;
import java.util.concurrent.locks.StampedLock;


/**
 * A map of pointer values to Java wrappers retained by soft references.
 * This class is convenient for avoiding the creation of duplicated elements,
 * as in the example below:
 *
 * <pre>{@code
 *     long pointer = ...
 *     IdentifiableObject wrapper = CACHE.get(pointer);
 *     if (wrapper == null) {
 *         wrapper = ...;                           // Create the value here.
 *         IdentifiableObject existing = CACHE.putIfAbsent(pointer, wrapper);
 *         if (existing != null) {                  // Created concurrently.
 *             wrapper.release();
 *             wrapper = existing;
 *         }
 *     }
 * }</pre>
 *
 * <h2>Design note</h2>
 * We use soft references instead than weak references because PROJ-JNI does not retain hard reference
 * to components. For example {@link CRS} does not have a hard reference to its {@link CS} component.
 * If we were using weak references, the component wrapper could be recreated almost every time the
 * {@link CRS#getCoordinateSystem()} method is invoked.
 *
 * <p>This class extends {@link StampedLock} for implementation convenience only.
 * We allow ourself to do that because this class is not public.
 * Callers should not rely on this implementation detail.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 */
@SuppressWarnings("serial")
final class SharedObjects extends StampedLock implements Runnable {
    /**
     * Number of nanoseconds to wait before to rehash the table for reducing its size.
     * When the garbage collector collects a lot of elements, we will wait at least this amount of time
     * before rehashing the tables, in case lot of news elements are going to be added. Without this
     * field, we noticed many "reduce", "expand", "reduce", "expand", <i>etc.</i> cycles.
     */
    private static final long REHASH_DELAY = 4000_000_000L;             // 4 seconds.

    /**
     * Map capacities as prime numbers close to powers of 2.
     */
    private static final int[] CAPACITIES = {61, 127, 257, 509, 1021, 2053, 4093, 8191, 16381, 32771, 65537};

    /**
     * An entry in the {@link SharedObjects}. This is a soft reference
     * to the Java wrapper together with the pointer value.
     */
    static final class Entry extends SoftReference<IdentifiableObject> {
        /**
         * Raw memory address of the native object. This is not the memory address of the shared pointer.
         * This value is used only for identifying if the value pointed by a shared pointer is the same
         * than the value pointed by another shared pointer.
         */
        final long key;

        /**
         * The next entry, or {@code null} if there is none.
         * This is used when more than one entry has the same hash code value.
         */
        private Entry next;

        /**
         * The object containing the code for releasing native resource.
         */
        final SharedPointer cleaner;

        /**
         * Constructs a new soft reference.
         *
         * @param  key    raw memory address of the native object.
         * @param  value  the object for which to release native resource after garbage collection.
         * @param  next   next entry having the same hash value, or {@code null} if there is none.
         */
        private Entry(final long key, final IdentifiableObject value, final Entry next) {
            super(value, CleanerThread.QUEUE);
            this.key     = key;
            this.next    = next;
            this.cleaner = value.impl;
        }

        /**
         * Removes this entry from the given table of entries at the given index.
         * This method should be invoked only after the soft reference has been cleaned.
         *
         * @param  table     the table from which to remove this entry.
         * @param  removeAt  the index of this entry in the given table.
         * @return {@code true} if this entry has been found and removed, or {@code false} otherwise.
         */
        private boolean removeFrom(final Entry[] table, final int removeAt) {
            Entry prev = null;
            Entry e = table[removeAt];
            while (e != null) {
                if (e == this) {
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        table[removeAt] = e.next;
                    }
                    // We can not continue the loop pass that point, since `e` is no longer valid.
                    return true;
                }
                prev = e;
                e = e.next;
            }
            return false;
        }
    }

    /**
     * Table of soft references.
     */
    private Entry[] table;

    /**
     * Number of non-null elements in {@link #table}.
     * This is used for determining when {@link #rehash(Entry[], int)} needs to be invoked.
     */
    private int count;

    /**
     * The last time when {@link #table} was not in need for rehash. When the garbage collector
     * collected a lot of elements, we will wait a few seconds before rehashing {@link #table}
     * in case lot of news entries are going to be added. Without this field, we noticed many
     * "reduce", "expand", "reduce", "expand", <i>etc.</i> cycles.
     */
    private transient long lastTimeNormalCapacity;

    /**
     * The unique instance of {@link SharedObjects}.
     */
    static final SharedObjects CACHE = new SharedObjects();

    /**
     * Creates a map of shared objects.
     */
    private SharedObjects() {
        lastTimeNormalCapacity = System.nanoTime();
        table = new Entry[CAPACITIES[0]];
    }

    /**
     * If the number of elements is less than this threshold,
     * then the table should be rehashed for saving space.
     *
     * @param  capacity  the table capacity.
     * @return minimal number of elements for not rehashing.
     */
    private static int lowerCapacityThreshold(final int capacity) {
        return capacity >>> 2;
    }

    /**
     * If the number of elements is greater than this threshold,
     * then the table should be rehashed for better performance.
     *
     * @param  capacity  the table capacity.
     * @return maximal number of elements for not rehashing.
     */
    private static int upperCapacityThreshold(final int capacity) {
        return capacity - (capacity >>> 2);
    }

    /**
     * Returns a hash value for the given pointer when used in a table of the given capacity.
     *
     * @param  key       the pointer value.
     * @param  capacity  the hash table capacity.
     * @return index in the table of given capacity.
     */
    private static int hash(final long key, final int capacity) {
        return (int) ((key & Long.MAX_VALUE) % capacity);
    }

    /**
     * Rehashes the given table.
     *
     * @param  oldTable  the table to rehash.
     * @param  count     number of elements in the table (including chained elements).
     * @return the new table array, or {@code oldTable} if no rehash were needed.
     */
    private static Entry[] rehash(final Entry[] oldTable, final int count) {
        /*
         * Compute the capacity as twice the expected number of elements, then take
         * (if possible) the closest prime number. This is based on classical books
         * saying that prime values reduce the risk of key collisions.
         */
        int capacity = count * 2;
        int i = ~Arrays.binarySearch(CAPACITIES, capacity);
        if (i >= 0 && i < CAPACITIES.length) {
            if (i != 0 && capacity - CAPACITIES[i-1] < CAPACITIES[i] - capacity) {
                i--;
            }
            capacity = CAPACITIES[i];
        }
        if (capacity == oldTable.length) {
            return oldTable;
        }
        /*
         * Rehash the table.
         */
        final Entry[] table = new Entry[capacity];
        for (Entry next : oldTable) {
            while (next != null) {
                final Entry e = next;
                next = next.next;                       // Fetch `next` now because its value will change.
                final int index = hash(e.key, capacity);
                e.next = table[index];
                table[index] = e;
            }
        }
        return table;
    }

    /**
     * Invoked by {@link CleanerThread} when an element has been collected by the garbage collector.
     * This method removes the soft reference from the {@link #table}. It is caller's responsibility
     * to invoke {@link SharedPointer#release()} if invoked from {@link CleanerThread}.
     *
     * @param  toRemove  the entry to remove from this map.
     */
    final void remove(final Entry toRemove) {
        final long stamp = writeLock();
        try {
            removeUnderLock(toRemove);
        } finally {
            unlockWrite(stamp);
        }
    }

    /**
     * Implementation of {@link #remove(Entry)} invoked when the caller already has a lock.
     * This variant is required because {@link StampedLock} is not re-entrant.
     * This method does nothing if the given entry is not found.
     *
     * @param  toRemove  the entry to remove from this map.
     */
    private void removeUnderLock(final Entry toRemove) {
        assert isWriteLocked();
        final int capacity = table.length;
        if (toRemove.removeFrom(table, hash(toRemove.key, capacity))) {
            if (--count < lowerCapacityThreshold(capacity)) {
                final long currentTime = System.nanoTime();
                if (currentTime - lastTimeNormalCapacity > REHASH_DELAY) {
                    table = rehash(table, count);
                    lastTimeNormalCapacity = currentTime;
                }
            }
            assert isValid();
        }
    }

    /**
     * Checks if this {@code SharedObjects} is valid. This method counts the number of elements
     * and compares it to {@link #count}. This method is invoked in assertions only.
     *
     * @return whether {@link #count} matches the expected value.
     */
    private boolean isValid() {
        if (count >= table.length) {
            throw new AssertionError(count);
        }
        int n = 0;
        for (Entry e : table) {
            while (e != null) {
                n++;
                e = e.next;
            }
        }
        return n == count;
    }

    /**
     * Returns the value to which this map maps the specified key.
     * Returns {@code null} if the map contains no mapping for this key.
     * Null keys are considered never present.
     *
     * @param  key  key whose associated value is to be returned.
     * @return the value to which this map maps the specified key.
     */
    final IdentifiableObject get(final long key) {
        /*
         * First, try to get the entry without acquiring a lock. We look only at the entry
         * found directly in the table; we do not follow the chain of entries because they
         * may be inconsistent.
         */
        long stamp = tryOptimisticRead();
        if (stamp != 0) {
            final Entry[] t = table;                    // Protect from concurrent change.
            final Entry e = t[hash(key, t.length)];
            if (validate(stamp)) {
                if (e == null) {
                    return null;
                } else if (e.key == key) {
                    return e.get();
                }
            }
        }
        /*
         * If the optimistic read did not worked, perform the "real" read here.
         */
        stamp = readLock();
        try {
            final int index = hash(key, table.length);
            for (Entry e = table[index]; e != null; e = e.next) {
                if (e.key == key) {
                    return e.get();
                }
            }
        } finally {
            unlockRead(stamp);
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map if no value were previously associated.
     * If an other value is already associated to the given key, then the map is left unchanged and the current
     * value is returned. Otherwise the specified value is associated to the key using a {@link SoftReference}
     * and {@code null} is returned.
     *
     * @param  key    key with which the specified value is to be associated.
     * @param  value  value to be associated with the specified key.
     * @return the current value associated with specified key, or {@code null} if there was no mapping for key.
     */
    final IdentifiableObject putIfAbsent(final long key, final IdentifiableObject value) {
        final long stamp = writeLock();
        try {
            int index = hash(key, table.length);
            for (Entry e = table[index]; e != null; e = e.next) {
                if (e.key == key) {
                    final IdentifiableObject oldValue = e.get();
                    if (oldValue != null) {
                        return oldValue;
                    }
                    removeUnderLock(e);
                    index = hash(key, table.length);
                }
            }
            if (++count >= lowerCapacityThreshold(table.length)) {
                if (count > upperCapacityThreshold(table.length)) {
                    table = rehash(table, count);
                    index = hash(key, table.length);
                }
                lastTimeNormalCapacity = System.nanoTime();
            }
            table[index] = new Entry(key, value, table[index]);
            assert isValid();
        } finally {
            unlockWrite(stamp);
        }
        return null;
    }

    /**
     * Invoked at JVM shutdown time for releasing all shared pointers,
     * then destroying all {@code PJ_CONTEXT} instances.
     */
    @Override
    public void run() {
        boolean found;
        do {
            final Entry[] entries;
            final long stamp = writeLock();
            try {
                entries = table;
                table = new Entry[3];       // In case some write operation continue concurrently (but should not happen).
            } finally {
                unlockWrite(stamp);
            }
            found = false;
            for (Entry e : entries) {
                while (e != null) {
                    e.cleaner.release();
                    e = e.next;
                    found = true;
                }
            }
        } while (found);
        Context.destroyAll();
    }

    /**
     * Register a shutdown hook for releasing all PROJ objects on shutdown.
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(CACHE, "PROJ-JNI shutdown"));
    }
}
