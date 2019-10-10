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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;


/**
 * Wrappers around {@code osgeo::proj::operation::CoordinateOperation} subtypes.
 * Each subtype is represented by an inner class in this file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class Operation extends IdentifiableObject implements CoordinateOperation, MathTransform {
    /**
     * The source and target coordinate reference systems, or {@code null} if unspecified.
     */
    private CRS sourceCRS, targetCRS;

    /**
     * Creates a new wrapper for the given {@code osgeo::proj::operation::CoordinateOperation}.
     * The source and target CRS needs to be specified after construction.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    Operation(final long ptr) {
        super(ptr);
    }

    /**
     * Sets the source and target CRS. This is invoked after construction when
     * the coordinate operation has been created from a pair of Java CRS objects.
     *
     * @param  source  the source CRS.
     * @param  target  the target CRS.
     *
     * @todo Compare the pointers against the ones returned from C++ API
     *       and create new CRS objects if the pointers do not match.
     */
    final void setCRSs(final CRS source, final CRS target) {
        sourceCRS = source;
        targetCRS = target;
    }

    /**
     * Returns the source CRS. Mandatory for {@linkplain Transformation transformations} only.
     *
     * @return the source CRS, or {@code null} if not available.
     */
    @Override
    public final CoordinateReferenceSystem getSourceCRS() {
        return sourceCRS;
    }

    /**
     * Gets the dimension of input points.
     *
     * @return the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        return 2;   // TODO
    }

    /**
     * Returns the target CRS. Mandatory for {@linkplain Transformation transformations} only.
     *
     * @return the target CRS, or {@code null} if not available.
     */
    @Override
    public final CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    /**
     * Gets the dimension of output points.
     *
     * @return the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        return 2;   // TODO
    }

    /**
     * Version of the instantiation due to the stochastic nature of the parameters.
     *
     * @return the coordinate operation version, or {@code null} in none.
     */
    @Override
    public String getOperationVersion() {
        return null;
    }

    /**
     * Estimate(s) of the impact of this operation on point accuracy.
     *
     * @return the position error estimates, or an empty collection if not available.
     */
    @Override
    public Collection<PositionalAccuracy> getCoordinateOperationAccuracy() {
        return Collections.emptySet();
    }

    /**
     * Gets the object which will perform the actual coordinate operation.
     * Current wrapper implements the {@code MathTransform} interface in the same class,
     * but a future version may dissociate the objects if useful.
     *
     * @return the transform from source to target CRS.
     */
    @Override
    public MathTransform getMathTransform() {
        return this;
    }

    /**
     * Returns {@code true} if this transform is the identity transform. Note that a value of
     * {@code false} does not mean that the transform is not an identity transform, since this
     * case is a bit difficult to determine from PROJ API.
     */
    @Override
    public boolean isIdentity() {
        return false;                   // TODO
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result in {@code ptDst}.
     * If {@code ptDst} is {@code null}, a new {@link DirectPosition} object is allocated
     * and then the result of the transformation is stored in this object. In either case,
     * {@code ptDst}, which contains the transformed point, is returned for convenience.
     * If {@code ptSrc} and {@code ptDst} are the same object,
     * the input point is correctly overwritten with the transformed point.
     *
     * @param  ptSrc the specified coordinate point to be transformed.
     * @param  ptDst the specified coordinate point that stores the result of transforming {@code ptSrc}, or {@code null}.
     * @return the coordinate point after transforming {@code ptSrc} and storing the result.
     * @throws MismatchedDimensionException if {@code ptSrc} or {@code ptDst} does not have the expected dimension.
     * @throws TransformException if the point can not be transformed.
     */
    @Override
    public DirectPosition transform(final DirectPosition ptSrc, DirectPosition ptDst) throws TransformException {
        final int srcDim = getSourceDimensions();
        final int tgtDim = getTargetDimensions();
        if (ptSrc.getDimension() != srcDim) {
            throw new MismatchedDimensionException();
        }
        double[] ordinates = new double[Math.max(srcDim, tgtDim)];
        for (int i=0; i<srcDim; i++) {
            ordinates[i] = ptSrc.getOrdinate(i);
        }
        try (Context c = Context.acquire()) {
            final Transform tr = new Transform(impl, c);        // TODO: should use cached instance.
            try {
                tr.transform(ordinates.length, ordinates, 0, 1);
            } finally {
                tr.destroy();
            }
        }
        if (ptDst != null) {
            if (ptDst.getDimension() != tgtDim) {
                throw new MismatchedDimensionException();
            }
            for (int i=0; i<tgtDim; i++) {
                ptDst.setOrdinate(i, ordinates[i]);
            }
        } else {
            if (ordinates.length != tgtDim) {
                ordinates = Arrays.copyOf(ordinates, tgtDim);
            }
            ptDst = new SimpleDirectPosition(targetCRS, ordinates);
        }
        return ptDst;
    }

    /**
     * Transforms an array of coordinate tuples.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same than {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff,
                          final int numPts) throws TransformException
    {
        final int srcDim = getSourceDimensions();
        final int tgtDim = getTargetDimensions();
        if (srcDim == tgtDim) {
            if (srcPts != dstPts || srcOff != dstOff) {
                final int length = tgtDim * numPts;
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, length);
            }
        } else {
            // TODO: need special check for overlapping arrays.
            throw new TransformException("Transformation between CRS of different dimensions not yet supported.");
        }
        /*
         * TODO: Creation of Transform object is potentially expensive. Should cache.
         */
        try (Context c = Context.acquire()) {
            final Transform tr = new Transform(impl, c);
            try {
                tr.transform(tgtDim, dstPts, dstOff, numPts);
            } finally {
                tr.destroy();
            }
        }
    }

    /**
     * Copies the {@code float} arrays to {@code double} arrays, then transforms the coordinate tuples.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same than {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final float[] srcPts, int srcOff,
                          final float[] dstPts, int dstOff,
                          int numPts) throws TransformException
    {
        throw new TransformException("Not supported yet.");
    }

    /**
     * Copies the {@code float} arrays to {@code double} arrays, then transforms the coordinate tuples.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final float[]  srcPts, int srcOff,
                          final double[] dstPts, int dstOff,
                          final int numPts) throws TransformException
    {
        throw new TransformException("Not supported yet.");
    }

    /**
     * Copies the {@code float} arrays to {@code double} arrays, then transforms the coordinate tuples.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float[]  dstPts, int dstOff,
                          final int numPts) throws TransformException
    {
        throw new TransformException("Not supported yet.");
    }

    /**
     * The PROJ library does not provide derivative functions.
     *
     * @return the derivative at the given position.
     * @throws TransformException if the derivative can not be computed.
     */
    @Override
    public Matrix derivative(final DirectPosition point) throws TransformException {
        throw new TransformException(NativeResource.UNSUPPORTED);
    }

    /**
     * Returns the inverse transform.
     *
     * @return the inverse transform.
     * @throws NoninvertibleTransformException if the inverse transform can not be computed.
     */
    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        throw new NoninvertibleTransformException("Not supported yet.");
    }
}
