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

import java.util.Collection;
import java.util.Collections;
import java.io.Serializable;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.Result;
import org.opengis.util.InternationalString;


/**
 * A {@link PositionalAccuracy} containing only a description as a character string.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 * @module
 */
final class SimpleAccuracy implements PositionalAccuracy, ConformanceResult, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4356484311605435920L;

    /**
     * A description of the accuracy.
     */
    private final String result;

    /**
     * Constructs a new accuracy description.
     *
     * @param  result  a description of the accuracy.
     */
    SimpleAccuracy(final String result) {
        this.result = result;
    }

    /**
     * Description of the measure being determined.
     *
     * @return "Coordinate operation accuracy."
     */
    @Override
    public InternationalString getMeasureDescription() {
        return new SimpleCitation("Coordinate operation accuracy.");
    }

    /**
     * Type of method used to evaluate quality of the dataset.
     * Method of evaluating the quality of a dataset based on external knowledge.
     *
     * @return type of method used to evaluate quality, or {@code null}.
     */
    @Override
    public EvaluationMethodType getEvaluationMethodType() {
        return EvaluationMethodType.INDIRECT;
    }

    /**
     * Value that describe the accuracy.
     *
     * @return {@code this}.
     */
    @Override
    public Collection<? extends Result> getResults() {
        return Collections.singleton(this);
    }

    /**
     * Specification against which data is being evaluated.
     * This property is mandatory according ISO model, but we do not have this information.
     *
     * @return {@code null}.
     */
    @Override
    public Citation getSpecification() {
        return null;
    }

    /**
     * Explanation of the meaning of conformance for this result.
     *
     * @return explanation of the meaning of conformance.
     */
    @Override
    public InternationalString getExplanation() {
        return new SimpleCitation(result);
    }

    /**
     * Indication of the conformance result.
     * This property is mandatory according ISO model, but we do not have this information.
     *
     * @return {@code null}.
     */
    @Override
    public Boolean pass() {
        return null;
    }

    /**
     * Returns a hash code value for this accuracy result.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return result.hashCode() ^ 45;
    }

    /**
     * Tests whether the given object is an positional accuracy with the same result.
     *
     * @param  obj  the other object to compare with this object.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SimpleAccuracy) && result.equals(((SimpleAccuracy) obj).result);
    }

    /**
     * Returns the positional accuracy result directly.
     *
     * @return the positional accuracy result.
     */
    @Override
    public String toString() {
        return result;
    }
}
