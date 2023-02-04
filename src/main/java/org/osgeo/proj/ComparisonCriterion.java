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


/**
 * The criterion to use for comparing PROJ objects.
 *
 * @version 1.1
 * @since   1.0
 *
 * @see Proj#areEquivalent(Object, Object, ComparisonCriterion)
 * @see <a href="https://proj.org/development/reference/cpp/util.html#_CPPv4N5osgeo4proj4util9CriterionE">PROJ C++ API</a>
 */
public enum ComparisonCriterion {
    /**
     * All properties are identical.
     */
    STRICT,

    /**
     * The objects are equivalent for the purpose of coordinate operations.
     * They can differ by the name of their objects, identifiers, other metadata.
     * Parameters may be expressed in different units, provided that the value is
     * (with some tolerance) the same once expressed in a common unit.
     */
    EQUIVALENT,

    /**
     * Same as {@link #EQUIVALENT}, relaxed with an exception that the axis order of the base CRS of a
     * {@link org.opengis.referencing.crs.DerivedCRS}/{@link org.opengis.referencing.crs.ProjectedCRS}
     * or the axis order of a {@link org.opengis.referencing.crs.GeographicCRS} is ignored.
     * Only to be used with {@code DerivedCRS}, {@code ProjectedCRS} or {@code GeographicCRS}.
     */
    EQUIVALENT_EXCEPT_AXIS_ORDER_GEOGCRS
}
