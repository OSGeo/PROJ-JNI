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

import java.lang.annotation.Native;


/**
 * Identification of object types.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Type {
    /**
     * Do not allow instantiation of this class.
     */
    private Type() {
    }

    /**
     * Kind of geodetic objects created by native functions invoked from this class.
     */
    @Native
    static final short
            ANY                         =  0,
            IDENTIFIER                  =  1,
            UNIT_OF_MEASURE             =  2,
            AXIS                        =  3,
            COORDINATE_SYSTEM           =  4,
            CARTESIAN_CS                =  5,
            SPHERICAL_CS                =  6,
            VERTICAL_CS                 =  7,
            TEMPORAL_CS                 =  8,
            ELLIPSOIDAL_CS              =  9,
            ELLIPSOID                   = 10,
            PRIME_MERIDIAN              = 11,
            DATUM                       = 12,
            GEODETIC_REFERENCE_FRAME    = 13,
            GEODETIC_CRS                = 14,
            GEOGRAPHIC_CRS              = 15,
            GEOCENTRIC_CRS              = 16,
            PROJECTED_CRS               = 17,
            VERTICAL_REFERENCE_FRAME    = 18,
            VERTICAL_CRS                = 19,
            TEMPORAL_DATUM              = 20,
            TEMPORAL_CRS                = 21,
            ENGINEERING_DATUM           = 22,
            ENGINEERING_CRS             = 23,
            COMPOUND_CRS                = 24,
            COORDINATE_REFERENCE_SYSTEM = 25,
            COORDINATE_OPERATION        = 26,
            OPERATION_METHOD            = 27,
            CONVERSION                  = 28,
            TRANSFORMATION              = 29,
            PARAMETER                   = 30;
}
