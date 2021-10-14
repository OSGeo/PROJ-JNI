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

import javax.measure.Unit;
import org.opengis.util.CodeList;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;


/**
 * Wrappers around {@code osgeo::proj::cs::CoordinateSystemAxis}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
final class Axis extends IdentifiableObject implements CoordinateSystemAxis {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::cs::CoordinateSystemAxis}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    Axis(final long ptr) {
        super(ptr);
    }

    /**
     * Returns the abbreviation used for this coordinate system axes.
     *
     * @return the coordinate system axis abbreviation.
     */
    @Override
    public String getAbbreviation() {
        return impl.getStringProperty(Property.ABBREVIATION);
    }

    /**
     * Returns the direction of this coordinate system axis. Examples:
     * {@linkplain AxisDirection#NORTH north} or {@linkplain AxisDirection#SOUTH south},
     * {@linkplain AxisDirection#EAST  east}  or {@linkplain AxisDirection#WEST  west},
     * {@linkplain AxisDirection#UP    up}    or {@linkplain AxisDirection#DOWN  down}.
     *
     * @return the coordinate system axis direction.
     */
    @Override
    public AxisDirection getDirection() {
        final String dir = impl.getStringProperty(Property.DIRECTION);
        return search(AxisDirection.class, dir);
    }

    /**
     * Searches for the given code, ignoring case. The string should be the UML identifier,
     * not Java or C/C++ field name. However this method accepts both.
     *
     * @param  <T>   compile-time value of {@code type}.
     * @param  type  class of the code to search.
     * @param  code  name of the code that we are searching.
     * @return the code list for the given UML identifier.
     */
    private static <T extends CodeList<T>> T search(final Class<T> type, final String code) {
        return CodeList.valueOf(type, (candidate) ->
                code.equalsIgnoreCase(candidate.identifier()) ||
                code.equalsIgnoreCase(candidate.name()), code);
    }

    /**
     * Returns the unit of measure used for this coordinate system axis.
     *
     * @return the coordinate system axis unit.
     */
    @Override
    public Unit<?> getUnit() {
        return (Unit<?>) impl.getObjectProperty(Property.AXIS_UNIT);
    }

    /**
     * Returns the minimum value normally allowed for this axis,
     * in the {@linkplain #getUnit() unit of measure for the axis}.
     *
     * @return the minimum value, or {@link Double#NEGATIVE_INFINITY} if none.
     */
    @Override
    public double getMinimumValue() {
        final double v = impl.getNumericProperty(Property.MINIMUM);
        return Double.isNaN(v) ? Double.NEGATIVE_INFINITY : v;
    }

    /**
     * Returns the maximum value normally allowed for this axis,
     * in the {@linkplain #getUnit() unit of measure for the axis}.
     *
     * @return the maximum value, or {@link Double#POSITIVE_INFINITY} if none.
     */
    @Override
    public double getMaximumValue() {
        final double v = impl.getNumericProperty(Property.MAXIMUM);
        return Double.isNaN(v) ? Double.POSITIVE_INFINITY : v;
    }
}
