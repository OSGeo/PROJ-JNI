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

import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * A trivial implementation of {@link DirectPosition}.
 * Serialization of this class excludes the CRS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class SimpleDirectPosition implements DirectPosition, Serializable {
    /**
     * For cross-version compatibility of serialized objects.
     */
    private static final long serialVersionUID = 1768239094900135558L;

    /**
     * The coordinate reference system, or {@code null} if unknown.
     * This coordinate reference system is not serialized because PROJ wrappers are not serializable.
     */
    private final CRS crs;

    /**
     * The ordinates.
     */
    private final double[] coordinates;

    /**
     * Creates a new direct position initialized to the given ordinate values.
     *
     * @param crs          the coordinate reference system, or {@code null} if unknown.
     * @param coordinates  the coordinate values. This array is <strong>not</strong> cloned.
     */
    SimpleDirectPosition(final CRS crs, final double[] coordinates) {
        this.crs         = crs;
        this.coordinates = coordinates;
    }

    /**
     * Returns coordinate reference system, or {@code null} if unknown.
     *
     * @return the coordinate reference system, or {@code null}.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Returns the length of coordinate sequence (the number of entries).
     *
     * @return the dimensionality of this position.
     */
    @Override
    public int getDimension() {
        return coordinates.length;
    }

    /**
     * Returns a <b>copy</b> of the coordinates presented as an array of double values.
     *
     * @return a copy of the coordinates.
     */
    @Override
    public double[] getCoordinate() {
        return coordinates.clone();
    }

    /**
     * Returns the coordinate at the specified dimension.
     *
     * @param  dimension  the dimension in the range 0 to {@linkplain #getDimension dimension}-1.
     * @return the coordinate at the specified dimension.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public double getOrdinate(final int dimension) {
        return coordinates[dimension];
    }

    /**
     * Sets the coordinate value along the specified dimension.
     *
     * @param  dimension  the dimension for the coordinate of interest.
     * @param  value      the coordinate value of interest.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public void setOrdinate(final int dimension, final double value) {
        coordinates[dimension] = value;
    }

    /**
     * Returns the direct position, which is {@code this}. This method is inherited by the
     * {@link org.opengis.geometry.coordinate.Position} parent interface but become trivial
     * in the {@link DirectPosition} sub-interface.
     *
     * @return {@code this}.
     */
    @Override
    public DirectPosition getDirectPosition() {
        return this;
    }

    /**
     * Returns {@code true} if this direct position is equals to the given object.
     * The comparison criterion is defined in {@link DirectPosition#equals(Object)}.
     *
     * @param  object  the object to compare with this direct position for equality.
     * @return {@code true} if the given object is equals to this direct position.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof DirectPosition) {
            final DirectPosition other = (DirectPosition) object;
            return Objects.equals(crs, other.getCoordinateReferenceSystem() == null)
                 && Arrays.equals(coordinates, other.getCoordinate());
        }
        return false;
    }

    /**
     * Returns a hash code value for this direct position.
     * The calculation performed in this method is specified by {@link DirectPosition#hashCode()}.
     *
     * @return a hash code value for this direct position.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates) + Objects.hashCode(crs);
    }

    /**
     * Returns a string representation of this direct position in <cite>Well-Known Text</cite> (WKT) format.
     *
     * @return a WKT {@code POINT} construct.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Well-known_text">Well-known text on Wikipedia</a>
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("POINT");
        char separator = '(';
        for (final double ordinate : coordinates) {
            buffer.append(separator).append(ordinate);
            separator = ' ';
        }
        return buffer.append(')').toString();
    }
}
