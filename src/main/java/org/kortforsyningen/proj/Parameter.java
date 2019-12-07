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

import java.io.File;
import java.net.URI;
import java.util.Set;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;


/**
 * Wrappers around {@code osgeo::proj::operation::OperationParameterValue} type.
 * PROJ {@code OperationParameterValue} is an aggregate of {@code OperationParameter} and
 * {@code ParameterValue} (PROJ object storing measure, integer, string or filename value).
 * We implement both parameter descriptor and parameter value interfaces in the same class
 * because of the way PROJ organizes its data: some information required by the descriptor
 * (e.g. the value type) are provided by the parameter value class instead.
 *
 * <p>We do not parameterize this class (i.e. we use raw types) because the parameter type
 * is known only in native code.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
@SuppressWarnings("rawtypes")
class Parameter extends IdentifiableObject implements ParameterDescriptor {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::operation::OperationParameterValue}.
     * May also be a {@code osgeo::proj::operation::OperationParameter}, in which case the call to
     * any {@code ParameterValue} method will cause an exception to be thrown.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    Parameter(final long ptr) {
        super(ptr);
    }

    /**
     * Returns the type of this parameter. This base class returns {@code Object.class} because PROJ
     * defines value type with the parameter value instead than with the parameter descriptor,
     * so the expected type is unknown. The {@link Value} subclass overrides this method.
     *
     * @return the type of this parameter.
     */
    @Override
    public Class getValueClass() {
        return Object.class;
    }

    /**
     * Returns the unit of numerical parameter values. This base class returns {@code null} because PROJ
     * defines unit of measurement with the parameter value instead than with the parameter descriptor,
     * so the expected unit of measurement is unknown. The {@link Value} subclass overrides this method.
     *
     * @return the unit of this parameter.
     */
    @Override
    public Unit<?> getUnit() {
        return null;
    }

    @Override public int        getMinimumOccurs() {return 1;}
    @Override public int        getMaximumOccurs() {return 1;}
    @Override public Set        getValidValues()   {return null;}
    @Override public Comparable getMinimumValue()  {return null;}
    @Override public Comparable getMaximumValue()  {return null;}
    @Override public Object     getDefaultValue()  {return null;}

    /**
     * Read-only parameter value.
     */
    static final class Value extends Parameter implements ParameterValue {
        /**
         * Creates a new wrapper for the given {@code osgeo::proj::operation::OperationParameterValue}.
         *
         * @param  ptr  pointer to the wrapped PROJ object.
         */
        Value(final long ptr) {
            super(ptr);
        }

        /**
         * Returns a description of this parameter.
         *
         * @return {@code this}.
         */
        @Override
        public ParameterDescriptor getDescriptor() {
            return this;
        }

        /**
         * Returns the type of this parameter.
         *
         * @return the type of this parameter.
         * @throws UnsupportedOperationException if the type is unknown.
         *         It may happen if new type has been added in a new PROJ version,
         *         in which case we may need to upgrade PROJ-JNI accordingly.
         */
        private ParameterType type() {
            return ParameterType.get(impl.getIntegerProperty(Property.PARAMETER_TYPE));
        }

        /**
         * Returns the class of parameter values.
         *
         * @return the type of parameter values.
         */
        @Override
        public Class getValueClass() {
            return type().type;
        }

        /**
         * Returns the parameter value as an object. The object type may be {@link Double},
         * {@link Integer}, {@link Boolean}, {@link String} or {@link URI}.
         *
         * @return the parameter value as an object, or {@code null} if no value has been set.
         */
        @Override
        public Object getValue() {
            switch (type()) {
                case MEASURE:  return doubleValue();
                case INTEGER:  return intValue();
                case BOOLEAN:  return booleanValue();
                case STRING:   return stringValue();
                case FILENAME: return valueFile();
                default: throw new AssertionError();        // Should never happen.
            }
        }

        /**
         * Returns the unit of measurement of values returned by {@link #doubleValue()}, or {@code null} if none.
         *
         * @return unit of measurement of {@link #doubleValue()}.
         */
        @Override
        public Unit<?> getUnit() {
            return (Unit<?>) impl.getObjectProperty(Property.PARAMETER_UNIT);
        }

        /**
         * Returns the parameter value in the given unit of measurement.
         *
         * @param  unit  the desired unit of measurement.
         * @return the parameter value converted to the given unit.
         * @throws IllegalArgumentException if the specified unit is invalid for this parameter.
         * @throws InvalidParameterTypeException if the value is not a numeric type.
         */
        @Override
        public double doubleValue(final Unit unit) {
            double value = doubleValue();
            if (unit != null) {
                final Unit<?> source = getUnit();
                if (source != null) try {
                    value = source.getConverterToAny(unit).convert(value);
                } catch (IncommensurableException e) {
                    throw new IllegalArgumentException("Can not convert \"" +
                            getName().getCode() + "\" values to unit " + unit, e);
                }
            }
            return value;
        }

        /**
         * Returns the parameter value as a floating point number.
         * The unit of measurement is specified by {@link #getUnit()}.
         *
         * @return the numeric parameter value.
         * @throws InvalidParameterTypeException if the value is not a numeric type.
         */
        @Override
        public double doubleValue() {
            return impl.getNumericProperty(Property.PARAMETER_VALUE);
        }

        /**
         * Returns the parameter value as an integer value.
         *
         * @return the integer parameter value.
         * @throws InvalidParameterTypeException if the value is not an integer type.
         */
        @Override
        public int intValue() {
            return impl.getIntegerProperty(Property.PARAMETER_INT);
        }

        /**
         * Returns the parameter value as a boolean value.
         *
         * @return the boolean parameter value.
         * @throws InvalidParameterTypeException if the value is not a boolean type.
         */
        @Override
        public boolean booleanValue() {
            return impl.getBooleanProperty(Property.PARAMETER_BOOL);
        }

        /**
         * Returns the string value of this parameter.
         *
         * @return the string value represented by this parameter.
         * @throws InvalidParameterTypeException if the value is not a string.
         */
        @Override
        public String stringValue() {
            return impl.getStringProperty(Property.PARAMETER_STRING);
        }

        /**
         * Returns a reference to a file or a part of a file containing one or more parameter values.
         *
         * @return the reference to a file containing parameter values.
         * @throws InvalidParameterTypeException if the value is not a reference to a file or a URI.
         */
        @Override
        public URI valueFile() {
            final String file = impl.getStringProperty(Property.PARAMETER_STRING);
            return (file != null) ? new File(file).toURI() : null;
        }

        /**
         * Returns an ordered sequence of numeric values in the specified unit of measure.
         *
         * @param  unit  the unit of measure for the value to be returned.
         * @return the sequence of values represented by this parameter after conversion to given unit.
         * @throws IllegalArgumentException if the specified unit is invalid for this parameter.
         * @throws InvalidParameterTypeException if the value is not an array of {@code double}s.
         */
        @Override
        public double[] doubleValueList(final Unit unit) {
            return new double[] {doubleValue(unit)};
        }

        /**
         * Returns an ordered sequence of numeric values of this parameter.
         *
         * @return the sequence of values represented by this parameter.
         * @throws InvalidParameterTypeException if the value is not an array of {@code double}s.
         */
        @Override
        public double[] doubleValueList() {
            return new double[] {doubleValue()};
        }

        /**
         * Returns an ordered sequence of integer values of this parameter.
         *
         * @return the sequence of values represented by this parameter.
         * @throws InvalidParameterTypeException if the value is not an array of {@code int}s.
         */
        @Override
        public int[] intValueList() {
            return new int[] {intValue()};
        }

        @Override public void setValue(double[] value, Unit unit) {throw new UnsupportedOperationException("Read-only parameter.");}
        @Override public void setValue(double   value, Unit unit) {throw new UnsupportedOperationException("Read-only parameter.");}
        @Override public void setValue(double   value)            {throw new UnsupportedOperationException("Read-only parameter.");}
        @Override public void setValue(int      value)            {throw new UnsupportedOperationException("Read-only parameter.");}
        @Override public void setValue(boolean  value)            {throw new UnsupportedOperationException("Read-only parameter.");}
        @Override public void setValue(Object   value)            {throw new UnsupportedOperationException("Read-only parameter.");}

        /**
         * Returns a copy of this parameter value that user can modify.
         *
         * @return a modifiable copy of this parameter value.
         */
        @Override
        @SuppressWarnings("CloneDoesntCallSuperClone")
        public ParameterValue clone() {
            final ParameterValue value = createValue();
            value.setValue(getValue());
            return value;
        }
    }

    /**
     * Creates a modifiable parameter value with initially no value.
     *
     * @return a modifiable parameter value.
     */
    @Override
    public ParameterValue createValue() {
        throw new UnsupportedOperationException();      // TODO
    }

    /**
     * Returns a string representation of this parameter for debugging purposes.
     * This method format a pseudo-WKT string, with the parameter name between quotes.
     * If the parameter name is unspecified, then the first identifier is formatted as
     * if it was the name (because the name is supposed to be mandatory).
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("PARAMETER[\"");
        String name = getNameString(false);
        if (name == null) {
            for (final ReferenceIdentifier id : getIdentifiers()) {
                name = id.getCode();
                if (name != null) {
                    final String cs = id.getCodeSpace();
                    if (cs != null) {
                        buffer.append(cs).append(':');
                    }
                    break;
                }
            }
        }
        buffer.append(name).append('"');
        return buffer.append(']').toString();
    }
}
