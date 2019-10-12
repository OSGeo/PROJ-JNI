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
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.test.referencing.TransformTestCase;

import static org.junit.Assert.*;


/**
 * Tests {@link OperationFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class OperationFactoryTest extends TransformTestCase {
    /**
     * The factory to test.
     */
    private final OperationFactory factory;

    /**
     * The factory for creating CRS using EPSG codes.
     */
    private final AuthorityFactory.API crsFactory;

    /**
     * Creates a new test case.
     */
    public OperationFactoryTest() {
        factory = new OperationFactory(null);
        crsFactory = new AuthorityFactory.API("EPSG");

        // Disable tests of unsupported features.
        isDoubleToFloatSupported    = false;
        isFloatToDoubleSupported    = false;
        isOverlappingArraySupported = false;
        isInverseTransformSupported = false;
    }

    /**
     * Tests the requests for Mercator projection and the conversion of a coordinate.
     *
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testMercator() throws FactoryException, TransformException {
        final CoordinateReferenceSystem source = crsFactory.createCoordinateReferenceSystem("4326");
        final CoordinateReferenceSystem target = crsFactory.createCoordinateReferenceSystem("3395");
        final CoordinateOperation operation = factory.createOperation(source, target);
        assertTrue(operation.toWKT().startsWith("CONVERSION[\"World Mercator\","));
        assertSame("sourceCRS", source, operation.getSourceCRS());
        assertSame("targetCRS", target, operation.getTargetCRS());

        transform = operation.getMathTransform();
        tolerance = 0.01;                                           // Request one centimetre accuracy.
        verifyTransform(new double[] {40, 60},                      // (latitude, longitude) point to transform.
                        new double[] {6679169.45, 4838471.40});     // Expected (easting, northing) values.
    }
}
