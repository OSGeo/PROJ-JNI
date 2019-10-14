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
import java.util.List;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.util.FactoryException;


/**
 * Creates coordinate operations from a pair of CRS, optionally with some contextual information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class OperationFactory implements CoordinateOperationFactory {
    /**
     * The context in which coordinate operations are to be used.
     */
    private final CoordinateOperationContext context;

    /**
     * Creates a new operation factory.
     *
     * @param  context in which coordinate operations are to be used, or {@code null} for the default.
     */
    OperationFactory(final CoordinateOperationContext context) {
        if (context == null) {
            this.context = new CoordinateOperationContext();
        } else {
            this.context = context.clone();
        }
    }

    /**
     * Returns the project responsible for creating this factory implementation, which is "PROJ".
     * {@link Citation#getEdition()} contains the PROJ version string.
     *
     * @return a citation for "PROJ".
     */
    @Override
    public Citation getVendor() {
        return SimpleCitation.PROJ();
    }

    /**
     * Returns an error message for a coordinate operation not found between the given pair of CRS.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return message to give to {@link OperationNotFoundException} constructor.
     */
    static String notFound(final CRS sourceCRS, final CRS targetCRS) {
        return "Can not find a coordinate operation from “" + sourceCRS.label() + "” to “" + targetCRS.label() + "”.";
    }

    /**
     * Returns the ordinal value of the given enumeration, or -1 if the enumeration is for the default value.
     *
     * @param  e  the enumeration for which to get the ordinal value.
     * @return the ordinal value, or -1 for the PROJ default value.
     */
    private static int ordinal(final Enum<?> e) {
        return (e == null) || CoordinateOperationContext.DEFAULT.equals(e.name()) ? -1 : e.ordinal();
    }

    /**
     * Returns operations for conversion or transformation between two coordinate reference systems,
     * taking in account the given context. If no coordinate operation is found, then this method
     * returns an empty list.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  context    context in which the coordinate operation is to be used.
     * @return coordinate operations from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException if the operation creation failed.
     */
    static List<CoordinateOperation> findOperations(final CRS sourceCRS, final CRS targetCRS,
            final CoordinateOperationContext context) throws FactoryException
    {
        final String  authority                   = context.getAuthority();
        final double  desiredAccuracy             = context.getDesiredAccuracy();
        final int     sourceAndTargetCRSExtentUse = ordinal(context.getSourceAndTargetCRSExtentUse());
        final int     spatialCriterion            = ordinal(context.getSpatialCriterion());
        final int     gridAvailabilityUse         = ordinal(context.getGridAvailabilityUse());
        final int     allowUseIntermediateCRS     = ordinal(context.getAllowUseIntermediateCRS());
        final boolean discardSuperseded           = context.getDiscardSuperseded();
        final Extent  extent                      = context.getAreaOfInterest();
        /*
         * ISO 19115 allows the extent to be specified in many way (it can be a polygon for instance),
         * but current version supports only geographic bounding boxes. The latitudes and longitudes
         * are on an unspecified ellipsoid; the exact datum does not matter since this information is
         * only approximate.
         */
        double westBoundLongitude = Double.POSITIVE_INFINITY;
        double southBoundLatitude = Double.POSITIVE_INFINITY;
        double eastBoundLongitude = Double.NEGATIVE_INFINITY;
        double northBoundLatitude = Double.NEGATIVE_INFINITY;
        if (extent != null) {
            for (final GeographicExtent ge : extent.getGeographicElements()) {
                if (ge instanceof GeographicBoundingBox) {
                    final GeographicBoundingBox bbox = (GeographicBoundingBox) ge;
                    double v;
                    v = bbox.getWestBoundLongitude(); if (v < westBoundLongitude) westBoundLongitude = v;
                    v = bbox.getEastBoundLongitude(); if (v > eastBoundLongitude) eastBoundLongitude = v;
                    v = bbox.getSouthBoundLatitude(); if (v < southBoundLatitude) southBoundLatitude = v;
                    v = bbox.getNorthBoundLatitude(); if (v > northBoundLatitude) northBoundLatitude = v;
                }
            }
        }
        final Operation result;
        try (Context c = Context.acquire()) {
            result = c.factory(authority).createOperation(
                        sourceCRS.impl,     targetCRS.impl,
                        westBoundLongitude, eastBoundLongitude,
                        southBoundLatitude, northBoundLatitude,
                        desiredAccuracy,
                        sourceAndTargetCRSExtentUse, spatialCriterion, gridAvailabilityUse, allowUseIntermediateCRS,
                        discardSuperseded);
            result.setCRSs(sourceCRS, targetCRS);
        }
        return java.util.Collections.singletonList(result);
    }

    /**
     * Returns an operation for conversion or transformation between two coordinate reference systems.
     * If more than one operation exists, the first one is returned.
     * If no operation exists, then an {@link OperationNotFoundException} is thrown.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code source} to {@code target} CRS.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS) throws FactoryException
    {
        final CRS source = CRS.cast("sourceCRS", sourceCRS);
        final CRS target = CRS.cast("targetCRS", targetCRS);
        final List<CoordinateOperation> operations = findOperations(source, target, context);
        if (operations.isEmpty()) {
            throw new OperationNotFoundException(notFound(source, target));
        }
        return operations.get(0);
    }

    /**
     * Returns an operation using a particular method for conversion or transformation
     * between two coordinate reference systems. The current implementation ignores the
     * given method.
     *
     * @param  sourceCRS  input coordinate reference system.
     * @param  targetCRS  output coordinate reference system.
     * @param  method     currently ignored.
     * @return a coordinate operation from {@code sourceCRS} to {@code targetCRS}.
     * @throws OperationNotFoundException if no operation path was found from {@code source} to {@code target} CRS.
     * @throws FactoryException if the operation creation failed for some other reason.
     */
    @Override
    public CoordinateOperation createOperation(final CoordinateReferenceSystem sourceCRS,
                                               final CoordinateReferenceSystem targetCRS,
                                               final OperationMethod method) throws FactoryException
    {
        return createOperation(sourceCRS, targetCRS);
    }

    /**
     * Creates a concatenated operation from a sequence of operations.
     *
     * @param  properties  name and other properties to give to the new object.
     * @param  operations  the sequence of operations.
     * @return the concatenated operation.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public CoordinateOperation createConcatenatedOperation(final Map<String,?> properties,
            final CoordinateOperation... operations) throws FactoryException
    {
        throw new FactoryException("Not supported yet.");
    }

    /**
     * Creates a defining conversion with no source and target CRS and no math transform.
     * The purpose of defining conversions is to be given as an argument to derived CRS
     * and projected CRS constructors.
     *
     * @param  properties  set of properties. Shall contains at least {@code "name"}.
     * @param  method      the operation method.
     * @param  parameters  the parameter values.
     * @return the defining conversion.
     * @throws FactoryException if the object creation failed.
     */
    @Override
    public Conversion createDefiningConversion(final Map<String,?> properties,
            final OperationMethod method, final ParameterValueGroup parameters) throws FactoryException
    {
        throw new FactoryException("Not supported yet.");
    }
}
