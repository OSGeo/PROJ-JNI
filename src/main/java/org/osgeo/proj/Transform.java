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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;


/**
 * Wrappers around {@code PJ}, which performs the actual coordinate operations.
 * Each {@code Transform} instance shall be used by only one thread at a time.
 * Furthermore each {@code Transform} is created for a specific {@link Context}
 * and should be used only in that context.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Transform extends NativeResource {
    /**
     * Creates a new {@code PJ}.
     *
     * @param  operation  wrapper for the operation for which to create a transform.
     * @param  context    the thread context in which the operation will be executed.
     * @throws FactoryException if the PROJ object can not be allocated.
     * @throws TransformException if the construction failed.
     */
    Transform(final NativeResource operation, final Context context) throws FactoryException, TransformException {
        super(context.createPJ(operation));
    }

    /**
     * Assigns a {@code PJ_CONTEXT} to the {@code PJ} wrapped by this {@code Transform}.
     * This method must be invoked before and after call to {@link #transform} method.
     *
     * @param  context  the context to assign, or {@code null} for removing context assignment.
     */
    native void assign(Context context);

    /**
     * Transforms in-place the coordinates in the given array.
     * The coordinates array shall contain (<var>x</var>,<var>y</var>,<var>z</var>,<var>t</var>,…) tuples,
     * where the <var>z</var> and any additional dimensions are optional.
     * Note that any dimension after the <var>t</var> value are ignored.
     *
     * <p>It is caller's responsibility to ensure that the following conditions:</p>
     * <ul>
     *   <li>{@code coordinates} is non-null.</li>
     *   <li>{@code offset + numPts*dimension < coordinates.length}.</li>
     *   <li>{@code dimension}, {@code offset} and {@code numPts} are positive.</li>
     * </ul>
     *
     * @param  dimension    the dimension of each coordinate value. Shall be strictly positive and not too large.
     * @param  coordinates  the coordinates to transform, as a sequence of (<var>x</var>,<var>y</var>,<var>z</var>,…) tuples.
     * @param  offset       offset of the first coordinate in the given array.
     * @param  numPts       number of points to transform.
     * @throws TransformException if the operation failed.
     */
    native void transform(int dimension, double[] coordinates, int offset, int numPts) throws TransformException;

    /**
     * Destroys the {@code PJ} object.
     */
    native void destroy();
}
