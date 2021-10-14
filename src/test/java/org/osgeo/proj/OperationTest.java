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

import org.junit.Test;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.test.referencing.TransformTestCase;


/**
 * Tests coordinate operations executed with {@link Operation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class OperationTest extends TransformTestCase {
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
    public OperationTest() {
        factory = TestFactorySource.OPERATIONS;
        crsFactory = TestFactorySource.EPSG;
    }

    /**
     * Sets {@link #transform} field to an operation from the given source to given target.
     *
     * @param  sourceCode  EPSG code of source CRS.
     * @param  targetCode  EPSG code of target CRS.
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     */
    private void initialize(final String sourceCode, final String targetCode) throws FactoryException {
        final CoordinateReferenceSystem source = crsFactory.createCoordinateReferenceSystem(sourceCode);
        final CoordinateReferenceSystem target = crsFactory.createCoordinateReferenceSystem(targetCode);
        final CoordinateOperation operation = factory.createOperation(source, target);
        transform = operation.getMathTransform();
    }

    /**
     * Coordinates to use for testing an operation, in (latitude, longitude) order.
     *
     * @return (latitude, longitude) coordinates in degrees.
     */
    private static float[] testData() {
        return new float[] {
            45.500f,  -73.567f,             // Montreal
            49.250f, -123.100f,             // Vancouver
            35.653f,  139.839f,             // Tokyo
            48.865f,    2.349f,             // Paris
            21.029f,  105.805f,             // Hanoi
            55.676f,   12.568f,             // Copenhagen
           -12.046f,  -77.043f              // Lima
        };
    }

    /**
     * Tests the requests for Mercator projection and the conversion of a coordinate.
     * Operation code is EPSG::19883.
     *
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testMercator() throws FactoryException, TransformException {
        initialize("4326", "3395");
        tolerance = 0.01;                                           // Request one centimetre accuracy.
        verifyTransform(new double[] {40, 60},                      // (latitude, longitude) point to transform.
                        new double[] {6679169.45, 4838471.40});     // Expected (easting, northing) values.

        verifyConsistency(testData());
    }

    /**
     * Tests an operation that reduce the number of dimensions. The tested operation does (λ,φ,h) → (φ,λ).
     * The coordinate swapping performed by that operation is a simple way to verify that the transform is
     * executed.
     *
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testDimensionReduce() throws FactoryException, TransformException {
        initialize("7042", "4171");
        tolerance = 0.0001;
        verifyTransform(new double[] {61, 41, 0},
                        new double[] {41, 61});

        verifyConsistency(new float[] {
                45.500f,  -73.567f, 10f,
                49.250f, -123.100f, 12f,
                35.653f,  139.839f, 15f,
                48.865f,    2.349f, 10f});
    }

    /**
     * Tests an operation that increase the number of dimensions. The tested operation does (φ,λ) → (λ,φ,h).
     * The coordinate swapping performed by that operation is a simple way to verify that the transform is
     * executed.
     *
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testDimensionIncrease() throws FactoryException, TransformException {
        initialize("4171", "7042");
        tolerance = 0.0001;
        verifyTransform(new double[] {42, 62},
                        new double[] {62, 42, 0});

        verifyConsistency(testData());
    }
}
