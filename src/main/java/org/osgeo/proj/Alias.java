/*
 * Copyright © 2019-2021 Agency for Data Supply and Efficiency
 * Copyright © 2021 Open Source Geospatial Foundation
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

import org.opengis.util.GenericName;


/**
 * A {@link GenericName} which is an item of a list provided by a larger object.
 * We do not wrap {@code osgeo::proj::util::GenericName} directly because in most
 * cases, only the {@link #toString()} method is of interest. If nevertheless the
 * user asks for another property, then we will fetch that information from parent.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Alias extends SimpleName {
    /**
     * The object which owns this name.
     */
    private final IdentifiableObject owner;

    /**
     * Index of the this item in the aliases vector.
     */
    private final int index;

    /**
     * Creates a new name which is an item of the specified object.
     *
     * @param  name   the name.
     * @param  owner  the object which owns this name.
     * @param  index  index of the this item in the aliases vector.
     */
    Alias(final String name, final IdentifiableObject owner, final int index) {
        super(name);
        this.owner = owner;
        this.index = index;
    }

    /**
     * Returns a property value as an string at the given index of a {@code std::vector}.
     *
     * @param  property  one of {@link Property#ALIAS_NS}, <i>etc.</i> values.
     * @return value of the specified property, or {@code null} if undefined.
     * @throws RuntimeException if the specified property does not exist for {@link #owner}.
     * @throws IndexOutOfBoundsException if {@link #index} is out of bounds.
     */
    private String getElement(final short property) {
        return (String) owner.impl.getVectorElement(property, index);
    }

    /**
     * If the fully-qualified name differs from this name, returns it.
     * Otherwise returns {@code this}.
     *
     * @return the fully-qualified name (never {@code null}).
     */
    @Override
    public GenericName toFullyQualifiedName() {
        final String qn = getElement(Property.FULLY_QUALIFIED);
        return name.equals(qn) ? this : new SimpleName(qn);
    }

    /**
     * Indicates whether this namespace is a "top level" namespace.
     *
     * @return {@code true} if this namespace is the global namespace.
     */
    @Override
    public boolean isGlobal() {
        return Boolean.parseBoolean(getElement(Property.ALIAS_NS_IS_GLOBAL));
    }

    /**
     * Represents the identifier of this namespace.
     *
     * @return the identifier of this namespace.
     */
    @Override
    public GenericName name() {
        final String ns = getElement(Property.ALIAS_NS);
        return (ns != null) ? new SimpleName(ns) : null;
    }

    /**
     * Returns {@code true} if the other object is also an {@link Alias} with the same name.
     *
     * @param  other  the other object to compare with this alias.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            final Alias that = (Alias) other;
            return index == that.index && owner.equals(that.owner);
        }
        return false;
    }

    /**
     * Returns a hash code value for this alias.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 31 * owner.hashCode();
    }
}
