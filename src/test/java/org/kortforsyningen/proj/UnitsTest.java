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

import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Time;
import javax.measure.quantity.Dimensionless;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Units} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public final strictfp class UnitsTest {
    /**
     * Verifies the {@link Units} constants that are system units.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSystemUnits() {
        final Unit<?>[]  units      = {Units.METRE,  Units.SECOND, Units.RADIAN, Units.SCALE_UNITY};
        final Class<?>[] quantities = {Length.class, Time.class,   Angle.class,  Dimensionless.class};
        for (int i=0; i<units.length; i++) {
            final UnitOfMeasure<?> unit = (UnitOfMeasure<?>) units[i];
            final Class<?> q = quantities[i];
            assertEquals ("toSI",          1,    unit.toSI, 0);
            assertEquals ("type",          q,    unit.type);
            assertSame   ("asType",        unit, unit.asType(q.asSubclass(Quantity.class)));
            assertSame   ("getSystemUnit", unit, unit.getSystemUnit());
            assertNotNull("getName",             unit.getName());
        }
    }

    /**
     * Verifies the {@link Units} constants that are derived from system units with a scale factor.
     */
    @Test
    public void testScaledUnits() {
        final Unit<?>[] units = {
            Units.PARTS_PER_MILLION,
            Units.MICRORADIAN,
            Units.DEGREE,
            Units.ARC_SECOND,
            Units.GRAD,
            Units.YEAR
        };
        final Unit<?>[] base = {
            Units.SCALE_UNITY,
            Units.RADIAN,
            Units.RADIAN,
            Units.RADIAN,
            Units.RADIAN,
            Units.SECOND
        };
        for (int i=0; i<units.length; i++) {
            final UnitOfMeasure<?> unit = (UnitOfMeasure<?>) units[i];
            assertFalse  ("toSI",          unit.toSI == 1);
            assertSame   ("getSystemUnit", base[i], unit.getSystemUnit());
            assertNotNull("getName",       unit.getName());
        }
    }
}
