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
 * Base class of all wrappers around PROJ objects referenced by a shared pointer.
 * The native PROJ resource is not hold directly in this class, but indirectly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
abstract class SharedObject {
    /**
     * Manager of objects having native resources to be released after the Java object has been garbage-collected.
     * This manager will decrement the reference count of the shared pointer.
     */
    private static final Cleaner DISPOSER = Cleaner.create(SharedObject::cleanerThread);

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
     * Task invoked when the enclosing {@link SharedObject} instance has been garbage-collected.
     * {@code Implementation} shall not contain any reference to the enclosing {@code SharedObject}.
     * See {@link Cleaner} for explanation about why this separation is required.
     */
    static final class Implementation extends NativeResource implements Runnable {
        /**
         * The pointer to PROJ structure allocated in the C/C++ heap. This value has no meaning in Java code.
         * <strong>Do not modify</strong>, since this value is required for using PROJ. Do not rename neither,
         * unless potential usage of this field is also verified in the C/C++ source code.
         */
        private final long ptr;

        /**
         * Creates a wrapper for the PROJ object referenced by the given pointer.
         *
         * @param  ptr  pointer to the PROJ structure to wrap.
         */
        Implementation(final long ptr) {
            this.ptr = ptr;
        }

        /**
         * Invoked by the cleaner thread when the enclosing {@link SharedObject} is no longer reachable.
         * This method shall never been invoked by us.
         */
        @Override
        public void run() {
            release(ptr);
        }
    }

    /**
     * Decrements the references count of the given shared pointer. This method is invoked automatically
     * by the default {@link Implementation} implementation when this {@link SharedObject} is garbage collected.
     * It may also be invoked if an error occurred at construction time before a disposer is registered.
     * No reference to the disposed {@link SharedObject} shall exist after this method call.
     *
     * @param  ptr  the pointer to the wrapped PROJ structure.
     */
    private static native void release(long ptr);

    /**
     * Provides access to the PROJ implementation.
     */
    final Implementation impl;

    /**
     * Creates a wrapper for the given pointer to a PROJ structure. If the given pointer is null,
     * then this constructor assumes that the PROJ object allocation failed because of out of memory.
     *
     * @param  ptr  pointer to the PROJ structure to wrap, or 0 if memory allocation failed.
     * @throws OutOfMemoryError if the given {@code ptr} is null.
     */
    SharedObject(final long ptr) {
        if (ptr == 0) {
            throw new OutOfMemoryError("Can not allocate PROJ object.");
        }
        try {
            impl = new Implementation(ptr);
            DISPOSER.register(this, impl);
        } catch (Throwable e) {
            release(ptr);
            throw e;
        }
    }

    /**
     * Returns a <cite>Well-Known Text</cite> (WKT) for this object.
     * This method can be invoked only if the wrapped PROJ object is
     * an instance of {@code osgeo::proj::io::IWKTExportable}.
     *
     * @return the Well-Known Text (WKT) for this object.
     * @throws UnsupportedOperationException if this object can not be formatted as WKT.
     * @throws FormattingException if an error occurred during formatting.
     */
    public String toWKT() {
        final String wkt = impl.toWKT(0, true, true);
        if (wkt != null) {
            return wkt;
        } else {
            throw new UnsupportedOperationException("This object is not exportable to WKT.");
        }
    }

    /**
     * Returns a simplified <cite>Well-Known Text</cite> (WKT) for this object,
     * or an arbitrary string if this object can not be formatted in WKT.
     *
     * @return string representation of this object.
     */
    @Override
    public String toString() {
        try {
            final String wkt = impl.toWKT(2, true, false);
            if (wkt != null) {
                return wkt;
            }
        } catch (FormattingException e) {
            return e.toString();
        }
        return super.toString();
    }
}
