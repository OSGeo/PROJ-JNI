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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;


/**
 * Parses and format referencing objects in <cite>Well Known Text</cite>, JSON or PROJ format.
 * {@code ReferencingFormat} allows to chose the format (WKT, JSON or PROJ), the format version
 * (e.g. WKT 2 versus WKT 1), or the "flavor" when there is different interpretations of the same format.
 * {@code ReferencingFormat} provides also some control on the formatting process, for example the
 * number of spaces in indentations.
 *
 * <h2>Differences with {@code toWKT()}</h2>
 * WKT formatting can be done more easily by invoking the {@link IdentifiedObject#toWKT()} method.
 * However the {@code toWKT()} method, like the {@code toString()} method, uses only the information
 * available in the object to format. This {@link ReferencingFormat} class differs in that it may
 * complete those information by an access to the database.
 *
 * <h2>Limitations</h2>
 * <p>{@code ReferencingFormat} is <em>not</em> thread-safe. If used in a multi-thread environment,
 * then each thread should have its own instance, or synchronization shall be done by the user.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public class ReferencingFormat {
    /**
     * The convention to use for formatting referencing objects.
     */
    private Convention convention;

    /**
     * Whether the WKT result should by in many lines.
     * If {@code false}, then the output will be in a single line.
     */
    private boolean multiline;

    /**
     * Number of spaces in indentations when multilines output is enabled.
     */
    private int indentation;

    /**
     * Whether the operation should fail if it can not be performed using
     * strictly standard compliant WKT format.
     */
    private boolean strict;

    /**
     * The warnings that occurred during parsing, or an empty list if none.
     */
    private final List<String> warnings;

    /**
     * Creates a new formatter initialized to default configuration.
     * The default configuration uses {@link Convention#WKT} and
     * formats the WKT in a multi-lines layout.
     */
    public ReferencingFormat() {
        convention  = Convention.WKT;
        multiline   = true;
        indentation = 4;
        warnings    = new ArrayList<>();
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
     * This method allows to select a different format (e.g. JSON or PROJ)
     * or a different flavor of legacy WKT 1 format.
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
     * Returns the number of spaces in indentations when multilines output is enabled.
     *
     * @return number of spaces for each indentation level.
     */
    public int getIndentationWidth() {
        return indentation;
    }

    /**
     * Sets the number of spaces in indentations when multilines output is enabled.
     * If this setter is not invoked, then the default value is 4.
     *
     * @param n  number of spaces for each indentation level.
     */
    public void setIndentationWidth(final int n) {
        if (n < 0 || n > Short.MAX_VALUE) {         // Arbitrary upper limit.
            throw new IllegalArgumentException("Invalid indentation width.");
        }
        indentation = n;
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
     * @return the given object in WKT, JSON or PROJ format.
     * @throws UnformattableObjectException if the given object can not be formatted.
     */
    public String format(final Object object) throws UnformattableObjectException {
        warnings.clear();
        Objects.requireNonNull(object);
        if (object instanceof IdentifiableObject) {
            final String text;
            try (Context c = Context.acquire()) {
                text = ((IdentifiableObject) object).impl.format(c,
                        convention.ordinal(), indentation, multiline, strict);
            }
            if (text != null) {
                return text;
            }
        }
        throw new UnformattableObjectException("Can not format the given object.");
    }

    /**
     * Parses the given characters string. The format (WKT, PROJ) must be the
     * format specified by the last call to {@link #setConvention(Convention)}.
     * If the given string contains some non-fatal errors, warnings can be obtained
     * by {@link #getWarnings()}.
     *
     * @param  text  the object definition to parse.
     * @return object parsed from the given characters string.
     * @throws UnparsableObjectException if an error occurred during parsing.
     *
     * @see Proj#createFromUserInput(String)
     */
    public Object parse(final String text) throws UnparsableObjectException {
        warnings.clear();
        try (Context c = Context.acquire()) {
            return parse(text, c, convention.ordinal(), strict);
        }
    }

    /**
     * Parses the given characters string.
     *
     * @param  text        the object definition to parse.
     * @param  context     the thread context, or {@code null} if none.
     * @param  convention  ordinal value of the {@link ReferencingFormat.Convention} to use.
     * @param  strict      whether to enforce strictly standard format.
     * @return object parsed from the given characters string.
     * @throws UnparsableObjectException if an error occurred during parsing.
     */
    final native Object parse(String text, Context context, int convention, boolean strict) throws UnparsableObjectException;

    /**
     * Adds the given message to the warnings. This method is invoked from native code;
     * method signature shall not be modified unless the native code is updated accordingly.
     *
     * @param  message  the warning message to add.
     */
    private void addWarning(final String message) {
        warnings.add(message);
    }

    /**
     * Returns the warnings emitted during the last parsing operation.
     * If no warning occurred, then this method returns an empty list.
     *
     * @return the warnings that occurred during the last parsing operation.
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }



    /**
     * Controls some aspects in formatting referencing objects as <cite>Well Known Text</cite> (WKT),
     * JSON or PROJ strings.
     * The WKT format has two major versions (WKT 1 and WKT 2), with WKT 2 defined by ISO 19162.
     * Those WKT versions have different compatibility characteristics:
     *
     * <ul>
     *   <li>All WKT 2 flavors in this enumeration are consistent between them.</li>
     *   <li>The legacy WKT 1 format defined in <a href="https://www.opengeospatial.org/standards/sfs">OGC
     *       99-049 — Simple Feature Implementation (1999)</a> had various interpretations with some
     *       incompatibilities between them (e.g. regarding units of measurement). A CRS formatted with
     *       one WKT 1 flavor is not guaranteed to be read correctly with a different WKT 1 flavor.</li>
     *   <li>The WKT 1 format defined in <a href="http://www.opengeospatial.org/standards/ct">OGC
     *       01-009 — Coordinate Transformation Service (2001)</a> fixed many OGC 99-049 ambiguities
     *       but is not supported by PROJ.</li>
     * </ul>
     *
     * The {@link #WKT} and {@link #WKT_SIMPLIFIED} fields are aliases to the most recent WKT
     * versions supported by current implementation.
     *
     * <h2>Note about WKT in GeoPackage</h2>
     * The <a href="http://www.geopackage.org/spec/#gpkg_spatial_ref_sys_cols_crs_wkt">GeoPackage standard</a>
     * defines two columns for specifying Coordinate Reference System in Well Known Text format:
     * the {@code "definition"} column shall contain a WKT 1 string as defined by OGC 01-009 while
     * the {@code "definition_12_063"} column contains a WKT 2 string as defined by OGC 12-063.
     * Since PROJ does not support OGC 01-009, we recommend to always provided a value in the
     * {@code "definition_12_063"} column formatted by {@link #WKT2_2015}.
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
         * @see #WKT
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
         *
         * @see #WKT_SIMPLIFIED
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
         * Well Known Text version 1 as traditionally written by ESRI software in Shapefiles.
         * This format has the same departures than {@link #WKT1_GDAL} with respect to OGC 01-009,
         * plus some more differences in map projection parameter names.
         * The {@code AXIS} and {@code AUTHORITY} elements are omitted in this format.
         */
        WKT1_ESRI,

        /**
         * Implementation-specific string format for CRS and coordinate operations.
         * The PROJ format is very compact but can not express all aspects of referencing objects.
         * The format output depends on the type of object to format:
         *
         * <ul>
         *   <li>For a {@link CoordinateReferenceSystem}, formats the same as {@link #PROJ_4}.
         *       It should be noted that the export of a CRS as a PROJ string may cause loss
         *       of many important aspects of a CRS definition.
         *       Consequently it is discouraged to use it for interoperability in newer projects.
         *       The choice of a WKT representation will be a better option.</li>
         *   <li>For {@link CoordinateOperation}, returns a PROJ pipeline.</li>
         * </ul>
         */
        PROJ_5,

        /**
         * Implementation-specific string format for PROJ 4 compatibility.
         * This format was extensively used in PROJ 4, but since PROJ 6 the {@link #WKT} format
         * is preferred for better interoperability and for more complete object descriptions.
         * The PROJ 4 convention formats a string compatible with the {@code OGRSpatialReference::exportToProj4()}
         * of GDAL ≥ 2.3. It is only compatible with a few CRS objects. The PROJ string will also contain a
         * {@code +type=crs} parameter to disambiguate the nature of the string from a coordinate operation.
         *
         * <ul>
         *   <li>For a {@link org.opengis.referencing.crs.GeographicCRS}, returns a {@code proj=longlat} string,
         *       with ellipsoid / datum / prime meridian information, ignoring axis order and unit information.</li>
         *   <li>For a geocentric {@code GeodeticCRS}, returns the transformation from
         *       geographic coordinates into geocentric coordinates.</li>
         *   <li>For a {@link org.opengis.referencing.crs.ProjectedCRS}, returns the projection method,
         *       ignoring axis order.</li>
         *   <li>For a {@code BoundCRS}, returns the PROJ string of its source/base CRS,
         *       amended with {@code towgs84} / {@code nadgrids} parameter when the deriving conversion
         *       can be expressed in that way.</li>
         * </ul>
         */
        PROJ_4,

        /**
         * JSON format (non-standard).
         * This format is PROJ-specific for now, but the structure of the JSON objects
         * follow closely the model described by ISO 19111.
         */
        JSON;

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
