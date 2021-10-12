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


/**
 * Source of factories used for the tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class TestFactorySource {
    /**
     * The EPSG authority factory to use for testing creation from EPSG codes.
     */
    static final AuthorityFactory.API EPSG = new AuthorityFactory.API("EPSG");

    /**
     * The factory to use for testing component creations.
     */
    static final ObjectFactory OBJECTS = ObjectFactory.INSTANCE;

    /**
     * The factory to use for testing operation creations.
     */
    static final OperationFactory OPERATIONS = new OperationFactory(null);

    /**
     * Do not allow instantiation of this class.
     */
    private TestFactorySource() {
    }

    /**
     * If the name of an object was different in a previous version of EPSG database,
     * returns the previous name expected by GIGS tests. Those renaming are temporary,
     * until we update GIGS tests for the new names.
     *
     * @param  name  the name selected by PROJ.
     * @return the name to compare against expected values.
     */
    static String renameToPreviousVersion(String name) {
        if ("Camacupa 1948".equals(name)) {
            name = "Camacupa";
        }
        return name;
    }
}
