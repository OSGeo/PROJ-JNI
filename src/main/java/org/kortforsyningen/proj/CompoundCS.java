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

import org.opengis.metadata.Identifier;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;


/**
 * The coordinate system of a {@link CRS.Compound}.
 * ISO 19111 does not associate coordinate system to compound CRS,
 * but GeoAPI does for user convenience.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
public class CompoundCS implements CoordinateSystem {
    /**
     * The coordinate reference system for which to provide axes. We need to keep a reference to the
     * {@link CRS} object, not its {@link CRS#impl} field, in order to protect from garbage collection.
     */
    private final CRS.Compound crs;

    /**
     * Creates a new compound coordinate system.
     *
     * @param  crs  the coordinate reference system for which to provide axes.
     */
    CompoundCS(final CRS.Compound crs) {
        this.crs = crs;
    }

    /**
     * Returns {@code null} since this coordinate system has no name.
     *
     * @return null.
     */
    @Override
    public Identifier getName() {
        return null;
    }

    /**
     * Returns the number of dimensions in this coordinate system.
     *
     * @return number of dimensions in this coordinate system.
     */
    @Override
    public int getDimension() {
        return getDimension(crs.impl);
    }

    /**
     * Returns the number of dimensions in the given single or compound CRS.
     *
     * @param   crs  wrapper to a PROJ CRS.
     * @return  number of dimension in the given CRS.
     */
    static native int getDimension(SharedPointer crs);

    /**
     * Returns the axis for this coordinate system at the specified dimension.
     *
     * @param  dimension  the zero based index of axis.
     * @return the axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     */
    @Override
    @SuppressWarnings("OverlyStrongTypeCast")       // Casting to final class is easier for the JVM.
    public CoordinateSystemAxis getAxis(int dimension) {
        return (Axis) getAxis(crs.impl, dimension);
    }

    /**
     * Returns the axis for the given compound CRS at the specified dimension.
     *
     * @param  crs  wrapper to a PROJ compound CRS.
     * @param  dimension  the zero based index of axis.
     * @return the axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     */
    static native Object getAxis(SharedPointer crs, int dimension);
}
