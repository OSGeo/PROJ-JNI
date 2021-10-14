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

import java.util.ServiceLoader;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the automatic discovery of factories using {@link java.util.ServiceLoader}.
 * The providers are defined in the {@link org.osgeo.proj.spi} package.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class ProvidersTest {
    /**
     * Tests the automatic discovery of {@link CRSAuthorityFactory}.
     */
    @Test
    public void testCRSAuthorityFactory() {
        boolean foundEPSG = false;
        boolean foundIAU  = false;
        for (CRSAuthorityFactory factory : ServiceLoader.load(CRSAuthorityFactory.class)) {
            assertEquals("PROJ", factory.getVendor().getTitle().toString());
            final String authority = factory.getAuthority().getTitle().toString();
            foundEPSG |= authority.equals("EPSG");
            foundIAU  |= authority.equals("IAU");
        }
        assertTrue("EPSG", foundEPSG);
        assertTrue("IAU",  foundIAU);
    }

    /**
     * Tests the automatic discovery of {@link CoordinateOperationFactory}.
     */
    @Test
    public void testOperationFactory() {
        boolean found = false;
        for (CoordinateOperationFactory factory : ServiceLoader.load(CoordinateOperationFactory.class)) {
            assertEquals("PROJ", factory.getVendor().getTitle().toString());
            found = true;
        }
        assertTrue(found);
    }
}
