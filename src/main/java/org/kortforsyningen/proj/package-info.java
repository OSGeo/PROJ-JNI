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

/**
 * Java Native Interface for the <a href="https://proj.org/">PROJ</a> C/C++ library.
 * PROJ is a generic coordinate transformation software that transforms geospatial
 * coordinates from one coordinate reference system (CRS) to another.
 * This includes cartographic projections as well as geodetic transformations.
 * This package exposes PROJ services as implementations of <a href="http://www.geoapi.org">GeoAPI</a> interfaces.
 * Both PROJ 6 and GeoAPI are modeled according the ISO 19111 international standard.
 * The use of GeoAPI interfaces allows developers to write their software in an implementation independent way,
 * using the {@link Proj} class only as a starting point or for PROJ-specific actions.
 *
 * <p>This package requires installation of native libraries.
 * The presence of those libraries can be tested as below:</p>
 *
 * <blockquote><pre>
 * {@linkplain Proj#version()}.isPresent()</pre>
 * </blockquote>
 *
 * <p>Coordinate operations can be performed as below. In this example, only the first line is PROJ-specific.
 * All other lines should work in the same way with any GeoAPI implementations.</p>
 *
 * <blockquote><pre>
 * CRSAuthorityFactory       factory   = Proj.getAuthorityFactory("EPSG");
 * CoordinateReferenceSystem sourceCRS = factory.createCoordinateReferenceSystem("4326");   // WGS 84
 * CoordinateReferenceSystem sourceCRS = factory.createCoordinateReferenceSystem("3395");   // WGS 84 / World Mercator
 * // TODO: complete.</pre>
 * </blockquote>
 *
 * <p><b>Multi-threading:</b></p>
 * <p>Unless otherwise noted in Javadoc, all classes are safe for use in multi-thread environment.
 * However there is a limit in the number of concurrent threads which can use efficiently the same
 * {@link org.opengis.referencing.operation.MathTransform}. This limit can be controlled by assigning an
 * integer to the "{@systemProperty org.kortforsyningen.proj.optimalNumThreads}" system property at startup time.
 * A low value does not necessarily block more threads from using a
 * {@link org.opengis.referencing.operation.MathTransform} concurrently,
 * but the extra threads may observe a performance degradation.
 * Conversely a too high value may retain more resources than necessary.
 * The current default value is 4.</p>
 *
 * <p><b>Security:</b></p>
 * <p>PROJ-JNI can be executed in a security constrained environment if the {@code "loadLibrary.libproj-binding"}
 * runtime permission is granted. An example is given in the {@code security.policy} file.</p>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li><a href="http://docs.opengeospatial.org/as/18-005r4/18-005r4.html">OGC Abstract Specification Topic 2: Referencing by coordinates</a>
 *   <li><a href="https://www.geoapi.org/3.0/index.html">OGC GeoAPI 3.0.1</a> (derived from OGC Topic 2)</li>
 *   <li><a href="https://proj.org/development/reference/cpp/index.html">PROJ C++ API</a> (derived from OGC Topic 2)</li>
 *   <li><a href="https://github.com/Kortforsyningen/PROJ-JNI">PROJ-JNI Source code</a></li>
 *   <li><a href="https://docs.oracle.com/en/java/javase/13/docs/specs/jni/index.html">Java Native Interface (JNI) Specification</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
package org.kortforsyningen.proj;
