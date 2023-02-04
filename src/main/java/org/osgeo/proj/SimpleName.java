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

import java.util.List;
import java.util.Collections;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.LocalName;
import org.opengis.util.NameSpace;
import org.opengis.util.ScopedName;


/**
 * Simple implementation of {@link LocalName} for storing CRS aliases.
 * This class provides the most trivial implementation of every {@link LocalName} methods,
 * making this class merely a container for a {@link String}. Other libraries may provide
 * more sophisticated implementations behaving like a path in a file directory structure.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
class SimpleName implements LocalName, NameSpace {
    /**
     * The name.
     */
    final String name;

    /**
     * Creates a new name.
     *
     * @param  name  the name.
     */
    SimpleName(final String name) {
        this.name = name;
    }

    /**
     * Returns always 1 for a local name.
     *
     * @return always 1 for a local name.
     */
    @Override
    public final int depth() {
        return 1;
    }

    /**
     * Returns a singleton containing only {@code this}, since this name is itself a local name.
     *
     * @return a {@linkplain Collections#singletonList singleton} containing only {@code this}.
     */
    @Override
    public final List<? extends LocalName> getParsedNames() {
        return Collections.singletonList(this);
    }

    /**
     * Returns {@code this} since this object is already a local name.
     */
    @Override
    public final LocalName head() {
        return this;
    }

    /**
     * Returns {@code this} since this object is already a local name.
     */
    @Override
    public final LocalName tip() {
        return this;
    }

    /**
     * Returns {@code this} since the {@linkplain #scope() scope} of this name
     * is already {@linkplain NameSpace#isGlobal() global}.
     *
     * @return the fully-qualified name (never {@code null}).
     */
    @Override
    public GenericName toFullyQualifiedName() {
        return this;
    }

    /**
     * Returns the scope (name space) in which this name is local.
     * Current implementation assumes that all names are local in the global scope.
     *
     * @return the scope of this name.
     */
    @Override
    public final NameSpace scope() {
        return this;
    }

    /**
     * Indicates whether this namespace is a "top level" namespace.  Global, or top-level
     * namespaces are not contained within another namespace. The global namespace has no
     * parent.
     *
     * @return {@code true} if this namespace is the global namespace.
     */
    @Override
    public boolean isGlobal() {
        return true;
    }

    /**
     * Represents the identifier of this namespace.
     *
     * @return the identifier of this namespace.
     */
    @Override
    public GenericName name() {
        return null;
    }

    /**
     * Unsupported operation. If supported, this method would have expanded this name with the specified scope.
     * One may represent this operation as a concatenation of the specified {@code scope} with {@code this}.
     *
     * @param  scope  the name to use as prefix.
     * @return a concatenation of the given scope with this name.
      */
    @Override
    public final ScopedName push(GenericName scope) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Compares this name with an other name for order.
     *
     * @param  other  the other object to be compared to this name.
     * @return a negative integer, zero, or a positive integer as this name is lexicographically
     *         less than, equal to, or greater than the specified name.
     */
    @Override
    public final int compareTo(final GenericName other) {
        return name.compareTo(other.toString());
    }

    /**
     * Returns {@code true} if the other object is also a {@link SimpleName} with the same name.
     *
     * @param  other  the other object to compare with this name.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        return (other != null) && getClass().equals(other.getClass()) && name.equals(((SimpleName) other).name);
    }

    /**
     * Returns a hash code value for this name.
     *
     * @return  an arbitrary hash code value.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ 1793790372;
    }

    /**
     * Returns a locale-independent string representation of this local name.
     *
     * @return the local-independent string representation of this name.
     */
    @Override
    public final String toString() {
        return name;
    }

    /**
     * Returns a local-dependent string representation of this generic name.
     * Current implementation supports only one locale, the default one.
     *
     * @return a localizable string representation of this name.
     */
    @Override
    public final InternationalString toInternationalString() {
        return new SimpleCitation(name);
    }
}
