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

import java.util.Optional;
import org.junit.Test;
import org.opengis.util.FactoryException;
import org.opengis.referencing.IdentifiedObject;

import static org.junit.Assert.*;


/**
 * Tests the {@link Proj} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class ProjTest {
    /**
     * Tests {@link Proj#version()}. This tests verifies that the release string
     * contains 3 numbers of the form x.y.z. This number is followed by the date,
     * but this test does not verify that aspect.
     */
    @Test
    public void testVersion() {
        final Optional<String> version = Proj.version();
        assertTrue("PROJ library not found.", version.isPresent());
        assertTrue(version.get().matches(".*\\d+\\.\\d+\\.\\d+.*"));
    }

    /**
     * Tests {@link Proj#createFromUserInput(String)}.
     *
     * @throws FactoryException if the object creation failed.
     */
    @Test
    public void testCreateFromUserInput() throws FactoryException {
        final IdentifiedObject obj = Proj.createFromUserInput("EPSG:3395");
        assertTrue(obj instanceof CRS);

        // Verify that the hash code value is stable.
        assertEquals(obj.hashCode(), obj.hashCode());
    }
}
