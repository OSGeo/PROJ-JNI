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
 * Wrappers around C++ shared pointer ({@code std::shared_ptr}).
 * {@code SharedPointer} can be {@linkplain IdentifiableObject#cleanWhenUnreachable() registered}
 * for automatic release of C++ shared pointer when an instance of another object is garbage collected.
 * The other object is usually an {@link IdentifiableObject}, but other objects could be used as well.
 * The navigation shall be in only one direction, from {@link IdentifiableObject} to {@code SharedPointer}.
 * See {@link java.lang.ref.Cleaner} for explanation about why this class shall not contains any reference
 * to the {@code IdentifiableObject}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class SharedPointer extends NativeResource implements Runnable {
    /**
     * Wraps the shared pointer at the given address.
     * A null pointer is assumed caused by a failure to allocate memory from C/C++ code.
     *
     * @param  ptr  pointer to the {@code std::shared_ptr}, or 0 if out of memory.
     * @throws OutOfMemoryError if {@code ptr} is 0.
     */
    SharedPointer(final long ptr) {
        super(ptr);
    }

    /**
     * Returns the number of dimensions of the wrapped object.
     * This method can be used with the following types:
     *
     * <ul>
     *   <li>{@code osgeo::proj::cs::CoordinateSystem}</li>
     *   <li>{@code osgeo::proj::crs::CRS}</li>
     * </ul>
     *
     * @return number of dimensions in wrapped object, or 0 if unknown.
     */
    final native int getDimension();

    /**
     * Returns the memory address of the PROJ object wrapped by this {@code NativeResource}.
     * This method is used for {@link IdentifiableObject#hashCode()} and
     * {@link IdentifiableObject#equals(Object)} implementations only.
     *
     * @return memory address of the wrapper PROJ object.
     */
    final native long rawPointer();

    /**
     * Returns a <cite>Well-Known Text</cite> (WKT) for this object.
     * This is allowed only if the wrapped PROJ object implements {@code osgeo::proj::io::IWKTExportable},
     * {@code osgeo::proj::io::IJSONExportable} or {@code osgeo::proj::io::IPROJStringExportable}, depending
     * on the convention used.
     *
     * @param  context     the thread context, or {@code null} if none.
     * @param  convention  ordinal value of the {@link ReferencingFormat.Convention} to use.
     * @param  indentation number of spaces for each indentation level, or -1 for the default value.
     * @param  multiline   whether the WKT will use multi-line layout.
     * @param  strict      whether to enforce strictly standard format.
     * @return the Well-Known Text (WKT) for this object, or {@code null} if the PROJ object
     *         does not implement the {@code osgeo::proj::io::IWKTExportable} interface.
     * @throws FormattingException if an error occurred during formatting.
     */
    final native String format(Context context, int convention, int indentation, boolean multiline, boolean strict);

    /**
     * Invoked by the cleaner thread when the {@link IdentifiableObject} has been garbage collected.
     * This method is invoked by the cleaner thread and shall never been invoked directly by us.
     * This implementation assumes that {@link #ptr} points to a C++ shared pointer.
     */
    @Override
    public native void run();
}
