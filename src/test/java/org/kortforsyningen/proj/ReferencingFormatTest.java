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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import static org.junit.Assert.*;


/**
 * Tests {@link ReferencingFormat}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class ReferencingFormatTest {
    /**
     * The prefix used in {@code ReferencingFormat.h} for lines defining constant values.
     */
    private static final String DEFINE = "#define Format_";

    /**
     * Verifies the {@link ReferencingFormat.Convention} ordinal values.
     * This method parses the {@code ReferencingFormat.h} file
     * and compares the defined values against {@link ReferencingFormat.Convention#ordinal()}.
     *
     * @throws IOException if an error occurred while reading the {@code ReferencingFormat.h} file.
     */
    @Test
    public void verifyOrdinalValues() throws IOException {
        final EnumSet<ReferencingFormat.Convention> remainings = EnumSet.allOf(ReferencingFormat.Convention.class);
        for (final String line : Files.readAllLines(Paths.get("src/main/cpp/org_kortforsyningen_proj_ReferencingFormat$Convention.h"))) {
            if (line.startsWith(DEFINE)) {
                final int s1 = line.indexOf(' ', DEFINE.length());
                final int s2 = line.lastIndexOf(' ');
                if ((s1 | s2) < 0 ) {
                    fail("Malformed line: " + line);
                }
                final String key = line.substring(DEFINE.length(), s1);
                final int  value = Integer.parseInt(line.substring(s2 + 1));
                final ReferencingFormat.Convention c = ReferencingFormat.Convention.valueOf(key);
                assertTrue(key, remainings.remove(c));
                assertEquals(key, c.ordinal(), value);
            }
        }
        for (final ReferencingFormat.Convention c : remainings) {
            fail("Missing convention: " + c);
        }
    }

    /**
     * Tests {@link IdentifiableObject#toWKT()}.
     *
     * @throws FactoryException if an error occurred while creating the test CRS.
     */
    @Test
    public void testToString() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("4326");
        final String wkt = crs.toWKT();
        assertTrue(wkt, wkt.startsWith(
                "GEOGCRS[\"WGS 84\",\n" +
                "    DATUM[\"World Geodetic System 1984\",\n" +
                "        ELLIPSOID[\"WGS 84\","));
        /*
         * We verify only the first few lines in order to reduce the risk to break the tests
         * if some output details change in future PROJ versions. The part tested above are
         * more likely to be stable.
         */
    }

    /**
     * Tests {@link ReferencingFormat#format(Object)} to a WKT format.
     *
     * @throws FactoryException if an error occurred while creating the test CRS.
     */
    @Test
    public void testFormatWKT() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("4326");
        final ReferencingFormat formatter = new ReferencingFormat();
        formatter.setConvention(ReferencingFormat.Convention.WKT1_ESRI);
        final String wkt = formatter.format(crs);
        assertTrue(wkt, wkt.startsWith(
                "GEOGCS[\"GCS_WGS_1984\",\n" +
                "    DATUM[\"D_WGS_1984\",\n" +
                "        SPHEROID[\"WGS_1984\","));
    }

    /**
     * Tests {@link ReferencingFormat#format(Object)} to a WKT format.
     *
     * @throws FactoryException if an error occurred while creating the test CRS.
     */
    @Test
    public void testFormatJSON() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("4326");
        final ReferencingFormat formatter = new ReferencingFormat();
        formatter.setConvention(ReferencingFormat.Convention.JSON);
        final String wkt = formatter.format(crs);
        assertTrue(wkt, wkt.contains("\"name\": \"WGS 84\""));
    }

    /**
     * Tests {@link ReferencingFormat#format(Object)} to a PROJ format.
     *
     * @throws FactoryException if an error occurred while creating the test CRS.
     */
    @Test
    public void testFormatPROJ() throws FactoryException {
        final AuthorityFactory.API factory = new AuthorityFactory.API("EPSG");
        final CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("3395");
        final ReferencingFormat formatter = new ReferencingFormat();
        formatter.setConvention(ReferencingFormat.Convention.PROJ_5);
        final String wkt = formatter.format(crs);
        assertEquals("+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs +type=crs", wkt);
    }
}
