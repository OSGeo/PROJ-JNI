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


/**
 * Thrown when a method can not execute because a given argument value is not a PROJ implementation.
 * This exception may happen when invoking a method with arguments declared as GeoAPI interfaces,
 * but that method can work only with values provided by this PROJ-JNI implementation.
 * In may happen in particular in the following contexts:
 *
 * <ul>
 *   <li>Methods expecting {@link org.opengis.referencing.crs.CoordinateReferenceSystem} argument.</li>
 *   <li>Any method from {@link org.opengis.referencing.crs.CRSFactory} or other component factory.</li>
 * </ul>
 *
 * This exception can happen only if PROJ-JNI is used together with another implementation of GeoAPI
 * interfaces. Not also that mixing implementations will not necessarily cause this exception;
 * it happens only if PROJ-JNI can not map the "foreigner" implementation to PROJ implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
public class UnsupportedImplementationException extends IllegalArgumentException {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -84175258452782989L;

    /**
     * Construct an exception with no detail message.
     */
    public UnsupportedImplementationException() {
    }

    /**
     * Constructs an exception with the specified detail message.
     *
     * @param message  the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public UnsupportedImplementationException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with a message for the specified argument value.
     *
     * @param  name   the argument name.
     * @param  value  the illegal argument value.
     */
    UnsupportedImplementationException(final String name, final Object value) {
        super(message(name, value));
    }

    /**
     * Formats an error message for an illegal argument. Example:
     * <cite>"Argument sourceCRS expects a PROJ implementation, but got an instance of Foo class."</cite>
     *
     * @param  name   argument name.
     * @param  value  argument value.
     * @return message to give to {@link IllegalArgumentException}.
     */
    private static String message(final String name, final Object value) {
        final StringBuilder message = new StringBuilder(100)
                .append("Argument ").append(name).append(" expects a PROJ implementation, but got ");
        if (value == null) {
            message.append("a null value.");
        } else {
            message.append("an instance of ").append(value.getClass().getSimpleName()).append(" class.");
        }
        return message.toString();
    }
}
