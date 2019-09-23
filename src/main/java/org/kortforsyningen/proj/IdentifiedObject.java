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
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Base class of all objects wrapping a PROJ {@code osgeo::proj::common::IdentifiedObject}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
abstract class IdentifiedObject extends ObjectReference implements org.opengis.referencing.IdentifiedObject {
    /**
     * Creates a new wrapper for the given {@code osgeo::proj::common::IdentifiedObject}.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     */
    IdentifiedObject(final long ptr) {
        super(ptr, true);
    }

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return the primary name.
     * @throws UnsupportedOperationException if this object does not provide a name.
     */
    @Override
    public ReferenceIdentifier getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns alternative names by which this object is identified.
     *
     * @return alternative names and abbreviations, or an empty collection if there is none.
     */
    @Override
    public Collection<GenericName> getAlias() {
        return Collections.emptySet();
    }

    /**
     * Returns identifiers which reference elsewhere the object's defining information.
     *
     * @return this object identifiers, or an empty collection if there is none.
     */
    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * Returns comments on or information about this object, including data source information.
     *
     * @return the remarks, or {@code null} if none.
     */
    @Override
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
     */
    @Override
    public String toWKT() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
