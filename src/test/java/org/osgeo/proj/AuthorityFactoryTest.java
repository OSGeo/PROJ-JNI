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
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.OperationMethod;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests the {@link AuthorityFactory} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
public final strictfp class AuthorityFactoryTest {
    /**
     * Frequently used constant.
     */
    private static final String EPSG = "EPSG";

    /**
     * A temporary flag for disabling some tests until we upgrade PROJ-JNI for supporting datum ensembles.
     *
     * @since 1.1
     */
    private static final boolean ENSEMBLE_SUPPORTED = false;

    /**
     * Tests {@link AuthorityFactory} instantiation.
     *
     * @throws FactoryException if the factory can not be created.
     */
    @Test
    public void testNewInstance() throws FactoryException {
        try (Context c = Context.acquire()) {
            AuthorityFactory epsg = c.factory(EPSG);
            AuthorityFactory iau  = c.factory("IAU");
            assertNotSame(epsg, iau);

            assertSame(epsg, c.factory(EPSG));
            assertSame(iau,  c.factory("IAU"));
        }
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)} with an invalid code.
     * We expect a {@link NoSuchAuthorityCodeException} to be thrown with information about the invalid code.
     *
     * @throws FactoryException if the operation failed for another reason than the expected exception.
     */
    @Test
    public void testInvalidCode() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        try {
            factory.createCoordinateSystem("-52");
            fail("An exception should have been thrown.");
        } catch (NoSuchAuthorityCodeException e) {
            assertEquals("getAuthority",     EPSG,  e.getAuthority());
            assertEquals("getAuthorityCode", "-52", e.getAuthorityCode());
        }
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the prime meridian creation failed.
     */
    @Test
    public void testCreatePrimeMeridian() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        final PrimeMeridian meridian = factory.createPrimeMeridian("8903");
        assertSame("unit", Units.GRAD, meridian.getAngularUnit());
        assertEquals("longitude", 2.5969213, meridian.getGreenwichLongitude(), 1E-7);
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the CS creation failed.
     */
    @Test
    public void testCreateCoordinateSystem() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        final EllipsoidalCS cs = factory.createEllipsoidalCS("6422");
        assertEquals("EPSG:6422", String.format("%#s", cs));
        assertEquals("dimension", 2, cs.getDimension());
        final CoordinateSystemAxis lat = cs.getAxis(0);
        final CoordinateSystemAxis lon = cs.getAxis(1);
        assertEquals("Lat", lat.getAbbreviation());
        assertEquals("Lon", lon.getAbbreviation());
        try {
            cs.getAxis(2);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
        assertSame(AxisDirection.NORTH, lat.getDirection());
        assertSame(AxisDirection.EAST,  lon.getDirection());
        assertSame(Units.DEGREE,        lat.getUnit());
        assertSame(Units.DEGREE,        lon.getUnit());
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the CRS creation failed.
     */
    @Test
    public void testCreateProjectedCRS() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        final ProjectedCRS crs = factory.createProjectedCRS("3395");
        assertEquals("EPSG:3395", String.format("%#s", crs));
        assertIdentifierEquals(EPSG, "3395", crs.getIdentifiers());
        assertEquals("dimension", 2, ((CRS) crs).getDimension());

        final CartesianCS cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertEquals("E", cs.getAxis(0).getAbbreviation());
        assertEquals("N", cs.getAxis(1).getAbbreviation());
        assertSame(AxisDirection.EAST,  cs.getAxis(0).getDirection());
        assertSame(AxisDirection.NORTH, cs.getAxis(1).getDirection());

        final GeodeticDatum datum = crs.getDatum();
        final GeographicCRS base  = crs.getBaseCRS();
        assertSame(datum, base.getDatum());
        if (ENSEMBLE_SUPPORTED) {
            assertNotNull(datum);

            final Ellipsoid ellipsoid = datum.getEllipsoid();
            assertFalse(ellipsoid.isSphere());
            assertTrue (ellipsoid.isIvfDefinitive());
            assertEquals(6378137, ellipsoid.getSemiMajorAxis(),     0.5);
            assertEquals(6356752, ellipsoid.getSemiMinorAxis(),     0.5);
            assertEquals(298.257, ellipsoid.getInverseFlattening(), 0.0005);

            final PrimeMeridian pm = datum.getPrimeMeridian();
            assertEquals(0, pm.getGreenwichLongitude(), 0);
        }
        final Projection conv = crs.getConversionFromBase();
        assertSame(base, conv.getSourceCRS());
        assertSame(crs,  conv.getTargetCRS());

        final OperationMethod method = conv.getMethod();
        assertIdentifierEquals(EPSG, "9804", method.getIdentifiers());        // Mercator (variant A)
        ParameterTest.verifyWorldMercator(conv);
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     * This test uses <cite>"RGF93 / Lambert-93 + NGF-IGN69 height"</cite>.
     *
     * @throws FactoryException if the factory can not be created or if the CRS creation failed.
     */
    @Test
    public void testCreateCompoundCRS() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        final CompoundCRS crs = factory.createCompoundCRS("5698");
        assertEquals("dimension", 3, ((CRS) crs).getDimension());

        final List<CoordinateReferenceSystem> components = crs.getComponents();
        assertEquals(2, components.size());
        final CoordinateReferenceSystem horizontal = components.get(0);
        final CoordinateReferenceSystem vertical   = components.get(1);
        assertInstanceOf("horizontal", ProjectedCRS.class, horizontal);
        assertInstanceOf("vertical",    VerticalCRS.class, vertical);
        try {
            components.get(2);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertSame(AxisDirection.EAST,  cs.getAxis(0).getDirection());
        assertSame(AxisDirection.NORTH, cs.getAxis(1).getDirection());
        assertSame(AxisDirection.UP,    cs.getAxis(2).getDirection());
        try {
            cs.getAxis(3);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests indirectly {@link AuthorityFactory#getDescriptionText(String)}.
     *
     * @throws FactoryException if the operation failed.
     */
    @Test
    public void testGetDescriptionText() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        try {
            factory.getDescriptionText("-52");
            fail("An exception should have been thrown.");
        } catch (NoSuchAuthorityCodeException e) {
            assertEquals("getAuthority",     EPSG,  e.getAuthority());
            assertEquals("getAuthorityCode", "-52", e.getAuthorityCode());
        }
        final InternationalString text = factory.getDescriptionText("4326");
        assertNotNull(text);
        assertFalse(text.toString().isEmpty());
    }

    /**
     * Verifies that asking twice for the same code returns the same wrapper.
     *
     * @throws FactoryException if the factory can not be created or if the CS creation failed.
     */
    @Test
    public void testCache() throws FactoryException  {
        final AuthorityFactory.API factory = new AuthorityFactory.API(EPSG);
        final EllipsoidalCS cs  = factory.createEllipsoidalCS("6422");
        final GeographicCRS crs = factory.createGeographicCRS("4326");
        assertSame(cs,  factory.createEllipsoidalCS("6422"));
        assertSame(crs, factory.createGeographicCRS("4326"));
        assertSame(cs, crs.getCoordinateSystem());
    }

    /**
     * Asserts that the given collection contains an identifier for the given code space with the given value.
     * If the collection contains also identifiers in other code spaces, those additional identifiers are ignored.
     *
     * @param  codeSpace    the code space of the identifier to verify.
     * @param  code         the expected code in the given code space.
     * @param  identifiers  the collection to verify.
     */
    private static void assertIdentifierEquals(final String codeSpace, final String code,
            final Iterable<ReferenceIdentifier> identifiers)
    {
        boolean found = false;
        for (final ReferenceIdentifier id : identifiers) {
            if (codeSpace.equalsIgnoreCase(id.getCodeSpace())) {
                assertEquals("code", code, id.getCode());
                found = true;
            }
        }
        assertTrue("Identifier not found.", found);
    }
}
