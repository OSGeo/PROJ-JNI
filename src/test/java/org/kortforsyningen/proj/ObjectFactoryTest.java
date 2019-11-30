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

import java.util.Map;
import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ObjectFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class ObjectFactoryTest {
    /**
     * The factory to use for testing.
     */
    private final ObjectFactory factory;

    /**
     * Creates a new test case.
     */
    public ObjectFactoryTest() {
        factory = ObjectFactory.INSTANCE;
    }

    /**
     * Tests {@link ObjectFactory#createCoordinateSystemAxis(Map, String, AxisDirection, Unit)}.
     *
     * @throws FactoryException if creation of the coordinate system axis failed.
     */
    @Test
    public void testCoordinateSystemAxis() throws FactoryException {
        CoordinateSystemAxis axis = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My axis"),
                "x", AxisDirection.NORTH_EAST, Units.METRE);

        assertEquals("My axis", axis.getName().getCode());
        assertEquals(AxisDirection.NORTH_EAST, axis.getDirection());
        assertSame  (Units.METRE, axis.getUnit());
    }

    /**
     * Tests {@link ObjectFactory#createCartesianCS(Map, CoordinateSystemAxis, CoordinateSystemAxis)}.
     *
     * @throws FactoryException if creation of the coordinate system axis failed.
     */
    @Test
    public void testCartesianCS() throws FactoryException {
        CoordinateSystemAxis axis0 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My axis"),
                "x", AxisDirection.WEST, Units.METRE);

        CoordinateSystemAxis axis1 = factory.createCoordinateSystemAxis(
                Map.of(CoordinateSystemAxis.NAME_KEY, "My other axis"),
                "y", AxisDirection.SOUTH, Units.METRE);

        CartesianCS cs = factory.createCartesianCS(
                Map.of(CartesianCS.NAME_KEY, "My CS"),
                axis0, axis1);

        assertEquals("My CS", cs.getName().getCode());
        assertEquals("dimension", 2, cs.getDimension());
        assertSame(axis0, cs.getAxis(0));
        assertSame(axis1, cs.getAxis(1));
    }
}
