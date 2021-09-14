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

/**
 * Specifies how source and target CRS extents should be used to restrict candidate operations.
 * This is taken into account only if no explicit area of interest is specified.
 *
 * @version 1.0
 * @since   1.0
 *
 * @see <a href="https://proj.org/development/reference/cpp/operation.html#_CPPv4N5osgeo4proj9operation24SourceTargetCRSExtentUseE">PROJ C++ API</a>
 */
public enum SourceTargetCRSExtentUse {
    /**
     * Ignore CRS extent.
     */
    NONE,

    /**
     * Test coordinate operation extent against both CRS extents.
     */
    BOTH,

    /**
     * Test coordinate operation extent against the intersection of both CRS extents.
     */
    INTERSECTION,

    /**
     * Test coordinate operation against the smallest of both CRS extents.
     */
    SMALLEST,

    /**
     * The PROJ default value.
     * Documented as {@link #SMALLEST} as of PROJ 6.2.
     */
    DEFAULT
}
