/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021-2023 Open Source Geospatial Foundation
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
package org.osgeo.proj;

import java.util.StringJoiner;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.FactoryException;


/**
 * An implementation of {@link Identifier} as a wrapper
 * around an {@code osgeo::proj::metadata::Identifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 * @since   1.0
 */
final class ObjectIdentifier extends IdentifiableObject implements ReferenceIdentifier {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::metadata::Identifier}.
     *
     * @param  ptr  pointer to the PROJ structure to wrap.
     * @throws FactoryException if {@code ptr} is 0.
     */
    ObjectIdentifier(final long ptr) throws FactoryException {
        super(ptr);
    }

    /**
     * Returns a non-null label identifying this object.
     *
     * @param  alternate  whether to return an authority code instead.
     * @return a non-null label for error messages or short formatting.
     */
    @Override
    String getNameString(final boolean alternate) {
        String t;
        final StringJoiner s = new StringJoiner(":");
        if ((t = getCodeSpace()) != null) s.add(t);
        if ((t = getCode())      != null) s.add(t);
        return s.toString();
    }

    /**
     * Returns the organization or party responsible for definition and maintenance
     * of the {@linkplain #getCode() code}, or {@code null} if none. It can be a
     * bibliographical reference to an international standard such as ISO 19115.
     *
     * @return the authority given at construction time, or {@code null} if none.
     */
    @Override
    public Citation getAuthority() {
        final String title = impl.getStringProperty(Property.CITATION_TITLE);
        return (title != null) ? new SimpleCitation(title) : null;
    }

    /**
     * Returns the identifier or namespace in which the code is valid, or {@code null} if none.
     *
     * @return the code space given at construction time, or {@code null} if none.
     */
    @Override
    public String getCodeSpace() {
        return impl.getStringProperty(Property.CODESPACE);
    }

    /**
     * Returns the alphanumeric value identifying an instance in the namespace.
     * It can be for example the name of a class defined by the international standard
     * referenced by the {@linkplain #getAuthority() authority} citation.
     *
     * @return the code given at construction time, or {@code null} if none.
     */
    @Override
    public String getCode() {
        return impl.getStringProperty(Property.CODE);
    }

    /**
     * Version identifier for the namespace, as specified by the code authority.
     * When appropriate, the edition is identified by the effective date, coded
     * using ISO 8601 date format.
     *
     * @return the version given at construction time, or {@code null} if none.
     */
    @Override
    public String getVersion() {
        return impl.getStringProperty(Property.VERSION);
    }




    /**
     * An identifier which fallback on {@link Property#NAME_STRING} when the code value is missing.
     * The identifier code should be mandatory, but PROJ does not always provide it.
     * This class is a workaround for the case where the code is missing.
     */
    final class PrimaryName implements ReferenceIdentifier {
        /**
         * The object from which to fetch {@link Property#NAME_STRING} if needed.
         */
        private final IdentifiableObject parent;

        /**
         * Creates a new identifier for the primary name of the given parent.
         *
         * @param  parent  the object from which to fetch {@link Property#NAME_STRING} if needed.
         */
        PrimaryName(final IdentifiableObject parent) {
            this.parent = parent;
        }

        /**
         * Returns the identifier code, with fallback on the parent primary name if the code is missing.
         */
        @Override
        public String getCode() {
            String code = ObjectIdentifier.this.getCode();
            if (code == null) {
                code = parent.impl.getStringProperty(Property.NAME_STRING);
            }
            return code;
        }

        @Override public String   getCodeSpace() {return ObjectIdentifier.this.getCodeSpace();}
        @Override public Citation getAuthority() {return ObjectIdentifier.this.getAuthority();}
        @Override public String   getVersion()   {return ObjectIdentifier.this.getVersion();}

        /**
         * Returns a hash code value for this object.
         * This method is defined for consistency with {@link #equals(Object)}.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return ObjectIdentifier.this.hashCode() + parent.hashCode();
        }

        /**
         * Returns {@code true} if this native resource is wrapping the same PROJ object than {@code other}.
         *
         * @param  other  the other object to compare with this native resource.
         * @return whether the two objects are wrapping the same PROJ object.
         */
        @Override
        public boolean equals(final Object other) {
            if (other instanceof PrimaryName) {
                final PrimaryName p = (PrimaryName) other;
                return p.wrapperFor(impl) && parent.equals(p.parent);
            }
            return false;
        }

        /**
         * Returns whether this identifier is a wrapper for the given PROJ identifier.
         *
         * @param  other  pointer to the PROJ identifier.
         * @return whether this object is (indirectly) a wrapper for the given identifier.
         */
        private boolean wrapperFor(final SharedPointer other) {
            return impl.rawPointer() == other.rawPointer();
        }

        /**
         * Returns a string representation of this identifier.
         *
         * @return a pseudo-WKT for this identifier.
         */
        @Override
        public String toString() {
            return format(this);
        }
    }

    /**
     * Returns a string representation of this identifier.
     *
     * @return a pseudo-WKT for this identifier.
     */
    @Override
    public String toString() {
        return format(this);
    }

    /**
     * Returns a string representation of the given identifier.
     *
     * @param  id  the identifier for which to get string representation.
     * @return a pseudo-WKT for the given identifier.
     */
    private static String format(final ReferenceIdentifier id) {
        final StringBuilder buffer = new StringBuilder("ID[\"");
        String t = id.getCodeSpace();
        if (t != null) {
            buffer.append(t).append(':');
        }
        t = id.getCode();
        if (t != null) {
            buffer.append(t);
        }
        return buffer.append("\"]").toString();
    }
}
