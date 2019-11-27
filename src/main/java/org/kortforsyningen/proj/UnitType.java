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

import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;


/**
 * Maps {@code osgeo::proj::common::UnitOfMeasure::Type} to JSR-363 elements.
 * Enumeration values shall be declared in the same order than in C++ header.
 * The C++ code have some additional types that we can not map to JSR-363,
 * for example "parametric" (mapping would need a more precise type).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see UnitOfMeasure
 *
 * @since 1.0
 */
enum UnitType {
    /**
     * Unknown unit of measure.
     */
    UNKNOWN(null),

    /**
     * No unit of measure.
     */
    NONE(Dimensionless.class),

    /**
     * Angular unit of measure.
     */
    ANGULAR(Angle.class),

    /**
     * Linear unit of measure.
     */
    LINEAR(Length.class),

    /**
     * Scale unit of measure.
     */
    SCALE(Dimensionless.class),

    /**
     * Temporal unit of measure.
     */
    TIME(Time.class);

    /**
     * The type of quantity represented by the unit of measurement,
     * or {@code null} if unknown.
     */
    final Class<? extends Quantity<?>> type;

    /**
     * All values, fetched only once.
     */
    private static final UnitType[] VALUES = values();

    /**
     * Creates a new enumeration value.
     */
    private UnitType(final Class<? extends Quantity<?>> type) {
        this.type = type;
    }

    /**
     * Returns the unit type from the given ordinal value.
     */
    static UnitType get(final int ordinal) {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        } else {
            throw new NoUnitImplementationException();
        }
    }
}
