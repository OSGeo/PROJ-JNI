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
 * Wrapper for {@code PJ_CONTEXT}, the PROJ threading-context.
 * We need one threading-context per thread used by the program.
 * This ensures that all {@code PJ} objects created in the same context
 * will be sharing resources such as error-numbers and loaded grids.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Context {
    /**
     * Manager of objects having native resources to be released after the Java object has been garbage-collected.
     * This manager will invoke a {@code proj_xxx_destroy(…)} method where <var>xxx</var> depends on the resource
     * which has been registered.
     */
    static final Cleaner DISPOSER = Cleaner.create(Context::cleanerThread);

    /**
     * Creates a new thread for disposing PROJ objects after their Java wrappers have been garbage-collected.
     * Only one thread should be created. We create a thread ourselves mostly for specying a more explicit name
     * than the default name.
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
     * {@code PJ_CONTEXT} instances associated to each Java thread
     * that needed to perform at least one PROJ operation.
     */
    private static final ThreadLocal<Context> CONTEXTS = ThreadLocal.withInitial(Context::new);

    /**
     * The pointer to {@code PJ_CONTEXT} structure allocated in the C/C++ heap.
     * This value has no meaning in Java code. <strong>Do not modify</strong>, since this value is used by PROJ.
     */
    private final long ptr;

    /**
     * Task invoked when the enclosing {@link Context} object has been garbage-collected.
     * This object shall not contain any reference to enclosing {@code Context}.
     * Instead, the {@link Context#ptr} value is copied.
     */
    private static final class Disposer implements Runnable {
        /**
         * Pointer to {@code PJ_CONTEXT}.
         */
        private final long ptr;

        /**
         * Creates a disposer for the given PROJ object.
         *
         * @param  ptr  copy of {@link Context#ptr} value.
         */
        Disposer(final long ptr) {
            this.ptr = ptr;
        }

        /**
         * Invoked by the cleaner thread when the enclosing {@link Context} is no longer reachable.
         */
        @Override
        public void run() {
            destroy(ptr);
        }
    }

    /**
     * Creates and wraps a new {@code PJ_CONTEXT}.
     */
    private Context() {
        ptr = create();
        if (ptr == 0) {
            throw new OutOfMemoryError();
        }
        try {
            DISPOSER.register(this, new Disposer(ptr));
        } catch (Throwable e) {
            destroy(ptr);
            throw e;
        }
    }

    /**
     * Invokes the C/C++ {@code proj_context_create()} method.
     * It is caller's responsibility to verify that the returned value is non-null.
     *
     * @return pointer to the {@code PJ_CONTEXT} allocated by PROJ, or 0 if out of memory.
     */
    private static native long create();

    /**
     * Invokes the C/C++ {@code proj_context_destroy(…)} method.
     * This method shall be invoked exactly once when {@link Context} is garbage collected.
     *
     * @param  ptr  pointer to the {@code PJ_CONTEXT} allocated by PROJ.
     */
    private static native void destroy(long ptr);

    /**
     * Gets the PROJ context of current thread.
     *
     * @return  pointer to the {@code PJ_CONTEXT} structure in PROJ heap.
     */
    static long current() {
        return CONTEXTS.get().ptr;
    }
}
