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
package org.osgeo.proj;

import java.util.Set;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import javax.measure.Unit;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;


/**
 * Wrappers around {@code osgeo::proj::io::AuthorityFactory}.
 * This is an entry point by which geodetic objects can be created from authority codes.
 *
 * <p>Each {@code osgeo::proj::io::AuthorityFactory} contains (indirectly) a reference
 * to a {@code PJ_CONTEXT} object. Consequently those two objects shall be used in the
 * same thread.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
final class AuthorityFactory extends NativeResource {
    /**
     * Creates a new factory for the given authority.
     * This constructor should not be invoked directly; use {@link Context#factory(String)} instead.
     *
     * @param  context    pointer to the PROJ thread context.
     * @param  authority  the authority name, for example {@code "EPSG"}.
     * @throws FactoryException if the factory can not be created.
     *
     * @see Context#factory(String)
     */
    AuthorityFactory(final Context context, final String authority) throws FactoryException {
        super(newInstance(context, Objects.requireNonNull(authority)));
    }

    /**
     * Instantiates an {@code osgeo::proj::io::AuthorityFactory}.
     * Also creates the {@code osgeo::proj::io::DatabaseContext}.
     * Each database context can be used by only one thread.
     *
     * @param  context    pointer to the PROJ thread context.
     * @param  authority  name of the authority. Shall not be null.
     * @return shared pointer to the factory, or 0 if out of memory.
     * @throws FactoryException if the factory can not be created.
     */
    private static native long newInstance(Context context, String authority) throws FactoryException;

    /**
     * Gets a description of the object corresponding to a code.
     * In case of several objects of different types with the same code,
     * one of them will be arbitrarily selected.
     *
     * @param  code  object code allocated by authority (e.g. "4326").
     * @return description of the identified object, or null if that object has no description.
     * @throws FactoryException if the description can not be obtained for the given code.
     */
    private native String getDescriptionText(String code) throws FactoryException;

    /**
     * Creates an {@code osgeo::proj::common::IdentifiedObject} from the specified code.
     * The PROJ method invoked by this function is determined by the {@code type} argument.
     *
     * @param  type  one of {@link Type#ELLIPSOID}, {@link Type#PRIME_MERIDIAN}, <i>etc.</i> constants.
     * @param  code  object code allocated by authority.
     * @return wrapper for the PROJ shared object, or {@code null} if out of memory.
     * @throws FactoryException if no object can be created for the given code.
     */
    private native Object createGeodeticObject(short type, String code) throws FactoryException;

    /**
     * Finds a list of coordinate operation between the given source and target CRS.
     * The operations are sorted with the most relevant ones first: by descending area
     * (intersection of the transformation area with the area of interest, or intersection
     * of the transformation with the area of use of the CRS), and by increasing accuracy.
     * Operations with unknown accuracy are sorted last, whatever their area.
     *
     * <p>All enumeration values in arguments are represented by integer, with -1 for the PROJ default value.</p>
     *
     * @param  sourceCRS                    input coordinate reference system.
     * @param  targetCRS                    output coordinate reference system.
     * @param  westBoundLongitude           the minimal <var>x</var> value (degrees).
     * @param  eastBoundLongitude           the maximal <var>x</var> value (degrees).
     * @param  southBoundLatitude           the minimal <var>y</var> value (degrees).
     * @param  northBoundLatitude           the maximal <var>y</var> value (degrees).
     * @param  desiredAccuracy              desired accuracy (in metres), or 0 for the best accuracy available.
     * @param  sourceAndTargetCRSExtentUse  how CRS extents are used when considering if a transformation can be used.
     * @param  spatialCriterion             criterion when comparing the areas of validity.
     * @param  gridAvailabilityUse          how grid availability is used.
     * @param  allowUseIntermediateCRS      whether an intermediate pivot CRS can be used for researching coordinate operations.
     * @param  discardSuperseded            whether transformations that are superseded (but not deprecated) should be discarded.
     * @return the coordinate operations.
     * @throws FactoryException if an error occurred while searching the coordinate operations.
     *
     * @todo add missing parameters, returns a list.
     */
    native Operation createOperation(NativeResource sourceCRS, NativeResource targetCRS,
            double westBoundLongitude, double eastBoundLongitude,
            double southBoundLatitude, double northBoundLatitude,
            double desiredAccuracy,
            int sourceAndTargetCRSExtentUse, int spatialCriterion, int gridAvailabilityUse, int allowUseIntermediateCRS,
            boolean discardSuperseded)
            throws FactoryException;

    /**
     * Releases resources used by this factory. This method decrements the {@code object.use_count()}
     * value of the shared pointer. The authority factory is not necessarily immediately destroyed;
     * it depends on whether it is still used by other C++ code.
     */
    native void release();




    /**
     * Public API to all supported authority factories. All {@code createFoo(code)} methods invoked in this class
     * delegate their work to the enclosing {@link AuthorityFactory}, which is itself a wrapper for PROJ factory.
     * We use a single class for CRS, CS and datum factories for implementation convenience, because we delegate
     * to a single factory implementation anyway.
     *
     * <p>This class is thread-safe, contrarily to the enclosing {@link AuthorityFactory} which is not.
     * This class uses a pool of {@link AuthorityFactory} instances for making sure that each instance
     * is used by only one thread at a time. There is no guarantee that two consecutive invocations of
     * {@code createFoo(…)} methods in the same thread will use the same {@link AuthorityFactory} instance.</p>
     */
    static final class API implements CRSAuthorityFactory, CSAuthorityFactory, DatumAuthorityFactory,
            CoordinateOperationAuthorityFactory
    {
        /**
         * The authority name of this factory.
         */
        private final String authority;

        /**
         * Creates a new factory for the given authority.
         * It is caller responsibility to ensure that the given argument is non-null.
         *
         * @param  authority  authority name of this factory.
         */
        API(final String authority) {
            this.authority = authority;
        }

        /**
         * Creates an object (coordinate system, CRS, datum, …) for the given authority code.
         *
         * @param  <T>     compile-time value of {@code classe} argument.
         * @param  classe  the expected Java class of the object to create.
         * @param  type    one of {@link Type#ELLIPSOID}, {@link Type#PRIME_MERIDIAN}, <i>etc.</i> constants.
         * @param  code    object code allocated by authority.
         * @return wrapper for the PROJ object.
         * @throws FactoryException if no object can be created for the given code.
         */
        private <T> T createGeodeticObject(final Class<T> classe, final short type, final String code) throws FactoryException {
            Objects.requireNonNull(code);
            final T result;
            try (Context c = Context.acquire()) {
                result = classe.cast(c.factory(authority).createGeodeticObject(type, code));
            } catch (ClassCastException e) {
                throw (NoSuchAuthorityCodeException) new NoSuchAuthorityCodeException(
                        authority + ':' + code + " identifies an object of a different kind.",
                        authority, code).initCause(e);
            }
            if (result != null) {
                return result;
            }
            /*
             * Following exception should happen only in case of out of memory.
             * If the operation failed for another reason, a more descriptive
             * exception should have been thrown from the native code.
             */
            throw new FactoryException("Can not get PROJ object.");
        }

        /**
         * Returns the project responsible for creating this factory implementation, which is "PROJ".
         * {@link Citation#getEdition()} contains the PROJ version string.
         *
         * @return a citation for "PROJ".
         */
        @Override
        public Citation getVendor() {
            return SimpleCitation.PROJ();
        }

        /**
         * Returns the organization or party responsible for definition and maintenance of the database.
         *
         * @return the organization responsible for definition of the database.
         */
        @Override
        public Citation getAuthority() {
            return new SimpleCitation(authority) {
                @Override public Collection<PresentationForm> getPresentationForms() {
                    return Collections.singleton(PresentationForm.TABLE_DIGITAL);
                }
            };
        }

        @Override
        public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
            throw new FactoryException("Not supported yet.");       // TODO
        }

        /**
         * Gets a description of the object corresponding to a code.
         *
         * @param  code  value allocated by authority.
         * @return a description of the object, or {@code null} if none.
         * @throws FactoryException if the description can not be obtained.
         */
        @Override
        public InternationalString getDescriptionText(final String code) throws FactoryException {
            Objects.requireNonNull(code);
            final String text;
            try (Context c = Context.acquire()) {
                text = c.factory(authority).getDescriptionText(code);
            }
            return (text != null) ? new SimpleCitation(text) : null;
        }

        /**
         * Returns an arbitrary object from a code.
         *
         * @param  code  value allocated by authority.
         * @return the object for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public IdentifiedObject createObject(final String code) throws FactoryException {
            return createGeodeticObject(IdentifiedObject.class, Type.ANY, code);
        }

        /**
         * Returns an unit of measurement from a code.
         *
         * @param  code  value allocated by authority.
         * @return the unit of measurement for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public Unit<?> createUnit(final String code) throws FactoryException {
            return createGeodeticObject(Unit.class, Type.UNIT_OF_MEASURE, code);
        }

        /**
         * Returns a coordinate system axis from a code.
         * As of PROJ 6.2.0, there is no method for creating an individual axis.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system axis for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public CoordinateSystemAxis createCoordinateSystemAxis(final String code) throws FactoryException {
            throw new FactoryException(UNSUPPORTED);
        }

        /**
         * Returns an arbitrary coordinate system from a code.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
            return createGeodeticObject(CS.class, Type.COORDINATE_SYSTEM, code);
        }

        /**
         * Returns a coordinate system which is expected to be Cartesian.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public CartesianCS createCartesianCS(final String code) throws FactoryException {
            return createGeodeticObject(CS.Cartesian.class, Type.CARTESIAN_CS, code);
        }

        /**
         * Returns a coordinate system which is expected to be spherical.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public SphericalCS createSphericalCS(final String code) throws FactoryException {
            return createGeodeticObject(CS.Spherical.class, Type.SPHERICAL_CS, code);
        }

        /**
         * Returns a coordinate system which is expected to be ellipsoidal.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public EllipsoidalCS createEllipsoidalCS(final String code) throws FactoryException {
            return createGeodeticObject(CS.Ellipsoidal.class, Type.ELLIPSOIDAL_CS, code);
        }

        /**
         * Returns a coordinate system which is expected to be vertical.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public VerticalCS createVerticalCS(final String code) throws FactoryException {
            return createGeodeticObject(CS.Vertical.class, Type.VERTICAL_CS, code);
        }

        /**
         * Returns a coordinate system which is expected to be temporal.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public TimeCS createTimeCS(final String code) throws FactoryException {
            return createGeodeticObject(CS.Time.class, Type.TEMPORAL_CS, code);
        }

        /**
         * Returns a coordinate system which is expected to be polar.
         * PROJ does not yet support this type of coordinate system.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public PolarCS createPolarCS(final String code) throws FactoryException {
            throw new FactoryException(UNSUPPORTED);
        }

        /**
         * Returns a coordinate system which is expected to be cylindrical.
         * PROJ does not yet support this type of coordinate system.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate system for the given code.
         * @throws FactoryException if the object creation failed or the CS is another type.
         */
        @Override
        public CylindricalCS createCylindricalCS(final String code) throws FactoryException {
            throw new FactoryException(UNSUPPORTED);
        }

        /**
         * Returns a prime meridian from a code.
         *
         * @param  code  value allocated by authority.
         * @return the prime meridian for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public PrimeMeridian createPrimeMeridian(final String code) throws FactoryException {
            return createGeodeticObject(Datum.PrimeMeridian.class, Type.PRIME_MERIDIAN, code);
        }

        /**
         * Returns an ellipsoid from a code.
         *
         * @param  code  value allocated by authority.
         * @return the ellipsoid for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public Ellipsoid createEllipsoid(final String code) throws FactoryException {
            return createGeodeticObject(Datum.Ellipsoid.class, Type.ELLIPSOID, code);
        }

        /**
         * Returns a datum from a code.
         *
         * @param  code  value allocated by authority.
         * @return the datum for the given code.
         * @throws FactoryException if the object creation failed or the datum is another type.
         */
        @Override
        public org.opengis.referencing.datum.Datum createDatum(final String code) throws FactoryException {
            return createGeodeticObject(Datum.class, Type.DATUM, code);
        }

        /**
         * Returns a datum for geodetic or geographic CRS.
         *
         * @param  code  value allocated by authority.
         * @return the datum for the given code.
         * @throws FactoryException if the object creation failed or the datum is another type.
         */
        @Override
        public GeodeticDatum createGeodeticDatum(final String code) throws FactoryException {
            return createGeodeticObject(Datum.Geodetic.class, Type.GEODETIC_REFERENCE_FRAME, code);
        }

        /**
         * Returns a datum for vertical CRS.
         *
         * @param  code  value allocated by authority.
         * @return the datum for the given code.
         * @throws FactoryException if the object creation failed or the datum is another type.
         */
        @Override
        public VerticalDatum createVerticalDatum(final String code) throws FactoryException {
            return createGeodeticObject(Datum.Vertical.class, Type.VERTICAL_REFERENCE_FRAME, code);
        }

        /**
         * Returns a datum for temporal CRS.
         *
         * @param  code  value allocated by authority.
         * @return the datum for the given code.
         * @throws FactoryException if the object creation failed or the datum is another type.
         */
        @Override
        public TemporalDatum createTemporalDatum(final String code) throws FactoryException {
            return createGeodeticObject(Datum.Temporal.class, Type.TEMPORAL_DATUM, code);
        }

        /**
         * Returns a datum for engineering CRS.
         *
         * @param  code  value allocated by authority.
         * @return the datum for the given code.
         * @throws FactoryException if the object creation failed or the datum is another type.
         */
        @Override
        public EngineeringDatum createEngineeringDatum(final String code) throws FactoryException {
            return createGeodeticObject(Datum.Engineering.class, Type.ENGINEERING_DATUM, code);
        }

        /**
         * Deprecated type removed from latest ISO standard.
         *
         * @param  code  value allocated by authority.
         * @return the datum for the given code.
         * @throws FactoryException if the object creation failed or the datum is another type.
         */
        @Override
        public ImageDatum createImageDatum(final String code) throws FactoryException {
            throw new FactoryException(UNSUPPORTED);
        }

        /**
         * Returns an arbitrary coordinate reference system from a code.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
            return createGeodeticObject(CRS.class, Type.COORDINATE_REFERENCE_SYSTEM, code);
        }

        /**
         * Returns a coordinate reference system which is expected to be geographic.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public GeographicCRS createGeographicCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Geographic.class, Type.GEOGRAPHIC_CRS, code);
        }

        /**
         * Returns a coordinate reference system which is expected to be geocentric.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Geocentric.class, Type.GEOCENTRIC_CRS, code);
        }

        /**
         * Returns a coordinate reference system which is expected to be projected.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Projected.class, Type.PROJECTED_CRS, code);
        }

        /**
         * Returns a coordinate reference system which is expected to be vertical.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public VerticalCRS createVerticalCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Vertical.class, Type.VERTICAL_CRS, code);
        }

        /**
         * Returns a coordinate reference system which is expected to be temporal.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed or the CRS is another type.
         */
        @Override
        public TemporalCRS createTemporalCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Temporal.class, Type.TEMPORAL_CRS, code);
        }

        /**
         * Returns a coordinate reference system which is expected to be engineering.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed or the CRS is another type.
         */
        @Override
        public EngineeringCRS createEngineeringCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Engineering.class, Type.ENGINEERING_CRS, code);
        }

        /**
         * Deprecated type removed from latest ISO standard.
         *
         * @param  code  value allocated by authority.
         * @return the CRS for the given code.
         * @throws FactoryException if the object creation failed or the CRS is another type.
         */
        @Override
        public ImageCRS createImageCRS(final String code) throws FactoryException {
            throw new FactoryException(UNSUPPORTED);
        }

        @Override
        public DerivedCRS createDerivedCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");       // TODO
        }

        /**
         * Returns a coordinate reference system which is expected to be compound.
         *
         * @param  code  value allocated by authority.
         * @return the coordinate reference system for the given code.
         * @throws FactoryException if the object creation failed or the CRS is another type.
         */
        @Override
        public CompoundCRS createCompoundCRS(final String code) throws FactoryException {
            return createGeodeticObject(CRS.Compound.class, Type.COMPOUND_CRS, code);
        }

        /**
         * Returns an operation method for the given authority code.
         * PROJ does not yet support this factory method.
         *
         * @param  code  value allocated by authority.
         * @return the operation method for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public OperationMethod createOperationMethod(final String code) throws FactoryException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        /**
         * Creates an operation from a single operation code.
         *
         * @param  code  coded value for coordinate operation.
         * @return the operation for the given code.
         * @throws FactoryException if the object creation failed.
         */
        @Override
        public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
            return createGeodeticObject(Operation.class, Type.COORDINATE_OPERATION, code);
        }

        @Override
        public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(final String source, final String target) throws FactoryException {
            throw new UnsupportedOperationException("Not supported yet.");      // TODO
        }
    }
}
