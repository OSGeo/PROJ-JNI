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

import java.util.List;
import java.util.Locale;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterTypeException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Conversion;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Parameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class ParameterTest {
    /**
     * Asserts that the given string starts with the given prefix.
     *
     * @param  prefix  the expected prefix.
     * @param  actual  the text to verify.
     */
    static void assertStartsWith(final String prefix, final String actual) {
        if (!actual.startsWith(prefix)) {
            // Let JUnit formats an error message with an emphasis on the difference.
            assertEquals(prefix, actual.substring(StrictMath.min(prefix.length(), actual.length())));
        }
    }

    /**
     * Asserts that the given parameter descriptor has a name starting with "latitude", ignoring case.
     * We actually expect "Latitude of natural origin" but be tolerant in case "latitude_something" is used.
     *
     * @param  descriptor  the descriptor to verify.
     */
    private static void assertLatitude(final ParameterDescriptor<?> descriptor) {
        assertStartsWith("latitude", descriptor.getName().getCode().toLowerCase(Locale.US));
    }

    /**
     * Tests parameters of Mercator projection.
     *
     * @throws FactoryException if an error occurred while creating the operation.
     */
    @Test
    public void testMercator() throws FactoryException {
        AuthorityFactory.API factory = TestFactorySource.EPSG;
        verifyWorldMercator((Conversion) factory.createCoordinateOperation("19883"));
    }

    /**
     * Verifies parameters of an operation which is expected to be "World Mercator".
     *
     * @param  operation  a "World Mercator (WGS84)" operation.
     */
    static void verifyWorldMercator(final Conversion operation) {
        assertEquals("World Mercator",  operation.getName().getCode());
        /*
         * Verify the parameter description (without values). The expected parameter values are
         * Latitude of natural origin, Longitude of natural origin, Scale factor at natural origin,
         * False easting, False northing, but we do not test that in case there is some variations
         * between different PROJ versions.
         */
        final OperationMethod method = operation.getMethod();
        final ParameterDescriptorGroup descriptorGroup = method.getParameters();
        final List<GeneralParameterDescriptor> descriptors = descriptorGroup.descriptors();
        assertStartsWith("Mercator", method.getName().getCode());
        assertFalse(descriptors.isEmpty());
        assertLatitude((ParameterDescriptor) descriptors.get(0));
        /*
         * Verify the parameter values.
         */
        final ParameterValueGroup valueGroup = operation.getParameterValues();
        final List<GeneralParameterValue> parameters = valueGroup.values();
        assertEquals(descriptors.size(), parameters.size());
        final ParameterValue<?> first = (ParameterValue) parameters.get(0);
        assertEquals(Double.class, first.getDescriptor().getValueClass());
        assertEquals(0, first.doubleValue(), 0);
        assertSame(Units.DEGREE, first.getUnit());
        assertLatitude(first.getDescriptor());
        try {
            first.intValue();
            fail("Expected InvalidParameterTypeException.");
        } catch (InvalidParameterTypeException e) {
            assertEquals("This parameter is not an integer.", e.getMessage());
            assertNotNull(e.getParameterName());
        }
        /*
         * Verify the search of parameters.
         */
        assertEquals("False easting", descriptorGroup.descriptor("False easting").getName().getCode());
        assertEquals("False easting", valueGroup.parameter("False easting").getDescriptor().getName().getCode());
    }
}
