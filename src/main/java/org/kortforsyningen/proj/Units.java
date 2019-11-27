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
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import javax.measure.quantity.Dimensionless;
import javax.measure.spi.ServiceProvider;
import javax.measure.spi.SystemOfUnits;
import javax.measure.spi.SystemOfUnitsService;


/**
 * A set of predefined units of measurements used by PROJ.
 * Units of measurement are represented by the standard {@link Unit} interface, which is implemented by
 * <a href="https://jcp.org/aboutJava/communityprocess/implementations/jsr363/index.html">external libraries</a>.
 * PROJ-JNI uses whatever JSR-363 implementation is found on the classpath at the time this {@link Units} class is
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
     * No implementation of this class is provided by PROJ-JNI; it has to be
     * provided by an external library at user choice.
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
            /*
             * The call to NativeResource.logger() is necessary - do not replace by
             * System.getLogger(NativeResource.LOGGER_NAME). The code below has the
             * desired side effect of initializing the NativeResource class, which
             * is necessary for using the UnitOfMeasure fallback.
             */
            NativeResource.logger().log(System.Logger.Level.DEBUG, "No JSR-363 implementation found.");
        }
        SI = system;
    }

    /**
     * All predefined units. Indices are {@link UnitOfMeasure} constant values
     * and elements are {@link #SCALE_UNITY}, {@link #PARTS_PER_MILLION}, etc.
     *
     * @see #getUnit(short)
     */
    private static final Unit<?>[] PREDEFINED = new Unit<?>[UnitOfMeasure.YEAR + 1];

    /**
     * Creates a unit of measurement for the given quantity type. If a JSR-363 implementation
     * is available, it will be used. Otherwise {@link UnitOfMeasure} is used as a fallback.
     *
     * @param  type  the type of quantity represented by the unit of measurement.
     * @param  toSI  the conversion factory to system unit. By convention a negative value means that we shall divide.
     * @param  code  one of {@link UnitOfMeasure} constants.
     */
    private static <Q extends Quantity<Q>> Unit<Q> create(final Class<Q> type, final double toSI, final short code) {
        Unit<Q> unit = null;
        if (SI != null) {
            unit = SI.getUnit(type);
            if (unit != null && toSI != 1) {
                if (toSI >= 0) {
                    unit = unit.multiply(toSI);
                } else {
                    unit = unit.divide(-toSI);
                }
            }
        }
        if (unit == null) {
            unit = UnitOfMeasure.create(code);
            assert ((UnitOfMeasure<?>) unit).equals(type, toSI) : unit;
        }
        PREDEFINED[code] = unit;
        return unit;
    }

    /**
     * System unit of measurement for scale factors.
     */
    public static final Unit<Dimensionless> SCALE_UNITY = create(Dimensionless.class, 1, UnitOfMeasure.SCALE_UNITY);

    /**
     * Unit of measurement for scale factors equals to one millionth of {@link #SCALE_UNITY}.
     */
    public static final Unit<Dimensionless> PARTS_PER_MILLION = create(Dimensionless.class, -1E+6, UnitOfMeasure.PARTS_PER_MILLION);

    /**
     * System unit of measurement for linear measurements.
     */
    public static final Unit<Length> METRE = create(Length.class, 1, UnitOfMeasure.METRE);

    /**
     * System unit of measurement for angular measurements.
     */
    public static final Unit<Angle> RADIAN = create(Angle.class, 1, UnitOfMeasure.RADIAN);

    /**
     * Unit of measurement for angular measurements equals to one millionth of {@link #RADIAN}.
     */
    public static final Unit<Angle> MICRORADIAN = create(Angle.class, -1E+6, UnitOfMeasure.MICRORADIAN);

    /**
     * Unit of measurement for angular measurements.
     */
    public static final Unit<Angle> DEGREE = create(Angle.class, Math.PI / 180, UnitOfMeasure.DEGREE);

    /**
     * Unit of measurement for angular measurements.
     */
    public static final Unit<Angle> ARC_SECOND = create(Angle.class, Math.PI / (180 * 60 * 60), UnitOfMeasure.ARC_SECOND);

    /**
     * Unit of measurement for angular measurements.
     */
    public static final Unit<Angle> GRAD = create(Angle.class, Math.PI / 200, UnitOfMeasure.GRAD);

    /**
     * Unit of measurement for temporal measurements.
     * This is a {@linkplain Unit#getSystemUnit() system unit}.
     */
    public static final Unit<Time> SECOND = create(Time.class, 1, UnitOfMeasure.SECOND);

    /**
     * Unit of measurement for temporal measurements.
     */
    public static final Unit<Time> YEAR = create(Time.class, 31556925.445, UnitOfMeasure.YEAR);

    /**
     * Do not allow instantiation of this class.
     */
    private Units() {
    }

    /**
     * Returns a unit of measurement for this given {@link UnitOfMeasure} constant.
     */
    static Unit<?> getUnit(final short code) {
        return PREDEFINED[code];
    }
}
