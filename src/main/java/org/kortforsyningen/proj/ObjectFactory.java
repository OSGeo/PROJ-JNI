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

import java.util.Map;
import javax.measure.Unit;
import java.lang.annotation.Native;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.cs.*;


/**
 * Creates geodetic objects from their components.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class ObjectFactory extends NativeResource implements CSFactory {
    /**
     * The error message when a given component is not a PROJ implementation.
     */
    private static final String UNSUPPORTED_IMPLEMENTATION = "This factory accepts only components that are PROJ implementations.";

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
     * @param  c1  the single component.
     * @return the array of components.
     * @throws UnsupportedImplementationException is a component is not a PROJ implementation.
     */
    private static SharedPointer[] components(final IdentifiedObject c1) {
        try {
            return new SharedPointer[] {
                ((IdentifiableObject) c1).impl
            };
        } catch (ClassCastException e) {
            throw new UnsupportedImplementationException(UNSUPPORTED_IMPLEMENTATION);
        }
    }

    /**
     * Wraps the given components in an array suitable for native method call.
     *
     * @param  c1  the first  component.
     * @param  c2  the second component.
     * @return the array of components.
     * @throws UnsupportedImplementationException is a component is not a PROJ implementation.
     */
    private static SharedPointer[] components(final IdentifiedObject c1, final IdentifiedObject c2) {
        try {
            return new SharedPointer[] {
                ((IdentifiableObject) c1).impl,
                ((IdentifiableObject) c2).impl
            };
        } catch (ClassCastException e) {
            throw new UnsupportedImplementationException(UNSUPPORTED_IMPLEMENTATION);
        }
    }

    /**
     * Wraps the given components in an array suitable for native method call.
     *
     * @param  c1  the first  component.
     * @param  c2  the second component.
     * @param  c3  the third  component.
     * @return the array of components.
     * @throws UnsupportedImplementationException is a component is not a PROJ implementation.
     */
    private static SharedPointer[] components(final IdentifiedObject c1, final IdentifiedObject c2, final IdentifiedObject c3) {
        try {
            return new SharedPointer[] {
                ((IdentifiableObject) c1).impl,
                ((IdentifiableObject) c2).impl,
                ((IdentifiableObject) c3).impl
            };
        } catch (ClassCastException e) {
            throw new UnsupportedImplementationException(UNSUPPORTED_IMPLEMENTATION);
        }
    }

    /**
     * Index in properties array where to store property value. This is used for
     * converting {@code Map<String,?>} into something easier to use from C++ code.
     */
    @Native
    private static final int NAME       = 0,
                             ALIAS      = 1,
                             IDENTIFIER = 2,
                             CODESPACE  = 3,
                             REMARKS    = 4,
                             DEPRECATED = 5;

    /**
     * Argument for {@link #flat(Map, int)} when only the name, alias, identifier and remarks are desired.
     */
    private static final int BASIC_PROPERTIES = DEPRECATED + 1;

    /**
     * Returns properties as a flat array with determinist order.
     *
     * @param  properties  the properties to extract as a flat array.
     * @param  n           one of {@link #BASIC_PROPERTIES} values.
     * @return the properties as a flat array.
     */
    @SuppressWarnings("fallthrough")
    private static String[] flat(final Map<String,?> properties, final int n) {
        final String[] array = new String[n];
        switch (n) {
            default: // Fallthrough everywhere.
            case 1 + DEPRECATED: store(properties, array,                 "deprecated",     DEPRECATED);
            case 1 + REMARKS:    store(properties, array, IdentifiedObject.REMARKS_KEY,     REMARKS);
            case 1 + IDENTIFIER: store(properties, array, IdentifiedObject.IDENTIFIERS_KEY, IDENTIFIER);
            case 1 + ALIAS:      store(properties, array, IdentifiedObject.ALIAS_KEY,       ALIAS);
            case 1 + NAME:       store(properties, array, IdentifiedObject.NAME_KEY,        NAME);
            case 0: break;
        }
        return array;
    }

    /**
     * Stores a value from the property map to the given flat array.
     *
     * @param properties  the map to read.
     * @param array       the array to write.
     * @param key         key of the map entry to read.
     * @param index       index where to write in the array.
     */
    private static void store(final Map<String,?> properties, final String[] array, final String key, final int index) {
        Object value = properties.get(key);
        if (value != null) {
            if (value instanceof Identifier) {
                final Identifier id = (Identifier) value;
                if ((array[index] = id.getCode()) != null) {
                    if (id instanceof ReferenceIdentifier) {
                        array[CODESPACE] = ((ReferenceIdentifier) id).getCodeSpace();
                    }
                    return;
                }
            }
            array[index] = value.toString();
        }
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
     * @param  unit          unit of measurement as given by {@link Units#findUnitID(Unit)}.
     * @param  type          one of the {@link Type} constants.
     * @return the geodetic object.
     * @throws FactoryException if object creation failed.
     */
    private native Object create(String[] properties, SharedPointer[] components,
            String[] stringValues, double[] doubleValues, int unit, short type)
            throws FactoryException;

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
        return (CoordinateSystemAxis) create(flat(properties, BASIC_PROPERTIES),
                null, new String[] {abbreviation, identifier(direction)},
                null, Units.findUnitID(unit), Type.AXIS);
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
        return (CartesianCS) create(flat(properties, BASIC_PROPERTIES),
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
        return (CartesianCS) create(flat(properties, BASIC_PROPERTIES),
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
        return (SphericalCS) create(flat(properties, BASIC_PROPERTIES),
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
        return (EllipsoidalCS) create(flat(properties, BASIC_PROPERTIES),
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
        return (EllipsoidalCS) create(flat(properties, BASIC_PROPERTIES),
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
        return (VerticalCS) create(flat(properties, BASIC_PROPERTIES),
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
        return (TimeCS) create(flat(properties, BASIC_PROPERTIES),
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
}
