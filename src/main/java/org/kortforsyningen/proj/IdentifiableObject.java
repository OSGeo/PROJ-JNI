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
import java.util.Collection;
import java.util.Collections;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.extent.Extent;


/**
 * Base class of wrappers around PROJ objects referenced by a shared pointer.
 * The native PROJ resource is referenced indirectly through the {@link NativeResource} class.
 * The subclass will typically implements {@link org.opengis.referencing.IdentifiedObject} interface,
 * but not necessarily; it may also be some other type like unit of measurement or geographic extent.
 * We consider those other types as kind of identifiable objects too since they are usually created
 * from EPSG codes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
abstract class IdentifiableObject implements Formattable {
    /**
     * Provides access to the PROJ implementation.
     */
    final SharedPointer impl;

    /**
     * Creates a wrapper for the given pointer to a PROJ structure.
     * It is caller's responsibility to invoke {@link #cleanWhenUnreachable()} after construction.
     *
     * @param  ptr  pointer to the PROJ structure to wrap.
     * @throws OutOfMemoryError if {@code ptr} is 0.
     */
    IdentifiableObject(final long ptr) {
        impl = new SharedPointer(ptr);
    }

    /**
     * Creates an {@code IdentifiableObject} using a predetermined wrapper to a PROJ structure.
     * This is a variant of {@link #IdentifiableObject(long)} constructor for the cases when a
     * subclass of {@link SharedPointer} is desired. It still caller's responsibility to invoke
     * {@link #cleanWhenUnreachable()} after construction.
     *
     * @param  ptr  wrapper to a pointer to the PROJ structure.
     */
    IdentifiableObject(final SharedPointer ptr) {
        impl = ptr;
    }

    /**
     * Registers a cleaner which will release PROJ resources when this {@code IdentifiableObject}
     * is garbage collected. This method shall be invoked exactly once after construction.
     * This method usually returns {@code this}, unless another wrapper has been created concurrently
     * for the same PROJ object. In the later case, {@code this} wrapper is destroyed and the existing
     * wrapper is returned.
     *
     * @return the wrapper to use (usually {@code this} unless another wrapper has been created concurrently).
     */
    final IdentifiableObject cleanWhenUnreachable() {
        final IdentifiableObject existing = SharedObjects.CACHE.putIfAbsent(impl.rawPointer(), this);
        if (existing == null) {
            return this;            // Normal case.
        } else {
            impl.release();         // Destroy this wrapper, use the existing one instead.
            return existing;
        }
    }

    /**
     * Returns a non-null label identifying this object.
     * This is used for formatting error messages only.
     *
     * @return a non-null label for error messages.
     */
    final String label() {
        final ReferenceIdentifier name = getName();
        if (name != null) {
            final String code = name.getCode();
            if (code != null) return code;
        }
        return "?";
    }

    /*
     * Following methods are defined here for the convenience of subclasses
     * implementing the org.opengis.referencing.IdentifiedObject interface.
     */

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return the primary name, or {@code null} if this object does not provide a name.
     */
    public ReferenceIdentifier getName() {
        return null;
    }

    /**
     * Returns alternative names by which this object is identified.
     *
     * @return alternative names and abbreviations, or an empty collection if there is none.
     */
    public Collection<GenericName> getAlias() {
        return Collections.emptySet();
    }

    /**
     * Returns identifiers which reference elsewhere the object's defining information.
     *
     * @return this object identifiers, or an empty collection if there is none.
     */
    public Set<ReferenceIdentifier> getIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * Area or region or time frame in which this CRS, datum or coordinate operation is valid.
     *
     * @return the CRS, datum or operation valid domain, or {@code null} if not available.
     */
    public Extent getDomainOfValidity() {
        return null;
    }

    /**
     * Description of domain of usage, or limitations of usage,
     * for which this CRS, datum or operation object is valid.
     *
     * @return the CRS, datum or operation domain of usage, or {@code null} if none.
     */
    public InternationalString getScope() {
        return getProperty(SharedPointer.SCOPE);
    }

    /**
     * Returns comments on or information about this object, including data source information.
     *
     * @return the remarks, or {@code null} if none.
     */
    public InternationalString getRemarks() {
        return getProperty(SharedPointer.REMARKS);
    }

    /**
     * Returns a property value as an international string.
     *
     * @param  property  one of {@link SharedPointer#ABBREVIATION}, <i>etc.</i> values.
     * @return value of the specified property, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for this object.
     */
    private InternationalString getProperty(final short property) {
        final String value = impl.getStringProperty(property);
        return (value != null) ? new SimpleCitation(value) : null;
    }

    /**
     * Returns a <cite>Well-Known Text</cite> (WKT) for this object.
     * This method can be invoked only if the wrapped PROJ object is
     * an instance of {@code osgeo::proj::io::IWKTExportable}.
     *
     * @return the Well-Known Text (WKT) for this object.
     * @throws UnsupportedOperationException if this object can not be formatted as WKT.
     * @throws FormattingException if an error occurred during formatting.
     */
    public String toWKT() {
        final String wkt = impl.format(null, ReferencingFormat.Convention.WKT.ordinal(), -1, true, true);
        if (wkt != null) {
            return wkt;
        } else {
            throw new UnsupportedOperationException("This object is not exportable to WKT.");
        }
    }

    /**
     * Returns a simplified <cite>Well-Known Text</cite> (WKT) for this object,
     * or an arbitrary string if this object can not be formatted in WKT.
     *
     * @return string representation of this object.
     */
    @Override
    public String toString() {
        try {
            final String wkt = impl.format(null, ReferencingFormat.Convention.WKT_SIMPLIFIED.ordinal(), -1, true, false);
            if (wkt != null) {
                return wkt;
            }
        } catch (FormattingException e) {
            return e.toString();
        }
        return super.toString();
    }

    /**
     * Formats the name or identifier of this object using the provider formatter.
     * This method is invoked when an {@code IdentifiableObject} object is formatted
     * using the {@code "%s"} conversion specifier of {@link Formatter}.
     * Users don't need to invoke this method explicitly.
     *
     * <p>If the alternate flags is present (as in {@code "%#s"}), then this method
     * will format the identifier (if present) instead than the object name.</p>
     *
     * @param  formatter  the formatter in which to format this object.
     * @param  flags      whether to apply left alignment, use upper-case letters and/or use alternate form.
     * @param  width      minimal number of characters to write, padding with {@code ' '} if necessary.
     * @param  precision  maximal number of characters to write, or -1 if no limit.
     */
    @Override
    public void formatTo(final Formatter formatter, final int flags, int width, int precision) {
        String value = null;
        if ((flags & FormattableFlags.ALTERNATE) != 0) {
            value = impl.getStringProperty(SharedPointer.AUTHORITY_CODE);
        }
        if (value == null) {
            value = impl.getStringProperty(SharedPointer.NAME_STRING);
            if (value == null) {
                value = "unnamed";
            }
        }
        /*
         * Converting to upper cases may change the string length in some locales.
         * So we need to perform this conversion before to check the length.
         */
        boolean isUpperCase = (flags & FormattableFlags.UPPERCASE) != 0;
        if (isUpperCase && (width > 0 || precision >= 0)) {
            value = value.toUpperCase(formatter.locale());
            isUpperCase = false;                            // Because conversion has already been done.
        }
        /*
         * If the string is longer than the specified "precision", truncate
         * and add "…" for letting user know that there is missing characters.
         * This loop counts the number of Unicode code points rather than characters.
         */
        int length = value.length();
        if (precision >= 0) {
            for (int i=0,n=0; i<length; i += n) {
                if (--precision < 0) {
                    /*
                     * Found the amount of characters to keep. The 'n' variable can be
                     * zero only if precision == 0, in which case the string is empty.
                     */
                    if (n == 0) {
                        value = "";
                    } else {
                        length = (i -= n) + 1;
                        final StringBuilder buffer = new StringBuilder(length);
                        value = buffer.append(value, 0, i).append('…').toString();
                    }
                    break;
                }
                n = Character.charCount(value.codePointAt(i));
            }
        }
        /*
         * If the string is shorter than the minimal width, add spaces on the left or right side.
         * We double check with `width > length` since it is faster than codePointCount(…).
         */
        final String format;
        final Object[] args;
        if (width > length && (width -= value.codePointCount(0, length)) > 0) {
            format = "%s%s";
            args = new Object[] {value, value};
            args[(flags & FormattableFlags.LEFT_JUSTIFY) != 0 ? 1 : 0] = " ".repeat(width);
        } else {
            format = isUpperCase ? "%S" : "%s";
            args = new Object[] {value};
        }
        formatter.format(format, args);
    }

    /**
     * Returns a hash code value for this object.
     * This method is defined for consistency with {@link #equals(Object)}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(impl.rawPointer());
    }

    /**
     * Returns {@code true} if this native resource is wrapping the same PROJ object than {@code other}.
     *
     * @param  other  the other object to compare with this native resource.
     * @return whether the two objects are wrapping the same PROJ object.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        return (other != null) && other.getClass().equals(getClass()) &&
                ((IdentifiableObject) other).impl.rawPointer() == impl.rawPointer();
    }
}
