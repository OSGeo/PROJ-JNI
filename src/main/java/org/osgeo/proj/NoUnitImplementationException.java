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

import javax.measure.Unit;


/**
 * Thrown when an unit operation is requested but no JSR-385 implementation is found on the classpath.
 * The PROJ-JNI binding provides only very minimalist support of the {@link Unit} interface.
 * For advanced operations such as units arithmetic, a JSR-385 implementation must be provided.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 2.0
 *
 * @see Units
 *
 * @since 1.0
 */
public class NoUnitImplementationException extends UnsupportedOperationException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8591455304816250290L;

    /**
     * Construct an exception with a default message saying that a JSR-385 implementation should be provided.
     * Default message is: <cite>"The PROJ-JNI binding provides only minimal support for Unit of Measurement
     * operations. For more advanced operations, a JSR-385 implementation should be added to the classpath."</cite>
     */
    public NoUnitImplementationException() {
        super("The PROJ-JNI binding provides only minimal support for Unit of Measurement operations. "
            + "For more advanced operations, a JSR-385 implementation should be added to the classpath.");
    }

    /**
     * Constructs an exception for an unexpected unit implementation.
     *
     * @param  type  class of the unexpected {@link Unit} instance.
     */
    NoUnitImplementationException(final Class<?> type) {
        super("PROJ-JNI can not handle unit of class " + type.getSimpleName());
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public NoUnitImplementationException(String message) {
        super(message);
    }
}
