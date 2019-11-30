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
import java.util.Date;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ObjectFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class ObjectFactoryTest {
    /**
     * The factory to use for testing.
     */
    private final ObjectFactory factory;

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
     *
     * @throws FactoryException if creation of the coordinate system failed.
     */
    @Test
    public void testCartesianCS() throws FactoryException {
        CoordinateSystemAxis axis0 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My axis"),
                "x", AxisDirection.WEST, Units.METRE);

        CoordinateSystemAxis axis1 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My other axis"),
                "y", AxisDirection.SOUTH, Units.METRE);

        CartesianCS cs = factory.createCartesianCS(
                Map.of(CartesianCS.NAME_KEY, "My CS"),
                axis0, axis1);

        assertEquals("My CS", cs.getName().getCode());
        assertEquals("dimension", 2, cs.getDimension());
        assertSame(axis0, cs.getAxis(0));
        assertSame(axis1, cs.getAxis(1));
    }

    /**
     * Tests {@link ObjectFactory#createEllipsoid(Map, double, double, Unit)}.
     *
     * @throws FactoryException if creation of the ellipsoid failed.
     */
    @Test
    public void testEllipsoid() throws FactoryException {
        Ellipsoid ellipsoid = factory.createEllipsoid(
                Map.of(Ellipsoid.NAME_KEY, "My ellipsoid"),
                14, 12, Units.METRE);

        assertEquals("My ellipsoid", ellipsoid.getName().getCode());
        assertEquals(14, ellipsoid.getSemiMajorAxis(), 1E-12);
        assertEquals(12, ellipsoid.getSemiMinorAxis(), 1E-12);
        assertSame  (Units.METRE, ellipsoid.getAxisUnit());
        assertFalse (ellipsoid.isIvfDefinitive());
    }

    /**
     * Tests {@link ObjectFactory#createFlattenedSphere(Map, double, double, Unit)}.
     *
     * @throws FactoryException if creation of the ellipsoid failed.
     */
    @Test
    public void testFlattenedSphere() throws FactoryException {
        Ellipsoid ellipsoid = factory.createFlattenedSphere(
                Map.of(Ellipsoid.NAME_KEY, "My ellipsoid"),
                14, 300, Units.METRE);

        assertEquals("My ellipsoid", ellipsoid.getName().getCode());
        assertEquals( 14, ellipsoid.getSemiMajorAxis(), 1E-12);
        assertEquals(300, ellipsoid.getInverseFlattening(), 1E-12);
        assertSame  (Units.METRE, ellipsoid.getAxisUnit());
        assertTrue  (ellipsoid.isIvfDefinitive());
    }

    /**
     * Tests {@link ObjectFactory#createPrimeMeridian(Map, double, Unit)}.
     *
     * @throws FactoryException if creation of the prime meridian failed.
     */
    @Test
    public void testPrimeMeridian() throws FactoryException {
        PrimeMeridian meridian = factory.createPrimeMeridian(
                Map.of(PrimeMeridian.NAME_KEY, "My meridian"),
                1.1, Units.DEGREE);

        assertEquals("My meridian", meridian.getName().getCode());
        assertEquals(1.1, meridian.getGreenwichLongitude(), 1E-12);
        assertSame  (Units.DEGREE, meridian.getAngularUnit());
    }

    /**
     * Tests {@link ObjectFactory#createGeodeticDatum(Map, Ellipsoid, PrimeMeridian)}.
     *
     * @throws FactoryException if creation of the datum failed.
     */
    @Test
    public void testGeodeticReferenceFrame() throws FactoryException {
        GeodeticDatum datum = factory.createGeodeticDatum(Map.of(GeodeticDatum.NAME_KEY, "My datum"),
                factory.createFlattenedSphere(Map.of(Ellipsoid.NAME_KEY, "My ellipsoid"), 14, 300, Units.METRE),
                factory.createPrimeMeridian  (Map.of(Ellipsoid.NAME_KEY, "My meridian"),  1.1, Units.DEGREE));

        assertEquals("My datum", datum.getName().getCode());

        Ellipsoid ellipsoid = datum.getEllipsoid();
        assertEquals("My ellipsoid", ellipsoid.getName().getCode());
        assertEquals( 14, ellipsoid.getSemiMajorAxis(), 1E-12);
        assertEquals(300, ellipsoid.getInverseFlattening(), 1E-12);
        assertSame  (Units.METRE, ellipsoid.getAxisUnit());
        assertTrue  (ellipsoid.isIvfDefinitive());

        PrimeMeridian meridian = datum.getPrimeMeridian();
        assertEquals("My meridian", meridian.getName().getCode());
        assertEquals(1.1, meridian.getGreenwichLongitude(), 1E-12);
        assertSame  (Units.DEGREE, meridian.getAngularUnit());
    }

    /**
     * Tests {@link ObjectFactory#createTemporalDatum(Map, Date)}.
     *
     * @throws FactoryException if creation of the datum failed.
     */
    @Test
    public void testTemporalDatum() throws FactoryException {
        final Date origin = new Date();
        TemporalDatum datum = factory.createTemporalDatum(
                Map.of(TemporalDatum.NAME_KEY, "My datum"), origin);

        assertEquals("My datum", datum.getName().getCode());
//      assertEquals(origin, datum.getOrigin());    // TODO
    }
}
