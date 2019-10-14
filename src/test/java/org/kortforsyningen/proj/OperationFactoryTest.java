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
        crsFactory = new AuthorityFactory.API("EPSG");
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
        assertTrue(operation.toWKT().startsWith("CONVERSION[\"World Mercator\","));
        assertSame("sourceCRS", source, operation.getSourceCRS());
        assertSame("targetCRS", target, operation.getTargetCRS());
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
         * should be "USA - CONUS including EEZ".
         */
        context.setAreaOfInterest(-120, -75, 25, 42);
        OperationFactory factory = new OperationFactory(context);
        CoordinateOperation operation = factory.createOperation(source, target);
        // TODO: test AREA. We expect "USA - CONUS including EEZ"
        /*
         * Fetch an operation over Canada. Operation domain of validity
         * should be "Canada - NAD27".
         */
        context.setAreaOfInterest(-120, -75, 45, 55);
        factory = new OperationFactory(context);
        operation = factory.createOperation(source, target);
        // TODO: test AREA. We expect "Canada - NAD27".
    }
}
