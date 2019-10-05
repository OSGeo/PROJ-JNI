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
    private static final int
            ANY                         =  0,
            PRIME_MERIDIAN              =  1,
            ELLIPSOID                   =  2,
            DATUM                       =  3,
            GEODETIC_REFERENCE_FRAME    =  4,
            VERTICAL_REFERENCE_FRAME    =  5,
            UNIT_OF_MEASURE             =  6,
            COORDINATE_SYSTEM           =  7,
            COORDINATE_REFERENCE_SYSTEM =  8,
            GEODETIC_CRS                =  9,
            GEOGRAPHIC_CRS              = 10,
            VERTICAL_CRS                = 11,
            PROJECTED_CRS               = 12,
            COMPOUND_CRS                = 13,
            CONVERSION                  = 14,
            COORDINATE_OPERATION        = 15;

    /**
     * Creates a new factory for the given authority.
     * This constructor should not be invoked directly; use {@link Context#factory(String)} instead.
     *
     * @param  context    pointer to the PROJ thread context.
     * @param  authority  the authority name, for example {@code "EPSG"}.
     * @param  sibling    if another factory has been created for the same context, that factory.
     *                    Otherwise {@code null}. This is used for sharing the same database context.
     * @throws FactoryException if the factory can not be created.
     *
     * @see Context#factory(String)
     */
    AuthorityFactory(final long context, final String authority, final AuthorityFactory sibling) throws FactoryException {
        super(newInstance(context, Objects.requireNonNull(authority), sibling));
    }

    /**
     * Instantiates an {@code osgeo::proj::io::AuthorityFactory}.
     * Also creates the {@code osgeo::proj::io::DatabaseContext}.
     * Each database context can be used by only one thread.
     *
     * @param  context    pointer to the PROJ thread context.
     * @param  authority  name of the authority. Shall not be null.
     * @param  sibling    if another factory has been created for the same context, that factory.
     *                    Otherwise {@code null}. This is used for sharing the same database context.
     * @return shared pointer to the factory, or 0 if out of memory.
     * @throws FactoryException if the factory can not be created.
     */
    private static native long newInstance(long context, String authority, AuthorityFactory sibling) throws FactoryException;

    /**
     * Gets a description of the object corresponding to a code.
     * In case of several objects of different types with the same code,
     * one of them will be arbitrarily selected.
     *
     * @param  code  object code allocated by authority. (e.g. "4326").
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
    private native IdentifiableObject createGeodeticObject(int type, String code) throws FactoryException;

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
         * @param  type  one of {@link #ELLIPSOID}, {@link #PRIME_MERIDIAN}, <i>etc.</i> constants.
         * @param  code  object code allocated by authority.
         * @return wrapper for the PROJ object.
         * @throws FactoryException if no object can be created for the given code.
         */
        private IdentifiableObject createGeodeticObject(final int type, final String code) throws FactoryException {
            Objects.requireNonNull(code);
            final IdentifiableObject result;
            try (Context c = Context.acquire()) {
                result = c.factory(authority).createGeodeticObject(type, code);
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
            return new SimpleCitation("PROJ") {
                @Override public InternationalString getEdition() {
                    try {
                        final String version = version();
                        if (version != null) {
                            return new SimpleCitation(version);
                        }
                    } catch (UnsatisfiedLinkError e) {
                        // Ignore.
                    }
                    return null;
                }
            };
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

        @Override
        public IdentifiedObject createObject(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
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
            return (CS) createGeodeticObject(COORDINATE_SYSTEM, code);
        }

        @Override
        public CartesianCS createCartesianCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public EllipsoidalCS createEllipsoidalCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public SphericalCS createSphericalCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public CylindricalCS createCylindricalCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public PolarCS createPolarCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public VerticalCS createVerticalCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public TimeCS createTimeCS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
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
        @SuppressWarnings("OverlyStrongTypeCast")
        public CoordinateReferenceSystem createCoordinateReferenceSystem(final String code) throws FactoryException {
            return (CRS) createGeodeticObject(COORDINATE_REFERENCE_SYSTEM, code);
        }

        @Override
        public GeographicCRS createGeographicCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public GeocentricCRS createGeocentricCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public ProjectedCRS createProjectedCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public VerticalCRS createVerticalCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public TemporalCRS createTemporalCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
        }

        @Override
        public EngineeringCRS createEngineeringCRS(final String code) throws FactoryException {
            throw new FactoryException("Not supported yet.");
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
        @SuppressWarnings("OverlyStrongTypeCast")
        public CoordinateOperation createCoordinateOperation(final String code) throws FactoryException {
            return (Operation) createGeodeticObject(COORDINATE_OPERATION, code);
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
    private static IdentifiableObject wrapGeodeticObject(final int type, final long ptr) throws FactoryException {
        final org.kortforsyningen.proj.IdentifiableObject obj;
        switch (type) {
            case GEODETIC_CRS:
            case GEOGRAPHIC_CRS:
            case VERTICAL_CRS:
            case PROJECTED_CRS:
            case COMPOUND_CRS:
            case COORDINATE_REFERENCE_SYSTEM: obj = new CRS      (ptr); break;
            case COORDINATE_SYSTEM:           obj = new CS       (ptr); break;
            case COORDINATE_OPERATION:        obj = new Operation(ptr); break;
            default: throw new FactoryException("Unknown object type.");
        }
        obj.cleanWhenUnreachable();
        return obj;
    }
}
