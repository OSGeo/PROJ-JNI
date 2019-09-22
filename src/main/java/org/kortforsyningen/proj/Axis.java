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
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;


/**
 * Wrappers around {@code osgeo::proj::cs::CoordinateSystemAxis}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Axis extends BaseObject implements CoordinateSystemAxis {
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
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the unit of measure used for this coordinate system axis.
     *
     * @return the coordinate system axis unit.
     */
    @Override
    public Unit<?> getUnit() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the minimum value normally allowed for this axis,
     * in the {@linkplain #getUnit() unit of measure for the axis}.
     *
     * @return the minimum value, or {@link Double#NEGATIVE_INFINITY} if none.
     */
    @Override
    public double getMinimumValue() {
        return Double.NEGATIVE_INFINITY;
    }

    /**
     * Returns the maximum value normally allowed for this axis,
     * in the {@linkplain #getUnit() unit of measure for the axis}.
     *
     * @return the maximum value, or {@link Double#POSITIVE_INFINITY} if none.
     */
    @Override
    public double getMaximumValue() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Returns the meaning of axis value range specified by the {@linkplain #getMinimumValue()
     * minimum} and {@linkplain #getMaximumValue() maximum} values.
     *
     * @return the range meaning, or {@code null} in none.
     */
    @Override
    public RangeMeaning getRangeMeaning() {
        return null;
    }
}
