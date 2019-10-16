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

import org.opengis.referencing.operation.NoninvertibleTransformException;


/**
 * Wrappers around C++ shared pointer ({@code std::shared_ptr}).
 * {@code SharedPointer} can be {@linkplain IdentifiableObject#releaseWhenUnreachable() registered}
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
class SharedPointer extends NativeResource {
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
     * Returns a property value as an object.
     *
     * @param  property  one of {@link Property#COORDINATE_SYSTEM}, <i>etc.</i> values.
     * @param  index     index of the element to return. Ignored if the property is not a vector.
     * @return value of the specified property, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native IdentifiableObject getObjectProperty(short property, int index);

    /**
     * Returns a property value as a string.
     *
     * @param  property  one of {@link Property#NAME_STRING}, <i>etc.</i> values.
     * @return value of the specified property, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native String getStringProperty(short property);

    /**
     * Returns a property value as a floating point number.
     *
     * @param  property  one of {@link Property#MINIMUM}, <i>etc.</i> values.
     * @return value of the specified property, or {@link Double#NaN} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native double getNumericProperty(short property);

    /**
     * Returns the size of the identified property.
     * If {@code property} is {@link Property#AXIS}, the returned value is the number of dimensions.
     *
     * @param  property  one of {@link Property#IDENTIFIER}, <i>etc.</i> values.
     * @return number of elements in the vector of the C++ structure.
     */
    final native int getPropertySize(short property);

    /**
     * Creates the inverse of the wrapped operation.
     * This method can be used with the following types:
     *
     * <ul>
     *   <li>{@code osgeo::proj::operation::CoordinateOperation}</li>
     * </ul>
     *
     * @return inverse operation.
     * @throws NoninvertibleTransformException if the inverse transform can not be computed.
     */
    final native IdentifiableObject inverse() throws NoninvertibleTransformException;

    /**
     * Returns a <cite>Well-Known Text</cite> (WKT) for this object.
     * This method can be used with the following types:
     *
     * <ul>
     *   <li>{@code osgeo::proj::io::IWKTExportable}</li>
     *   <li>{@code osgeo::proj::io::IJSONExportable}</li>
     *   <li>{@code osgeo::proj::io::IPROJStringExportable}</li>
     * </ul>
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
     * Compares this object with the given object for equality.
     * Note: we do not use this method for {@link #equals(Object)} implementation
     * because we can not guarantee consistency with hash code values.
     *
     * @param  other      the other object to compare with this object.
     * @param  criterion  a {@link ComparisonCriterion} ordinal value.
     * @return whether the two objects are equal.
     */
    final native boolean isEquivalentTo(SharedPointer other, int criterion);

    /**
     * Returns the memory address of the PROJ object wrapped by this {@code NativeResource}.
     * This method is used for {@link IdentifiableObject#hashCode()} and
     * {@link IdentifiableObject#equals(Object)} implementations only.
     *
     * @return memory address of the wrapper PROJ object.
     */
    final native long rawPointer();

    /**
     * Invoked by the cleaner thread when the {@link IdentifiableObject} has been garbage collected.
     * This method is invoked by the cleaner thread and shall never been invoked directly by us.
     * This implementation assumes that {@link #ptr} points to a C++ shared pointer.
     */
    native void release();
}
