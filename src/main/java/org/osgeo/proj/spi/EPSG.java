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
package org.osgeo.proj.spi;

import org.osgeo.proj.Proj;
import org.opengis.referencing.crs.CRSAuthorityFactory;


/**
 * Providers of {@link CRSAuthorityFactory} for EPSG authority.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
public final class EPSG extends CRSAuthorityFactoryProvider {
    /**
     * Creates a new provider.
     */
    public EPSG() {
        super("EPSG");
    }

    /**
     * Returns the EPSG authority factory. This method is invoked automatically by
     * {@link java.util.ServiceLoader} when needed and does not need to be invoked explicitly.
     *
     * @return EPSG authority factory.
     *
     * @see Proj#getAuthorityFactory(String)
     */
    public static CRSAuthorityFactory provider() {
        return Proj.getAuthorityFactory("EPSG");
    }
}
