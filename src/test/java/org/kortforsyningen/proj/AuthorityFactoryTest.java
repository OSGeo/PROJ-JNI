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

import org.junit.Test;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;

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
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(int, String)} with an invalid code.
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
     * Tests indirectly {@link AuthorityFactory#createGeodeticObject(int, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the CS creation failed.
     */
    @Test
    public void testCreateCoordinateSystem() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final CoordinateSystem cs = factory.createCoordinateSystem("6422");
        // TODO
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
}
