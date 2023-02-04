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
 * <p>PROJ is generic coordinate transformation software that transforms geospatial
 * coordinates from one coordinate reference system (CRS) to another.
 * This includes cartographic projections as well as geodetic transformations.
 * This module exposes PROJ services as implementations of <a href="https://www.geoapi.org">GeoAPI</a> interfaces.
 * Both PROJ 6 and GeoAPI are modeled according the ISO 19111 international standard.
 * The use of GeoAPI interfaces allows developers to write their software in an implementation independent way,
 * using the {@link org.osgeo.proj.Proj} class only as a starting point or for PROJ-specific actions.
 *
 * <h2>Installation</h2>
 * <p>This module requires installation of native libraries.
 * Compilation instructions are given <a href="https://github.com/OSGeo/PROJ-JNI/wiki/Install">in this page</a>.
 * The presence of native libraries can be tested as below:</p>
 *
 * <blockquote><pre>
 * {@linkplain org.osgeo.proj.Proj#version()}.isPresent()</pre>
 * </blockquote>
 *
 * <h2>References</h2>
 * <ul>
 *   <li><a href="https://docs.opengeospatial.org/as/18-005r4/18-005r4.html">OGC Abstract Specification Topic 2: Referencing by coordinates</a>
 *   <li><a href="https://www.geoapi.org/3.0/index.html">OGC GeoAPI 3.0</a> (derived from OGC Topic 2)</li>
 *   <li><a href="https://proj.org/development/reference/cpp/index.html">PROJ C++ API</a> (derived from OGC Topic 2)</li>
 *   <li><a href="https://github.com/OSGeo/PROJ-JNI">PROJ-JNI Source code</a></li>
 *   <li><a href="https://docs.oracle.com/en/java/javase/13/docs/specs/jni/index.html">Java Native Interface (JNI) Specification</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
module org.osgeo.proj {
    requires java.logging;
    requires transitive org.opengis.geoapi;

    exports org.osgeo.proj;

    uses javax.measure.spi.ServiceProvider;

    provides org.opengis.referencing.crs.CRSAuthorityFactory
        with org.osgeo.proj.spi.EPSG,
             org.osgeo.proj.spi.IAU;

    provides org.opengis.referencing.operation.CoordinateOperationFactory
        with org.osgeo.proj.spi.OperationFactoryProvider;
}
