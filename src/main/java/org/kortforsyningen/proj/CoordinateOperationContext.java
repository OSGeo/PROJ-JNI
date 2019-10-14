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

import java.util.Objects;
import java.io.Serializable;
import org.opengis.metadata.extent.Extent;


/**
 * Optional information about the context in which a coordinate operation will be used.
 * When given to {@link Proj#createCoordinateOperation Proj.createCoordinateOperation(…)} method,
 * {@code CoordinateOperationContext} can modify the criterion by which PROJ will select the "best"
 * coordinate operation. Some criterion are:
 *
 * <ul>
 *   <li>The {@linkplain #setAreaOfInterest(Extent) area of interest}.</li>
 *   <li>The {@linkplain #setDesiredAccuracy(double) desired accuracy}.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * {@code CoordinateOperationContext} is <em>not</em> thread-safe.
 * It is only a temporary object for creating coordinate operations
 * and rarely need to be shared between different threads.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 *
 * @see <a href="https://proj.org/development/reference/cpp/operation.html#_CPPv4N5osgeo4proj9operation26CoordinateOperationContextE">PROJ C++ API</a>
 */
public class CoordinateOperationContext implements Cloneable, Serializable {
    /**
     * For cross-version compatibility of serialized objects.
     */
    private static final long serialVersionUID = 6084066145640864767L;

    /**
     * Name of the enumeration value used for PROJ default value.
     * All enumerations shall use the same name.
     */
    static final String DEFAULT = "DEFAULT";

    /**
     * The authority to use for searching coordinate operations.
     * Shall never be {@code null}.
     */
    private String authority;

    /**
     * The desired area of interest, or {@code null} if unknown.
     */
    private Extent areaOfInterest;

    /**
     * The desired accuracy (in metres), or 0 for the best accuracy available.
     */
    private double desiredAccuracy;

    /**
     * How source and target CRS extents should be used when considering if a transformation can be used.
     * The {@code null} value stands for the PROJ default.
     */
    private SourceTargetCRSExtentUse sourceAndTargetCRSExtentUse;

    /**
     * The spatial criterion to use when comparing the area of validity of coordinate operations
     * with the area of interest / area of validity of source and target CRS.
     * The {@code null} value stands for the PROJ default.
     */
    private SpatialCriterion spatialCriterion;

    /**
     * Specifies how grid availability is used.
     * The {@code null} value stands for the PROJ default.
     */
    private GridAvailabilityUse gridAvailabilityUse;

    /**
     * Whether an intermediate pivot CRS can be used for researching coordinate operations
     * between a source and target CRS.
     */
    private IntermediateCRSUse allowUseIntermediateCRS;

    /**
     * Whether transformations that are superseded (but not deprecated) should be discarded.
     */
    private boolean discardSuperseded;

    /**
     * Creates a new context initialized to default value.
     */
    public CoordinateOperationContext() {
        authority                   = "";
        sourceAndTargetCRSExtentUse = SourceTargetCRSExtentUse.DEFAULT;
        spatialCriterion            = SpatialCriterion.DEFAULT;
        gridAvailabilityUse         = GridAvailabilityUse.DEFAULT;
        allowUseIntermediateCRS     = IntermediateCRSUse.DEFAULT;
        discardSuperseded           = true;
    }

    /**
     * Sets the authority to use for searching coordinate operations.
     * Special values:
     *
     * <ul>
     *   <li>{@code ""} (empty string): coordinate operations from any authority will be searched,
     *     with the restrictions set in the {@code "authority_to_authority_preference"} database table.</li>
     *   <li>{@code "any"}: coordinate operations from any authority will be searched.</li>
     * </ul>
     *
     * Any other value will cause coordinate operations to be searched only in that authority name space.
     * The default value is {@code "default"}.
     *
     * @param  name  name of the new authority.
     * @throws NullPointerException if {@code name} is {@code null}.
     */
    public void setAuthority(final String name) {
        authority = name.trim();                    // Intentional NullPointerException if name is null.
    }

    /**
     * Returns the authority to use for searching coordinate operations.
     * This is the value given in last call to the {@linkplain #setAuthority setter},
     * or the default value ({@code ""}) if no value has been explicitly set.
     *
     * @return name of the current authority.
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Sets the desired area of interest from the given latitude and longitude bounds.
     * This convenience method creates an {@link Extent} from the given argument values,
     * then delegates to {@link #setAreaOfInterest(Extent)}.
     *
     * <p><strong>Caution:</strong> Arguments are expected in the same order than they appear in
     * the ISO 19115 specification. This is different than the order commonly found in Java world,
     * which is rather (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>,
     * <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>).</p>
     *
     * @param  westBoundLongitude  the minimal <var>x</var> value.
     * @param  eastBoundLongitude  the maximal <var>x</var> value.
     * @param  southBoundLatitude  the minimal <var>y</var> value.
     * @param  northBoundLatitude  the maximal <var>y</var> value.
     * @throws IllegalArgumentException if (<var>west bound</var> &gt; <var>east bound</var>)
     *         or (<var>south bound</var> &gt; <var>north bound</var>).
     *         Note that {@linkplain Double#NaN NaN} values are allowed.
     */
    public void setAreaOfInterest(final double westBoundLongitude,
                                  final double eastBoundLongitude,
                                  final double southBoundLatitude,
                                  final double northBoundLatitude)
    {
        setAreaOfInterest(new SimpleExtent(westBoundLongitude, eastBoundLongitude,
                                           southBoundLatitude, northBoundLatitude));
    }

    /**
     * Sets the desired area of interest.
     * The default value is {@code null}.
     *
     * @param  extent  the new area of interest, or {@code null} if unknown.
     */
    public void setAreaOfInterest(final Extent extent) {
        areaOfInterest = extent;
    }

    /**
     * Returns the desired area of interest, or null if none.
     * This is the value given in last call to the {@linkplain #setAreaOfInterest setter},
     * or the default value ({@code null}) if no value has been explicitly set.
     * That value is not cloned.
     *
     * @return the desired area of interest, or {@code null} if unknown.
     */
    public Extent getAreaOfInterest() {
        return areaOfInterest;
    }

    /**
     * Sets the desired accuracy (in metres).
     * A value of 0 stands for the best accuracy available.
     * The default value is 0.
     *
     * @param  accuracy  the desired accuracy (in metres), or 0 for the best accuracy available.
     * @throws IllegalArgumentException if the given value is not finite and positive.
     */
    public void setDesiredAccuracy(final double accuracy) {
        if (accuracy >= 0 && accuracy != Double.POSITIVE_INFINITY) {
            desiredAccuracy = accuracy;
        } else {
            throw new IllegalArgumentException("Accuracy shall be a positive number.");
        }
    }

    /**
     * Returns the desired accuracy (in metres).
     * This is the value given in last call to the {@linkplain #setDesiredAccuracy setter},
     * or the default value (zero) if no value has been explicitly set.
     *
     * @return the desired accuracy (in metres), or 0 for the best accuracy available.
     */
    public double getDesiredAccuracy() {
        return desiredAccuracy;
    }

    /**
     * Sets how source and target CRS extents should be used when considering if a transformation can be used.
     * This parameter takes effect only if no area of interest is explicitly defined.
     * The default is {@link SourceTargetCRSExtentUse#DEFAULT}.
     *
     * @param  use  the new policy about CRS extents use.
     */
    public void setSourceAndTargetCRSExtentUse(final SourceTargetCRSExtentUse use) {
        sourceAndTargetCRSExtentUse = Objects.requireNonNull(use);
    }

    /**
     * Returns how source and target CRS extents should be used when considering if a transformation can be used.
     * This is the value given in last call to the {@linkplain #setSourceAndTargetCRSExtentUse setter},
     * or the {@linkplain SourceTargetCRSExtentUse#DEFAULT default value} if no value has been explicitly set.
     *
     * @return the current policy about CRS extents use.
     */
    public SourceTargetCRSExtentUse getSourceAndTargetCRSExtentUse() {
        return sourceAndTargetCRSExtentUse;
    }

    /**
     * Sets the spatial criterion to use when comparing the area of validity of coordinate operations
     * with the area of interest / area of validity of source and target CRS.
     * The default is {@link SpatialCriterion#DEFAULT}.
     *
     * @param  criterion  the new spatial criterion.
     */
    public void setSpatialCriterion(final SpatialCriterion criterion) {
        spatialCriterion = Objects.requireNonNull(criterion);
    }

    /**
     * Returns the spatial criterion to use when comparing the area of validity of coordinate operations
     * with the area of interest / area of validity of source and target CRS.
     * This is the value given in last call to the {@linkplain #setSpatialCriterion setter},
     * or the {@linkplain SpatialCriterion#DEFAULT default value} if no value has been explicitly set.
     *
     * @return the current spatial criterion.
     */
    public SpatialCriterion getSpatialCriterion() {
        return spatialCriterion;
    }

    /**
     * Sets how grid availability is used.
     * The default is {@link GridAvailabilityUse#DEFAULT}.
     *
     * @param  use  the new policy about grid availability use.
     */
    public void setGridAvailabilityUse(final GridAvailabilityUse use) {
        gridAvailabilityUse = Objects.requireNonNull(use);
    }

    /**
     * Return how grid availability is used.
     * This is the value given in last call to the {@linkplain #setGridAvailabilityUse setter},
     * or the {@linkplain GridAvailabilityUse#DEFAULT default value} if no value has been explicitly set.
     *
     * @return the current policy about grid availability use.
     */
    public GridAvailabilityUse getGridAvailabilityUse() {
        return gridAvailabilityUse;
    }

    /**
     * Set whether an intermediate pivot CRS can be used for researching coordinate operations
     * between a source and target CRS. Concretely if in the database there is an operation
     * from <var>A</var> to <var>C</var> (or <var>C</var> to <var>A</var>), and another one
     * from <var>C</var> to <var>B</var> (or <var>B</var> to <var>C</var>), but no direct operation
     * between <var>A</var> and <var>B</var>, setting this parameter to
     * {@link IntermediateCRSUse#ALWAYS ALWAYS} or
     * {@link IntermediateCRSUse#IF_NO_DIRECT_TRANSFORMATION IF_NO_DIRECT_TRANSFORMATION},
     * allow chaining both operations.
     *
     * <p>The PROJ 6.2 implementation is limited to researching one intermediate step.
     * By default, with the {@code IF_NO_DIRECT_TRANSFORMATION} strategy, all potential
     * <var>C</var> candidates will be used if there is no direct transformation.</p>
     *
     * <p>The default is {@link IntermediateCRSUse#DEFAULT}.</p>
     *
     * @param  use  the new policy about intermediate pivot CRS use.
     */
    public void setAllowUseIntermediateCRS(final IntermediateCRSUse use) {
        allowUseIntermediateCRS = Objects.requireNonNull(use);
    }

    /**
     * Return whether an intermediate pivot CRS can be used for researching coordinate operations
     * between a source and target CRS.
     * This is the value given in last call to the {@linkplain #setAllowUseIntermediateCRS setter},
     * or the {@linkplain IntermediateCRSUse#DEFAULT default value} if no value has been explicitly set.
     *
     * @return the current policy about intermediate pivot CRS use.
     */
    public IntermediateCRSUse getAllowUseIntermediateCRS() {
        return allowUseIntermediateCRS;
    }

    /**
     * Sets whether transformations that are superseded (but not deprecated) should be discarded.
     * The default is true.
     *
     * @param  discard  the new policy about superseded transformations.
     */
    public void setDiscardSuperseded(final boolean discard) {
        discardSuperseded = discard;
    }

    /**
     * Returns whether transformations that are superseded (but not deprecated) should be discarded.
     * This is the value given in last call to the {@linkplain #setDiscardSuperseded setter},
     * or the default value ({@code true}) if no value has been explicitly set.
     *
     * @return the current policy about superseded transformations.
     */
    public boolean getDiscardSuperseded() {
        return discardSuperseded;
    }

    /**
     * Returns a hash code value for this context.
     * This value does not need to be stable between different versions of this class.
     *
     * @return an implementation-dependent hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(authority, areaOfInterest, desiredAccuracy, sourceAndTargetCRSExtentUse,
                spatialCriterion, gridAvailabilityUse, allowUseIntermediateCRS, discardSuperseded);
    }

    /**
     * Compares this context with the given object for equality.
     *
     * @param  obj  the object to compare with this context.
     * @return whether the given object is non-null and equal to this context.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final CoordinateOperationContext other = (CoordinateOperationContext) obj;
        /*
         * Note 1: The area of interest is often the only thing to vary between different
         *         CoordinateOperationContext instances, so we test it first.
         * Note 2: In the calls to equals(…), use `this` on the left side and `other` on
         *         the right side for consistency with argument order in this method call.
         */
        return Objects.equals(areaOfInterest, other.areaOfInterest) && authority.equals(other.authority) &&
               Double.doubleToLongBits(other.desiredAccuracy) == Double.doubleToLongBits(desiredAccuracy)
                                    && other.discardSuperseded           == discardSuperseded
                                    && other.sourceAndTargetCRSExtentUse == sourceAndTargetCRSExtentUse
                                    && other.spatialCriterion            == spatialCriterion
                                    && other.gridAvailabilityUse         == gridAvailabilityUse
                                    && other.allowUseIntermediateCRS     == allowUseIntermediateCRS;
    }

    /**
     * Returns a shallow copy of this context. The cloned context will share the
     * same {@linkplain #getAreaOfInterest() extent} instance than this context.
     *
     * @return a shallow copy of this coordinate operation context.
     */
    @Override
    public CoordinateOperationContext clone() {
        try {
            return (CoordinateOperationContext) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);            // Should never happen since we are cloneable.
        }
    }
}
