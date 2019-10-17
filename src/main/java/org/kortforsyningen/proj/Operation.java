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

import java.util.Set;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formattable;
import java.util.Formatter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.GeneralDerivedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.Formula;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.OperationMethod;
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
                    Integer.getInteger("org.kortforsyningen.proj.maxThreadsPerInstance"));
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
         * The maximum is also arbitrary; we need to put a relatively low maximum because
         * the simple algorithm used for the `transforms` array does not scale to a large
         * number of entries. It should not be necessary to allow high numbers because it
         * is only the maximum number of threads per Operation instance, not a global maximum.
         */
        NUM_THREADS = (n != null) ? Math.max(1, Math.min(16, n)) : 4;
    }

    /**
     * The dimensions of source and target coordinate reference systems, or 0 if unknown.
     */
    private final int srcDim, dstDim;

    /**
     * The inverse transform, created only when first needed.
     *
     * @see #inverse()
     */
    private transient Operation inverse;

    /**
     * The objects which will perform the actual coordinate operations.
     * Each {@code Transform} instance can be used by only one thread at a time.
     * We cache the {@code Transform} instances after use so they can be reused
     * by the same thread or another thread.
     *
     * <p>The array length is an arbitrary limit on the number of instances to cache,
     * but this will not limit the number of concurrent threads doing transformations.
     * It only means that the additional threads will go through the most costly process
     * of creating new {@link Transform} instances.</p>
     *
     * <p><b>Design note:</b> the use of {@link java.util.concurrent.ArrayBlockingQueue}
     * would be more efficient, but it is also a relatively heavy class for this simple need.
     * We use an array for now, with the requirement that all accesses to this array must be
     * synchronized of {@code transforms}.</p>
     */
    private final Transform[] transforms;

    /**
     * Task executed when the enclosing {@link Operation} is garbage collected.
     * This task destroys all {@link Transform} cached by the enclosing class.
     *
     * <b>Reminder:</b> this class shall not contain any reference to {@link Operation}.
     */
    private static final class Cleaner extends SharedPointer {
        /**
         * A copy of the {@link Operation#transforms} reference.
         * They are the references to {@code PJ} objects to destroy.
         */
        private final Transform[] transforms;

        /**
         * Wraps the shared pointer at the given address.
         * A null pointer is assumed caused by a failure to allocate memory from C/C++ code.
         *
         * @param  ptr  pointer to the {@code std::shared_ptr}, or 0 if out of memory.
         * @throws OutOfMemoryError if {@code ptr} is 0.
         */
        Cleaner(final long ptr) {
            super(ptr);
            transforms = new Transform[NUM_THREADS];
        }

        /**
         * Invoked by the cleaner thread when the {@link Operation} has been garbage collected.
         * This method destroy all @code PJ} objects, then the PROJ {@code CoordinateOperation}.
         */
        @Override
        final void release() {
            /*
             * Synchronization should not be needed since the array should not be used anymore.
             * But we still want the memory barrier effect, and the synchronization is a safety.
             */
            synchronized (transforms) {
                for (int i=transforms.length; --i >= 0;) {
                    final Transform tr = transforms[i];
                    if (tr != null) {
                        transforms[i] = null;       // Theoretically not needed but done as a safety.
                        tr.destroy();
                    }
                }
            }
            super.release();
        }
    }

    /**
     * Creates a new wrapper for the given {@code osgeo::proj::operation::CoordinateOperation}.
     * The source and target CRS needs to be specified after construction.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    Operation(final long ptr) {
        super(new Cleaner(ptr));
        transforms = ((Cleaner) impl).transforms;
        srcDim = getDimension(0);
        dstDim = getDimension(1);
    }

    /**
     * Returns the number of dimension of the specified CRS, or 0 if unknown.
     *
     * @param  i  0 for source CRS, or 1 for target CRS.
     * @return number of dimension of specified CRS, or 0 in unknown.
     */
    private int getDimension(final int i) {
        final CRS crs = getCRS(i);
        return (crs != null) ? crs.getDimension() : 0;
    }

    /**
     * Returns the source or target CRS, or {@code null} if unknown.
     *
     * @param  i  0 for source CRS, or 1 for target CRS.
     * @return the specified CRS, or {@code null} in unknown.
     */
    private CRS getCRS(final int i) {
        return (CRS) impl.getVectorElement(Property.SOURCE_TARGET_CRS, i);
    }

    /**
     * Returns the source CRS. Mandatory for {@linkplain Transformation transformations} only.
     *
     * @return the source CRS, or {@code null} if not available.
     */
    @Override
    public final CoordinateReferenceSystem getSourceCRS() {
        return getCRS(0);
    }

    /**
     * Gets the dimension of input points.
     *
     * @return the dimension of input points.
     */
    @Override
    public final int getSourceDimensions() {
        assert dimensionMatches(0, srcDim) : srcDim;
        return srcDim;
    }

    /**
     * Returns the target CRS. Mandatory for {@linkplain Transformation transformations} only.
     *
     * @return the target CRS, or {@code null} if not available.
     */
    @Override
    public final CoordinateReferenceSystem getTargetCRS() {
        return getCRS(1);
    }

    /**
     * Gets the dimension of output points.
     *
     * @return the dimension of output points.
     */
    @Override
    public final int getTargetDimensions() {
        assert dimensionMatches(1, dstDim) : dstDim;
        return dstDim;
    }

    /**
     * Verifies if the number of dimension of source or target CRS matches the expected value.
     * This is used for assertions only.
     *
     * @param  i    0 for source CRS, or 1 for target CRS.
     * @param  dim  the expected number of dimensions.
     * @return whether the dimension matches.
     */
    private boolean dimensionMatches(final int i, final int dim) {
        final CRS crs = getCRS(i);
        return (crs == null) || (dim == crs.getDimension());
    }

    /**
     * Version of the instantiation due to the stochastic nature of the parameters.
     *
     * @return the coordinate operation version, or {@code null} in none.
     */
    @Override
    public String getOperationVersion() {
        return impl.getStringProperty(Property.OPERATION_VERSION);
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
     * Returns the operation method.
     * This method is defined in {@link org.opengis.referencing.operation.SingleOperation}.
     *
     * @return the operation method.
     */
    @SuppressWarnings("OverlyStrongTypeCast")       // Casting to final class is easier for the JVM.
    public OperationMethod getMethod() {
        return (Method) impl.getObjectProperty(Property.OPERATION_METHOD);
    }

    /**
     * Definition of an algorithm used to perform a coordinate operation.
     */
    static final class Method extends IdentifiableObject implements OperationMethod {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param  ptr  pointer to the wrapped PROJ object.
         */
        Method(final long ptr) {
            super(ptr);
        }

        /** Removed from ISO 19111:2019. */ @Override public Integer getSourceDimensions() {return null;}
        /** Removed from ISO 19111:2019. */ @Override public Integer getTargetDimensions() {return null;}

        /**
         * Formula(s) or procedure used by this operation method.
         * This may be a reference to a publication.
         *
         * @return the formula used by this method.
         */
        @Override
        public Formula getFormula() {
            return new Formula() {
                @Override public InternationalString getFormula() {
                    return getProperty(Property.FORMULA);
                }

                @Override public Citation getCitation() {
                    final String text = impl.getStringProperty(Property.FORMULA_TITLE);
                    return (text != null) ? new SimpleCitation(text) : null;
                }

                @Override public String toString() {
                    String text = impl.getStringProperty(Property.FORMULA_TITLE);
                    if (text == null) {
                        text = impl.getStringProperty(Property.FORMULA);
                        if (text == null) {
                            text = "Not specified";
                        }
                    }
                    return text;
                }
            };
        }

        /**
         * Returns a description of the parameter values.
         *
         * @return a description of the parameter values.
         */
        @Override
        public ParameterDescriptorGroup getParameters() {
            return null;    // TODO
        }
    }

    /**
     * Returns the parameter values.
     * This method is defined in {@link org.opengis.referencing.operation.SingleOperation}.
     *
     * @return the parameter values.
     */
    public ParameterValueGroup getParameterValues() {
        return null;    // TODO
    }

    /**
     * A specialization of coordinate operation when there is no datum change.
     * In such case, there is no accuracy lost expected (ignoring rounding errors).
     */
    static final class Conversion extends Operation implements org.opengis.referencing.operation.Conversion {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param  ptr  pointer to the wrapped PROJ object.
         */
        Conversion(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A specialization of coordinate operation when there is datum change.
     */
    static final class Transformation extends Operation implements org.opengis.referencing.operation.Transformation {
        /**
         * Invoked by {@link AuthorityFactory#wrapGeodeticObject} only.
         * @param  ptr  pointer to the wrapped PROJ object.
         */
        Transformation(final long ptr) {
            super(ptr);
        }
    }

    /**
     * A specialization of coordinate operation used in projected CRS.
     * This type is specific to GeoAPI and does not exist in ISO 19111.
     */
    static final class Projection implements org.opengis.referencing.operation.Projection, Formattable {
        /**
         * The projected CRS for which we are creating this projection.
         */
        private final CRS.Projected crs;

        /**
         * The "base to projected CRS" conversion.
         */
        private final Conversion op;

        /**
         * Wraps the given conversion in a projection object.
         *
         * @param op   the "base to projected CRS" conversion.
         * @param crs  the projected CRS for which we are creating this projection.
         */
        Projection(final CRS.Projected crs, final Conversion op) {
            this.crs = crs;
            this.op  = op;
        }

        @Override public ReferenceIdentifier            getName()                        {return op.getName();}
        @Override public Collection<GenericName>        getAlias()                       {return op.getAlias();}
        @Override public Set<ReferenceIdentifier>       getIdentifiers()                 {return op.getIdentifiers();}
        @Override public InternationalString            getRemarks()                     {return op.getRemarks();}
        @Override public String                         getOperationVersion()            {return op.getOperationVersion();}
        @Override public OperationMethod                getMethod()                      {return op.getMethod();}
        @Override public ParameterValueGroup            getParameterValues()             {return op.getParameterValues();}
        @Override public Collection<PositionalAccuracy> getCoordinateOperationAccuracy() {return op.getCoordinateOperationAccuracy();}
        @Override public Extent                         getDomainOfValidity()            {return op.getDomainOfValidity();}
        @Override public InternationalString            getScope()                       {return op.getScope();}
        @Override public MathTransform                  getMathTransform()               {return op.getMathTransform();}
        @Override public String                         toWKT()                          {return op.toWKT();}
        @Override public String                         toString()                       {return op.toString();}
        @Override public CoordinateReferenceSystem      getTargetCRS()                   {return crs;}
        @Override public CoordinateReferenceSystem      getSourceCRS()                   {return crs.getBaseCRS();}
        @Override public int                            hashCode()                       {return crs.hashCode() ^ 37;}
        @Override public boolean                        equals(final Object other) {
            // Since the conversion was obtained from the CRS, comparing the CRS is sufficient.
            return (other instanceof Projection) && crs.equals(((Projection) other).crs);
        }

        @Override public void formatTo(final Formatter formatter, final int flags, final int width, final int precision) {
            op.formatTo(formatter, flags, width, precision);
        }
    }

    /**
     * Gets the object which will perform the actual coordinate operation,
     * or {@code null} if this operation is a defining conversion.
     * Current wrapper implements the {@code MathTransform} interface in the same class,
     * but a future version may dissociate the objects if useful.
     *
     * @return the transform from source to target CRS, or {@code null} if not applicable.
     */
    @Override
    public MathTransform getMathTransform() {
        return (srcDim != 0 && dstDim != 0) ? this : null;
    }

    /**
     * Returns {@code true} if this transform is the identity transform. Note that a value of
     * {@code false} does not mean that the transform is not an identity transform, since this
     * case is a bit difficult to determine from PROJ API.
     */
    @Override
    public boolean isIdentity() {
        final CRS sourceCRS = getCRS(0);
        final CRS targetCRS = getCRS(1);
        if (sourceCRS == targetCRS) {
            return true;
        }
        if (sourceCRS == null || targetCRS == null) {
            return false;
        }
        final ComparisonCriterion c;
        if (sourceCRS instanceof GeneralDerivedCRS &&
            targetCRS instanceof GeneralDerivedCRS)
        {
            c = ComparisonCriterion.EQUIVALENT_EXCEPT_AXIS_ORDER_GEOGCRS;
        } else {
            c = ComparisonCriterion.EQUIVALENT;
        }
        return sourceCRS.impl.isEquivalentTo(targetCRS.impl, c.ordinal());
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
        synchronized (transforms) {
            for (int i=transforms.length; --i >= 0;) {
                final Transform tr = transforms[i];
                if (tr != null) {
                    transforms[i] = null;
                    tr.assign(c);
                    return tr;
                }
            }
        }
        return new Transform(impl, c);
    }

    /**
     * Releases the {@code PJ} wrapper, or destroys it if the cache is full.
     *
     * @param  tr  wrapper of the {@code PJ} to cache for reuse or to destroy.
     */
    private void release(final Transform tr) {
        synchronized (transforms) {
            for (int i=transforms.length; --i >= 0;) {
                if (transforms[i] == null) {
                    transforms[i] = tr;
                    tr.assign(null);
                    return;
                }
            }
        }
        tr.destroy();
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
        if (ptSrc.getDimension() != srcDim) {
            throw new MismatchedDimensionException();
        }
        double[] ordinates = new double[Math.max(srcDim, dstDim)];
        for (int i=0; i<srcDim; i++) {
            ordinates[i] = ptSrc.getOrdinate(i);
        }
        /*
         * Delegate the transform to PROJ, which will overwrite the coordinates in-place.
         */
        try (Context c = Context.acquire()) {
            final Transform tr = acquire(c);
            try {
                tr.transform(ordinates.length, ordinates, 0, 1);
            } finally {
                release(tr);
            }
        }
        /*
         * Copy the result to final location.
         */
        if (ptDst != null) {
            if (ptDst.getDimension() != dstDim) {
                throw new MismatchedDimensionException();
            }
            for (int i=0; i<dstDim; i++) {
                ptDst.setOrdinate(i, ordinates[i]);
            }
        } else {
            if (ordinates.length != dstDim) {
                ordinates = Arrays.copyOf(ordinates, dstDim);
            }
            ptDst = new SimpleDirectPosition(getCRS(1), ordinates);
        }
        return ptDst;
    }

    /**
     * Verifies that the given offset and number of points are valid for the array capacity.
     *
     * @param arrayLength  length of the array where to read or write coordinate values.
     * @param offset       index of the first array element to read or write.
     * @param numPts       number of points to read or write. Must be positive.
     * @param dimension    number of dimensions. Must be positive.
     * @throws IllegalArgumentException if the offset or number of points is out of bounds.
     */
    private static void ensureValidRange(final int arrayLength, final int offset, final int numPts, final int dimension) {
        if (offset < 0 || Math.addExact(offset, Math.multiplyExact(numPts, dimension)) > arrayLength) {
            if (offset < 0 || offset >= arrayLength) {
                throw new IllegalArgumentException("Offset " + offset + " is out of bounds.");
            } else {
                throw new IllegalArgumentException("The given number of points exceeds array capacity by "
                            + (numPts - ((arrayLength - offset) + dimension - 1) / dimension) + " points.");
            }
        }
    }

    /**
     * Copies coordinate values between two arrays potentially having a different number of dimensions.
     * The {@code srcPts} and {@code dstPts} arrays should not be the same array since this method does
     * not check if there is overlapping.
     *
     * @param srcPts  the array of source coordinates to copy in the buffer.
     * @param srcOff  index of the first coordinate to read in the source array.
     * @param srcDim  number of dimensions of points in the source array.
     * @param dstPts  the array where to copy the coordinates values.
     * @param dstOff  index of the first coordinate to write in the destination.
     * @param dstDim  number of dimensions of points in the destination.
     * @param n       number of points to copy. Must be greater than 0.
     */
    private static void copy(final double[] srcPts, int srcOff, final int srcDim,
                             final double[] dstPts, int dstOff, final int dstDim, int n)
    {
        n *= srcDim;
        if (srcDim == dstDim) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, n);
        } else {
            final int dimension = Math.min(srcDim, dstDim);
            n += srcOff;
            do {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff, dimension);
                srcOff += srcDim;
                dstOff += dstDim;
            } while (srcOff < n);
        }
    }

    /**
     * Copies an array of float values into a buffer of double values.
     * The destination may have more dimensions than the source.
     * In the later case, the destination array should be initialized
     * to zero because not all destination values will be written.
     *
     * @param srcPts  the array of source coordinates to copy in the buffer.
     * @param srcOff  index of the first coordinate to read in the source array.
     * @param srcDim  number of dimensions of points in the source array.
     * @param dstPts  buffer where to copy the coordinates values.
     * @param dstOff  index of the first coordinate to write in the buffer.
     * @param dstDim  number of dimensions of points in the buffer. Must be ≥ {@code srcDim}.
     * @param n       number of points to copy. Must be greater than 0.
     */
    private static void floatsToDoubles(final float[]  srcPts, int srcOff, final int srcDim,
                                        final double[] dstPts, int dstOff, final int dstDim, int n)
    {
        n *= srcDim;
        final int skip = dstDim - srcDim;
        int nextStop = (skip == 0) ? n : srcDim;
        nextStop += srcOff;
        dstOff   -= srcOff;
        n        += srcOff;
        for (;;) {
            dstPts[dstOff + srcOff] = srcPts[srcOff];
            if (++srcOff == nextStop) {
                if (srcOff == n) break;
                nextStop += srcDim;
                dstOff   += skip;
            }
        }
    }

    /**
     * Copies a buffer of double values into an array of float values.
     * The destination may have less dimensions than the source.
     *
     * @param srcPts  the buffer from where to read coordinate values.
     * @param srcOff  index of the first coordinate to read in the buffer.
     * @param srcDim  number of dimensions of points in the buffer. Must be ≥ {@code dstDim}.
     * @param dstPts  the target array where to copy coordinate values.
     * @param dstOff  index of the first coordinate to write in the target array.
     * @param dstDim  number of dimensions of points in the target array.
     * @param n       number of points to copy. Must be greater than 0.
     */
    private static void doublesToFloats(final double[] srcPts, int srcOff, final int srcDim,
                                        final float[]  dstPts, int dstOff, final int dstDim, int n)
    {
        n *= dstDim;
        final int skip = srcDim - dstDim;
        int nextStop = (skip == 0) ? n : dstDim;
        nextStop += dstOff;
        srcOff   -= dstOff;
        n        += dstOff;
        for (;;) {
            dstPts[dstOff] = (float) srcPts[srcOff + dstOff];
            if (++dstOff == nextStop) {
                if (dstOff == n) break;
                nextStop += dstDim;
                srcOff   += skip;
            }
        }
    }

    /**
     * Transforms an array of coordinate tuples.
     * This is the most efficient {@code transform(…)} method available in this {@link Operation} class.
     * This implementation tries to avoid copying the array if possible, but can not guarantee that no
     * copying will be done.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same than {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws IllegalArgumentException if an offset or number of points argument is invalid.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final double[] srcPts, final int srcOff,
                          final double[] dstPts, final int dstOff,
                          final int numPts) throws TransformException
    {
        if (numPts > 0) {
            ensureValidRange(srcPts.length, srcOff, numPts, srcDim);
            ensureValidRange(dstPts.length, dstOff, numPts, dstDim);
            final int dimension = Math.max(srcDim, dstDim);
            final double[] buffer;
            final int bufOff;
            if (srcDim == dstDim) {
                /*
                 * If source and target points have the same number of dimensions, copy the source coordinates
                 * into the target (if not already there); we will transform those coordinates directly there.
                 */
                buffer = dstPts;
                bufOff = dstOff;
                if (srcPts != dstPts || srcOff != dstOff) {
                    System.arraycopy(srcPts, srcOff, dstPts, dstOff, dstDim*numPts);
                }
            } else if (srcPts != dstPts && srcDim <= dstDim) {
                /*
                 * If source points have less dimensions than the target points, copy the source into the
                 * target with additional dimensions initialized to zero. It will allows us to transform
                 * those coordinates directly there. We don't do that if the arrays are the same because
                 * the check for overlapping is non-trivial; it is easier to use a temporary buffer.
                 */
                buffer = dstPts;
                bufOff = dstOff;
                Arrays.fill(dstPts, dstOff, dstOff + dstDim*numPts, 0);
                copy(srcPts, srcOff, srcDim,
                     dstPts, dstOff, dstDim, numPts);
            } else {
                /*
                 * If we can not transform the coordinates directly in their target location, copy in a
                 * temporary array. We will have to copy results to destination array after transform.
                 */
                buffer = new double[dimension * numPts];
                bufOff = 0;
                copy(srcPts, srcOff, srcDim,
                     buffer, bufOff, dimension, numPts);
            }
            /*
             * Delegate the transform to PROJ, which will overwrite the coordinates in-place.
             * If we used a temporary buffer, we will need to copy the results to `dstPts`.
             */
            try (Context c = Context.acquire()) {
                final Transform tr = acquire(c);
                try {
                    tr.transform(dimension, buffer, bufOff, numPts);
                } finally {
                    release(tr);
                }
            }
            if (buffer != dstPts) {
                copy(buffer, bufOff, dimension,
                     dstPts, dstOff, dstDim, numPts);
            }
        }
    }

    /**
     * Copies the {@code float} arrays to {@code double} arrays, then transforms the coordinate tuples.
     * This method copies the coordinates in a temporary buffer of type {@code double[]}.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     *                 May be the same than {@code srcPts}.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws IllegalArgumentException if an offset or number of points argument is invalid.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final float[] srcPts, final int srcOff,
                          final float[] dstPts, final int dstOff,
                          final int numPts) throws TransformException
    {
        if (numPts > 0) {
            ensureValidRange(srcPts.length, srcOff, numPts, srcDim);
            ensureValidRange(dstPts.length, dstOff, numPts, dstDim);
            final int dimension = Math.max(srcDim, dstDim);
            final double[] buffer = new double[dimension * numPts];
            floatsToDoubles(srcPts, srcOff, srcDim, buffer, 0, dimension, numPts);
            try (Context c = Context.acquire()) {
                final Transform tr = acquire(c);
                try {
                    tr.transform(dimension, buffer, 0, numPts);
                } finally {
                    release(tr);
                }
            }
            doublesToFloats(buffer, 0, dimension, dstPts, dstOff, dstDim, numPts);
        }
    }

    /**
     * Copies the {@code float} arrays to {@code double} arrays, then transforms the coordinate tuples.
     * This method copies the coordinates in a temporary buffer of type {@code double[]}.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws IllegalArgumentException if an offset or number of points argument is invalid.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final double[] srcPts, int srcOff,
                          final float[]  dstPts, int dstOff,
                          final int numPts) throws TransformException
    {
        if (numPts > 0) {
            ensureValidRange(srcPts.length, srcOff, numPts, srcDim);
            ensureValidRange(dstPts.length, dstOff, numPts, dstDim);
            final int dimension = Math.max(srcDim, dstDim);
            final double[] buffer = new double[dimension * numPts];
            copy(srcPts, srcOff, srcDim, buffer, 0, dimension, numPts);
            try (Context c = Context.acquire()) {
                final Transform tr = acquire(c);
                try {
                    tr.transform(dimension, buffer, 0, numPts);
                } finally {
                    release(tr);
                }
            }
            doublesToFloats(buffer, 0, dimension, dstPts, dstOff, dstDim, numPts);
        }
    }

    /**
     * Copies the {@code float} arrays to {@code double} arrays, then transforms the coordinate tuples.
     *
     * @param  srcPts  the array containing the source point coordinates.
     * @param  srcOff  the offset to the first point to be transformed in the source array.
     * @param  dstPts  the array into which the transformed point coordinates are returned.
     * @param  dstOff  the offset to the location of the first transformed point that is stored in the destination array.
     * @param  numPts  the number of point objects to be transformed.
     * @throws IllegalArgumentException if an offset or number of points argument is invalid.
     * @throws TransformException if a point can not be transformed.
     */
    @Override
    public void transform(final float[]  srcPts, int srcOff,
                          final double[] dstPts, int dstOff,
                          final int numPts) throws TransformException
    {
        if (numPts > 0) {
            ensureValidRange(srcPts.length, srcOff, numPts, srcDim);
            ensureValidRange(dstPts.length, dstOff, numPts, dstDim);
            final int dimension = Math.max(srcDim, dstDim);
            final double[] buffer;
            final int bufOff;
            if (dimension == dstDim) {
                buffer = dstPts;                            // Will write directly in destination array.
                bufOff = dstOff;
                if (dimension != srcDim) {
                    Arrays.fill(dstPts, dstOff, dstOff + dstDim*numPts, 0);
                }
            } else {
                buffer = new double[dimension * numPts];    // Write in temporary buffer.
                bufOff = 0;
            }
            floatsToDoubles(srcPts, srcOff, srcDim, buffer, bufOff, dimension, numPts);
            try (Context c = Context.acquire()) {
                final Transform tr = acquire(c);
                try {
                    tr.transform(dimension, buffer, 0, numPts);
                } finally {
                    release(tr);
                }
            }
            if (buffer != dstPts) {
                copy(buffer, bufOff, dimension,
                     dstPts, dstOff, dstDim, numPts);
            }
        }
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
    public synchronized MathTransform inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            inverse = (Operation) impl.inverse();
            inverse.inverse = this;
        }
        return inverse;
    }
}
