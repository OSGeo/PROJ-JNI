/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency 
 * Copyright © 2021 Open Source Geospatial Foundation
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

import java.util.Objects;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import javax.measure.quantity.Dimensionless;
import javax.measure.spi.ServiceProvider;
import javax.measure.spi.SystemOfUnits;
import javax.measure.spi.SystemOfUnitsService;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A set of predefined units of measurements used by PROJ.
 *
 * <p>Units of measurement are represented by the standard {@link Unit} interface, which is implemented by
 * <a href="https://jcp.org/aboutJava/communityprocess/implementations/jsr363/index.html">external libraries</a>.
 * PROJ-JNI uses whichever JSR-363 implementation is found on the classpath at the time this {@link Units} class is
 * initialized. If such implementation is found, then the constants in this class ({@link #DEGREE}, {@link #METRE},
 * {@link #SECOND}, <i>etc.</i>) are references to {@link Unit} instances provided by that implementation.
 * Otherwise those constants are references to an internal fallback implementation with limited capability.
 *
 * <p>Providing a JSR-363 implementation is optional but recommended if operations such as unit arithmetic
 * (e.g. <code>METRE.{@linkplain Unit#divide(Unit) divide}(SECOND)</code>) and unit conversions
 * (e.g. <code>GRAD.{@linkplain Unit#getConverterTo(Unit) getConverterTo}(DEGREE)</code>) are to be performed.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final class Units {
    /**
     * The international system of units, or {@code null} if none was found.
     *
     * <p>No implementation of this class is provided by PROJ-JNI; it has to be
     * provided by an external library of the user's choice.
     */
    private static final SystemOfUnits SI;
    static {
        SystemOfUnits system = null;
        try {
            final SystemOfUnitsService service = ServiceProvider.current().getSystemOfUnitsService();
            if (service != null) {
                system = service.getSystemOfUnits("SI");
                if (system == null) {
                    system = service.getSystemOfUnits();
                }
            }
        } catch (IllegalStateException e) {
            // Ignore: logging message below.
        }
        if (system == null) {
            /*
             * The call to NativeResource.logger() is necessary - do not replace by
             * Logger.getLogger(NativeResource.LOGGER_NAME). The code below has the
             * desired side effect of initializing the NativeResource class, which
             * is necessary for using the UnitOfMeasure fallback.
             */
            NativeResource.logger().fine("No JSR-363 implementation found.");
        }
        SI = system;
    }

    /**
     * All predefined units.
     *
     * <p>Indices are {@link UnitOfMeasure} constant values
     * and elements are {@link #SCALE_UNITY}, {@link #PARTS_PER_MILLION}, etc.
     *
     * <p>This array shall be unmodifiable.
     *
     * @see #getUnit(short)
     */
    private static final Unit<?>[] PREDEFINED = new Unit<?>[UnitOfMeasure.YEAR + 1];

    /**
     * The scale factors from {@code PREDEFINED[i]} unit to its system unit.
     *
     * <p>This array shall be unmodifiable.
     */
    private static final double[] FACTORS = new double[PREDEFINED.length];

    /**
     * The next identifier to use when a new unit needs to be created.
     *
     * <p>This is used only for units not in the {@link #PREDEFINED} list.
     */
    static final AtomicInteger NEXT_IDENTIFIER = new AtomicInteger(PREDEFINED.length);

    /**
     * Creates a unit of measurement for the given quantity type.
     *
     * <p>If a JSR-363 implementation is available, it will be used. Otherwise {@link UnitOfMeasure}
     * is used as a fallback.
     *
     * @param  <Q>   compile time value of {@code type}.
     * @param  type  the type of quantity represented by the unit of measurement.
     * @param  toSI  the conversion factory to system unit. By convention a negative value means that we shall divide.
     * @param  code  one of {@link UnitOfMeasure} constants.
     * @return unit of measurement, either form JSR-363 implementation or as {@link UnitOfMeasure} instance.
     */
    private static <Q extends Quantity<Q>> Unit<Q> create(final Class<Q> type, final double toSI, final short code) {
        final boolean isDivisor = (toSI < 0);
        FACTORS[code] = isDivisor ? -1/toSI : toSI;
        Unit<Q> unit = null;
        if (SI != null) {
            unit = SI.getUnit(type);
            if (unit != null && toSI != 1) {
                if (isDivisor) {
                    unit = unit.divide(-toSI);      // May avoid rounding errors in some JSR-383 implementations.
                } else {
                    unit = unit.multiply(toSI);
                }
            }
        }
        if (unit == null) {
            unit = UnitOfMeasure.create(code).asType(type);
            assert Math.abs(((UnitOfMeasure<?>) unit).toSI - FACTORS[code]) < Math.ulp(FACTORS[code]) : unit;
        }
        PREDEFINED[code] = unit;
        return unit;
    }

    /**
     * System unit of measurement for scale factors.
     * EPSG code is 9201.
     */
    public static final Unit<Dimensionless> SCALE_UNITY = create(Dimensionless.class, 1, UnitOfMeasure.SCALE_UNITY);

    /**
     * Unit of measurement for scale factors equals to one millionth of {@link #SCALE_UNITY}.
     * EPSG code is 9202.
     */
    public static final Unit<Dimensionless> PARTS_PER_MILLION = create(Dimensionless.class, -1E+6, UnitOfMeasure.PARTS_PER_MILLION);

    /**
     * System unit of measurement for linear measurements.
     * EPSG code is 9001.
     */
    public static final Unit<Length> METRE = create(Length.class, 1, UnitOfMeasure.METRE);

    /**
     * System unit of measurement for angular measurements.
     * EPSG code is 9101.
     */
    public static final Unit<Angle> RADIAN = create(Angle.class, 1, UnitOfMeasure.RADIAN);

    /**
     * Unit of measurement for angular measurements equals to one millionth of {@link #RADIAN}.
     * EPSG code is 9109.
     */
    public static final Unit<Angle> MICRORADIAN = create(Angle.class, -1E+6, UnitOfMeasure.MICRORADIAN);

    /**
     * Unit of measurement for angular measurements.
     * EPSG code is 9122.
     */
    public static final Unit<Angle> DEGREE = create(Angle.class, Math.PI / 180, UnitOfMeasure.DEGREE);

    /**
     * Unit of measurement for angular measurements.
     * EPSG code is 9104.
     */
    public static final Unit<Angle> ARC_SECOND = create(Angle.class, Math.PI / (180 * 60 * 60), UnitOfMeasure.ARC_SECOND);

    /**
     * Unit of measurement for angular measurements.
     * EPSG code is 9105.
     */
    public static final Unit<Angle> GRAD = create(Angle.class, Math.PI / 200, UnitOfMeasure.GRAD);

    /**
     * System unit of measurement for temporal measurements.
     * EPSG code is 1040.
     */
    public static final Unit<Time> SECOND = create(Time.class, 1, UnitOfMeasure.SECOND);

    /**
     * Unit of measurement for temporal measurements.
     * EPSG code is 1029.
     */
    public static final Unit<Time> YEAR = create(Time.class, 31556925.445, UnitOfMeasure.YEAR);

    /**
     * Do not allow instantiation of this class.
     */
    private Units() {
    }

    /**
     * Returns a unit of measurement for this given {@link UnitOfMeasure} constant.
     * This method is invoked by {@link UnitType#getPredefinedUnit(double)} for
     * testing if a predefined unit matches a given scale factor.
     *
     * @param  code  one of {@link UnitOfMeasure} constants.
     * @return unit of measurement, either form JSR-363 implementation or as {@link UnitOfMeasure} instance.
     */
    static Unit<?> getUnit(final short code) {
        return PREDEFINED[code];
    }

    /**
     * Returns the scale factors from {@code getUnit(i)} unit to its system unit.
     * This method is invoked by {@link UnitType#getPredefinedUnit(double)} for
     * testing if a predefined unit matches a given scale factor.
     *
     * @param  code  one of {@link UnitOfMeasure} constants.
     * @return scale factor from specified unit to its base unit.
     */
    static double getFactor(final short code) {
        return FACTORS[code];
    }

    /**
     * Returns the {@link UnitOfMeasure} code for the given unit.
     * The native C/C++ code maps this identifier to the predefined units defined by PROJ.
     *
     * @param  unit  the unit of measure for which to get the code of a predefined unit.
     * @return one of {@link UnitOfMeasure} constants.
     * @throws IllegalArgumentException if no identifier can be found or generated for the given unit.
     */
    static int getUnitIdentifier(final Unit<?> unit) {
        Objects.requireNonNull(unit, "Unit shall not be null.");
        for (int i=0; i<PREDEFINED.length; i++) {
            if (unit.equals(PREDEFINED[i])) {
                return i;
            }
        }
        return UnitType.getUserDefinedUnitIdentifier(unit);
    }
}
