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
import org.opengis.referencing.IdentifiedObject;


/**
 * Parses and format geodetic objects in <cite>Well Known Text</cite> format.
 * WKT formatting can be done more easily by invoking the {@link IdentifiedObject#toWKT()} method.
 * This {@code WKTFormat} can be used when more control is desired on the formatting process,
 * for example by {@linkplain #setConvention selecting which WKT version is desired}.
 *
 * <p>{@code WKTFormat} is <strong>not</strong> thread-safe. If used in a multi-thread environment,
 * then each thread should have its own instance, or synchronization should be done by the user.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public class WKTFormat {
    /**
     * The convention to use for formatting geodetic objects.
     */
    private Convention convention;

    /**
     * Whether the WKT result should by in many lines.
     * If {@code false}, then the output will be in a single line.
     */
    private boolean multiline;

    /**
     * Whether the operation should fail if it can not be performed using
     * strictly standard compliant WKT format.
     */
    private boolean strict;

    /**
     * Creates a new formatter initialized to default configuration.
     * The default configuration uses {@link Convention#WKT} and
     * formats the WKT in a multi-lines layout.
     */
    public WKTFormat() {
        convention = Convention.WKT;
        multiline  = true;
    }

    /**
     * Returns the current convention used by this formatter.
     * The default value is {@link Convention#WKT}, which stands for the
     * latest supported WKT version (currently WKT 2 as published in 2019).
     *
     * @return the current convention.
     */
    public Convention getConvention() {
        return convention;
    }

    /**
     * Sets the convention to use for this formatter.
     * This method allows to select a different version or flavor of WKT format.
     * For example the conventions can be set to {@link Convention#WKT1_ESRI WKT1_ESRI}
     * if the legacy WKT format is desired instead than the one standardized by ISO 19162.
     *
     * <p>If this setter is not invoked, then the default value is {@link Convention#WKT}.</p>
     *
     * @param  convention  the new convention to apply.
     */
    public void setConvention(final Convention convention) {
        this.convention = Objects.requireNonNull(convention);
    }

    /**
     * Returns whether the output will be written in many lines.
     *
     * @return whether the WKT will use multi-line layout.
     */
    public boolean getMultiLine() {
        return multiline;
    }

    /**
     * Sets whether the output will be written in many lines.
     * If this setter is not invoked, then the default value is {@code true}.
     *
     * @param  multiline  whether the WKT will use multi-line layout.
     */
    public void setMultiLine(final boolean multiline) {
        this.multiline = multiline;
    }

    /**
     * Returns whether the operation should fail if it can not be performed using
     * strictly standard compliant WKT format.
     *
     * @return whether to enforce strictly standard format.
     */
    public boolean getStrict() {
        return strict;
    }

    /**
     * Sets whether the operation should fail if it can not be performed using
     * strictly standard compliant WKT format.
     *
     * @param  strict  whether to enforce strictly standard format.
     */
    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    /**
     * Formats the given object. It must be a PROJ implementation.
     *
     * @param  object  the PROJ object to format.
     * @return the given object in WKT format.
     * @throws FormattingException if the given object can not be formatted.
     */
    public String format(final Object object) throws FormattingException {
        Objects.requireNonNull(object);
        if (object instanceof SharedObject) {
            final String wkt = ((SharedObject) object).impl.toWKT(convention.ordinal(), multiline, strict);
            if (wkt != null) {
                return wkt;
            }
        }
        throw new FormattingException("Can not format the given object.");
    }




    /**
     * Controls some aspects in formatting of geodetic objects as <cite>Well Known Text</cite>.
     * The WKT format has two major versions (WKT 1 and WKT 2), with WKT 2 defined by ISO 19162.
     * The legacy WKT 1 format had various interpretations with some incompatibilities between
     * them (e.g. regarding units of measurement).
     * The WKT 2 format is consistent between all flavors in this enumeration.
     * The {@link #WKT} and {@link #WKT_SIMPLIFIED} fields are aliases to the most recent WKT
     * versions supported by current implementation.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.0
     * @since   1.0
     */
    public enum Convention {
        /**
         * Full Well Known Text version 2 string, conforming to ISO 19162:2019 / OGC 18-010.
         * The output contains all possible nodes and new keyword names.
         * Non-normative list of differences:
         *
         * <ul>
         *   <li>{@code WKT2_2019} uses {@code GEOGCRS} / {@code BASEGEOGCRS} keywords
         *       for {@link org.opengis.referencing.crs.GeographicCRS}.</li>
         * </ul>
         *
         * @see <a href="http://docs.opengeospatial.org/is/18-010r7/18-010r7.html">Well-known
         *      text representation of coordinate reference systems (2019)</a>
         */
        WKT2_2019,

        /**
         * Full Well Known Text version 2 string, conforming to ISO 19162:2015(E) / OGC 12-063r5.
         * The output contains all possible nodes and new keyword names.
         *
         * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">Well-known
         *      text representation of coordinate reference systems (2015)</a>
         */
        WKT2_2015,

        /**
         * Well Known Text version 2 (2019) in a more compact form.
         * Same as {@link #WKT2_2019} with the following exceptions:
         *
         * <ul>
         *   <li>{@code UNIT} keyword used.</li>
         *   <li>{@code ID} node only on top element.</li>
         *   <li>No {@code ORDER} element in {@code AXIS} element.</li>
         *   <li>{@code PRIMEM} node omitted if it is Greenwich.</li>
         *   <li>{@code ELLIPSOID.UNIT} node omitted if the unit of measure is metre.</li>
         *   <li>{@code PARAMETER.UNIT} / {@code PRIMEM.UNIT} omitted if same as {@code AXIS}.</li>
         *   <li>{@code AXIS.UNIT} omitted and replaced by a common {@code GEODCRS.UNIT}
         *       if they are all the same on all axis.</li>
         * </ul>
         */
        WKT2_2019_SIMPLIFIED,

        /**
         * Well Known Text version 2 (2015) in a more compact form.
         * Same as {@link #WKT2_2015} with the same differences than
         * the ones documented in {@link #WKT2_2019_SIMPLIFIED}.
         */
        WKT2_2015_SIMPLIFIED,

        /**
         * Well Known Text version 1 as traditionally written by GDAL.
         * A notable departure from {@code WKT1_GDAL} with respect to OGC 01-009 is that
         * in {@code WKT1_GDAL}, the unit of the {@code PRIMEM} value is always degrees.
         */
        WKT1_GDAL,

        /**
         * Well Known Text version 1 as traditionally written by ESRI software.
         * This is derived from OGC 01-009.
         *
         * @see <a href="http://www.opengeospatial.org/standards/ct">Coordinate Transformation Service (2001)</a>
         */
        WKT1_ESRI;

        /**
         * The most recent version of WKT supported by current implementation.
         * This is currently set as an alias to {@link #WKT2_2019}.
         * This alias may be changed in a future version if a new revision of WKT 2 specification is published.
         */
        public static final Convention WKT = WKT2_2019;

        /**
         * The most recent version of "simplified" WKT supported by current implementation.
         * This is currently set as an alias to {@link #WKT2_2019_SIMPLIFIED}.
         * This alias may be changed in a future version if a new revision of WKT 2 specification is published.
         */
        public static final Convention WKT_SIMPLIFIED = WKT2_2019_SIMPLIFIED;
    }
}
