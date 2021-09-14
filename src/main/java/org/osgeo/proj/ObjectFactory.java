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

import java.util.Map;
import java.util.Date;
import java.util.Objects;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import java.lang.annotation.Native;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.cs.*;


/**
 * Creates geodetic objects from their components.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class ObjectFactory extends NativeResource implements DatumFactory, CSFactory, CRSFactory {
    /**
     * The singleton instance.
     */
    static final ObjectFactory INSTANCE = new ObjectFactory();

    /**
     * Creates the singleton instance.
     */
    private ObjectFactory() {
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
     * Return the UML identifier of the given code list, or its name if no UML identifier is found.
     * This is used for giving a code list value to PROJ as a {@link String} instance.
     * PROJ expects UML identifiers for code list values.
     *
     * @param  code  the code list, or null.
     * @return UML identifier or name of the given code list.
     */
    private static String identifier(final CodeList<?> code) {
        if (code != null) {
            String id = code.identifier();
            if (id == null) id = code.name();
            return id;
        }
        return null;
    }

    /**
     * Wraps the given components in an array suitable for native method call.
     *
     * @param  components  the components.
     * @return PROJ implementations of components.
     * @throws UnsupportedImplementationException is a component is not a PROJ implementation.
     */
    private static SharedPointer[] components(final IdentifiedObject... components) {
        final SharedPointer[] ptr = new SharedPointer[components.length];
        try {
            for (int i=0; i<components.length; i++) {
                ptr[i] = ((IdentifiableObject) components[i]).impl;
            }
        } catch (ClassCastException e) {
            throw new UnsupportedImplementationException(
                    "This factory accepts only components that are PROJ implementations.");
        }
        return ptr;
    }

    /**
     * Index in properties array where to store property value. This is used for
     * converting {@code Map<String,?>} into something easier to use from C++ code.
     * Properties most likely to be used should be first.
     */
    @Native
    static final int NAME               = 0,    // InternationalString
                     IDENTIFIER         = 1,    // Identifier
                     CODESPACE          = 2,    // String (to be injected in Identifier)
                     ALIAS              = 3,    // GenericName
                     REMARKS            = 4,    // InternationalString
                     DEPRECATED         = 5,    // Boolean
                     ANCHOR_POINT       = 6,    // InternationalString
                  // REALIZATION_EPOCH  = 7,    // Date (skipped, pending dynamic CRS).
                  // DOMAIN_OF_VALIDITY = 7,    // Extent (skipped because uneasy to format)
                     SCOPE              = 7;    // InternationalString

    /**
     * Property keys in the order of {@link #NAME}, {@link #IDENTIFIER}, <i>etc.</i> constants.
     * For example {@code PROPERTY_KEYS[REMARKS]} must be {@value IdentifiedObject#REMARKS_KEY}.
     */
    private static final String[] PROPERTY_KEYS = {
            IdentifiedObject.NAME_KEY,
            IdentifiedObject.IDENTIFIERS_KEY,
                            "codespace",
            IdentifiedObject.ALIAS_KEY,
            IdentifiedObject.REMARKS_KEY,
                            "deprecated",
                       Datum.ANCHOR_POINT_KEY,
                       Datum.SCOPE_KEY};

    /**
     * For JUnit test purpose only.
     *
     * @param  index  one of {@link #NAME}, {@link #IDENTIFIER}, <i>etc.</i> indices.
     * @return property key for the given indices.
     */
    static final String propertyKey(final int index) {
        return PROPERTY_KEYS[index];
    }

    /**
     * Returns properties as a flat array with determinist order.
     *
     * @param  properties  the properties to extract as a flat array.
     * @return the properties as a flat array, or {@code null} if empty.
     */
    @SuppressWarnings("fallthrough")
    private static String[] flat(final Map<String,?> properties) {
        String[] array = null;
        for (int i=PROPERTY_KEYS.length; --i >= 0;) {
            Object value = properties.get(PROPERTY_KEYS[i]);
            if (value != null) {
                if (value instanceof Identifier) {
                    final Identifier id = (Identifier) value;
                    value = id.getCode();
                    if (value == null) {
                        continue;
                    }
                    if (i == IDENTIFIER && id instanceof ReferenceIdentifier) {
                        if (array == null) array = new String[CODESPACE + 1];
                        array[CODESPACE] = ((ReferenceIdentifier) id).getCodeSpace();
                    }
                } else if (value instanceof Date) {
                    value = ((Date) value).toInstant();         // ISO 8601 representation.
                }
                if (array == null) {
                    array = new String[i + 1];
                }
                array[i] = value.toString();
            }
        }
        return array;
    }

    /**
     * Creates a geodetic object of the given type. If a {@code components}, {@code stringValues},
     * {@code doubleValues} or {@code unit} argument does not apply, it will be ignored and can be
     * null or zero.
     *
     * @param  properties    the result of {@code properties(Map, int)} call. Shall never be null.
     * @param  components    the components of the geodetic object to create, or null.
     * @param  stringValues  any arguments that need to be passed as character string, or null.
     * @param  doubleValues  any arguments that need to be passed as floating point value, or null.
     * @param  unit          unit of measurement as given by {@link Units#getUnitIdentifier(Unit)}.
     * @param  type          one of the {@link Type} constants.
     * @return the geodetic object.
     * @throws FactoryException if object creation failed.
     */
    private native Object create(String[] properties, SharedPointer[] components,
            String[] stringValues, double[] doubleValues, int unit, short type)
            throws FactoryException;

    /**
     * Creates a prime meridian, relative to Greenwich.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  longitude   longitude of prime meridian in supplied angular units East of Greenwich.
     * @param  unit        angular units of longitude.
     * @return the prime meridian for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public PrimeMeridian createPrimeMeridian(
            final Map<String,?> properties,
            final double        longitude,
            final Unit<Angle>   unit) throws FactoryException
    {
        return (PrimeMeridian) create(flat(properties),
                null, null, new double[] {longitude},
                Units.getUnitIdentifier(unit), Type.PRIME_MERIDIAN);
    }

    /**
     * Creates an ellipsoid from radius values.
     *
     * @param  properties     name and other properties to give to the new object.
     * @param  semiMajorAxis  equatorial radius in supplied linear units.
     * @param  semiMinorAxis  polar radius in supplied linear units.
     * @param  unit           linear units of ellipsoid axes.
     * @return the ellipsoid for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Ellipsoid createEllipsoid(
            final Map<String,?> properties,
            final double        semiMajorAxis,
            final double        semiMinorAxis,
            final Unit<Length>  unit) throws FactoryException
    {
        return (Ellipsoid) create(flat(properties),
                null, null, new double[] {semiMajorAxis, semiMinorAxis},
                Units.getUnitIdentifier(unit), Type.ELLIPSOID);
    }

    /**
     * Creates an ellipsoid from an major radius, and inverse flattening.
     *
     * @param  properties         name and other properties to give to the new object.
     * @param  semiMajorAxis      equatorial radius in supplied linear units.
     * @param  inverseFlattening  eccentricity of ellipsoid.
     * @param  unit               linear units of major axis.
     * @return the ellipsoid for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Ellipsoid createFlattenedSphere(
            final Map<String, ?> properties,
            final double         semiMajorAxis,
            final double         inverseFlattening,
            final Unit<Length>   unit) throws FactoryException
    {
        return (Ellipsoid) create(flat(properties),
                null, null, new double[] {semiMajorAxis, 0, inverseFlattening},
                Units.getUnitIdentifier(unit), Type.ELLIPSOID);

    }

    /**
     * Creates a coordinate system axis from an abbreviation and a unit.
     *
     * @param  properties    name and other properties to give to the new object.
     * @param  abbreviation  the coordinate axis abbreviation.
     * @param  direction     the axis direction.
     * @param  unit          the coordinate axis unit.
     * @return the axis for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateSystemAxis createCoordinateSystemAxis(
            final Map<String,?> properties,
            final String        abbreviation,
            final AxisDirection direction,
            final Unit<?>       unit) throws FactoryException
    {
        return (CoordinateSystemAxis) create(flat(properties),
                null, new String[] {abbreviation, identifier(direction)},
                null, Units.getUnitIdentifier(unit), Type.AXIS);
    }

    /**
     * Creates a two dimensional Cartesian coordinate system from the given pair of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CartesianCS createCartesianCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        return (CartesianCS) create(flat(properties),
                components(axis0, axis1), null, null, 0, Type.CARTESIAN_CS);
    }

    /**
     * Creates a three dimensional Cartesian coordinate system from the given set of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CartesianCS createCartesianCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        return (CartesianCS) create(flat(properties),
                components(axis0, axis1, axis2), null, null, 0, Type.CARTESIAN_CS);
    }

    /**
     * Creates a two dimensional coordinate system from the given pair of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public AffineCS createAffineCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a three dimensional coordinate system from the given set of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public AffineCS createAffineCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a polar coordinate system from the given pair of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public PolarCS createPolarCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a cylindrical coordinate system from the given set of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CylindricalCS createCylindricalCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a spherical coordinate system from the given set of axis.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public SphericalCS createSphericalCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        return (SphericalCS) create(flat(properties),
                components(axis0, axis1, axis2), null, null, 0, Type.SPHERICAL_CS);
    }

    /**
     * Creates an ellipsoidal coordinate system without ellipsoidal height.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        return (EllipsoidalCS) create(flat(properties),
                components(axis0, axis1), null, null, 0, Type.ELLIPSOIDAL_CS);
    }

    /**
     * Creates an ellipsoidal coordinate system with ellipsoidal height.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EllipsoidalCS createEllipsoidalCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        return (EllipsoidalCS) create(flat(properties),
                components(axis0, axis1, axis2), null, null, 0, Type.ELLIPSOIDAL_CS);
    }

    /**
     * Creates a vertical coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCS createVerticalCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        return (VerticalCS) create(flat(properties),
                components(axis), null, null, 0, Type.VERTICAL_CS);
    }

    /**
     * Creates a time coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TimeCS createTimeCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        return (TimeCS) create(flat(properties),
                components(axis), null, null, 0, Type.TEMPORAL_CS);
    }

    /**
     * Creates a linear coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis        the axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public LinearCS createLinearCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a two-dimensional user defined coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public UserDefinedCS createUserDefinedCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a three-dimensional user defined coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  axis0       the first  axis.
     * @param  axis1       the second axis.
     * @param  axis2       the third  axis.
     * @return the coordinate system for the given properties and axes.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public UserDefinedCS createUserDefinedCS(
            final Map<String,?> properties,
            final CoordinateSystemAxis axis0,
            final CoordinateSystemAxis axis1,
            final CoordinateSystemAxis axis2) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates geodetic datum from ellipsoid and prime meridian.
     *
     * @param  properties     name and other properties to give to the new object.
     * @param  ellipsoid      ellipsoid to use in new geodetic datum.
     * @param  primeMeridian  prime meridian to use in new geodetic datum.
     * @return the datum for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeodeticDatum createGeodeticDatum(
            final Map<String,?> properties,
            final Ellipsoid     ellipsoid,
            final PrimeMeridian primeMeridian) throws FactoryException
    {
        return (GeodeticDatum) create(flat(properties),
                components(ellipsoid, primeMeridian), null, null, 0, Type.GEODETIC_REFERENCE_FRAME);
    }

    /**
     * Creates a vertical datum from an enumerated type value.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  type        the type of this vertical datum (often "geoidal").
     * @return the datum for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalDatum createVerticalDatum(
            final Map<String,?> properties,
            final VerticalDatumType type) throws FactoryException
    {
        return (VerticalDatum) create(flat(properties),
                null, null, null, 0, Type.VERTICAL_REFERENCE_FRAME);
    }

    /**
     * Creates a temporal datum from an enumerated type value.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  origin      the date and time origin of this temporal datum.
     * @return the datum for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalDatum createTemporalDatum(
            final Map<String,?> properties,
            final Date          origin) throws FactoryException
    {
        return (TemporalDatum) create(flat(properties),
                null, new String[] {origin.toInstant().toString()}, null, 0, Type.TEMPORAL_DATUM);
    }

    /**
     * Creates an engineering datum.
     *
     * @param  properties  name and other properties to give to the new object.
     * @return the datum for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringDatum createEngineeringDatum(final Map<String,?> properties) throws FactoryException {
        return (EngineeringDatum) create(flat(properties),
                null, null, null, 0, Type.ENGINEERING_DATUM);
    }

    /**
     * Creates an image datum.
     *
     * @param  properties   name and other properties to give to the new object.
     * @param  pixelInCell  specification of the way the image grid is associated with the image data attributes.
     * @return the datum for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ImageDatum createImageDatum(
            final Map<String,?> properties,
            final PixelInCell pixelInCell) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a geographic coordinate reference system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       geodetic datum to use in created CRS.
     * @param  cs          the ellipsoidal coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeographicCRS createGeographicCRS(
            final Map<String,?> properties,
            final GeodeticDatum datum,
            final EllipsoidalCS cs) throws FactoryException
    {
        return (GeographicCRS) create(flat(properties),
                components(datum, cs), null, null, 0, Type.GEOGRAPHIC_CRS);
    }

    /**
     * Creates a geocentric coordinate reference system from a Cartesian coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       geodetic datum to use in created CRS.
     * @param  cs          the Cartesian coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeocentricCRS createGeocentricCRS(
            final Map<String,?> properties,
            final GeodeticDatum datum,
            final CartesianCS   cs) throws FactoryException
    {
        return (GeocentricCRS) create(flat(properties),
                components(datum, cs), null, null, 0, Type.GEOCENTRIC_CRS);
    }

    /**
     * Creates a geocentric coordinate reference system from a spherical coordinate system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       geodetic datum to use in created CRS.
     * @param  cs          the spherical coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public GeocentricCRS createGeocentricCRS(
            final Map<String,?> properties,
            final GeodeticDatum datum,
            final SphericalCS   cs) throws FactoryException
    {
        return (GeocentricCRS) create(flat(properties),
                components(datum, cs), null, null, 0, Type.GEOCENTRIC_CRS);
    }

    /**
     * Creates a vertical coordinate reference system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       vertical datum to use in created CRS.
     * @param  cs          the vertical coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public VerticalCRS createVerticalCRS(
            final Map<String,?> properties,
            final VerticalDatum datum,
            final VerticalCS    cs) throws FactoryException
    {
        return (VerticalCRS) create(flat(properties),
                components(datum, cs), null, null, 0, Type.VERTICAL_CRS);
    }

    /**
     * Creates a temporal coordinate reference system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       temporal datum to use in created CRS.
     * @param  cs          the temporal coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public TemporalCRS createTemporalCRS(
            final Map<String,?> properties,
            final TemporalDatum datum,
            final TimeCS        cs) throws FactoryException
    {
        return (TemporalCRS) create(flat(properties),
                components(datum, cs), null, null, 0, Type.TEMPORAL_CRS);
    }

    /**
     * Creates a engineering coordinate reference system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       engineering datum to use in created CRS.
     * @param  cs          the coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public EngineeringCRS createEngineeringCRS(
            final Map<String,?> properties,
            final EngineeringDatum datum,
            final CoordinateSystem cs) throws FactoryException
    {
        return (EngineeringCRS) create(flat(properties),
                components(datum, cs), null, null, 0, Type.ENGINEERING_CRS);
    }

    /**
     * Creates an image coordinate reference system.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  datum       image datum to use in created CRS.
     * @param  cs          the Cartesian or Oblique Cartesian coordinate system for the created CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ImageCRS createImageCRS(
            final Map<String,?> properties,
            final ImageDatum    datum,
            final AffineCS      cs) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a projected coordinate reference system from a defining conversion. The {@code conversionFromBase}
     * argument shall contain the {@linkplain Conversion#getParameterValues() parameter values} required for the
     * projection.
     *
     * @param  properties          name and other properties to give to the new object.
     * @param  baseCRS             geographic coordinate reference system to base the projection on.
     * @param  conversionFromBase  the {@linkplain CoordinateOperationFactory#createDefiningConversion defining conversion}.
     * @param  derivedCS           the coordinate system for the projected CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public ProjectedCRS createProjectedCRS(
            final Map<String,?> properties,
            final GeographicCRS baseCRS,
            final Conversion    conversionFromBase,
            final CartesianCS   derivedCS) throws FactoryException
    {
        return (ProjectedCRS) create(flat(properties),
                components(baseCRS, conversionFromBase, derivedCS), null, null, 0, Type.PROJECTED_CRS);
    }

    /**
     * Creates a derived coordinate reference system. The {@code conversionFromBase} argument shall contain
     * the {@linkplain Conversion#getParameterValues() parameter values} required for the conversion.
     *
     * @todo Same remark than in {@code createProjectedCRS}.
     *
     * @param  properties          name and other properties to give to the new object.
     * @param  baseCRS             coordinate reference system to base the projection on.
     * @param  conversionFromBase  the {@linkplain CoordinateOperationFactory#createDefiningConversion defining conversion}.
     * @param  derivedCS           the coordinate system for the derived CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public DerivedCRS createDerivedCRS(
            final Map<String,?> properties,
            final CoordinateReferenceSystem baseCRS,
            final Conversion conversionFromBase,
            final CoordinateSystem derivedCS) throws FactoryException
    {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a compound coordinate reference system from an ordered
     * list of {@code CoordinateReferenceSystem} instances.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  components  the sequence of coordinate reference systems making the compound CRS.
     * @return the coordinate reference system for the given properties.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CompoundCRS createCompoundCRS(
            final Map<String, ?> properties,
            final CoordinateReferenceSystem... components) throws FactoryException
    {
        return (CompoundCRS) create(flat(properties), components(components), null, null, 0, Type.COMPOUND_CRS);
    }

    /**
     * Creates a coordinate reference system object from a GML string.
     *
     * @param  xml  coordinate reference system encoded in GML format.
     * @return the coordinate reference system for the given GML.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createFromXML(final String xml) throws FactoryException {
        throw new FactoryException(UNSUPPORTED);
    }

    /**
     * Creates a coordinate reference system object from a <cite>Well-Known Text</cite>.
     *
     * @param  wkt  coordinate system encoded in Well-Known Text format.
     * @return the coordinate reference system for the given WKT.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateReferenceSystem createFromWKT(final String wkt) throws FactoryException {
        Objects.requireNonNull(wkt);
        final ReferencingFormat parser = new ReferencingFormat();
        parser.setConvention(ReferencingFormat.Convention.WKT);
        parser.setStrict(true);
        try {
            return (CoordinateReferenceSystem) parser.parse(wkt);
        } catch (UnparsableObjectException | ClassCastException e) {
            throw new FactoryException(e.getMessage(), e);
        }
    }
}
