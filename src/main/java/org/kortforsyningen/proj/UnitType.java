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

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.UnitConverter;
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
     * Ordinal value of the first enumeration value with a non-null {@link #predefined} array.
     */
    private static final int FIRST_DEFINED = 2;

    /**
     * The type of quantity represented by the unit of measurement,
     * or {@code null} if unknown.
     */
    final Class<? extends Quantity<?>> type;

    /**
     * List of predefined units of this type, or {@code null} if none.
     * Values are {@link UnitOfMeasure} constants. The first item shall be the system unit,
     * and the remaining items should be ordered with most frequently-used units first.
     *
     * @see #getSystemUnit()
     */
    private final short[] predefined;

    /**
     * Identifiers of customized units that are not in the {@link #predefined} units list.
     * This array can be expanded, but previous values shall not be modified.
     * Elements in this array shall be in strictly increasing order.
     */
    private int[] userDefinedUnitIdentifiers;

    /**
     * Factors of units in the {@link #userDefinedUnitIdentifiers} array.
     * This array can be expanded, but previous values shall not be modified.
     */
    private double[] userDefinedUnitFactors;

    /**
     * All values, fetched only once.
     */
    private static final UnitType[] VALUES = values();
    static {
        final int[]    emptyInt    = new int   [0];
        final double[] emptyDouble = new double[0];
        for (final UnitType t : VALUES) {
            t.userDefinedUnitIdentifiers = emptyInt;
            t.userDefinedUnitFactors =  emptyDouble;
        }
    }

    /**
     * Provides {@link UnitType} instances for a given quantity type.
     * This is similar to {@link #forOrdinal(int)} method, but using
     * a {@link Class} argument instead than an enumeration ordinal.
     */
    static final Map<Class<?>, UnitType> FOR_QUANTITY_TYPE;
    static {
        final HashMap<Class<?>, UnitType> m = new HashMap<>(8);
        m.put(Time.class, TIME);
        m.put(Angle.class, ANGULAR);
        m.put(Length.class, LINEAR);
        m.put(Dimensionless.class, SCALE);
        FOR_QUANTITY_TYPE = m;
    }

    /**
     * Returns the unit type from the given ordinal value.
     * This method is invoked by {@link UnitOfMeasure} constructor, which is a
     * fallback used when no JSR-363 implementation is found on the classpath.
     *
     * @param  ordinal  {@link #ordinal()} value of the desired {@link UnitType}.
     * @return the type for the given ordinal value.
     * @throws NoUnitImplementationException if the given ordinal is illegal. In this case we assume
     *         that caller actually requested an unit which is valid but which is not one of the units
     *         supported by our simple fallback implementation.
     */
    static UnitType forOrdinal(final int ordinal) {
        if (ordinal >= 0 && ordinal < VALUES.length) {
            return VALUES[ordinal];
        } else {
            throw new NoUnitImplementationException();
        }
    }

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
     * Returns the system unit. Conversion factors of all other units are relative to the system unit.
     *
     * @return the system unit for this unit type.
     */
    private Unit<?> getSystemUnit() {
        return Units.getUnit(predefined[0]);
    }

    /**
     * Returns a unit of this type with the given scale factor, or {@code null}
     * if the caller should instantiate an {@link UnitOfMeasure} itself.
     *
     * @param  scale  scale factor from the desired unit to system unit of this type.
     * @return the unit, or {@code null} if caller should instantiate {@link UnitOfMeasure} itself.
     */
    final Unit<?> getPredefinedUnit(final double scale) {
        if (predefined != null) {
            final double tolerance = Math.scalb(Math.ulp(scale), 5);        // Empirical scale factor.
            for (final short c : predefined) {
                if (Math.abs(scale - Units.getFactor(c)) <= tolerance) {
                    return Units.getUnit(c);
                }
            }
            final Unit<?> unit = getSystemUnit();
            if (unit != null && !(unit instanceof UnitOfMeasure<?>)) {
                // Do NOT invoke on UnitOfMeasure instance; it would create a never-ending loop.
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

    /**
     * If no predefined unit has been found, searches among customized units.
     * If no customized unit is found neither, adds the given scale factor to
     * the list of customized units.
     *
     * @param  scale  the conversion factor to system unit.
     * @return the identifier for the given conversion factor.
     */
    private synchronized int getUserDefinedUnitIdentifier(final double scale) {
        final int n = userDefinedUnitFactors.length;
        for (int i=n; --i >= 0;) {
            if (userDefinedUnitFactors[i] == scale) {
                return userDefinedUnitIdentifiers[i];
            }
        }
        if (Double.isFinite(scale)) {
            userDefinedUnitFactors     = Arrays.copyOf(userDefinedUnitFactors,     n+1);
            userDefinedUnitIdentifiers = Arrays.copyOf(userDefinedUnitIdentifiers, n+1);
            final int identifier       = Units.NEXT_IDENTIFIER.getAndIncrement();
            userDefinedUnitIdentifiers[n] = identifier;
            userDefinedUnitFactors[n] = scale;
            return identifier;
        }
        throw new IllegalArgumentException("Conversion factor shall be a real number.");
    }

    /**
     * Returns a unit identifier the given customized unit. This method shall be invoked
     * only when the caller determined that the given unit is not one of the predefined units.
     * This method returns the identifier of an existing unit if possible, or create a new
     * identifier otherwise.
     *
     * @param  <Q>   quantity type of the unit given in argument.
     * @param  unit  the unit for which to get an identifier.
     * @return identifier for the given unit, possibly generated.
     * @throws IllegalArgumentException if the given type is not supported.
     */
    static <Q extends Quantity<Q>> int getUserDefinedUnitIdentifier(final Unit<Q> unit) {
        final Unit<Q> base = unit.getSystemUnit();
        for (int i=FIRST_DEFINED; i<VALUES.length; i++) {
            final UnitType type = VALUES[i];
            if (base.isCompatible(type.getSystemUnit())) {
                final UnitConverter c = unit.getConverterTo(base);
                if (c.isLinear()) {
                    return type.getUserDefinedUnitIdentifier(c.convert(1));
                }
                break;
            }
        }
        throw new IllegalArgumentException("Unsupported unit of measure.");
    }

    /**
     * Returns the ordinal value of unit type together with the factor to system units
     * for the unit identified by the given number. This method is invoked from native
     * code; do not change method signature unless native code is updated accordingly.
     *
     * @param  identifier  the identifier for which to get unit type and scale.
     * @return unit type and scale in an array of length 2.
     *
     * @see NativeResource#getPredefinedUnit(int, double)
     */
    private static double[] getUserDefinedTypeAndScale(final int identifier) {
        for (final UnitType type : VALUES) {
            final int i = Arrays.binarySearch(type.userDefinedUnitIdentifiers, identifier);
            if (i >= 0) {
                return new double[] {type.ordinal(), type.userDefinedUnitFactors[i]};
            }
        }
        throw new IllegalArgumentException("Unknown unit identifier.");
    }
}
