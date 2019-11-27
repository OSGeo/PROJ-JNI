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
import java.util.List;
import java.util.Objects;
import java.lang.annotation.Native;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.IncommensurableException;
import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import javax.measure.quantity.Dimensionless;


/**
 * Mirror of {@code osgeo::proj::common::UnitOfMeasure} constants. This class does not wrap native object.
 * It is used only as a fallback when no JSR-363 implementation has been found on the classpath.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class UnitOfMeasure<Q extends Quantity<Q>> implements Unit<Q> {
    /**
     * Constants identifying PROJ predefined units. We can provide only constants for quantities enumerated
     * in {@link UnitType}. For example PROJ 6.2 does not provide {@code UnitOfMeasure::Type::SPEED} value,
     * so we have to skip the {@code METRE_PER_YEAR} unit.
     */
    @Native
    static final short
            SCALE_UNITY         =  0,
            PARTS_PER_MILLION   =  1,
            METRE               =  2,
            RADIAN              =  3,
            MICRORADIAN         =  4,
            DEGREE              =  5,
            ARC_SECOND          =  6,
            GRAD                =  7,
            SECOND              =  8,
            YEAR                =  9;
            // If YEAR is no longer the last unit, update Units.PREDEFINED array length.

    /**
     * The type of quantity represented by the unit of measurement.
     */
    final Class<Q> type;

    /**
     * The unit name. This is provided by PROJ.
     */
    private final String name;

    /**
     * The conversion factor to system unit. This is provided by PROJ.
     */
    final double toSI;

    /**
     * Creates a new mirror of {@code osgeo::proj::common::UnitOfMeasure}.
     * This constructor is invoked from native code.
     *
     * @param  type  the unit type as an {@link UnitType} ordinal value.
     * @param  name  unit name (provided by PROJ, matches the name in EPSG database).
     * @param  toSI  the conversion factory to system unit (provided by PROJ).
     */
    @SuppressWarnings("unchecked")
    private UnitOfMeasure(final int type, final String name, final double toSI) {
        this.type = (Class<Q>) UnitType.get(type).type;
        this.name = name;
        this.toSI = toSI;
    }

    /**
     * Creates a new mirror of {@code osgeo::proj::common::UnitOfMeasure}.
     *
     * @param  type  the type of quantity represented by the unit of measurement.
     * @param  toSI  the conversion factory to system unit (provided by PROJ).
     */
    private UnitOfMeasure(final Class<Q> type, final double toSI) {
        this.type = type;
        this.name = null;
        this.toSI = toSI;
    }

    /**
     * Creates a new mirror of {@code osgeo::proj::common::UnitOfMeasure}.
     * The unit name and conversion factor to SI will be provided by PROJ.
     *
     * @param  code  one of {@link UnitOfMeasure} constants.
     */
    static native <Q extends Quantity<Q>> UnitOfMeasure<Q> create(short code);

    /**
     * Returns the symbol of this unit, or {@code null} if this unit has no specific symbol associated with.
     */
    @Override
    public String getSymbol() {
        return null;
    }

    /**
     * Returns the name of this unit. This method is allowed to return {@code null}
     * if this unit has no specific name associated with.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Unsupported operation for this simple unit implementation.
     */
    @Override
    public Dimension getDimension() {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns the unscaled system unit from which this unit is derived.
     */
    @Override
    public Unit<Q> getSystemUnit() {
        final short code;
        if (Length.class.equals(type)) {
            code = METRE;
        } else if (Angle.class.equals(type)) {
            code = RADIAN;
        } else if (Time.class.equals(type)) {
            code = SECOND;
        } else if (Dimensionless.class.equals(type)) {
            code = SCALE_UNITY;
        } else {
            throw new NoUnitImplementationException();
        }
        return Units.getUnit(code).asType(type);
    }

    /**
     * Returns the base units and their exponent whose product is this unit,
     * or {@code null} if this unit is a base unit.
     */
    @Override
    public Map<? extends Unit<?>, Integer> getBaseUnits() {
        throw new NoUnitImplementationException();
    }

    /**
     * Indicates if this unit is compatible with the unit specified.
     */
    @Override
    public boolean isCompatible(final Unit<?> other) {
        if (other instanceof UnitOfMeasure<?>) {
            return type.equals(((UnitOfMeasure<?>) other).type);
        }
        throw new NoUnitImplementationException(Objects.requireNonNull(other).getClass());
    }

    /**
     * Casts this unit to a parameterized unit of specified nature or
     * throw a {@code ClassCastException} if the dimensions do not match.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Quantity<T>> Unit<T> asType(final Class<T> target) throws ClassCastException {
        if (target.isAssignableFrom(type)) {
            return (Unit<T>) this;
        }
        throw new ClassCastException("Unit<" + type.getSimpleName() + "> can not be casted to Unit<"
                                           + target.getSimpleName() + ">.");
    }

    /**
     * A straightforward converter implementation which only multiplies values by a factor.
     * No attempt is made to avoid rounding errors (more sophisticated implementations may
     * use ratio for example instead than a IEEE 754 double-prevision value).
     */
    private static final class Converter implements UnitConverter {
        /** The conversion factor. */ private final double factor;

        /** Creates a new converter with the given conversion factor. */
        Converter(final double factor) {this.factor = factor;}
        @Override public UnitConverter       inverse()             {return new Converter(1 / factor);}
        @Override public boolean             isLinear()            {return true;}
        @Override public boolean             isIdentity()          {return factor == 1;}
        @Override public Number              convert(Number value) {return factor * value.doubleValue();}
        @Override public double              convert(double value) {return factor * value;}
        @Override public List<UnitConverter> getConversionSteps()  {return List.of(this);}
        @Override public UnitConverter       concatenate(final UnitConverter other) {
            if (other.isLinear()) {
                return new Converter(factor * other.convert(1));
            } else {
                // More sophisticated implementation would be able to create a concatenated converter.
                throw new NoUnitImplementationException("Can not concatenate non-linear converter.");
            }
        }
    }

    /**
     * Returns a converter of numeric values from this unit to another unit of same type.
     */
    @Override
    public UnitConverter getConverterTo(final Unit<Q> target) {
        if (target instanceof UnitOfMeasure<?>) {
            return new Converter(toSI / ((UnitOfMeasure<?>) target).toSI);
        }
        throw new NoUnitImplementationException(Objects.requireNonNull(target).getClass());
    }

    /**
     * Returns a converter from this unit to the specified unit of type unknown.
     */
    @Override
    public UnitConverter getConverterToAny(final Unit<?> target) throws IncommensurableException {
        if (target instanceof UnitOfMeasure<?>) {
            final Class<?> t = ((UnitOfMeasure<?>) target).type;
            if (type.equals(t)) {
                return new Converter(toSI / ((UnitOfMeasure<?>) target).toSI);
            }
            throw new IncommensurableException("Can not convert " + type.getSimpleName() + " to " + t.getSimpleName());
        }
        throw new NoUnitImplementationException(Objects.requireNonNull(target).getClass());
    }

    /**
     * Returns a system unit equivalent to this unscaled standard unit but used
     * in expressions to distinguish between quantities of a different nature.
     */
    @Override
    public Unit<Q> alternate(final String label) {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns the result of setting the origin of the scale of measurement to the given value.
     */
    @Override
    public Unit<Q> shift(final double offset) {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns the result of multiplying this unit by the specified factor.
     */
    @Override
    public Unit<Q> multiply(final double multiplier) {
        if (multiplier == 1) return this;
        return new UnitOfMeasure<>(type, toSI * multiplier);
    }

    /**
     * Returns the product of this unit with the one specified.
     */
    @Override
    public Unit<?> multiply(final Unit<?> multiplier) {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns the reciprocal (multiplicative inverse) of this unit.
     */
    @Override
    public Unit<?> inverse() {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns the result of dividing this unit by a divisor.
     */
    @Override
    public Unit<Q> divide(final double divisor) {
        if (divisor == 1) return this;
        return new UnitOfMeasure<>(type, toSI / divisor);
    }

    /**
     * Returns the quotient of this unit with the one specified.
     */
    @Override
    public Unit<?> divide(final Unit<?> divisor) {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns an unit that is the n-th (integer) root of this unit.
     */
    @Override
    public Unit<?> root(int n) {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns an unit raised to the n-th (integer) power of this unit.
     */
    @Override
    public Unit<?> pow(int n) {
        throw new NoUnitImplementationException();
    }

    /**
     * Returns the unit derived from this unit using the specified converter.
     */
    @Override
    public Unit<Q> transform(final UnitConverter operation) {
        throw new NoUnitImplementationException();
    }

    /**
     * Verifies that this unit is equals to the given parameters,
     * with an arbitrary tolerance threshold for the conversion factor.
     * By convention, a negative value means that the factor needs to be inverted.
     */
    final boolean equals(final Class<?> t, double c) {
        if (c < 0) c = -1 / c;
        return type == t && Math.abs(toSI - c) < Math.ulp(toSI);
    }

    /**
     * Compares this unit with the given object for strict equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof UnitOfMeasure<?>) {
            final UnitOfMeasure<?> other = (UnitOfMeasure<?>) obj;
            return type.equals(other.type) && Objects.equals(name, other.name) &&
                   Double.doubleToLongBits(toSI) == Double.doubleToLongBits(other.toSI);
        }
        return false;
    }

    /**
     * Returns a hash code value for this unit.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name) + type.hashCode() + Double.hashCode(toSI);
    }

    /**
     * Returns a string representation of this unit. Current implementation returns the unit name.
     */
    @Override
    public String toString() {
        return getName();
    }
}
