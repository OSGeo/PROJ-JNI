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

import java.util.StringJoiner;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ReferenceIdentifier;


/**
 * An implementation of {@link Identifier} as a wrapper
 * around an {@code osgeo::proj::metadata::Identifier}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class ObjectIdentifier extends IdentifiableObject implements ReferenceIdentifier {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::metadata::Identifier}.
     *
     * @param  ptr  pointer to the PROJ structure to wrap.
     * @throws OutOfMemoryError if {@code ptr} is 0.
     */
    ObjectIdentifier(final long ptr) {
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
}
