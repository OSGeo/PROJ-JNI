/*
 * Copyright © 2019 Agency for Data Supply and Efficiency
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
package org.kortforsyningen.proj;

import java.lang.ref.Cleaner;


/**
 * Base class of all wrappers around PROJ objects referenced by a raw pointer.
 * The type of pointer (and consequently the action to take at disposal time)
 * depends on the subclass: it may be a pointer to a traditional C/C++ object
 * allocated by {@code malloc(…)}, or it may be a C++ shared pointer.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
abstract class ObjectReference {
    /**
     * Manager of objects having native resources to be released after the Java object has been garbage-collected.
     * This manager will invoke a {@code proj_xxx_destroy(…)} method where <var>xxx</var> depends on the resource
     * which has been registered, or decrement the reference count of a shared pointer.
     */
    private static final Cleaner DISPOSER = Cleaner.create(ObjectReference::cleanerThread);

    /**
     * Creates a new thread for disposing PROJ objects after their Java wrappers have been garbage-collected.
     * We create a thread ourselves mostly for specifying a more explicit name than the default name.
     *
     * @param  cleaner  provided by {@link Cleaner}.
     * @return the thread to use for disposing PROJ objects.
     */
    private static Thread cleanerThread(final Runnable cleaner) {
        final Thread t = new Thread(cleaner);
        t.setPriority(Thread.MAX_PRIORITY - 2);
        t.setName("PROJ objects disposer");
        return t;
    }

    /**
     * The pointer to PROJ structure allocated in the C/C++ heap. This value has no meaning in Java code.
     * <strong>Do not modify</strong>, since this value is required for using PROJ. Do not rename neither,
     * unless potential usage of this field is also verified in the C/C++ source code.
     */
    final long ptr;

    /**
     * Task invoked when the enclosing {@link ObjectReference} instance has been garbage-collected.
     * {@code Disposer} shall not contain any reference to the enclosing {@code ObjectReference}.
     * Instead, this class holds a copy of the {@link ObjectReference#ptr} value.
     * See {@link Cleaner} for explanation about why this separation is required.
     *
     * <p>The default implementation assumes that {@link #ptr} is a shared pointer and that releasing
     * the object consists in decrementing the references count. If {@link #ptr} is a different kind
     * of pointer, then the {@link #run()} method must be overridden.</p>
     */
    static class Disposer implements Runnable {
        /**
         * Pointer to PROJ structure to release after the enclosing object has been garbage collected.
         * This is a copy of {@link ObjectReference#ptr}. Releasing is done by calling {@link #run()}.
         */
        final long ptr;

        /**
         * Creates a disposer for the PROJ object referenced by the given pointer.
         * The type of pointer (classical or shared) depends on the class of this {@code Disposer}.
         *
         * @param  ptr  copy of {@link ObjectReference#ptr} value.
         */
        Disposer(final long ptr) {
            this.ptr = ptr;
        }

        /**
         * Invoked by the cleaner thread when the enclosing {@link ObjectReference} is no longer reachable.
         * This method shall never been invoked by us. The default implementation assumes that {@link #ptr}
         * is a shared pointer and decrement the references count. If {@link #ptr} is another kind of pointer,
         * then this method must be overridden.
         */
        @Override
        public void run() {
            release(ptr);
        }
    }

    /**
     * Decrements the references count of the given shared pointer. This method is invoked automatically by
     * the default {@link Disposer} implementation when this {@link ObjectReference} is garbage collected.
     * This method will be ignored if this {@link ObjectReference} uses another disposal mechanism.
     *
     * @param  ptr  the shared pointer allocated by PROJ.
     */
    private static native void release(long ptr);

    /**
     * Creates a wrapper for the given pointer to a PROJ structure. If the given pointer is null,
     * then this constructor assumes that the PROJ object allocation failed because of out of memory.
     *
     * <p>If {@code isShared} is true, then this constructor automatically register a handler for
     * decrementing the references count after this {@link ObjectReference} is garbage collected.
     * Otherwise the caller is responsible for invoking {@link #onDispose(Disposer)} after construction.</p>
     *
     * @param  ptr       pointer to the PROJ structure to wrap.
     * @param  isShared  whether the given pointer is a shared pointer.
     * @throws OutOfMemoryError if the given {@code ptr} is null.
     */
    ObjectReference(final long ptr, final boolean isShared) {
        this.ptr = ptr;
        if (ptr == 0) {
            throw new OutOfMemoryError();
        }
        if (isShared) try {
            onDispose(new Disposer(ptr));
        } catch (Throwable e) {
            release(ptr);
            throw e;
        }
    }

    /**
     * Sets the handler to invoke when this {@link ObjectReference} is garbage collected.
     * This method shall be invoked at most once during construction in a code like below:
     *
     * <pre>
     * try {
     *     onDispose(new Disposer(ptr));
     * } catch (Throwable e) {
     *     release(ptr);
     *     throw e;
     * }
     * </pre>
     *
     * This method shall not be invoked after {@link #ObjectReference(long, boolean)} if the
     * {@code isShared} flag was {@code true} because that constructor has automatically
     * invoked this method in such case.
     *
     * @param  handler  a task to execute when this {@link ObjectReference} is garbage-collected.
     */
    final void onDispose(final Disposer handler) {
        DISPOSER.register(this, handler);
    }
}
