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

import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;

import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;


/**
 * An {@link Extent} containing only a single {@link GeographicBoundingBox}.
 * In order to keep the model simpler, this simple geographic bounding box
 * is also an extent with no vertical or temporal elements.
 *
 * <p>The latitudes and longitudes are on an unspecified ellipsoid.
 * The exact datum does not matter since this information is only approximate.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
final class SimpleExtent implements GeographicBoundingBox, Extent, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1903587394190508333L;

    /**
     * The western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    private final double westBoundLongitude;

    /**
     * The eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     */
    private final double eastBoundLongitude;

    /**
     * The southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    private final double southBoundLatitude;

    /**
     * The northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     */
    private final double northBoundLatitude;

    /**
     * Creates a geographic bounding box initialized to the specified values.
     *
     * <p><strong>Caution:</strong> Arguments are expected in the same order than they appear in the
     * ISO 19115 specification. This is different than the order commonly found in Java world,
     * which is rather (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>,
     * <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>).</p>
     *
     * @param  westBoundLongitude  the minimal <var>x</var> value.
     * @param  eastBoundLongitude  the maximal <var>x</var> value.
     * @param  southBoundLatitude  the minimal <var>y</var> value.
     * @param  northBoundLatitude  the maximal <var>y</var> value.
     * @throws IllegalArgumentException if (<var>west bound</var> &gt; <var>east bound</var>)
     *         or (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     */
    SimpleExtent(final double westBoundLongitude,
                 final double eastBoundLongitude,
                 final double southBoundLatitude,
                 final double northBoundLatitude)
    {
        final String dim;
        final double min, max;
        if (westBoundLongitude > eastBoundLongitude) {
            min = westBoundLongitude;
            max = eastBoundLongitude;
            dim = "longitude";
            // Exception will be thrown below.
        } else if (southBoundLatitude > northBoundLatitude) {
            min = southBoundLatitude;
            max = northBoundLatitude;
            dim = "latitude";
            // Exception will be thrown below.
        } else {
            this.westBoundLongitude = westBoundLongitude;
            this.eastBoundLongitude = eastBoundLongitude;
            this.southBoundLatitude = southBoundLatitude;
            this.northBoundLatitude = northBoundLatitude;
            return;
        }
        throw new IllegalArgumentException("Illegal " + dim + " range: [" + min + " … " + max + "].");
    }

    /**
     * Returns the western-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * @return the western-most longitude between -180 and +180°.
     */
    @Override
    public double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    /**
     * Returns the eastern-most coordinate of the limit of the dataset extent.
     * The value is expressed in longitude in decimal degrees (positive east).
     *
     * @return the eastern-most longitude between -180 and +180°.
     */
    @Override
    public double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    /**
     * Returns the southern-most coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return the southern-most latitude between -90 and +90°.
     */
    @Override
    public double getSouthBoundLatitude() {
        return southBoundLatitude;
    }

    /**
     * Returns the northern-most, coordinate of the limit of the dataset extent.
     * The value is expressed in latitude in decimal degrees (positive north).
     *
     * @return the northern-most latitude between -90 and +90°.
     */
    @Override
    public double getNorthBoundLatitude() {
        return northBoundLatitude;
    }

    /**
     * Indication of whether the bounding box encompasses an area covered by the data
     * (<cite>inclusion</cite>) or an area where data is not present (<cite>exclusion</cite>).
     * The default implementation unconditionally returns {@link Boolean#TRUE}.
     *
     * @return {@code true} for inclusion, or {@code false} for exclusion.
     */
    @Override
    public Boolean getInclusion() {
        return Boolean.TRUE;
    }

    /**
     * Provides geographic component of the extent of the referring object.
     * The default implementation returns a singleton containing only this
     * geographic bounding box.
     *
     * @return the geographic extent, or an empty set if none.
     */
    @Override
    public Collection<? extends GeographicExtent> getGeographicElements() {
        return Collections.singleton(this);
    }

    /**
     * Returns {@code true} if the given floating point values are equal.
     *
     * @param  a  the first value to compare.
     * @param  b  the second value to compare.
     * @return whether the given values are equal.
     */
    private static boolean equals(final double a, final double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    /**
     * Compares this geographic bounding box with the specified object for equality.
     *
     * @param  object  the object to compare for equality.
     * @return {@code true} if the given object is equal to this box.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof SimpleExtent) {
            final SimpleExtent that = (SimpleExtent) object;
            return equals(this.southBoundLatitude, that.southBoundLatitude) &&
                   equals(this.northBoundLatitude, that.northBoundLatitude) &&
                   equals(this.eastBoundLongitude, that.eastBoundLongitude) &&
                   equals(this.westBoundLongitude, that.westBoundLongitude);
        }
        return false;
    }

    /**
     * Returns a hash code value for this bounding box.
     * This value may change in any future version.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Double.hashCode(serialVersionUID ^
               (Double.doubleToLongBits(westBoundLongitude) + 31*
               (Double.doubleToLongBits(eastBoundLongitude) + 31*
               (Double.doubleToLongBits(southBoundLatitude) + 31*
                Double.doubleToLongBits(northBoundLatitude)))));
    }

    /**
     * Returns a string representation of this extent.
     *
     * @return a string representation of this box.
     */
    @Override
    public String toString() {
        return "GeographicBoundingBox[" +
                "west="  + westBoundLongitude + ", " +
                "east="  + eastBoundLongitude + ", " +
                "south=" + southBoundLatitude + ", " +
                "north=" + northBoundLatitude + ']';
    }
}
