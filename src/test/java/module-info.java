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


/**
 * Java Native Interface for the <a href="https://proj.org/">PROJ</a> C/C++ library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
module org.osgeo.proj {
    requires java.logging;
    requires transitive org.opengis.geoapi;

    exports org.osgeo.proj;

    provides org.opengis.referencing.crs.CRSAuthorityFactory
        with org.osgeo.proj.spi.EPSG,
             org.osgeo.proj.spi.IAU;

    provides org.opengis.referencing.operation.CoordinateOperationFactory
        with org.osgeo.proj.spi.OperationFactoryProvider;

    /*
     * Code above this comment was copied from main directory.
     * Code below this comment is added for testing purposes.
     */
    requires junit;
    requires org.opengis.geoapi.conformance;

    uses org.opengis.referencing.crs.CRSAuthorityFactory;
    uses org.opengis.referencing.operation.CoordinateOperationFactory;
}
