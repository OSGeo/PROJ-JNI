/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021-2023 Open Source Geospatial Foundation
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

import java.util.Date;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
//     org.opengis.referencing.datum.Datum            — Not imported because this class is also named Datum.
//     org.opengis.referencing.datum.Ellipsoid        — Not imported because we define an Ellipsoid in this class.
//     org.opengis.referencing.datum.PrimeMeridian    — Not imported because we define a PrimeMeridian in this class.
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.opengis.referencing.datum.EngineeringDatum;


/**
 * Wrappers around {@code osgeo::proj::datum::Datum} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * <p>This class has the same name as {@link org.opengis.referencing.datum.Datum} and contains
 * inner classes of the same name than {@link org.opengis.referencing.datum.PrimeMeridian} and
 * {@link org.opengis.referencing.datum.Ellipsoid} for making more natural to user to recognize
 * what we implement when (s)he see the class name. The very minor inconvenience for PROJ-JNI
 * implementation is offset by the user convenience.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
class Datum extends IdentifiableObject implements org.opengis.referencing.datum.Datum {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::datum::Datum}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     * @throws FactoryException if {@code ptr} is 0.
     */
    Datum(final long ptr) throws FactoryException {
        super(ptr);
    }

    /**
     * A description, possibly including coordinates of an identified point or points, of the
     * relationship used to anchor the coordinate system to the Earth or alternate object.
     * Also known as the "origin", especially for Engineering datums.
     *
     * @return a description of the anchor point, or {@code null} if none.
     */
    @Override
    public InternationalString getAnchorPoint() {
        return getProperty(Property.ANCHOR_DEFINITION);
    }

    /**
     * The time after which this datum definition is valid.
     *
     * @return the datum realization epoch, or {@code null} if not available.
     */
    @Override
    public Date getRealizationEpoch() {
        return getDate(Property.PUBLICATION_DATE);
    }

    /**
     * A geodetic reference frame property.
     */
    static final class Ellipsoid extends IdentifiableObject implements org.opengis.referencing.datum.Ellipsoid {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         *
         * @param  ptr  pointer to the wrapped PROJ object.
         * @throws FactoryException if {@code ptr} is 0.
         */
        Ellipsoid(final long ptr) throws FactoryException {
            super(ptr);
        }

        @Override public Unit<Length> getAxisUnit()          {return getUnit(Length.class, Property.ELLIPSOID_UNIT);}
        @Override public double       getSemiMajorAxis()     {return impl.getNumericProperty(Property.SEMI_MAJOR);}
        @Override public double       getSemiMinorAxis()     {return impl.getNumericProperty(Property.SEMI_MINOR);}
        @Override public boolean      isIvfDefinitive()      {return impl.getBooleanProperty(Property.IVF_DEFINITIVE);}
        @Override public boolean      isSphere()             {return impl.getBooleanProperty(Property.IS_SPHERE);}
        @Override public double       getInverseFlattening() {
            final double f = impl.getNumericProperty(Property.INVERSE_FLAT);
            return (f == 0) ? Double.POSITIVE_INFINITY : f;
        }
    }

    /**
     * A geodetic reference frame property.
     */
    static final class PrimeMeridian extends IdentifiableObject implements org.opengis.referencing.datum.PrimeMeridian {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         *
         * @param  ptr  pointer to the wrapped PROJ object.
         * @throws FactoryException if {@code ptr} is 0.
         */
        PrimeMeridian(final long ptr) throws FactoryException {
            super(ptr);
        }

        @Override public double      getGreenwichLongitude() {return impl.getNumericProperty(Property.GREENWICH);}
        @Override public Unit<Angle> getAngularUnit()        {return getUnit(Angle.class, Property.MERIDIAN_UNIT);}
    }

    /**
     * A datum specialization.
     */
    static final class Geodetic extends Datum implements GeodeticDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         *
         * @param  ptr  pointer to the wrapped PROJ object.
         * @throws FactoryException if {@code ptr} is 0.
         */
        Geodetic(final long ptr) throws FactoryException {
            super(ptr);
        }

        /**
         * Returns the ellipsoid.
         */
        @SuppressWarnings("OverlyStrongTypeCast")       // Casting to final class is easier for the JVM.
        @Override public org.opengis.referencing.datum.Ellipsoid getEllipsoid() {
            return (Ellipsoid) impl.getObjectProperty(Property.ELLIPSOID);
        }

        /**
         * Returns the prime meridian.
         */
        @SuppressWarnings("OverlyStrongTypeCast")       // Casting to final class is easier for the JVM.
        @Override public org.opengis.referencing.datum.PrimeMeridian getPrimeMeridian() {
            return (PrimeMeridian) impl.getObjectProperty(Property.PRIME_MERIDIAN);
        }
    }

    /**
     * A datum specialization.
     */
    static final class Vertical extends Datum implements VerticalDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         *
         * @param  ptr  pointer to the wrapped PROJ object.
         * @throws FactoryException if {@code ptr} is 0.
         */
        Vertical(final long ptr) throws FactoryException {
            super(ptr);
        }

        /**
         * The type of this vertical datum.
         * This attribute is inherited from the specification published in 2003.
         * The 2007 revision of ISO 19111 removed this attribute, since this information
         * can be encoded in the <cite>anchor definition</cite>.
         */
        @Override
        public VerticalDatumType getVerticalDatumType() {
            return VerticalDatumType.valueOf("Unspecified");
        }
    }

    /**
     * A datum specialization.
     */
    static final class Temporal extends Datum implements TemporalDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         *
         * @param  ptr  pointer to the wrapped PROJ object.
         * @throws FactoryException if {@code ptr} is 0.
         */
        Temporal(final long ptr) throws FactoryException {
            super(ptr);
        }

        /**
         * The date and time origin of this temporal datum.
         */
        @Override
        public Date getOrigin() {
            return getDate(Property.TEMPORAL_ORIGIN);
        }
    }

    /**
     * A datum specialization. No new properties compared to parent CS.
     */
    static final class Engineering extends Datum implements EngineeringDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         *
         * @param  ptr pointer to the wrapped PROJ object.
         * @throws FactoryException if {@code ptr} is 0.
         */
        Engineering(final long ptr) throws FactoryException {
            super(ptr);
        }
    }
}
