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

import javax.measure.Unit;
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
    UNKNOWN(null, null),

    /**
     * No unit of measure.
     */
    NONE(Dimensionless.class, null),

    /**
     * Angular unit of measure.
     */
    ANGULAR(Angle.class, UnitOfMeasure.RADIAN, UnitOfMeasure.DEGREE, UnitOfMeasure.ARC_SECOND,
            UnitOfMeasure.MICRORADIAN, UnitOfMeasure.GRAD),

    /**
     * Linear unit of measure.
     */
    LINEAR(Length.class, UnitOfMeasure.METRE),

    /**
     * Scale unit of measure.
     */
    SCALE(Dimensionless.class, UnitOfMeasure.SCALE_UNITY, UnitOfMeasure.PARTS_PER_MILLION),

    /**
     * Temporal unit of measure.
     */
    TIME(Time.class, UnitOfMeasure.SECOND, UnitOfMeasure.YEAR);

    /**
     * The type of quantity represented by the unit of measurement,
     * or {@code null} if unknown.
     */
    final Class<? extends Quantity<?>> type;

    /**
     * List of predefined units of this type, or {@code null} if none.
     * Values are {@link UnitOfMeasure} constants. The first item shall be the system unit,
     * and the remaining items should be ordered with most frequently-used units first.
     */
    private final short[] predefined;

    /**
     * All values, fetched only once.
     */
    private static final UnitType[] VALUES = values();

    /**
     * Creates a new enumeration value.
     *
     * @param  type        the type of quantity represented by the unit, or {@code null} if unknown.
     * @param  predefined  list of predefined units of this type, or {@code null} if none.
     */
    private UnitType(final Class<? extends Quantity<?>> type, final short... predefined) {
        this.type       = type;
        this.predefined = predefined;
    }

    /**
     * Returns the unit type from the given ordinal value.
     *
     * @param  ordinal  {@link #ordinal()} value of the desired {@link UnitType}.
     * @return the type for the given ordinal value.
     * @throws NoUnitImplementationException if the given ordinal is illegal. In this case we assume
     *         that caller actually requested an unit which is valid but which is not one of the units
     *         supported by our simple fallback implementation.
     */
    static UnitType get(final int ordinal) {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        } else {
            throw new NoUnitImplementationException();
        }
    }

    /**
     * Returns a unit of this type with the given scale factor, or {@code null}
     * if the caller should instantiate an {@link UnitOfMeasure} itself.
     *
     * @param  scale  scale factor from the desired unit to system unit of this type.
     * @return the unit, or {@code null} if caller should instantiate {@link UnitOfMeasure} itself.
     */
    final Unit<?> getDefinedUnit(final double scale) {
        if (predefined != null) {
            final double tolerance = Math.scalb(Math.ulp(scale), 5);        // Empirical scale factor.
            for (final short c : predefined) {
                if (Math.abs(scale - Units.getFactor(c)) <= tolerance) {
                    return Units.getUnit(c);
                }
            }
            Unit<?> unit = Units.getUnit(predefined[0]);
            if (!(unit instanceof UnitOfMeasure<?>)) {
                return unit.multiply(scale);
            }
            /*
             * If there is no JSR-363 implementation, we have to instantiate an UnitOfMeasure.
             * The above unit.multiply(scale) call would work, but the unit name would be null.
             * By letting the native code instantiate UnitOfMeasure itself, we give it a chance
             * to set the unit name in addition of the scale factor.
             */
        }
        return null;
    }
}
