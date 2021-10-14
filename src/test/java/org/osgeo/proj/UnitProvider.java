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

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import javax.measure.Unit;
import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.quantity.Time;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Pressure;
import javax.measure.spi.QuantityFactory;
import javax.measure.spi.ServiceProvider;
import javax.measure.spi.SystemOfUnits;
import javax.measure.spi.SystemOfUnitsService;
import javax.measure.spi.UnitFormatService;


/**
 * Services provider for units of measurement, for testing purpose only.
 * This is required for running GeoAPI tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   2.0
 */
public final class UnitProvider extends ServiceProvider implements SystemOfUnitsService, SystemOfUnits {
    /**
     * Creates a new provider of units of measurements.
     */
    public UnitProvider() {
    }

    /**
     * Returns a name for the system of units provided by this class.
     * The name shall be different than {@code "SI"} for preventing
     * {@link Units} to select this provider.
     *
     * @return an arbitrary name different than {@code "SI"}.
     */
    @Override
    public String getName() {
        return "Proj fallback";
    }

    /**
     * Returns the default unit for the specified quantity or {@code null} if none.
     *
     * @param <Q>   the compile-time quantity type.
     * @param type  the quantity type.
     * @return the unit for the specified quantity.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <Q extends Quantity<Q>> Unit<Q> getUnit(final Class<Q> type) {
        if (Length       .class.equals(type)) return (Unit<Q>) Units.METRE;
        if (Angle        .class.equals(type)) return (Unit<Q>) Units.RADIAN;
        if (Time         .class.equals(type)) return (Unit<Q>) Units.SECOND;
        if (Dimensionless.class.equals(type)) return (Unit<Q>) Units.SCALE_UNITY;
        if (Pressure     .class.equals(type)) return (Unit<Q>) new UnitOfMeasure<>(Pressure.class, 1);
        return null;
    }

    /**
     * Returns the units explicitly defined by this system.
     *
     * @return all predefined units.
     */
    @Override
    public Set<Unit<?>> getUnits() {
        return Units.predefined();
    }

    /**
     * Returns the units defined in this system having the specified dimension (convenience method).
     *
     * @param  dim  the dimension of the units to be returned.
     * @return the units of specified dimension.
     */
    @Override
    public Set<Unit<?>> getUnits(final Dimension dim) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the only system of units services provided by this implementation.
     *
     * @return {@code this}.
     */
    @Override
    public SystemOfUnits getSystemOfUnits() {
        return this;
    }

    /**
     * Returns the system of units having the specified name or {@code null} if none.
     *
     * @param  name the system of unit name.
     * @return the system of units for the given name.
     */
    @Override
    public SystemOfUnits getSystemOfUnits(final String name) {
        return getName().equals(name) ? this : null;
    }

    /**
     * Returns the only system of units services provided by this implementation.
     *
     * @return {@code this} as a singleton.
     */
    @Override
    public Collection<SystemOfUnits> getAvailableSystemsOfUnits() {
        return Collections.singleton(this);
    }

    /**
     * Returns the only system of units services provided by this implementation.
     *
     * @return {@code this}.
     */
    @Override
    public SystemOfUnitsService getSystemOfUnitsService() {
        return this;
    }

    /**
     * Unsupported in this implementation.
     *
     * @return {@code null}.
     */
    @Override
    public UnitFormatService getUnitFormatService() {
        return null;
    }

    /**
     * Unsupported in this implementation.
     *
     * @return {@code null}.
     */
    @Override
    public <Q extends Quantity<Q>> QuantityFactory<Q> getQuantityFactory(Class<Q> type) {
        return null;
    }
}
