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
import java.util.List;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.cs.TimeCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.Identifier;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ObjectFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
public final strictfp class ObjectFactoryTest {
    /**
     * Tolerance threshold for floating point number comparisons in this class.
     */
    private static final double TOLERANCE = 1E-12;

    /**
     * The factory to use for testing.
     */
    private final ObjectFactory factory;

    /**
     * The origin used in creation of temporal datum.
     */
    private Date origin;

    /**
     * The Greenwich longitude used in test prime meridian.
     */
    private double greenwichLongitude;

    /**
     * The semi-major axis length used in test ellipsoid.
     */
    private double semiMajorAxis;

    /**
     * Creates a new test case.
     */
    public ObjectFactoryTest() {
        factory = ObjectFactory.INSTANCE;
    }

    /**
     * Verify consistency of {@link ObjectFactory#NAME}, {@link ObjectFactory#IDENTIFIER}, <i>etc.</i> indices.
     */
    @Test
    public void verifyPropertyIndices() {
        assertEquals(IdentifiedObject.NAME_KEY,         ObjectFactory.propertyKey(ObjectFactory.NAME));
        assertEquals(IdentifiedObject.IDENTIFIERS_KEY,  ObjectFactory.propertyKey(ObjectFactory.IDENTIFIER));
        assertEquals(                "codespace",       ObjectFactory.propertyKey(ObjectFactory.CODESPACE));
        assertEquals(IdentifiedObject.ALIAS_KEY,        ObjectFactory.propertyKey(ObjectFactory.ALIAS));
        assertEquals(IdentifiedObject.REMARKS_KEY,      ObjectFactory.propertyKey(ObjectFactory.REMARKS));
        assertEquals(                "deprecated",      ObjectFactory.propertyKey(ObjectFactory.DEPRECATED));
        assertEquals(           Datum.ANCHOR_POINT_KEY, ObjectFactory.propertyKey(ObjectFactory.ANCHOR_POINT));
        assertEquals(           Datum.SCOPE_KEY,        ObjectFactory.propertyKey(ObjectFactory.SCOPE));
    }

    /**
     * Tests {@link ObjectFactory#createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}.
     *
     * @throws FactoryException if creation of the coordinate system axis failed.
     */
    @Test
    public void testCoordinateSystemAxis() throws FactoryException {
        CoordinateSystemAxis axis = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My axis"),
                "x", AxisDirection.NORTH_EAST, Units.METRE);

        assertEquals("My axis", axis.getName().getCode());
        assertEquals(AxisDirection.NORTH_EAST, axis.getDirection());
        assertSame  (Units.METRE, axis.getUnit());
    }

    /**
     * Tests {@link ObjectFactory#createCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)}.
     * This method opportunistically tests the use of a custom unit.
     *
     * @throws FactoryException if creation of the coordinate system failed.
     * @throws IncommensurableException if an error occurred while checking unit factor.
     */
    @Test
    public void testCartesianCS() throws FactoryException, IncommensurableException {
        final Unit<Length> foot = Units.METRE.multiply(0.3048);
        CoordinateSystemAxis axis0 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My axis"),
                "x", AxisDirection.WEST, foot);

        CoordinateSystemAxis axis1 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My other axis"),
                "y", AxisDirection.SOUTH, foot);

        CartesianCS cs = factory.createCartesianCS(
                Map.of(CartesianCS.NAME_KEY, "My CS"),
                axis0, axis1);

        assertEquals("My CS", cs.getName().getCode());
        assertEquals("dimension", 2, cs.getDimension());
        assertSame(axis0, cs.getAxis(0));
        assertSame(axis1, cs.getAxis(1));
        assertEquals(0.3048, axis0.getUnit().getConverterToAny(Units.METRE).convert(1), TOLERANCE);
        assertEquals(0.3048, axis1.getUnit().getConverterToAny(Units.METRE).convert(1), TOLERANCE);
    }

    /**
     * Tests {@link ObjectFactory#createEllipsoid(Map, double, double, Unit)}.
     *
     * @throws FactoryException if creation of the ellipsoid failed.
     */
    @Test
    public void testEllipsoid() throws FactoryException {
        final Identifier id = new Identifier() {
            @Override public String   getCode()      {return "My code";}
            @Override public String   getCodeSpace() {return "My codespace";}
        };
        semiMajorAxis = 12.1 + 4*StrictMath.random();
        Ellipsoid ellipsoid = factory.createEllipsoid(
                Map.of(Ellipsoid.NAME_KEY, "My ellipsoid",
                       Ellipsoid.IDENTIFIERS_KEY, id),
                semiMajorAxis, 12, Units.METRE);

        assertEquals("My ellipsoid", ellipsoid.getName().getCode());
        assertEquals( semiMajorAxis, ellipsoid.getSemiMajorAxis(), TOLERANCE);
        assertEquals(            12, ellipsoid.getSemiMinorAxis(), TOLERANCE);
        assertSame  (   Units.METRE, ellipsoid.getAxisUnit());
        assertFalse (ellipsoid.isIvfDefinitive());

        final Identifier firstID = ellipsoid.getIdentifiers().iterator().next();
        assertEquals("My code",      firstID.getCode());
        assertEquals("My codespace", firstID.getCodeSpace());
    }

    /**
     * Tests {@link ObjectFactory#createFlattenedSphere(Map, double, double, Unit)}.
     *
     * @throws FactoryException if creation of the ellipsoid failed.
     */
    @Test
    public void testFlattenedSphere() throws FactoryException {
        verify(createFlattenedSphere());
    }

    /**
     * Creates a flattened sphere with arbitrary values for testing purpose.
     *
     * @return an arbitrary flattened sphere.
     * @throws FactoryException if creation of the ellipsoid failed.
     */
    private Ellipsoid createFlattenedSphere() throws FactoryException {
        semiMajorAxis = 10 + 4*StrictMath.random();
        return factory.createFlattenedSphere(
                Map.of(Ellipsoid.NAME_KEY, "My ellipsoid"),
                semiMajorAxis, 300, Units.METRE);
    }

    /**
     * Verifies the ellipsoid created by {@link #createFlattenedSphere()}.
     *
     * @param  ellipsoid  the test ellipsoid to verify.
     */
    private void verify(final Ellipsoid ellipsoid) {
        assertEquals("My ellipsoid", ellipsoid.getName().getCode());
        assertEquals( semiMajorAxis, ellipsoid.getSemiMajorAxis(),     TOLERANCE);
        assertEquals(           300, ellipsoid.getInverseFlattening(), TOLERANCE);
        assertSame  (   Units.METRE, ellipsoid.getAxisUnit());
        assertTrue  (                ellipsoid.isIvfDefinitive());
    }

    /**
     * Tests {@link ObjectFactory#createPrimeMeridian(Map, double, Unit)}.
     *
     * @throws FactoryException if creation of the prime meridian failed.
     */
    @Test
    public void testPrimeMeridian() throws FactoryException {
        verify(createPrimeMeridian());
    }

    /**
     * Creates a prime meridian with arbitrary values for testing purpose.
     *
     * @return an arbitrary prime meridian.
     * @throws FactoryException if creation of the ellipsoid failed.
     */
    private PrimeMeridian createPrimeMeridian() throws FactoryException {
        greenwichLongitude = 1 + 2*StrictMath.random();
        return factory.createPrimeMeridian(
                Map.of(PrimeMeridian.NAME_KEY, "My meridian"),
                greenwichLongitude, Units.DEGREE);
    }

    /**
     * Verifies the prime meridian created by {@link #createPrimeMeridian()}.
     *
     * @param  meridian  the test prime meridian to verify.
     */
    private void verify(final PrimeMeridian meridian) {
        assertEquals("My meridian", meridian.getName().getCode());
        assertEquals(greenwichLongitude, meridian.getGreenwichLongitude(), TOLERANCE);
        assertSame  (Units.DEGREE, meridian.getAngularUnit());
    }

    /**
     * Tests {@link ObjectFactory#createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}.
     *
     * @throws FactoryException if creation of the datum failed.
     */
    @Test
    public void testGeodeticReferenceFrame() throws FactoryException {
        verify(createGeodeticReferenceFrame());
    }

    /**
     * Creates a geodetic reference frame for testing purpose.
     *
     * @return an arbitrary geodetic reference frame.
     * @throws FactoryException if creation of the datum failed.
     */
    private GeodeticDatum createGeodeticReferenceFrame() throws FactoryException {
        return factory.createGeodeticDatum(Map.of(GeodeticDatum.NAME_KEY, "My datum"),
                       createFlattenedSphere(),
                       createPrimeMeridian());
    }

    /**
     * Verifies the geodetic reference frame created by {@link #createGeodeticReferenceFrame()}.
     *
     * @param  datum  the test datum to verify.
     */
    private void verify(final GeodeticDatum datum) {
        assertEquals("My datum", datum.getName().getCode());
        verify(datum.getEllipsoid());
        verify(datum.getPrimeMeridian());
    }

    /**
     * Tests {@link ObjectFactory#createTemporalDatum(Map, Date)}.
     *
     * @throws FactoryException if creation of the datum failed.
     */
    @Test
    public void testTemporalDatum() throws FactoryException {
        verify(createTemporalDatum());
    }

    /**
     * Creates a temporal datum for testing purpose.
     *
     * @return an arbitrary temporal datum.
     * @throws FactoryException if creation of the datum failed.
     */
    private TemporalDatum createTemporalDatum() throws FactoryException {
        origin = new Date();
        return factory.createTemporalDatum(
                Map.of(TemporalDatum.NAME_KEY, "My datum"), origin);
    }

    /**
     * Verifies the temporal datum created by {@link #createTemporalDatum()}.
     *
     * @param  datum  the test datum to verify.
     */
    private void verify(final TemporalDatum datum) {
        assertEquals("My datum", datum.getName().getCode());
        assertEquals(origin, datum.getOrigin());
    }

    /**
     * Tests {@link ObjectFactory#createGeographicCRS(Map, GeodeticDatum, EllipsoidalCS)}.
     *
     * @throws FactoryException if creation of the CRS failed.
     */
    @Test
    public void testGeographicCRS() throws FactoryException {
        verify(createGeographicCRS());
    }

    /**
     * Creates a geographic CRS for testing purpose.
     *
     * @return an arbitrary geographic CRS.
     * @throws FactoryException if creation of the CRS failed.
     */
    private GeographicCRS createGeographicCRS() throws FactoryException {
        CoordinateSystemAxis axis0 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My first axis"),
                "x", AxisDirection.WEST, Units.DEGREE);

        CoordinateSystemAxis axis1 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My other axis"),
                "y", AxisDirection.SOUTH, Units.DEGREE);

        EllipsoidalCS cs = factory.createEllipsoidalCS(
                Map.of(EllipsoidalCS.NAME_KEY, "My ellipsoidal CS"),
                axis0, axis1);

        return factory.createGeographicCRS(
                Map.of(GeographicCRS.NAME_KEY, "My geographic CRS"),
                createGeodeticReferenceFrame(), cs);
    }

    /**
     * Verifies the geographic CRS created by {@link #createGeographicCRS()}.
     *
     * @param  crs  the test CRS to verify.
     */
    private void verify(final GeographicCRS crs) {
        assertEquals("My geographic CRS", crs.getName().getCode());
        verify(crs.getDatum());

        final EllipsoidalCS cs = crs.getCoordinateSystem();
        assertEquals("My ellipsoidal CS", cs.getName().getCode());
        assertEquals("dimension", 2, cs.getDimension());
        final CoordinateSystemAxis axis0 = cs.getAxis(0);
        final CoordinateSystemAxis axis1 = cs.getAxis(1);
        assertEquals(AxisDirection.WEST,  axis0.getDirection());
        assertEquals(AxisDirection.SOUTH, axis1.getDirection());
        assertSame  (Units.DEGREE, axis0.getUnit());
        assertSame  (Units.DEGREE, axis1.getUnit());
    }

    /**
     * Tests {@link ObjectFactory#createTemporalCRS(Map, TemporalDatum, TimeCS)}.
     *
     * @throws FactoryException if creation of the CRS failed.
     */
    @Test
    public void testTemporalCRS() throws FactoryException {
        verify(createTemporalCRS());
    }

    /**
     * Creates a temporal CRS for testing purpose.
     *
     * @return an arbitrary temporal CRS.
     * @throws FactoryException if creation of the CRS failed.
     */
    private TemporalCRS createTemporalCRS() throws FactoryException {
        CoordinateSystemAxis axis = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My temporal axis"),
                "t", AxisDirection.FUTURE, Units.YEAR);

        TimeCS cs = factory.createTimeCS(
                Map.of(EllipsoidalCS.NAME_KEY, "My temporal CS"),
                axis);

        return factory.createTemporalCRS(
                Map.of(TemporalDatum.NAME_KEY, "My temporal CRS"),
                createTemporalDatum(), cs);
    }

    /**
     * Verifies the temporal CRS created by {@link #createTemporalCRS()}.
     *
     * @param  crs  the test CRS to verify.
     */
    private void verify(final TemporalCRS crs) {
        assertEquals("My temporal CRS", crs.getName().getCode());
        verify(crs.getDatum());

        final TimeCS cs = crs.getCoordinateSystem();
        assertEquals("My temporal CS", cs.getName().getCode());
        assertEquals("dimension", 1, cs.getDimension());
        final CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals(AxisDirection.FUTURE, axis.getDirection());
        assertSame  (Units.YEAR, axis.getUnit());
    }


    /**
     * Tests {@link ObjectFactory#createCompoundCRS(Map, CoordinateReferenceSystem...)}.
     *
     * @throws FactoryException if creation of the CRS failed.
     */
    @Test
    public void testCompoundCRS() throws FactoryException {
        verify(createCompoundCRS());
    }

    /**
     * Creates a compound CRS for testing purpose.
     *
     * @return an arbitrary compound CRS.
     * @throws FactoryException if creation of the CRS failed.
     */
    private CompoundCRS createCompoundCRS() throws FactoryException {
        return factory.createCompoundCRS(
                Map.of(TemporalDatum.NAME_KEY, "My compound CRS"),
                createGeographicCRS(),
                createTemporalCRS());
    }

    /**
     * Verifies the temporal CRS created by {@link #createCompoundCRS()}.
     *
     * @param  crs  the test CRS to verify.
     */
    private void verify(final CompoundCRS crs) {
        assertEquals("My compound CRS", crs.getName().getCode());
        final List<CoordinateReferenceSystem> components = crs.getComponents();
        assertEquals(2, components.size());
        verify((GeographicCRS) components.get(0));
        verify((TemporalCRS)   components.get(1));
    }
}
