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
package org.osgeo.proj;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengis.util.Factory;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.MathTransform;


/**
 * Static methods for coordinate reference systems and operations.
 * The methods provided in this class are specific to PROJ implementation,
 * but the objects returned by those methods can be used in an implementation independent way.
 * Some entry points are:
 *
 * <ul>
 *   <li>{@link #createFromUserInput(String)} — for creating a {@link CoordinateReferenceSystem}
 *       from an EPSG code, a WKT string, <i>etc</i>.</li>
 *   <li>{@link #createCoordinateOperation createCoordinateOperation(…)} — for creating the
 *       {@link CoordinateOperation} capable to transform coordinate values from one CRS to another.</li>
 * </ul>
 *
 * Alternatively the {@link #getAuthorityFactory(String)} and {@link #getOperationFactory(CoordinateOperationContext)}
 * methods provide similar functionality in a more implementation neutral way, at the cost of one indirection level
 * (need to fetch the factory before invoking methods on it).
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
     * then this method logs an error message and returns an empty value.
     * This method can be used as a way to check if the library is present.
     *
     * @return the PROJ release string, or an empty value if the native library is not found.
     */
    public static Optional<String> version() {
        final Level level;
        final LinkageError error;
        try {
            return Optional.of(NativeResource.version());
        } catch (UnsatisfiedLinkError | NoSuchFieldError e) {
            // Thrown the first time that we try to use the library.
            level = Level.SEVERE;
            error = e;
        } catch (NoClassDefFoundError e) {
            // Thrown on attempts after the first one if the exception was not caught.
            level = Level.FINER;
            error = e;
        }
        Logger.getLogger(NativeResource.LOGGER_NAME).log(level, "Can not link PROJ native library.", error);
        return Optional.empty();
    }

    /**
     * Returns a factory for creating coordinate reference systems from codes allocated by the given authority.
     * The authority is typically "EPSG", but not necessarily; other authorities like "IAU" are also allowed.
     * Typical values are:
     *
     * <ul>
     *   <li>{@code "EPSG"} — de facto standard for coordinate reference systems on Earth.</li>
     *   <li>{@code "IAU"} — extraterrestrial coordinate reference systems.</li>
     *   <li>{@code ""} (empty string) — PROJ default set of authorities.</li>
     * </ul>
     *
     * After a factory has been obtained, its {@link CRSAuthorityFactory#createCoordinateReferenceSystem(String)
     * createCoordinateReferenceSystem(…)} method can be invoked to create a CRS from an authority code.
     * For example the code below creates the <cite>"WGS 84"</cite> coordinate reference system
     * for the <cite>"EPSG::4326"</cite> code.
     * In that example, only the first line is PROJ-specific;
     * the remaining lines can be executed with any GeoAPI implementation:
     *
     * <blockquote><pre>
     * CRSAuthorityFactory factory = Proj.getAuthorityFactory("EPSG");
     * CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("4326");
     * System.out.println(crs.toWKT());</pre>
     * </blockquote>
     *
     * The factory returned by this method is safe for concurrent use in multi-threads environment.
     * The object returned by this method also implements the
     * {@link org.opengis.referencing.cs.CSAuthorityFactory},
     * {@link org.opengis.referencing.datum.DatumAuthorityFactory} and
     * {@link org.opengis.referencing.operation.CoordinateOperationAuthorityFactory} interfaces
     * (so it can be cast to any of those interfaces),
     * but typically only the {@link CRSAuthorityFactory} interface is used.
     *
     * @param  authority  authority name of the factory (e.g. {@code "EPSG"}).
     * @return the {@link CRSAuthorityFactory},
     *         {@link org.opengis.referencing.cs.CSAuthorityFactory},
     *         {@link org.opengis.referencing.datum.DatumAuthorityFactory} and
     *         {@link org.opengis.referencing.operation.CoordinateOperationAuthorityFactory}
     *         for the given authority.
     * @throws NullPointerException if {@code authority} is {@code null}.
     */
    public static CRSAuthorityFactory getAuthorityFactory(final String authority) {
        /*
         * No need to cache since AuthorityFactory.API is a very lightweight object.
         * The costly object is AuthorityFactory, which is cached by Context class.
         */
        return new AuthorityFactory.API(authority.trim());    // Intentional NullPointerException if authority is null.
    }

    /**
     * Creates a new operation factory for the given context.
     *
     * The context is an optional argument which allows
     * specifying the {@linkplain CoordinateOperationContext#setAreaOfInterest area of interest}
     * and {@linkplain CoordinateOperationContext#setDesiredAccuracy(double) desired accuracy}.
     * The returned factory can be used for creating {@link CoordinateOperation}s for given pairs of
     * {@link CoordinateReferenceSystem}s. Example:
     *
     * <blockquote><pre>
     * CoordinateReferenceSystem  sourceCRS = ...;
     * CoordinateReferenceSystem  targetCRS = ...;
     * CoordinateOperationFactory opFactory = Proj.getOperationFactory(null);
     * CoordinateOperation        operation = opFactory.createOperation(sourceCRS, targetCRS);
     * System.out.println(operation.toWKT());</pre>
     * </blockquote>
     *
     * @param  context in which coordinate operations are to be used, or {@code null}
     *                 for the {@linkplain #createCoordinateOperation default setting}.
     * @return a factory for creating coordinate operations in the given context.
     */
    public static CoordinateOperationFactory getOperationFactory(final CoordinateOperationContext context) {
        return new OperationFactory(context);
    }

    /**
     * Returns a factory of the given type. This method recognizes three groups of factories:
     *
     * <ul class="verbose">
     *   <li>
     *     {@link org.opengis.referencing.crs.CRSAuthorityFactory},
     *     {@link org.opengis.referencing.cs.CSAuthorityFactory},
     *     {@link org.opengis.referencing.datum.DatumAuthorityFactory} and
     *     {@link org.opengis.referencing.operation.CoordinateOperationAuthorityFactory}:
     *     equivalent to the factories returned by
     *     <code>{@linkplain #getAuthorityFactory(String) getAuthorityFactory}("")</code>.
     *   </li><li>
     *     {@link org.opengis.referencing.crs.CRSFactory},
     *     {@link org.opengis.referencing.cs.CSFactory} and
     *     {@link org.opengis.referencing.datum.DatumFactory}: no equivalence.
     *     Those factories allow to create customized CRS from components such as
     *     axes, datum, map projection parameters, <i>etc.</i>
     *   </li><li>
     *     {@link org.opengis.referencing.operation.CoordinateOperationFactory}:
     *     equivalent to the factory returned by
     *     <code>{@linkplain #getOperationFactory(CoordinateOperationContext) getOperationFactory}(null)</code>.
     *   </li>
     * </ul>
     *
     * @param  <F>   compile-time value of {@code type} argument.
     * @param  type  type of factory desired.
     * @return factory of the given type.
     * @throws IllegalArgumentException if the specified type is not one of the above-listed types.
     */
    public static <F extends Factory> F getFactory(final Class<F> type) {
        final Factory factory;
        if (org.opengis.referencing.crs.CRSFactory.class.equals(type) ||
            org.opengis.referencing.cs.CSFactory.class.equals(type) ||
            org.opengis.referencing.datum.DatumFactory.class.equals(type))
        {
            factory = ObjectFactory.INSTANCE;
        } else if (org.opengis.referencing.crs.CRSAuthorityFactory.class.equals(type) ||
                   org.opengis.referencing.cs.CSAuthorityFactory.class.equals(type) ||
                   org.opengis.referencing.datum.DatumAuthorityFactory.class.equals(type) ||
                   org.opengis.referencing.operation.CoordinateOperationAuthorityFactory.class.equals(type))
        {
            factory = new AuthorityFactory.API("");
        } else if (CoordinateOperationFactory.class.equals(type)) {
            factory = new OperationFactory(null);
        } else {
            throw new IllegalArgumentException("Unknown factory type: " + type.getSimpleName());
        }
        return type.cast(factory);
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
     * @param  text  one of the above mentioned text format.
     * @return a coordinate reference system or other kind of object created from the given text.
     * @throws FactoryException if the given text can not be parsed.
     *
     * @see #getAuthorityFactory(String)
     * @see CRSAuthorityFactory#createObject(String)
     * @see <a href="https://proj.org/development/reference/cpp/io.html#_CPPv4N5osgeo4proj2io19createFromUserInputERKNSt6stringEP10PJ_CONTEXT">PROJ C++ API</a>
     */
    public static IdentifiedObject createFromUserInput(final String text) throws FactoryException {
        Objects.requireNonNull(text);
        final Object result;
        try (Context c = Context.acquire()) {
            result = c.createFromUserInput(text);
        }
        if (result instanceof IdentifiedObject) {
            return (IdentifiedObject) result;
        }
        throw new FactoryException("Given input does not describe an IdentifiedObject.");
    }

    /**
     * Returns an operation for conversion or transformation between two coordinate reference systems,
     * taking in account the given context. If more than one operation exists, the preferred one is returned.
     * If no operation exists, then an exception is thrown.
     *
     * <p>The context is an optional argument which specifying the
     * {@linkplain CoordinateOperationContext#setAreaOfInterest area of interest} and
     * {@linkplain CoordinateOperationContext#setDesiredAccuracy(double) desired accuracy}.
     * If this argument is {@code null}, then the default setting is:</p>
     *
     * <ul>
     *   <li>Coordinate operations from any authority will be searched,
     *     with the restrictions set in the {@code "authority_to_authority_preference"} database table.</li>
     *   <li>Area of interest is unknown.</li>
     *   <li>Desired accuracy is best accuracy available.</li>
     *   <li>Source and target CRS extents use is {@link SourceTargetCRSExtentUse#SMALLEST}.</li>
     *   <li>Criterion for comparing areas of validity is {@link SpatialCriterion#STRICT_CONTAINMENT}.</li>
     *   <li>Grid availability use is {@link GridAvailabilityUse#USE_FOR_SORTING USE_FOR_SORTING}.</li>
     *   <li>Use of intermediate pivot CRS is allowed.</li>
     * </ul>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  context    context in which the coordinate operation is to be used.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws NullPointerException if {@code sourceCRS} or {@code targetCRS} is {@code null}.
     * @throws UnsupportedImplementationException if a CRS is not a PROJ-JNI implementation.
     * @throws FactoryException if the operation creation failed.
     *
     * @see #getOperationFactory(CoordinateOperationContext)
     * @see CoordinateOperationFactory#createOperation(CoordinateReferenceSystem, CoordinateReferenceSystem)
     */
    public static CoordinateOperation createCoordinateOperation(
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final CoordinateOperationContext context) throws FactoryException
    {
        final CRS source = CRS.cast("sourceCRS", sourceCRS);
        final CRS target = CRS.cast("targetCRS", targetCRS);
        final List<CoordinateOperation> operations = OperationFactory.findOperations(
                source, target, (context != null) ? context : new CoordinateOperationContext());
        if (operations.isEmpty()) {
            throw new OperationNotFoundException(OperationFactory.notFound(source, target));
        }
        return operations.get(0);
    }

    /**
     * Returns operations for conversion or transformation between two coordinate reference systems,
     * taking in account the given context. If no coordinate operation is found, then this method
     * returns an empty list.
     *
     * <p>The context is an optional argument which allows specifying the
     * {@linkplain CoordinateOperationContext#setAreaOfInterest area of interest} and
     * {@linkplain CoordinateOperationContext#setDesiredAccuracy(double) desired accuracy}.
     * If this argument is {@code null}, then the default values are
     * {@linkplain #createCoordinateOperation documented here}.</p>
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  context    context in which the coordinate operation is to be used.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws NullPointerException if {@code sourceCRS} or {@code targetCRS} is {@code null}.
     * @throws UnsupportedImplementationException if a CRS is not a PROJ-JNI implementation.
     * @throws FactoryException if the operation creation failed.
     */
    public static List<CoordinateOperation> createCoordinateOperations(
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final CoordinateOperationContext context) throws FactoryException
    {
        return OperationFactory.findOperations(
                CRS.cast("sourceCRS", sourceCRS),
                CRS.cast("targetCRS", targetCRS),
                (context != null) ? context : new CoordinateOperationContext());
    }

    /**
     * Creates a position with the given coordinate values and an optional CRS.
     *
     * <p>At least one of {@code crs} and {@code coordinates} arguments must be non-null.
     * If both arguments are non-null, then the number of dimensions of the given CRS
     * must match the number of coordinate values.
     *
     * <p>A {@link DirectPosition} instance can be transformed to another CRS by a call to
     * <code>factory.{@linkplain CoordinateOperationFactory#createOperation(CoordinateReferenceSystem,
     * CoordinateReferenceSystem) createOperation}(position.{@linkplain DirectPosition#getCoordinateReferenceSystem()
     * getCoordinateReferenceSystem()})</code> for fetching the coordinate operation, then
     * <code>operation.getMathTransform().{@linkplain MathTransform#transform(DirectPosition, DirectPosition)
     * transform}(position, null)</code> for executing that operation.
     * However those steps are costly and should be applied for only a small number of points.
     * For large number of points, consider using coordinate tuples in {@code float[]} or {@code double[]}
     * arrays instead.
     *
     * <h4>Serialization</h4>
     * The {@code DirectPosition} returned by this method is {@linkplain java.io.Serializable serializable},
     * but the CRS is lost in the serialization process because we do not serialize native PROJ objects.
     *
     * @param  crs          the coordinate reference system, or {@code null} is unspecified.
     * @param  coordinates  the coordinate values, or {@code null} for initializing the position to zero.
     * @return a direct position for the given coordinate values and optional CRS.
     * @throws NullPointerException if both {@code crs} and {@code coordinates} are {@code null}.
     * @throws UnsupportedImplementationException if the given CRS is not a PROJ-JNI implementation.
     * @throws MismatchedDimensionException if the given CRS is non-null but its number of dimensions
     *         is not equal to the length of the {@code coordinates} array.
     *
     * @see MathTransform#transform(DirectPosition, DirectPosition)
     */
    public static DirectPosition createPosition(final CoordinateReferenceSystem crs, double... coordinates) {
        CRS wrapper = null;
        if (crs != null) {
            wrapper = CRS.cast("crs", crs);
            final int dimension = wrapper.getDimension();
            if (coordinates == null) {
                return new SimpleDirectPosition(wrapper, new double[dimension]);
            }
            if (dimension != coordinates.length) {
                throw new MismatchedDimensionException("Number of dimensions in the CRS does not match the number of coordinate values.");
            }
        }
        return new SimpleDirectPosition(wrapper, coordinates.clone());
    }

    /**
     * Returns a coordinate operation with axis order such as the east direction is first
     * and the north direction is second, if possible.
     *
     * @param  operation  the operation for which to adjust axis order.
     * @return an operation with axis order convenient for visualization.
     */
    public static CoordinateOperation normalizeForVisualization(final CoordinateOperation operation) {
        Objects.requireNonNull(operation);
        if (operation instanceof IdentifiableObject) {
            return (CoordinateOperation) ((IdentifiableObject) operation).impl.normalizeForVisualization();
        } else {
            throw new UnsupportedImplementationException("operation", operation);
        }
    }

    /**
     * Returns {@code true} if the given objects are equivalent according the given criterion.
     * If the two given objects are {@code null}, this method returns {@code true}.
     * If one object is null and the other object is non-null, this method returns {@code false}.
     *
     * @param  obj1       first object to compare, or {@code null}.
     * @param  obj2       second object to compare, or {@code null}.
     * @param  criterion  the comparison criterion, which can be strict or relaxed.
     * @return whether the given objects are equivalent according the given criterion.
     */
    public static boolean areEquivalent(final Object obj1, final Object obj2, final ComparisonCriterion criterion) {
        Objects.requireNonNull(criterion);
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 instanceof IdentifiableObject && obj2 instanceof IdentifiableObject) {
            return ((IdentifiableObject) obj1).impl.isEquivalentTo(((IdentifiableObject) obj2).impl, criterion.ordinal());
        } else {
            return (obj1 != null) && obj1.equals(obj2);
        }
    }
}
