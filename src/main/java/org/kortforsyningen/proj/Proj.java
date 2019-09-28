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
     * Instantiate a geodetic object from a user specified text.The returned object will typically by a subtype of {@link CoordinateReferenceSystem}.
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
            return createFromUserInput(text, 0);    // TODO
        }
    }

    /**
     * Invokes the C++ {@code createFromUserInput(text, ctx)} method.
     *
     * @param  text  the text to parse. It is caller responsibility to ensure that this argument is non-null.
     * @param  ptr   pointer to the {@code PJ_CONTEXT} structure in PROJ heap.
     * @return a coordinate reference system or other kind of object created from the given text.
     */
    private static native Object createFromUserInput(final String text, final long ptr);
}
