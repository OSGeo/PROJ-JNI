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

import java.util.Collection;
import java.util.Iterator;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.junit.Test;
import org.opengis.referencing.datum.GeodeticDatum;

import static org.junit.Assert.*;


/**
 * Tests the {@link AuthorityFactory} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class AuthorityFactoryTest {
    /**
     * Tests {@link AuthorityFactory} instantiation.
     *
     * @throws FactoryException if the factory can not be created.
     */
    @Test
    public void testNewInstance() throws FactoryException {
        try (Context c = Context.acquire()) {
            AuthorityFactory epsg = c.factory("EPSG");
            AuthorityFactory iau  = c.factory("IAU");
            assertNotSame(epsg, iau);

            assertSame(epsg, c.factory("EPSG"));
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
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        try {
            factory.createCoordinateSystem("-52");
            fail("An exception should have been thrown.");
        } catch (NoSuchAuthorityCodeException e) {
            assertEquals("getAuthority",    "EPSG", e.getAuthority());
            assertEquals("getAuthorityCode", "-52", e.getAuthorityCode());
        }
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the CS creation failed.
     */
    @Test
    public void testCreateCoordinateSystem() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final EllipsoidalCS cs = factory.createEllipsoidalCS("6422");
        assertEquals("EPSG:6422", String.format("%#s", cs));
        assertEquals("dimension", 2, cs.getDimension());
        assertEquals("Lat", cs.getAxis(0).getAbbreviation());
        assertEquals("Lon", cs.getAxis(1).getAbbreviation());
        try {
            cs.getAxis(2);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
        assertSame(AxisDirection.NORTH, cs.getAxis(0).getDirection());
        assertSame(AxisDirection.EAST,  cs.getAxis(1).getDirection());
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the CRS creation failed.
     */
    @Test
    public void testCreateProjectedCRS() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final ProjectedCRS crs = factory.createProjectedCRS("3395");
        assertEquals("EPSG:3395", String.format("%#s", crs));
        assertIdentifierEquals("EPSG", "3395", crs.getIdentifiers());

        final CartesianCS cs = crs.getCoordinateSystem();
        assertEquals("dimension", 2, cs.getDimension());
        assertEquals("E", cs.getAxis(0).getAbbreviation());
        assertEquals("N", cs.getAxis(1).getAbbreviation());
        assertSame(AxisDirection.EAST,  cs.getAxis(0).getDirection());
        assertSame(AxisDirection.NORTH, cs.getAxis(1).getDirection());

        final GeodeticDatum datum = crs.getDatum();
        assertNotNull(datum);

        final GeographicCRS base = crs.getBaseCRS();
        assertSame(datum, base.getDatum());
        assertNotNull(crs.getConversionFromBase());
    }

    /**
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(short, String)}.
     * This test uses <cite>"RGF93 / Lambert-93 + NGF-IGN69 height"</cite>.
     *
     * @throws FactoryException if the factory can not be created or if the CRS creation failed.
     */
    @Test
    public void testCreateCompoundCRS() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("5698");
        assertEquals("dimension", 3, ((CRS) crs).getDimension());
    }

    /**
     * Tests indirectly {@link AuthorityFactory#getDescriptionText(String)}.
     *
     * @throws FactoryException if the operation failed.
     */
    @Test
    public void testGetDescriptionText() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        try {
            factory.getDescriptionText("-52");
            fail("An exception should have been thrown.");
        } catch (NoSuchAuthorityCodeException e) {
            assertEquals("getAuthority",    "EPSG", e.getAuthority());
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
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final EllipsoidalCS cs  = factory.createEllipsoidalCS("6422");
        final GeographicCRS crs = factory.createGeographicCRS("4326");
        assertSame(cs,  factory.createEllipsoidalCS("6422"));
        assertSame(crs, factory.createGeographicCRS("4326"));
        assertSame(cs, crs.getCoordinateSystem());
    }

    /**
     * Asserts that the given collection contains exactly one element,
     * and that the element is equal to the given value.
     *
     * @param  codeSpace   the expected code space.
     * @param  code        the expected code.
     * @param  collection  the collection to verify.
     */
    private static void assertIdentifierEquals(final String codeSpace, final String code,
            final Collection<ReferenceIdentifier> collection)
    {
        assertEquals("size", 1, collection.size());
        final Iterator<ReferenceIdentifier> it = collection.iterator();
        assertTrue("Expected an element.", it.hasNext());
        final ReferenceIdentifier id = it.next();
        assertEquals("codeSpace", codeSpace, id.getCodeSpace());
        assertEquals("code",      code,      id.getCode());
        assertFalse("Unexpected element.", it.hasNext());
    }
}
