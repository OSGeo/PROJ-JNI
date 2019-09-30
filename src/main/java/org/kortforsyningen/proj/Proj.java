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

import java.util.Objects;
import java.util.Optional;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Static methods for coordinate reference systems and operations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final class Proj {
    /**
     * Do not allow instantiation of this class.
     */
    private Proj() {
    }

    /**
     * Returns the version number of the PROJ library.
     * If the PROJ library is not installed on the current system,
     * then this method logs a warning and return an empty value.
     * This method can be used as a way to check if the library is present.
     *
     * @return the PROJ release string, or an empty value if the native library has not been found.
     */
    public static Optional<String> version() {
        final System.Logger.Level level;
        final LinkageError error;
        try {
            return Optional.of(NativeResource.version());
        } catch (UnsatisfiedLinkError e) {
            // Thrown the first time that we try to use the library.
            level = System.Logger.Level.WARNING;
            error = e;
        } catch (NoClassDefFoundError e) {
            // Thrown on attempts after the first one if the exception was not caught.
            level = System.Logger.Level.TRACE;
            error = e;
        }
        System.getLogger(NativeResource.LOGGER_NAME).log(level, "Can not link PROJ native library.", error);
        return Optional.empty();
    }

    /**
     * Returns a factory for creating coordinate reference systems from codes allocated by the given authority.
     * The authority is typically "EPSG", but not necessarily; other authorities like "IAU" are also allowed.
     * After a factory has been obtained, its {@link CRSAuthorityFactory#createCoordinateReferenceSystem(String)}
     * method can be invoked for creating a CRS from an authority code. For example the code below creates the
     * "WGS 84" coordinate reference system for the "EPSG::4326" authority code:
     *
     * <blockquote><pre>
     * CRSAuthorityFactory factory = Proj.getAuthorityFactory("EPSG");
     * CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("4326");
     * </pre></blockquote>
     *
     * <p>The {@link CRSAuthorityFactory} interface provides an implementation-neutral way to create
     * coordinate reference systems. In above example, only the first line is PROJ-specific.
     * The remaining lines can be executed with any GeoAPI implementation.</p>
     *
     * <p>The factory returned by this method is safe for concurrent use in multi-threads environment.
     * The object returned by this method implements also the
     * {@link org.opengis.referencing.cs.CSAuthorityFactory},
     * {@link org.opengis.referencing.datum.DatumAuthorityFactory} and
     * {@link org.opengis.referencing.operation.CoordinateOperationAuthorityFactory} interfaces
     * (so it can be casted to any of those interfaces),
     * but typically only the {@link CRSAuthorityFactory} interface is used.</p>
     *
     * @param  authority  authority name of the factory (e.g. {@code "EPSG"}).
     * @return factory for the given authority.
     */
    public static CRSAuthorityFactory getAuthorityFactory(final String authority) {
        /*
         * No need to cache since AuthorityFactory.API is a very lightweight object.
         * The costly object is AuthorityFactory, which is cached by Context class.
         */
        return new AuthorityFactory.API(Objects.requireNonNull(authority));
    }

    /**
     * Instantiate a geodetic object from a user specified text.
     * The returned object will typically by a subtype of {@link CoordinateReferenceSystem}.
     * The text can be a:
     *
     * <ul>
     *   <li>WKT string.</li>
     *   <li>PROJ string.</li>
     *   <li>database code, prefixed by its authority. Example: {@code “EPSG:4326”}.</li>
     *   <li>OGC URN. Examples:
     *       {@code “urn:ogc:def:crs:EPSG::4326”}, {@code “urn:ogc:def:coordinateOperation:EPSG::1671”},
     *       {@code “urn:ogc:def:ellipsoid:EPSG::7001”} or {@code “urn:ogc:def:datum:EPSG::6326”}.</li>
     *   <li>OGC URN combining references for compound coordinate reference systems. Examples:
     *       {@code “urn:ogc:def:crs,crs:EPSG::2393,crs:EPSG::5717”}.
     *       We also accept a custom abbreviated syntax {@code “EPSG:2393+5717”}.</li>
     *   <li>OGC URN combining references for references for projected or derived CRSs.
     *       Example for Projected 3D CRS <cite>“UTM zone 31N / WGS 84 (3D)”</cite>:
     *       {@code “urn:ogc:def:crs,crs:EPSG::4979,cs:PROJ::ENh,coordinateOperation:EPSG::16031”}.</li>
     *   <li>OGC URN combining references for concatenated operations. Example:
     *       {@code “urn:ogc:def:coordinateOperation,coordinateOperation:EPSG::3895,coordinateOperation:EPSG::1618”}.</li>
     *   <li>An Object name. Example: “WGS 84”, “WGS 84 / UTM zone 31N”. In that case as uniqueness is not guaranteed,
     *       the function may apply heuristics to determine the appropriate best match.</li>
     *   <li>PROJJSON string.</li>
     * </ul>
     *
     * @param  text  One of the above mentioned text format.
     * @return a coordinate reference system or other kind of object created from the given text.
     * @throws FactoryException if the given text can not be parsed.
     *
     * @see <a href="https://proj.org/development/reference/cpp/io.html#_CPPv4N5osgeo4proj2io19createFromUserInputERKNSt6stringEP10PJ_CONTEXT">PROJ C++ API</a>
     */
    public static Object createFromUserInput(final String text) throws FactoryException {
        Objects.requireNonNull(text);
        try (Context c = Context.acquire()) {
            return c.createFromUserInput(text);
        }
    }
}
