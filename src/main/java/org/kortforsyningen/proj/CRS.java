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

import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;


/**
 * Wrappers around {@code osgeo::proj::crs::CRS} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class CRS extends IdentifiableObject implements CoordinateReferenceSystem {
    /**
     * Casts the given value to {@link CRS}.
     *
     * @param  name   argument name, used only for formatting error message.
     * @param  value  value to cast.
     * @return the given CRS as a PROJ implementation.
     * @throws IllegalArgumentException if the given value is {@code null}
     *         or is not a PROJ implementation.
     */
    static CRS cast(final String name, final CoordinateReferenceSystem value) {
        if (value instanceof CRS) {
            return (CRS) value;
        } else {
            throw new UnsupportedImplementationException(UnsupportedImplementationException.message(name, value));
        }
    }

    /**
     * Creates a new wrapper for the given {@code osgeo::proj::crs::CRS}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    CRS(final long ptr) {
        super(ptr);
    }

    /**
     * Returns the number of dimensions of this CRS.
     *
     * @return the number of dimensions of this CRS.
     */
    final int getDimension() {
        return impl.getPropertySize(Property.AXIS);
    }

    /**
     * Returns the coordinate system casted to the given type.
     * The target type is specified by the CRS subclass.
     *
     * @param  <T>   the compile-time value of {@code type} argument.
     * @param  type  the expected coordinate system class.
     * @return the coordinate system.
     */
    final <T extends CS> T getCoordinateSystem(final Class<T> type) {
        return type.cast(impl.getObjectProperty(Property.COORDINATE_SYSTEM, 0));
    }

    /**
     * Returns the coordinate system of a single CRS, or a view over all coordinate systems of a compound CRS.
     *
     * @return the coordinate system.
     */
    @Override
    public CoordinateSystem getCoordinateSystem() {
        return getCoordinateSystem(CS.class);
    }

    /**
     * Returns the datum casted to the given type.
     * The target type is specified by the CRS subclass.
     *
     * @param  <T>   the compile-time value of {@code type} argument.
     * @param  type  the expected datum class.
     * @return the datum or reference frame.
     */
    final <T extends Datum> T getDatum(final Class<T> type) {
        return type.cast(impl.getObjectProperty(Property.DATUM, 0));
    }

    /**
     * A coordinate reference system specialization.
     */
    static class Geodetic extends CRS implements GeodeticCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Geodetic(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the geodetic specialization of the datum.
         */
        @Override
        public GeodeticDatum getDatum() {
            return getDatum(Datum.Geodetic.class);
        }
    }

    /**
     * A coordinate reference system specialization.
     */
    static final class Geographic extends Geodetic implements GeographicCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Geographic(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the ellipsoidal specialization of the coordinate system.
         */
        @Override
        public EllipsoidalCS getCoordinateSystem() {
            return getCoordinateSystem(CS.Ellipsoidal.class);
        }
    }

    /**
     * A coordinate reference system specialization.
     */
    static final class Vertical extends CRS implements VerticalCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Vertical(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the vertical specialization of the coordinate system.
         */
        @Override
        public VerticalCS getCoordinateSystem() {
            return getCoordinateSystem(CS.Vertical.class);
        }

        /**
         * Returns the vertical specialization of the datum.
         */
        @Override
        public VerticalDatum getDatum() {
            return getDatum(Datum.Vertical.class);
        }
    }

    /**
     * A coordinate reference system specialization.
     */
    static final class Temporal extends CRS implements TemporalCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Temporal(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the temporal specialization of the coordinate system.
         */
        @Override
        public TimeCS getCoordinateSystem() {
            return getCoordinateSystem(CS.Time.class);
        }

        /**
         * Returns the temporal specialization of the datum.
         */
        @Override
        public TemporalDatum getDatum() {
            return getDatum(Datum.Temporal.class);
        }
    }

    /**
     * A coordinate reference system specialization.
     */
    static final class Engineering extends CRS implements EngineeringCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Engineering(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the engineering specialization of the datum.
         */
        @Override
        public EngineeringDatum getDatum() {
            return getDatum(Datum.Engineering.class);
        }
    }
}
