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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
     * The maximum number of {@link Transform} instances to cache. This maximum should be the expected
     * maximum number of threads (or the "optimal" number of threads) using the same {@link Operation}
     * concurrently. A low value does not necessarily block more threads from using {@code Operation},
     * but the extra threads may observe a performance degradation.
     */
    private static final int NUM_THREADS;
    static {
        Integer n = null;
        try {
            /*
             * The AccessController is used for reading the property value in a security constrained environment.
             * It has no effect on the common case where no security manager is enforced. We must promise to not
             * execute any user-supplied parameter in the privileged block.
             */
            n = AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
                    Integer.getInteger("org.kortforsyningen.proj.maxThreadsPerTransform"));
        } catch (SecurityException e) {
            /*
             * If we do not have the authorization to read the property value, this is not a big issue.
             * We can work with the default value.
             */
            NativeResource.logger().log(System.Logger.Level.DEBUG, e.getLocalizedMessage(), e);
        }
        /*
         * The default value below (4) is arbitrary. If that default value is modified,
         * then the documentation in package-info.java file should be updated accordingly.
         */
        NUM_THREADS = (n != null) ? Math.max(1, Math.min(100, n)) : 4;
    }

    /**
     * The source and target coordinate reference systems, or {@code null} if unspecified.
     */
    private CRS sourceCRS, targetCRS;

    /**
     * The objects which will perform the actual coordinate operations.
     * Each {@code Transform} instance can be used by only one thread at a time.
     * We cache the {@code Transform} instances after use so they can be reused
     * by the same thread or another thread. We put an arbitrary limit on the number
     * of instances to cache, but this will not limit the number of concurrent threads
     * doing transformations. It only means that the additional threads will go through
     * the most costly process of creating new {@link Transform} instances.
     */
    private final Queue<Transform> transforms = new ArrayBlockingQueue<>(NUM_THREADS);

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
     * Returns a {@code PJ} wrapper, creating a new one if none exist in the cache.
     * The returned wrapper shall be used in a single thread.
     * The {@link #release(Transform)} method must be invoked after usage,
     * even on failure.
     *
     * @param  c  the current thread context.
     * @return the {@code PJ} wrapper for the current thread.
     * @throws TransformException if the {@code PJ} object can not be created.
     */
    private Transform acquire(final Context c) throws TransformException {
        Transform tr = transforms.poll();
        if (tr == null) {
            tr = new Transform(impl, c);
        } else {
            tr.assign(c);
        }
        return tr;
    }

    /**
     * Releases the {@code PJ} wrapper, or destroys it if the cache is full.
     *
     * @param  tr  wrapper of the {@code PJ} to cache for reuse or to destroy.
     */
    private void release(final Transform tr) {
        if (transforms.offer(tr)) {
            tr.assign(null);
        } else {
            tr.destroy();
        }
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
            final Transform tr = acquire(c);
            try {
                tr.transform(ordinates.length, ordinates, 0, 1);
            } finally {
                release(tr);
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
        try (Context c = Context.acquire()) {
            final Transform tr = acquire(c);
            try {
                tr.transform(tgtDim, dstPts, dstOff, numPts);
            } finally {
                release(tr);
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
