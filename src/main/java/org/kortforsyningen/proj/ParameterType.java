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

import java.net.URI;


/**
 * Enumeration of {@code osgeo::proj::operation::OperationParameter::Type} values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
enum ParameterType {
    /**
     * Value with a unit given by {@link Parameter#doubleValue()}.
     */
    MEASURE(Double.class),

    /**
     * Character string given by {@link Parameter#stringValue()}.
     */
    STRING(String.class),

    /**
     * Integer given by {@link Parameter#intValue()}.
     */
    INTEGER(Integer.class),

    /**
     * Boolean given by {@link Parameter#booleanValue()}.
     */
    BOOLEAN(Boolean.class),

    /**
     * Filename given by {@link Parameter#valueFile()}.
     */
    FILENAME(URI.class);

    /**
     * The Java class for this parameter type.
     */
    final Class<?> type;

    /**
     * Creates a new enumeration value.
     *
     * @param  type  the Java class for this parameter type.
     */
    private ParameterType(final Class<?> type) {
        this.type = type;
    }

    /**
     * All values, fetched only once.
     */
    private static final ParameterType[] VALUES = values();

    /**
     * Returns the parameter type from the given ordinal value.
     *
     * @param  ordinal  {@link #ordinal()} value of the desired {@link ParameterType}.
     * @return the type for the given ordinal value (never {@code null}).
     * @throws UnsupportedOperationException if the specified type is unknown.
     *         It may happen if new type has been added in a new PROJ version,
     *         in which case we may need to upgrade PROJ-JNI accordingly.
     */
    static ParameterType get(final int ordinal) {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        } else {
            throw new UnsupportedOperationException("Unknown parameter type.");
        }
    }
}
