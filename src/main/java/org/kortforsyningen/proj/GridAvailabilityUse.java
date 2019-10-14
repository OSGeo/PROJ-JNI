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


/**
 * Describes how grid availability is used.
 *
 * @version 1.0
 * @since   1.0
 *
 * @see <a href="https://proj.org/development/reference/cpp/operation.html#_CPPv4N5osgeo4proj9operation19GridAvailabilityUseE">PROJ C++ API</a>
 */
public enum GridAvailabilityUse {
    /**
     * Grid availability is only used for sorting results.
     * Operations where some grids are missing will be sorted last.
     */
    USE_FOR_SORTING,

    /**
     * Completely discard an operation if a required grid is missing.
     */
    DISCARD_OPERATION_IF_MISSING_GRID,

    /**
     * Ignore grid availability at all.
     * Results will be presented as if all grids were available.
     */
    IGNORE_GRID_AVAILABILITY,

    /**
     * The PROJ default value.
     * Documented as {@link #USE_FOR_SORTING} as of PROJ 6.2.
     */
    DEFAULT
}
