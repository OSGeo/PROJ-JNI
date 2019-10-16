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
            PRIME_MERIDIAN              =  2,
            ELLIPSOID                   =  3,
            DATUM                       =  4,
            GEODETIC_REFERENCE_FRAME    =  5,
            VERTICAL_REFERENCE_FRAME    =  6,
            TEMPORAL_DATUM              =  7,
            ENGINEERING_DATUM           =  8,
            UNIT_OF_MEASURE             =  9,
            AXIS                        = 10,
            COORDINATE_SYSTEM           = 11,
            CARTESIAN_CS                = 12,
            SPHERICAL_CS                = 13,
            ELLIPSOIDAL_CS              = 14,
            VERTICAL_CS                 = 15,
            TEMPORAL_CS                 = 16,
            COORDINATE_REFERENCE_SYSTEM = 17,
            GEODETIC_CRS                = 18,
            GEOGRAPHIC_CRS              = 19,
            VERTICAL_CRS                = 20,
            TEMPORAL_CRS                = 21,
            ENGINEERING_CRS             = 22,
            PROJECTED_CRS               = 23,
            COMPOUND_CRS                = 24,
            CONVERSION                  = 25,
            COORDINATE_OPERATION        = 26;
}
