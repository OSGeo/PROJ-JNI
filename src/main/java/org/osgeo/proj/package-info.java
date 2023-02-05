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

/**
 * Java Native Interface for the <a href="https://proj.org/">PROJ</a> C/C++ library.
 *
 * <h2>Usage example</h2>
 * <p>Coordinate operations can be performed as below (a more complete example is available
 * <a href="https://github.com/OSGeo/PROJ-JNI/blob/main/example/TransformPoints.java">here</a>).
 * In this example, only the calls to {@link Proj} static methods are specific to this implementation.
 * All other lines should work in the same way with any GeoAPI implementation.
 * Note that geographic coordinates are in <var>latitude</var>, <var>longitude</var> order,
 * as specified in the EPSG database and in agreement with centuries of practice.</p>
 *
 * <blockquote><pre>
 * CRSAuthorityFactory        factory   = Proj.getAuthorityFactory("EPSG");
 * CoordinateOperationFactory regops    = Proj.getOperationFactory(null);
 * CoordinateReferenceSystem  sourceCRS = factory.createCoordinateReferenceSystem("4326");   // WGS 84
 * CoordinateReferenceSystem  targetCRS = factory.createCoordinateReferenceSystem("3395");   // WGS 84 / World Mercator
 * CoordinateOperation        operation = regops.createOperation(sourceCRS, targetCRS);
 * double[] coordinates = {
 *     45.500,  -73.567,                    // Montreal
 *     49.250, -123.100,                    // Vancouver
 *     35.653,  139.839,                    // Tokyo
 *     48.865,    2.349                     // Paris
 * };
 * operation.getMathTransform().transform(
 *         coordinates, 0,                  // Source coordinates.
 *         coordinates, 0,                  // Target coordinates (in this case, overwrite sources).
 *         4);                              // Number of points to transform.
 *
 * System.out.printf("Montreal:  %11.1f %11.1f%n", coordinates[0], coordinates[1]);
 * System.out.printf("Vancouver: %11.1f %11.1f%n", coordinates[2], coordinates[3]);
 * System.out.printf("Tokyo:     %11.1f %11.1f%n", coordinates[4], coordinates[5]);
 * System.out.printf("Paris:     %11.1f %11.1f%n", coordinates[6], coordinates[7]);</pre>
 * </blockquote>
 *
 * <h2>Performance considerations</h2>
 * <p>Calls to {@code createCoordinateOperation(…)} methods may be costly.
 * Developers should get a {@link org.opengis.referencing.operation.CoordinateOperation} instance only once
 * for a given pair of {@link org.opengis.referencing.crs.CoordinateReferenceSystem}s and keep that reference
 * as long as they may need it.</p>
 *
 * <p>Calls to {@code MathTransform.transform(…)} methods may also be costly.
 * Developers should avoid invoking those methods repeatedly for each point to transform.
 * For example it is much more efficient to invoke {@code transform(double[], …)} only once
 * for an array of 4 points than to invoke that method 4 times (once for each point).
 * Above example shows the recommended way to use a transform.</p>
 *
 * <h2>Multi-threading</h2>
 * <p>Unless otherwise noted in Javadoc, all classes are safe for use in multi-thread environment.
 * However there is a limit in the number of concurrent threads which can use efficiently the same
 * {@link org.opengis.referencing.operation.MathTransform} instance.
 * This limit can be controlled by assigning an integer to the
 * "{@systemProperty org.osgeo.proj.maxThreadsPerInstance}" system property at startup time.
 * A low value does not necessarily block more threads from using a
 * {@link org.opengis.referencing.operation.MathTransform} concurrently,
 * but the extra threads may observe a performance degradation.
 * Conversely a too high value may retain more resources than necessary.
 * The current default value is 4.</p>
 *
 * <p>Note that there is no limit on Java side in the amount of threads that can use <em>different</em>
 * {@link org.opengis.referencing.operation.MathTransform} instances concurrently.</p>
 *
 * <h2>String representation</h2>
 * <p>Referencing objects such as CRS, datum, <i>etc.</i>
 * implement the {@link org.opengis.referencing.IdentifiedObject#toWKT()} method,
 * which can be used for getting a string representation in <cite>Well Known Text</cite> (WKT) version 2 format.
 * The {@link Object#toString()} method can also be used for getting a similar string representation,
 * but in a {@linkplain ReferencingFormat.Convention#WKT_SIMPLIFIED slightly simplified} version.
 * Note that those string representations do not perform database access, and consequently may be less
 * complete than the formatting done by {@link ReferencingFormat}.</p>
 *
 * <p>Referencing objects also implement the {@link java.util.Formattable} interface.
 * The {@code "%s"} flag formats the object name, while the alternative form {@code "%#s"}
 * formats the authority (typically EPSG) code.</p>
 *
 * <h2>Unsupported features</h2>
 * <p>The following method calls will cause an exception to be thrown:</p>
 * <ul>
 *   <li>{@link org.opengis.referencing.crs.CRSFactory#createFromXML(String)} — XML support requires GDAL.</li>
 *   <li>{@link org.opengis.referencing.AuthorityFactory#getAuthorityCodes(Class)} — not yet implemented in this binding.</li>
 *   <li>{@link org.opengis.referencing.operation.CoordinateOperationFactory#createDefiningConversion
 *       CoordinateOperationFactory.createDefiningConversion(…)} — not yet implemented in this binding.</li>
 *   <li>{@link org.opengis.referencing.crs.DerivedCRS} — not yet implemented in this binding.</li>
 *   <li>{@link org.opengis.referencing.operation.MathTransform#derivative MathTransform.derivative(DirectPosition)} —
 *       Jacobian matrix calculation is not yet supported by PROJ.</li>
 *   <li>{@link org.opengis.referencing.operation.ConcatenatedOperation} — not yet implemented explicitly in this binding
 *       (but concatenated operations created by PROJ still work).</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
package org.osgeo.proj;
