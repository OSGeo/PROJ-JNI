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
 * Spatial criterion to restrict candidate operations.
 *
 * @version 2.0
 * @since   1.0
 *
 * @see <a href="https://proj.org/development/reference/cpp/operation.html#_CPPv4N5osgeo4proj9operation16SpatialCriterionE">PROJ C++ API</a>
 */
public enum SpatialCriterion {
    /**
     * The area of validity of transforms should strictly contain the are of interest.
     */
    STRICT_CONTAINMENT,

    /**
     * The area of validity of transforms should at least intersect the area of interest.
     */
    PARTIAL_INTERSECTION,

    /**
     * The PROJ default value.
     * Documented as {@link #STRICT_CONTAINMENT} as of PROJ 6.2.
     */
    DEFAULT
}
