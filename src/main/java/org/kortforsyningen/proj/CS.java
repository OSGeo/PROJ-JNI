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


/**
 * Wrappers around {@code osgeo::proj::cs::CoordinateSystem} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
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
        return impl.getDimension();
    }

    /**
     * Returns the axis for this coordinate system at the specified dimension.
     *
     * @param  dimension  the zero based index of axis.
     * @return the axis at the specified dimension.
     * @throws IndexOutOfBoundsException if {@code dimension} is out of bounds.
     */
    @Override
    public CoordinateSystemAxis getAxis(int dimension) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
