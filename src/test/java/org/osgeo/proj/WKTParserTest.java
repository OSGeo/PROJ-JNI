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

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.opengis.test.wkt.CRSParserTest;



/**
 * Tests Well-Known Text parser using the tests defined in GeoAPI.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   2.0
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.JVM)      // Intentionally want some randomness
public final strictfp class WKTParserTest extends CRSParserTest {
    /**
     * Creates a new test case using the default {@code CRSFactory} implementation.
     */
    public WKTParserTest() {
        super(ObjectFactory.INSTANCE);
        /*
         * ISO 19111:2007 said that axis names shall be "geodetic latitude" and "geodetic longitude",
         * but PROJ keep "latitude" and "longitude" names as they appear in the WKT string.
         */
        validators.crs.enforceStandardNames = false;
    }

    /**
     * Skip test having a PROJ error. Error message is:
     * {@literal "Missing EDATUM / ENGINEERINGDATUM node"}.
     */
    @Ignore
    @Override
    public void testTemporal() {
    }

    /**
     * Skip test having a PROJ error. Error message is:
     * {@literal "unexpected TIMEUNIT"}.
     */
    @Ignore
    @Override
    public void testCompoundWithTime() {
    }

    /**
     * Skip test having a PROJ error. Error message is:
     * {@literal "Missing EDATUM / ENGINEERINGDATUM node"}.
     */
    @Ignore
    @Override
    public void testDerivedEngineeringFromGeodetic() {
    }

    /**
     * Skip test having a PROJ error. Error message is:
     * {@literal "Missing EDATUM / ENGINEERINGDATUM node"}.
     */
    @Ignore
    @Override
    public void testDerivedEngineeringFromProjected() {
    }

    /**
     * Skip test having a PROJ error. Error message is:
     * {@literal "Parsing error : syntax error, unexpected PRIMEM, expecting ID"}.
     */
    @Ignore
    @Override
    public void testProjectedWithImplicitParameterUnits() {
    }

    /**
     * Skip test failing validation. Error message is:
     * {@literal "(…) is of type Geographic while the expected type was DerivedCRS or a subtype"}.
     */
    @Ignore
    @Override
    public void testDerivedGeodetic() {
    }

    /**
     * Skip test failing validation. Error message is:
     * {@literal "CoordinateSystemAxis: abbreviation is mandatory"}.
     */
    @Ignore
    @Override
    public void testEngineeringRotated() {
    }

    /**
     * Skip test having a GeoAPI test failure. Error message is:
     * {@literal "CoordinateSystem.getAxis(*).getUnit() expected:<Unnamed> but was:<US survey foot>"}.
     */
    @Ignore
    @Override
    public void testProjectedWithFootUnits() {
    }

    /**
     * Skip test having a PROJ-JNI limitation. Error message is:
     * {@literal "The PROJ-JNI binding provides only minimal support for Unit of Measurement operations.
     * For more advanced operations, a JSR-363 implementation should be added to the classpath."}.
     */
    @Ignore
    @Override
    public void testParametric() {
    }

    /**
     * Skip test having a PROJ-JNI limitation. Error message is:
     * {@literal "The PROJ-JNI binding provides only minimal support for Unit of Measurement operations.
     * For more advanced operations, a JSR-363 implementation should be added to the classpath."}.
     */
    @Ignore
    @Override
    public void testCompoundWithParametric() {
    }
}
