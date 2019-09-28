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
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.util.FactoryException;

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
     * Tests indirectly {@link AuthorityFactory#createCoordinateSystem(long, String)}.
     *
     * @throws FactoryException if the factory can not be created or if the CS creation failed.
     */
    @Test
    public void testCreateCoordinateSystem() throws FactoryException {
        final AuthorityFactory.CS factory = new AuthorityFactory.CS("EPSG");
        final CoordinateSystem cs = factory.createCoordinateSystem("6422");
        // TODO
    }
}
