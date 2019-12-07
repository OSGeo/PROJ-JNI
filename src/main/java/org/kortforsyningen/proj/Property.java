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
 * Identification of a property to read in an object.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Property {
    /**
     * Do not allow instantiation of this class.
     */
    private Property() {
    }

    /**
     * Identify properties which can be returned by {@link SharedPointer#getObjectProperty(short)} method.
     */
    @Native
    static final short NAME              =  0,
                       COORDINATE_SYSTEM =  1,
                       AXIS_UNIT         =  2,
                       DATUM             =  3,
                       ELLIPSOID         =  4,
                       ELLIPSOID_UNIT    =  5,
                       PRIME_MERIDIAN    =  6,
                       MERIDIAN_UNIT     =  7,
                       BASE_CRS          =  8,
                       CONVERT_FROM_BASE =  9,
                       OPERATION_METHOD  = 10,
                       PARAMETER_UNIT    = 11;

    /**
     * Identify properties which can be returned by {@link SharedPointer#getVectorElement(short, int)} method.
     */
    @Native
    static final short AXIS                = 100,
                       IDENTIFIER          = 101,
                       METHOD_PARAMETER    = 102,
                       OPERATION_PARAMETER = 103,
                       CRS_COMPONENT       = 104,
                       SOURCE_TARGET_CRS   = 105;           // Index 0 for source, 1 for target.

    /**
     * Identify properties which can be returned by {@link SharedPointer#getStringProperty(short)} method.
     */
    @Native
    static final short NAME_STRING         = 200,
                       IDENTIFIER_STRING   = 201,
                       CODESPACE           = 202,
                       CODE                = 203,
                       VERSION             = 204,
                       CITATION_TITLE      = 205,
                       ABBREVIATION        = 206,
                       DIRECTION           = 207,
                       ANCHOR_DEFINITION   = 208,
                       TEMPORAL_ORIGIN     = 209,
                       PUBLICATION_DATE    = 210,
                       SCOPE               = 211,
                       POSITIONAL_ACCURACY = 212,
                       REMARKS             = 213,
                       FORMULA             = 214,
                       FORMULA_TITLE       = 215,
                       OPERATION_VERSION   = 216,
                       PARAMETER_STRING    = 217,
                       PARAMETER_FILE      = 218;

    /**
     * Identify properties which can be returned by {@link SharedPointer#getNumericProperty(short)} method.
     */
    @Native
    static final short MINIMUM         = 300,
                       MAXIMUM         = 301,
                       SEMI_MAJOR      = 302,
                       SEMI_MINOR      = 303,
                       INVERSE_FLAT    = 304,
                       GREENWICH       = 305,
                       PARAMETER_VALUE = 306;

    /**
     * Identify properties which can be returned by {@link SharedPointer#getArrayProperty(short)} method.
     */
    @Native
    static final short DOMAIN_OF_VALIDITY = 400;

    /**
     * Identify properties which can be returned by {@link SharedPointer#getIntegerProperty(short)} method.
     */
    @Native
    static final short PARAMETER_TYPE = 500,
                       PARAMETER_INT  = 501;

    /**
     * Identify properties which can be returned by {@link SharedPointer#getBooleanProperty(short)} method.
     */
    @Native
    static final short HAS_NAME       = 600,
                       IS_SPHERE      = 601,
                       IVF_DEFINITIVE = 602,
                       PARAMETER_BOOL = 603;
}
