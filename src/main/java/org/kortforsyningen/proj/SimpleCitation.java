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
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Series;


/**
 * A {@link Citation} containing only a title attribute as an {@link InternationalString}.
 * All other citation attributes are {@code null} or empty collections.
 *
 * <p>This class can also be used as an {@link InternationalString} implementation. Because
 * there is only one attribute - the {@linkplain #getTitle() title} - there is no ambiguity
 * about the value represented by the citation or the international string.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class SimpleCitation implements Citation, InternationalString {
    /**
     * Returns a citation for the PROJ project.
     * {@link Citation#getEdition()} contains the PROJ version string.
     *
     * @return a citation for "PROJ".
     */
    static Citation PROJ() {
        return new SimpleCitation("PROJ") {
            @Override public InternationalString getEdition() {
                try {
                    final String version = NativeResource.version();
                    if (version != null) {
                        return new SimpleCitation(version);
                    }
                } catch (UnsatisfiedLinkError e) {
                    // Ignore.
                }
                return null;
            }
        };
    }

    /**
     * The citation title to be returned by {@link #getTitle()} as an {@link InternationalString}.
     * This is also the value returned by the {@code InternationalString} methods like
     * {@link #toString(Locale)} and {@link #toString()}.
     *
     * @see #getTitle()
     * @see #toString()
     * @see #toString(Locale)
     */
    private final String title;

    /**
     * Creates a new citation having the given title. The given string will be returned,
     * directly or indirectly, by the {@link #getTitle()}, {@link #toString()} and
     * {@link #toString(Locale)} methods.
     *
     * @param title  the citation title to be returned indirectly by {@link #getTitle()}.
     */
    SimpleCitation(final String title) {
        Objects.requireNonNull(title);
        this.title = title;
    }

    /**
     * Returns the number of characters in the {@linkplain #getTitle() title}.
     *
     * @return the number of {@code char}s in the {@linkplain #getTitle() title}.
     */
    @Override
    public int length() {
        return title.length();
    }

    /**
     * Returns the {@linkplain #getTitle() title} character at the given index
     *
     * @param  index  the index of the {@code char} value to be returned.
     * @return the specified {@code char} value.
     */
    @Override
    public char charAt(final int index) {
        return title.charAt(index);
    }

    /**
     * Returns a {@linkplain String#substring(int, int) substring} of the title
     * for the given range of index.
     *
     * @param  start  the start index, inclusive.
     * @param  end    the end index, exclusive.
     * @return the specified substring of the {@linkplain #getTitle() title}.
     */
    @Override
    public String subSequence(final int start, final int end) {
        return title.substring(start, end);
    }

    /**
     * Returns the {@linkplain #getTitle() title} as an unlocalized string.
     * This method returns directly the string given to the constructor.
     *
     * @return the string given to the constructor.
     *
     * @see #getTitle()
     */
    @Override
    public String toString() {
        return title;
    }

    /**
     * Returns the {@linkplain #getTitle() title}, ignoring the given locale. For localization
     * support, an other class (or a subclass of this {@code SimpleCitation} class) is required.
     *
     * @param  locale  ignored by the {@code SimpleCitation} implementation.
     * @return the string given to the constructor, localized if supported by the implementation.
     *
     * @see #getTitle()
     */
    @Override
    public String toString(final Locale locale) {
        return title;
    }

    /*
     * Returns the citation title, which is represented directly by {@code this} implementation class.
     * This is the only {@link Citation} method in this class returning a non-null and non-empty value.
     *
     * @see #toString()
     */
    @Override
    public InternationalString getTitle() {
        return this;
    }

    /**
     * Short names or other language names by which the cited information is known.
     * This implementation has none.
     *
     * @return an empty set.
     */
    @Override
    public Collection<? extends InternationalString> getAlternateTitles() {
        return Collections.emptySet();
    }

    /**
     * Common title with holdings note. This implementation has none.
     *
     * @return null.
     */
    @Override
    @Deprecated
    public InternationalString getCollectiveTitle() {
        return null;
    }

    /**
     * Reference dates for the cited resource. This implementation has none.
     *
     * @return an empty set.
     */
    @Override
    public Collection<? extends CitationDate> getDates() {
        return Collections.emptySet();
    }

    /**
     * Version of the cited resource.
     *
     * @return default to null.
     */
    @Override
    public InternationalString getEdition() {
        return null;
    }

    /**
     * Date of the edition. This implementation has none.
     *
     * @return null.
     */
    @Override
    public Date getEditionDate() {
        return null;
    }

    /**
     * Unique identifier for the resource. This implementation has none.
     *
     * @return an empty set.
     */
    @Override
    public Collection<? extends Identifier> getIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * Role, name, contact and position information for individuals or organizations
     * that are responsible for the resource. This implementation has none.
     *
     * @return an empty set.
     */
    @Override
    public Collection<? extends ResponsibleParty> getCitedResponsibleParties() {
        return Collections.emptySet();
    }

    /**
     * Mode in which the resource is represented.
     *
     * @return default to an empty set.
     */
    @Override
    public Collection<PresentationForm> getPresentationForms() {
        return Collections.emptySet();
    }

    /**
     * Information about the series, or aggregate dataset, of which the dataset is a part.
     * This implementation has none.
     *
     * @return null.
     */
    @Override
    public Series getSeries() {
        return null;
    }

    /**
     * Other information required to complete the citation that is not recorded elsewhere.
     * This implementation has none.
     *
     * @return null.
     */
    @Override
    public InternationalString getOtherCitationDetails() {
        return null;
    }

    /**
     * International Standard Book Number. This implementation has none.
     *
     * @return null.
     */
    @Override
    public String getISBN() {
        return null;
    }

    /**
     * International Standard Serial Number. This implementation has none.
     *
     * @return null.
     */
    @Override
    public String getISSN() {
        return null;
    }

    /**
     * Compares the {@linkplain #getTitle() title} with the string representation
     * of the given object for order.
     *
     * @param   object  the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(final InternationalString object) {
        return title.compareTo(object.toString());
    }

    /**
     * Returns {@code true} if the given object is a {@code SimpleCitation} having
     * a title equals to this title.
     *
     * @param  object  the object to compare with this {@code SimpleCitation}, or {@code null}.
     * @return {@code true} if the given object is equals to this {@code SimpleCitation}.
     */
    @Override
    public boolean equals(final Object object) {
        return (object instanceof SimpleCitation) && title.equals(((SimpleCitation) object).title);
    }

    /**
     * Returns a hash code value for this citation. The hash code is computed
     * from the {@linkplain #getTitle() title} string given to the constructor.
     *
     * @return a hash code value for this citation.
     */
    @Override
    public int hashCode() {
        return title.hashCode() ^ 573943196;
    }
}
