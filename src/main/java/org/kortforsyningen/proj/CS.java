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

import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.ParametricCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.TimeCS;


/**
 * Wrappers around {@code osgeo::proj::cs::CoordinateSystem} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
class CS extends IdentifiableObject implements CoordinateSystem {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::cs::CoordinateSystem}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    CS(final long ptr) {
        super(ptr);
    }

    /**
     * Returns the dimension of the coordinate system.
     *
     * @return the dimension of the coordinate system.
     */
    @Override
    public int getDimension() {
        return impl.getVectorSize(Property.AXIS);
    }

    /**
     * Returns the axis for this coordinate system at the specified dimension.
     *
     * @param  dimension  the zero based index of axis.
     * @return the axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     */
    @Override
    @SuppressWarnings("OverlyStrongTypeCast")       // Casting to final class is easier for the JVM.
    public CoordinateSystemAxis getAxis(int dimension) throws IndexOutOfBoundsException {
        return (Axis) impl.getVectorElement(Property.AXIS, dimension);
    }

    /**
     * A coordinate system specialization. No new properties compared to parent CS.
     */
    static final class Cartesian extends CS implements CartesianCS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Cartesian(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A coordinate system specialization. No new properties compared to parent CS.
     */
    static final class Spherical extends CS implements SphericalCS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Spherical(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A coordinate system specialization. No new properties compared to parent CS.
     */
    static final class Ellipsoidal extends CS implements EllipsoidalCS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Ellipsoidal(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A coordinate system specialization. No new properties compared to parent CS.
     */
    static final class Vertical extends CS implements VerticalCS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Vertical(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A coordinate system specialization. No new properties compared to parent CS.
     */
    static final class Time extends CS implements TimeCS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Time(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A coordinate system specialization. No new properties compared to parent CS.
     */
    static final class Parametric extends CS implements ParametricCS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Parametric(final long ptr) {
            super(ptr);
        }
    }
}
