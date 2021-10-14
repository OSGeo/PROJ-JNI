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

import java.util.List;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.ParametricCS;
//     org.opengis.referencing.datum.Datum            — Not imported because we use Datum class from this package.
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.ParametricDatum;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.ParametricCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.CompoundCRS;


/**
 * Wrappers around {@code osgeo::proj::crs::CRS} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
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
            throw new UnsupportedImplementationException(name, value);
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
        return CompoundCS.getDimension(impl);
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
        return type.cast(impl.getObjectProperty(Property.COORDINATE_SYSTEM));
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
        return type.cast(impl.getObjectProperty(Property.DATUM));
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
     * This type is not defined by ISO 19111 and may be removed in a future GeoAPI version.
     */
    static final class Geocentric extends Geodetic implements GeocentricCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Geocentric(final long ptr) {
            super(ptr);
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
    static final class Parametric extends CRS implements ParametricCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Parametric(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the parametric specialization of the coordinate system.
         */
        @Override
        public ParametricCS getCoordinateSystem() {
            return getCoordinateSystem(CS.Parametric.class);
        }

        /**
         * Returns the parametric specialization of the datum.
         */
        @Override
        public ParametricDatum getDatum() {
            return getDatum(Datum.Parametric.class);
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

    /**
     * A coordinate reference system specialization.
     */
    static final class Projected extends CRS implements ProjectedCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Projected(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the base coordinate reference system, which must be geographic.
         */
        @Override
        @SuppressWarnings("OverlyStrongTypeCast")       // Casting to final class is easier for the JVM.
        public GeographicCRS getBaseCRS() {
            return (Geographic) impl.getObjectProperty(Property.BASE_CRS);
        }

        /**
         * Returns the map projection from the {@linkplain #getBaseCRS() base CRS} to this CRS.
         */
        @Override
        public Projection getConversionFromBase() {
            final Operation.Conversion op = (Operation.Conversion) impl.getObjectProperty(Property.CONVERT_FROM_BASE);
            return (op != null) ? new Operation.Projection(this, op) : null;
        }

        /**
         * Returns the Cartesian specialization of the coordinate system.
         */
        @Override
        public CartesianCS getCoordinateSystem() {
            return getCoordinateSystem(CS.Cartesian.class);
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
    static final class Compound extends CRS implements CompoundCRS {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Compound(final long ptr) {
            super(ptr);
        }

        /**
         * Returns a view over all coordinate systems of this compound CRS.
         */
        @Override
        public CoordinateSystem getCoordinateSystem() {
            return new CompoundCS(this);
        }

        /**
         * Returns the components of this CRS.
         */
        @Override
        public List<CoordinateReferenceSystem> getComponents() {
            return new PropertyList<>(CRS.class, Property.CRS_COMPONENT);
        }
    }
}
