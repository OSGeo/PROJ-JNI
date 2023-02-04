/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021-2023 Open Source Geospatial Foundation
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

import org.opengis.util.FactoryException;
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
 * @version 2.0
 * @since   1.0
 */
class SharedPointer extends NativeResource {
    /**
     * Wraps the shared pointer at the given address.
     * A null pointer is assumed caused by a failure to allocate memory from C/C++ code.
     *
     * @param  ptr  pointer to the {@code std::shared_ptr}, or 0 if out of memory.
     * @throws FactoryException if the PROJ object can not be allocated.
     */
    SharedPointer(final long ptr) throws FactoryException {
        super(ptr);
    }

    /**
     * Returns a property value as an object.
     *
     * @param  property  one of {@link Property#COORDINATE_SYSTEM}, <i>etc.</i> values.
     * @return value of the specified property, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native IdentifiableObject getObjectProperty(short property);

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
     * Returns a property value as an array of floating point number.
     *
     * @param  property  one of {@link Property#DOMAIN_OF_VALIDITY}, <i>etc.</i> values.
     * @return value of the specified property, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native double[] getArrayProperty(short property);

    /**
     * Returns a property value as an integer number.
     * This method can be used for enumeration values.
     *
     * @param  property  one of {@link Property#PARAMETER_TYPE}, <i>etc.</i> values.
     * @return value of the specified property, or {@link Double#NaN} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native int getIntegerProperty(short property);

    /**
     * Returns a property value as a boolean value.
     *
     * @param  property  one of {@link Property#IS_SPHERE}, <i>etc.</i> values.
     * @return value of the specified property, or {@code false} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native boolean getBooleanProperty(short property);

    /**
     * Returns the size of the identified property of kind {@code std::vector}.
     * If {@code property} is {@link Property#AXIS}, the returned value is the number of dimensions.
     *
     * @param  property  one of {@link Property#AXIS}, <i>etc.</i> values.
     * @return number of elements in the vector of the C++ structure.
     */
    final native int getVectorSize(short property);

    /**
     * Returns a property value as an object at the given index of a {@code std::vector}.
     *
     * @param  property  one of {@link Property#AXIS}, <i>etc.</i> values.
     * @param  index     index of the element to return.
     * @return value of the specified property at the given index, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    final native Object getVectorElement(short property, int index);

    /**
     * Returns a property value as an object for the given name.
     *
     * @param  property  one of {@link Property#OPERATION_PARAMETER}, <i>etc.</i> values.
     * @param  name      name of the element to return, case insensitive.
     * @return value of the specified property for the given name, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    final native IdentifiableObject searchVectorElement(short property, String name);

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
     * Returns an object with axis order such as the east direction is first and north direction is second,
     * if possible. This method can be applied on coordinate operations.
     *
     * @return an object with an axis order convenient for visualization.
     */
    final native IdentifiableObject normalizeForVisualization();

    /**
     * Returns a <cite>Well-Known Text</cite> (WKT) or other format for this object.
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
     * @throws UnformattableObjectException if an error occurred during formatting.
     */
    final native String format(Context context, int convention, int indentation, boolean multiline, boolean strict)
            throws UnformattableObjectException;

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
