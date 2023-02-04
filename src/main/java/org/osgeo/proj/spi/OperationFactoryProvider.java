/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021-2023 Open Source Geospatial Foundation
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
package org.osgeo.proj.spi;

import java.util.Map;
import org.osgeo.proj.Proj;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;


/**
 * Provider for {@link CoordinateOperationFactory}.
 * This is a temporary class, to be removed after use Jigsaw modularisation
 * in which case the public static {@code provider()} method should be sufficient.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 *
 * @see <a href="https://github.com/OSGeo/PROJ-JNI/issues/15">Issue #15</a>
 */
public final class OperationFactoryProvider implements CoordinateOperationFactory {
    /**
     * The implementation where to delegate all operations.
     */
    private final CoordinateOperationFactory impl;

    /**
     * Creates a new provider.
     */
    public OperationFactoryProvider() {
        impl = Proj.getOperationFactory(null);
    }

    @Override
    public Citation getVendor() {
        return impl.getVendor();
    }

    @Override
    public CoordinateOperation createOperation(CoordinateReferenceSystem crs, CoordinateReferenceSystem crs1) throws FactoryException {
        return impl.createOperation(crs, crs1);
    }

    @Override
    public CoordinateOperation createOperation(CoordinateReferenceSystem crs, CoordinateReferenceSystem crs1, OperationMethod om) throws FactoryException {
        return impl.createOperation(crs, crs1, om);
    }

    @Override
    public CoordinateOperation createConcatenatedOperation(Map<String, ?> map, CoordinateOperation... cos) throws FactoryException {
        return impl.createConcatenatedOperation(map, cos);
    }

    @Override
    public Conversion createDefiningConversion(Map<String, ?> map, OperationMethod om, ParameterValueGroup pvg) throws FactoryException {
        return impl.createDefiningConversion(map, om, pvg);
    }
}
