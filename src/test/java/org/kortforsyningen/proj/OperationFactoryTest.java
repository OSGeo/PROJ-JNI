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
import org.opengis.util.FactoryException;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.Result;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Conversion;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link OperationFactory}. This class tests only the creation of coordinate operation.
 * Transformations of coordinate values are tested by another class, {@link OperationTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class OperationFactoryTest {
    /**
     * The factory for creating CRS using EPSG codes.
     */
    private final AuthorityFactory.API crsFactory;

    /**
     * Creates a new test case.
     */
    public OperationFactoryTest() {
        crsFactory = TestFactorySource.EPSG;
    }

    /**
     * Tests creation of Mercator projection from a pair of CRS.
     *
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     */
    @Test
    public void testMercator() throws FactoryException {
        final OperationFactory          factory   = new OperationFactory(null);
        final CoordinateReferenceSystem source    = crsFactory.createCoordinateReferenceSystem("4326");
        final CoordinateReferenceSystem target    = crsFactory.createCoordinateReferenceSystem("3395");
        final CoordinateOperation       operation = factory.createOperation(source, target);
        assertSame("sourceCRS", source, operation.getSourceCRS());
        assertSame("targetCRS", target, operation.getTargetCRS());
        ParameterTest.assertStartsWith("CONVERSION[\"World Mercator\",", operation.toWKT());
        ParameterTest.verifyWorldMercator((Conversion) operation);
    }

    /**
     * Tests the effect of specifying an area of interest when searching for a coordinate operation.
     *
     * @throws FactoryException if an error occurred while creating a CRS or the operation.
     */
    @Test
    public void testAreaOfInterest() throws FactoryException {
        final CoordinateReferenceSystem  source = crsFactory.createCoordinateReferenceSystem("4267");
        final CoordinateReferenceSystem  target = crsFactory.createCoordinateReferenceSystem("4326");
        final CoordinateOperationContext context = new CoordinateOperationContext();
        /*
         * Fetch an operation over USA. Operation domain of validity
         * should be "USA - CONUS including EEZ" with 10 m accuracy.
         * But we do not test exact accuracy value since it depends
         * on whether datum shift grids are available.
         */
        context.setAreaOfInterest(-120, -75, 25, 42);
        OperationFactory factory = new OperationFactory(context);
        CoordinateOperation operation = factory.createOperation(source, target);
        GeographicBoundingBox bbox = bbox(operation.getDomainOfValidity());
        assertTrue(bbox.getWestBoundLongitude() <= -124.79);
        assertTrue(bbox.getEastBoundLongitude() >=  -66.91);
        assertTrue(bbox.getSouthBoundLatitude() <=   24.41);
        assertTrue(bbox.getNorthBoundLatitude() >=   49.38);
        assertNotNull(accuracy(operation.getCoordinateOperationAccuracy()));
        /*
         * Fetch an operation over Canada. Operation domain of validity
         * should be "Canada - NAD27" with 20 m accuracy.
         */
        context.setAreaOfInterest(-120, -75, 45, 55);
        factory = new OperationFactory(context);
        operation = factory.createOperation(source, target);
        bbox = bbox(operation.getDomainOfValidity());
        assertTrue(bbox.getWestBoundLongitude() <= -141.01);
        assertTrue(bbox.getEastBoundLongitude() >=  -47.74);
        assertTrue(bbox.getSouthBoundLatitude() <=   40.04);
        assertTrue(bbox.getNorthBoundLatitude() >=   83.17);
        assertNotNull(accuracy(operation.getCoordinateOperationAccuracy()));
    }

    /**
     * Returns the first geographic bounding box found in the given extent.
     *
     * @param  extent  the extent for which to get the geographic bounding box.
     * @return the first geographic bounding box.
     */
    private static GeographicBoundingBox bbox(final Extent extent) {
        if (extent != null) {
            for (final GeographicExtent ge : extent.getGeographicElements()) {
                if (ge instanceof GeographicBoundingBox) {
                    return (GeographicBoundingBox) ge;
                }
            }
        }
        fail("No geographic bounding box found.");
        return null;
    }

    /**
     * Returns the first accuracy description found in the given collection.
     * Note: this implementation searches for a result of type {@link ConformanceResult}
     * because PROJ provides the result as a character string. But other implementations
     * may provide {@link org.opengis.metadata.quality.QuantitativeResult} instead.
     *
     * @param  accuracy  the accuracies.
     * @return the first accuracy description.
     */
    private static String accuracy(final Collection<PositionalAccuracy> accuracy) {
        if (accuracy != null) {
            for (final PositionalAccuracy element : accuracy) {
                for (final Result result : element.getResults()) {
                    if (result instanceof ConformanceResult) {
                        return ((ConformanceResult) result).getExplanation().toString();
                    }
                    /*
                     * If we want to accept other GeoAPI implementations, we should
                     * also check `if (result instanceof QuantitativeResult)` here.
                     */
                }
            }
        }
        fail("No accuracy found.");
        return null;
    }
}
