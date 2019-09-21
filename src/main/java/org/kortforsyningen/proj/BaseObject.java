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


/**
 * Base class of all objects wrapping a PROJ {@code osgeo::proj::util::BaseObject}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class BaseObject {
    /**
     * The pointer to {@code osgeo::proj::util::BaseObject} allocated in the C/C++ heap.
     * This value has no meaning in Java code. <strong>Do not modify</strong>, since this value is used by PROJ.
     */
    private final long ptr;

    /**
     * Task invoked when the enclosing {@link BaseObject} object has been garbage-collected.
     * This object shall not contain any reference to enclosing {@code PJ}.
     * Instead, the {@link BaseObject#ptr} value is copied.
     */
    private static final class Disposer implements Runnable {
        /** Pointer to C++ BaseObject. */
        private final long ptr;

        Disposer(final long ptr) {this.ptr = ptr;}
        @Override public void run() {destroy(ptr);}
    }

    /**
     * Creates a new wrapper for the given {@code osgeo::proj::util::BaseObject}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    BaseObject(final long ptr) {
        this.ptr = ptr;
        try {
            Context.DISPOSER.register(this, new Disposer(ptr));
        } catch (Throwable e) {
            destroy(ptr);
            throw e;
        }
    }

    /**
     * Decrements the references count of the specified {@code osgeo::proj::util::BaseObject}.
     * This method shall be invoked exactly once when {@link BaseObject} is garbage collected.
     *
     * @param  ptr  pointer to the {@code BaseObject} allocated by PROJ.
     */
    private static native void destroy(long ptr);
}
