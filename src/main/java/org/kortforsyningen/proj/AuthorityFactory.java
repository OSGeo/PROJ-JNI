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

import java.util.Set;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.lang.annotation.Native;
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
 * @version 1.0
 * @since   1.0
 */
final class AuthorityFactory extends NativeResource {
    /**
     * Kind of geodetic objects created by native functions invoked from this class.
     */
    @Native
    private static final short
            ANY                         =  0,
            PRIME_MERIDIAN              =  1,
            ELLIPSOID                   =  2,
            DATUM                       =  3,
            GEODETIC_REFERENCE_FRAME    =  4,
            VERTICAL_REFERENCE_FRAME    =  5,
            UNIT_OF_MEASURE             =  6,
            COORDINATE_SYSTEM           =  7,
            CARTESIAN_CS                =  8,
            SPHERICAL_CS                =  9,
            ELLIPSOIDAL_CS              = 10,
            VERTICAL_CS                 = 11,
            TEMPORAL_CS                 = 12,
            COORDINATE_REFERENCE_SYSTEM = 13,
            GEODETIC_CRS                = 14,
            GEOGRAPHIC_CRS              = 15,
            VERTICAL_CRS                = 16,
            TEMPORAL_CRS                = 17,
            ENGINEERING_CRS             = 18,
            PROJECTED_CRS               = 19,
            COMPOUND_CRS                = 20,
            CONVERSION                  = 21,
            COORDINATE_OPERATION        = 22;

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
     * @param  type  one of {@link #ELLIPSOID}, {@link #PRIME_MERIDIAN}, <i>etc.</i> constants.
     * @param  code  object code allocated by authority.
     * @return wrapper for the PROJ shared object, or {@code null} if out of memory.
     * @throws FactoryException if no object can be created for the given code.
     */
    private native IdentifiableObject createGeodeticObject(short type, String code) throws FactoryException;

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
            double desiredAccuracy, int sourceAndTargetCRSExtentUse, int spatialCriterion,
            int gridAvailabilityUse, int allowUseIntermediateCRS, boolean discardSuperseded)
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
         * @param  type    one of {@link #ELLIPSOID}, {@link #PRIME_MERIDIAN}, <i>etc.</i> constants.
         * @param  code    object code allocated by authority.
         * @return wrapper for the PROJ object.
         * @throws FactoryException if no object can be created for the given code.
         */
        private <T extends IdentifiableObject> T createGeodeticObject(
                final Class<T> classe, final short type, final String code) throws FactoryException
        {
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
            throw new FactoryException("Not supported yet.");
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
            return (IdentifiedObject) createGeodeticObject(IdentifiableObject.class, ANY, code);
        }

        @Override
        public Unit<?> createUnit(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
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
        @SuppressWarnings("OverlyStrongTypeCast")
        public CoordinateSystem createCoordinateSystem(final String code) throws FactoryException {
            return createGeodeticObject(CS.class, COORDINATE_SYSTEM, code);
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
            return createGeodeticObject(CS.Cartesian.class, CARTESIAN_CS, code);
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
            return createGeodeticObject(CS.Spherical.class, SPHERICAL_CS, code);
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
            return createGeodeticObject(CS.Ellipsoidal.class, ELLIPSOIDAL_CS, code);
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
            return createGeodeticObject(CS.Vertical.class, VERTICAL_CS, code);
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
            return createGeodeticObject(CS.Time.class, TEMPORAL_CS, code);
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

        @Override
        public PrimeMeridian createPrimeMeridian(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public Ellipsoid createEllipsoid(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public Datum createDatum(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public GeodeticDatum createGeodeticDatum(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public VerticalDatum createVerticalDatum(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public TemporalDatum createTemporalDatum(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public EngineeringDatum createEngineeringDatum(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public ImageDatum createImageDatum(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
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
            return createGeodeticObject(CRS.class, COORDINATE_REFERENCE_SYSTEM, code);
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
            return createGeodeticObject(CRS.Geographic.class, GEOGRAPHIC_CRS, code);
        }

        @Override
        public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
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
            return createGeodeticObject(CRS.Vertical.class, VERTICAL_CRS, code);
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
            return createGeodeticObject(CRS.Temporal.class, TEMPORAL_CRS, code);
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
            return createGeodeticObject(CRS.Engineering.class, ENGINEERING_CRS, code);
        }

        @Override
        public ImageCRS createImageCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public DerivedCRS createDerivedCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public CompoundCRS createCompoundCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public OperationMethod createOperationMethod(final String code) throws FactoryException {
            throw new UnsupportedOperationException("Not supported yet.");
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
            return createGeodeticObject(Operation.class, COORDINATE_OPERATION, code);
        }

        @Override
        public Set<CoordinateOperation> createFromCoordinateReferenceSystemCodes(final String source, final String target) throws FactoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Creates an object of the given type. This method is invoked by native code; it shall not be moved,
     * renamed or have method signature modified unless the C++ bindings are updated accordingly.
     * If an exception is thrown in this Java method, the native method will release the memory allocated
     * for {@code ptr}.
     *
     * @param  type  one of the {@link #COORDINATE_REFERENCE_SYSTEM}, {@link #DATUM}, <i>etc.</i> constants.
     * @param  ptr   pointer to the object allocated by PROJ.
     * @return the Java object wrapping the PROJ object.
     * @throws FactoryException if the given type is not recognized.
     */
    private static IdentifiableObject wrapGeodeticObject(final short type, final long ptr) throws FactoryException {
        final org.kortforsyningen.proj.IdentifiableObject obj;
        switch (type) {
            case PROJECTED_CRS:
            case COMPOUND_CRS:
            case COORDINATE_REFERENCE_SYSTEM: obj = new CRS                 (ptr); break;
            case GEODETIC_CRS:                obj = new CRS.Geodetic        (ptr); break;
            case GEOGRAPHIC_CRS:              obj = new CRS.Geographic      (ptr); break;
            case VERTICAL_CRS:                obj = new CRS.Vertical        (ptr); break;
            case TEMPORAL_CRS:                obj = new CRS.Temporal        (ptr); break;
            case ENGINEERING_CRS:             obj = new CRS.Engineering     (ptr); break;
            case COORDINATE_SYSTEM:           obj = new CS                  (ptr); break;
            case CARTESIAN_CS:                obj = new CS.Cartesian        (ptr); break;
            case SPHERICAL_CS:                obj = new CS.Spherical        (ptr); break;
            case ELLIPSOIDAL_CS:              obj = new CS.Ellipsoidal      (ptr); break;
            case VERTICAL_CS:                 obj = new CS.Vertical         (ptr); break;
            case TEMPORAL_CS:                 obj = new CS.Time             (ptr); break;
            case CONVERSION:                  obj = new Operation.Conversion(ptr); break;
            case COORDINATE_OPERATION:        obj = new Operation           (ptr); break;
            default: throw new FactoryException("Unknown object type.");
        }
        obj.cleanWhenUnreachable();
        return obj;
    }
}
