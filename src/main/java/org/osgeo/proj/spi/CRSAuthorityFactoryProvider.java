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
package org.osgeo.proj.spi;

import java.util.Set;
import org.osgeo.proj.Proj;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;


/**
 * Provider for {@link CRSAuthorityFactory}.
 * This is a temporary class, to be removed after use Jigsaw modularisation
 * in which case the public static {@code provider()} methods in subclasses
 * should be sufficient.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 *
 * @see <a href="https://github.com/OSGeo/PROJ-JNI/issues/15">Issue #15</a>
 */
class CRSAuthorityFactoryProvider implements CRSAuthorityFactory {
    /**
     * The implementation where to delegate all operations.
     */
    private final CRSAuthorityFactory impl;

    /**
     * Creates a new provider.
     *
     * @param authority the authority for which to create a factory.
     */
    CRSAuthorityFactoryProvider(final String authority) {
        impl = Proj.getAuthorityFactory(authority);
    }

    @Override
    public Citation getVendor() {
        return impl.getVendor();
    }

    @Override
    public Citation getAuthority() {
        return impl.getAuthority();
    }

    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type) throws FactoryException {
        return impl.getAuthorityCodes(type);
    }

    @Override
    public InternationalString getDescriptionText(String code) throws FactoryException {
        return impl.getDescriptionText(code);
    }

    @Override
    public IdentifiedObject createObject(String code) throws FactoryException {
        return impl.createObject(code);
    }

    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(String code) throws FactoryException {
        return impl.createCoordinateReferenceSystem(code);
    }

    @Override
    public CompoundCRS createCompoundCRS(String code) throws FactoryException {
        return impl.createCompoundCRS(code);
    }

    @Override
    public DerivedCRS createDerivedCRS(String code) throws FactoryException {
        return impl.createDerivedCRS(code);
    }

    @Override
    public EngineeringCRS createEngineeringCRS(String code) throws FactoryException {
        return impl.createEngineeringCRS(code);
    }

    @Override
    public GeographicCRS createGeographicCRS(String code) throws FactoryException {
        return impl.createGeographicCRS(code);
    }

    @Override
    public GeocentricCRS createGeocentricCRS(String code) throws FactoryException {
        return impl.createGeocentricCRS(code);
    }

    @Override
    public ImageCRS createImageCRS(String code) throws FactoryException {
        return impl.createImageCRS(code);
    }

    @Override
    public ProjectedCRS createProjectedCRS(String code) throws FactoryException {
        return impl.createProjectedCRS(code);
    }

    @Override
    public TemporalCRS createTemporalCRS(String code) throws FactoryException {
        return impl.createTemporalCRS(code);
    }

    @Override
    public VerticalCRS createVerticalCRS(String code) throws FactoryException {
        return impl.createVerticalCRS(code);
    }
}
