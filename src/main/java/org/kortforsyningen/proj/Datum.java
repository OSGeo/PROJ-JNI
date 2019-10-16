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

import java.util.Date;
import org.opengis.referencing.datum.*;
import org.opengis.util.InternationalString;


/**
 * Wrappers around {@code osgeo::proj::datum::Datum} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class Datum extends IdentifiableObject implements org.opengis.referencing.datum.Datum {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::datum::Datum}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    Datum(final long ptr) {
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
        return null;        // TODO
    }

    /**
     * The time after which this datum definition is valid.
     *
     * @return the datum realization epoch, or {@code null} if not available.
     */
    @Override
    public Date getRealizationEpoch() {
        return null;        // TODO
    }

    /**
     * A datum specialization.
     */
    static final class Geodetic extends Datum implements GeodeticDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Geodetic(final long ptr) {
            super(ptr);
        }

        /**
         * Returns the ellipsoid.
         * @return the ellipsoid.
         */
        @Override
        public Ellipsoid getEllipsoid() {
            return null;            // TODO
        }

        /**
         * Returns the prime meridian.
         * @return the prime meridian.
         */
        @Override
        public PrimeMeridian getPrimeMeridian() {
            return null;            // TODO
        }
    }

    /**
     * A datum specialization.
     */
    static final class Vertical extends Datum implements VerticalDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Vertical(final long ptr) {
            super(ptr);
        }

        /**
         * The type of this vertical datum.
         * This attribute is inherited from the specification published in 2003.
         * The 2007 revision of ISO 19111 removed this attribute, since this information
         * can be encoded in the <cite>anchor definition</cite>.
         *
         * @return the type of this vertical datum.
         */
        @Override
        public VerticalDatumType getVerticalDatumType() {
            return null;
        }
    }

    /**
     * A datum specialization.
     */
    static final class Temporal extends Datum implements TemporalDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Temporal(final long ptr) {
            super(ptr);
        }

        /**
         * The date and time origin of this temporal datum.
         * @return the date and time origin of this temporal datum.
         */
        @Override
        public Date getOrigin() {
            return null;            // TODO
        }
    }

    /**
     * A datum specialization. No new properties compared to parent CS.
     */
    static final class Engineering extends Datum implements EngineeringDatum {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param ptr pointer to the wrapped PROJ object.
         */
        Engineering(final long ptr) {
            super(ptr);
        }
    }
}
