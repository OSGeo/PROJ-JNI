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
import java.util.Objects;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;


/**
 * Base class of PROJ objects which provide parameter descriptors or values.
 * The wrapped PROJ object is not necessarily a parameter group, but may be
 * a {@code CoordinateOperation} or an {@code OperationMethod}.
 *
 * <p>This class provides all methods for {@link ParameterDescriptorGroup}
 * and {@link ParameterValueGroup} but does not implement those interfaces.
 * It is subclass responsibility to implement the interfaces relevant to them.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
abstract class ParameterGroup extends IdentifiableObject {
    /**
     * Creates a new wrapper for the group of parameters included in the wrapped object.
     *
     * @param  ptr  pointer to the wrapped PROJ object.
     * @throws FactoryException if {@code ptr} is 0.
     */
    ParameterGroup(final long ptr) throws FactoryException {
        super(ptr);
    }

    /**
     * For {@link Operation} constructor only.
     *
     * @param  ptr  wrapper to a pointer to the PROJ structure.
     */
    ParameterGroup(final SharedPointer ptr) {
        super(ptr);
    }

    /**
     * The property to request for getting parameters.
     * This is the value of the fist argument to be given to
     * {@link SharedPointer#getVectorElement(short, int)}.
     * This property depends on the wrapped object type.
     *
     * @return {@link Property#OPERATION_PARAMETER} or {@link Property#METHOD_PARAMETER}.
     */
    abstract short parameterProperty();

    /**
     * The minimum number of times that values for this parameter group or parameter are required.
     *
     * @return fixed to 1.
     */
    public final int getMinimumOccurs() {
        return 1;
    }

    /**
     * The maximum number of times that values for this parameter group or parameter can be included.
     *
     * @return fixed to 1.
     */
    public final int getMaximumOccurs() {
        return 1;
    }

    /**
     * Returns a description of all parameters in this group.
     *
     * @return list of parameters provided by the wrapped operation or method.
     */
    public final List<GeneralParameterDescriptor> descriptors() {
        return new PropertyList<>(GeneralParameterDescriptor.class, parameterProperty());
    }

    /**
     * Returns the values of all parameters in this group.
     *
     * @return list of parameters provided by the wrapped operation or method.
     */
    public final List<GeneralParameterValue> values() {
        return new PropertyList<>(GeneralParameterValue.class, parameterProperty());
    }

    /**
     * Returns the parameter descriptor in this group for the specified name or code.
     *
     * @param  name  the name of identifier of the parameter to search for.
     * @return the parameter for the given identifier code.
     * @throws ParameterNotFoundException if there is no parameter for the given identifier.
     */
    public final GeneralParameterDescriptor descriptor(final String name) {
        return (GeneralParameterDescriptor) impl.searchVectorElement(parameterProperty(), Objects.requireNonNull(name));
    }

    /**
     * Returns the parameter value in this group for the specified name or code.
     *
     * @param  name  the name of identifier of the parameter to search for.
     * @return the parameter for the given identifier code.
     * @throws ParameterNotFoundException if there is no parameter for the given identifier.
     */
    public final ParameterValue<?> parameter(final String name) {
        return (ParameterValue<?>) impl.searchVectorElement(parameterProperty(), Objects.requireNonNull(name));
    }

    /**
     * Returns all subgroups with the specified name.
     *
     * @param  name  the name or identifier of the parameter group to search for.
     * @return the set of all parameter group for the given identifier code.
     * @throws ParameterNotFoundException if no group was found for the given name.
     */
    public final List<ParameterValueGroup> groups(final String name) {
        throw new ParameterNotFoundException("No parameter subgroup.", name);
    }

    /**
     * Creates a new subgroup of the specified name, and adds it to the list of subgroups.
     *
     * @param  name  the name or code of the parameter group to create.
     * @return a newly created parameter group for the given identifier code.
     * @throws ParameterNotFoundException if no descriptor is found for the given name.
     */
    public final ParameterValueGroup addGroup(final String name) {
        throw new ParameterNotFoundException("No parameter subgroup.", name);
    }

    /**
     * Creates a modifiable parameter group with initially no value.
     *
     * @return a modifiable parameter group.
     */
    public final ParameterValueGroup createValue() {
        throw new UnsupportedOperationException();      // TODO
    }

    /**
     * Returns a copy of this parameter group that user can modify.
     *
     * @return a modifiable copy of this parameter group.
     */
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final ParameterValueGroup clone() {
        final ParameterValueGroup group = createValue();
        // TODO: set all values.
        return group;
    }
}
