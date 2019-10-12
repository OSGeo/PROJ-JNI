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
import java.lang.ref.Cleaner;
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
abstract class IdentifiableObject {
    /**
     * Manager of objects having native resources to be released after the Java object has been garbage-collected.
     * This manager will decrement the reference count of the shared pointer.
     */
    private static final Cleaner DISPOSER = Cleaner.create(IdentifiableObject::cleanerThread);

    /**
     * Creates a new thread for disposing PROJ objects after their Java wrappers have been garbage-collected.
     * We create a thread ourselves mostly for specifying a more explicit name than the default name.
     *
     * @param  cleaner  provided by {@link Cleaner}.
     * @return the thread to use for disposing PROJ objects.
     */
    private static Thread cleanerThread(final Runnable cleaner) {
        final Thread t = new Thread(cleaner);
        t.setPriority(Thread.MAX_PRIORITY - 2);
        t.setName("PROJ objects disposer");
        return t;
    }

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
     */
    final void cleanWhenUnreachable() {
        DISPOSER.register(this, impl);
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
        return null;
    }

    /**
     * Returns comments on or information about this object, including data source information.
     *
     * @return the remarks, or {@code null} if none.
     */
    public InternationalString getRemarks() {
        return null;
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
