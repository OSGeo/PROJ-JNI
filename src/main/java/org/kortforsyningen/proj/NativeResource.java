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

import java.net.URL;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.lang.annotation.Native;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.opengis.util.FactoryException;


/**
 * Base class of all objects having a reference to a native resource.
 * The resource is referenced in a pointer of type {@code long} and named {@code "ptr"}.
 * The field name matter, since native code searches for a field having exactly that name.
 * It is subclass or caller responsibility to manage the release of the resource referenced
 * by {@link #ptr} when no longer referenced.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
abstract class NativeResource {
    /**
     * Name of the logger to use for all warnings or debug messages emitted by this package.
     */
    static final String LOGGER_NAME = "org.kortforsyningen.proj";

    /**
     * The message to provide in exception when a feature is not supported in PROJ,
     * or when we provide no mapping to it.
     */
    static final String UNSUPPORTED = "Not supported.";

    /**
     * The pointer to PROJ structure allocated in the C/C++ heap. This value has no meaning in Java code.
     * <strong>Do not modify</strong>, since this value is required for using PROJ. Do not rename neither,
     * unless usage of this field is also updated in the C/C++ source code.
     *
     * @see #initialize()
     */
    @Native
    private final long ptr;

    /**
     * Wraps the PROJ resource at the given address.
     * A null pointer is assumed caused by a failure to allocate memory from C/C++ code.
     *
     * @param  ptr  pointer to the PROJ resource, or 0 if out of memory.
     * @throws OutOfMemoryError if {@code ptr} is 0.
     */
    NativeResource(final long ptr) {
        this.ptr = ptr;
        if (ptr == 0) {
            throw new OutOfMemoryError("Can not allocate PROJ object.");
        }
    }

    /**
     * Returns the version number of the PROJ library.
     *
     * @return the PROJ release string.
     *
     * @see Proj#version()
     */
    static native String version();

    /**
     * Returns an absolute path to the Java Native Interface C/C++ code.
     * If the resources can not be accessed by an absolute path,
     * then this method copies the resource in a temporary file.
     *
     * @return absolute path to the library (may be a temporary file).
     * @throws URISyntaxException if an error occurred while creating a URI to the native file.
     * @throws IOException if an error occurred while copying the library to a temporary file.
     * @throws SecurityException if the security manager denies loading resource, creating absolute path, <i>etc</i>.
     * @throws UnsatisfiedLinkError if no native resource has been found for the current OS.
     *
     * @see System#load(String)
     */
    private static Path libraryPath() throws URISyntaxException, IOException {
        final String os = System.getProperty("os.name");
        final String libdir, suffix;
        if (os.contains("Windows")) {
            libdir = "windows";
            suffix = "dll";
        } else if (os.contains("Mac OS")) {
            libdir = "darwin";
            suffix = "so";
        } else if (os.contains("Linux")) {
            libdir = "linux";
            suffix = "so";
        } else {
            throw new UnsatisfiedLinkError("Unsupported operating system: " + os);
        }
        /*
         * If the native file is inside the JAR file, we need to extract it to a temporary file.
         * That file will be deleted on JVM exists, so a new file will be copied every time the
         * application is executed.
         *
         * Example of URL for a JAR entry: jar:file:/home/…/proj.jar!/org/…/NativeResource.class
         */
        final String nativeFile = libdir + "/libproj-binding." + suffix;
        final URL res = NativeResource.class.getResource(nativeFile);
        if (res == null) {
            throw new UnsatisfiedLinkError("Missing native file: " + nativeFile);
        }
        final Path location;
        if ("jar".equals(res.getProtocol())) {
            location = Files.createTempFile("libproj-binding", suffix);
            location.toFile().deleteOnExit();
            try (InputStream in = res.openStream()) {
                Files.copy(in, location, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            location = Paths.get(res.toURI());
        }
        return location;
    }

    /**
     * Loads the native library. If this initialization fails, an {@link UnsatisfiedLinkError} is thrown.
     * This will usually cause the application to terminate, but if that error was caught then subsequent
     * attempts to use this class will result in {@link NoClassDefFoundError}.
     */
    static {
        final String path;
        try {
            path = libraryPath().toAbsolutePath().toString();
        } catch (URISyntaxException | IOException e) {
            throw (UnsatisfiedLinkError) new UnsatisfiedLinkError("Can not get path to native file.").initCause(e);
        }
        /*
         * The AccessController is used for loading native code in a security constrained environment.
         * It has no effect on the common case where no security manager is enforced. We must promise
         * to not use any user-supplied parameter in the privileged block.
         */
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            System.load(path);
            return null;
        });
        initialize();
    }

    /**
     * Invoked at class initialization time for setting global variables in native code.
     * The native code caches an identifier used to access the {@link #ptr} field.
     * If this operation fails, then {@link NativeResource} class initialization should
     * fail since attempt to use native method in that class may cause JVM crash.
     *
     * @throws NoSuchFieldError if this method can not find the {@link #ptr} field identifier.
     *         Such error would be a programming error in PROJ-JNI rather than a user error.
     */
    private static native void initialize();

    /**
     * Verifies if a wrapper already exists for the given object. This method is invoked by native code;
     * it shall not be moved or have method signature modified unless the C++ binding is updated accordingly.
     * If an exception is thrown in this Java method, the native method will release the memory allocated
     * for {@code rawPointer}.
     *
     * @param  rawPointer  raw (non-shared) pointer to the PROJ object. Used for identification only.
     * @return existing wrapper for the given object, or {@code null} if none.
     */
    private IdentifiableObject findWrapper(final long rawPointer) {
        return SharedObjects.CACHE.get(rawPointer);
    }

    /**
     * Creates an object of the given type. This method is invoked by native code; it shall not be moved,
     * renamed or have method signature modified unless the C++ bindings are updated accordingly.
     * If an exception is thrown in this Java method, the native method will release the memory allocated
     * for {@code ptr}.
     *
     * @param  type  one of the {@link Type#COORDINATE_REFERENCE_SYSTEM}, <i>etc.</i> constants.
     * @param  ptr   pointer to the shared pointer for the object allocated by PROJ.
     * @return the Java object wrapping the PROJ object.
     * @throws FactoryException if the given type is not recognized.
     */
    private IdentifiableObject wrapGeodeticObject(final short type, final long ptr) throws FactoryException {
        final org.kortforsyningen.proj.IdentifiableObject obj;
        switch (type) {
            case Type.IDENTIFIER:                  obj = new ObjectIdentifier        (ptr); break;
            case Type.AXIS:                        obj = new Axis                    (ptr); break;
            case Type.COORDINATE_SYSTEM:           obj = new CS                      (ptr); break;
            case Type.CARTESIAN_CS:                obj = new CS.Cartesian            (ptr); break;
            case Type.SPHERICAL_CS:                obj = new CS.Spherical            (ptr); break;
            case Type.ELLIPSOIDAL_CS:              obj = new CS.Ellipsoidal          (ptr); break;
            case Type.VERTICAL_CS:                 obj = new CS.Vertical             (ptr); break;
            case Type.TEMPORAL_CS:                 obj = new CS.Time                 (ptr); break;
            case Type.DATUM:                       obj = new Datum                   (ptr); break;
            case Type.GEODETIC_REFERENCE_FRAME:    obj = new Datum.Geodetic          (ptr); break;
            case Type.VERTICAL_REFERENCE_FRAME:    obj = new Datum.Vertical          (ptr); break;
            case Type.TEMPORAL_DATUM:              obj = new Datum.Temporal          (ptr); break;
            case Type.ENGINEERING_DATUM:           obj = new Datum.Engineering       (ptr); break;
            case Type.ELLIPSOID:                   obj = new Datum.Ellipsoid         (ptr); break;
            case Type.PRIME_MERIDIAN:              obj = new Datum.PrimeMeridian     (ptr); break;
            case Type.COORDINATE_REFERENCE_SYSTEM: obj = new CRS                     (ptr); break;
            case Type.GEODETIC_CRS:                obj = new CRS.Geodetic            (ptr); break;
            case Type.GEOGRAPHIC_CRS:              obj = new CRS.Geographic          (ptr); break;
            case Type.GEOCENTRIC_CRS:              obj = new CRS.Geocentric          (ptr); break;
            case Type.PROJECTED_CRS:               obj = new CRS.Projected           (ptr); break;
            case Type.VERTICAL_CRS:                obj = new CRS.Vertical            (ptr); break;
            case Type.TEMPORAL_CRS:                obj = new CRS.Temporal            (ptr); break;
            case Type.ENGINEERING_CRS:             obj = new CRS.Engineering         (ptr); break;
            case Type.COMPOUND_CRS:                obj = new CRS.Compound            (ptr); break;
            case Type.COORDINATE_OPERATION:        obj = new Operation               (ptr); break;
            case Type.OPERATION_METHOD:            obj = new Operation.Method        (ptr); break;
            case Type.CONVERSION:                  obj = new Operation.Conversion    (ptr); break;
            case Type.TRANSFORMATION:              obj = new Operation.Transformation(ptr); break;
            default: throw new FactoryException("Unknown object type.");
        }
        return obj.releaseWhenUnreachable();
    }

    /**
     * Invoked by native code for getting the logger where to send a message.
     * If this method is renamed, then the native C++ code needs to be updated accordingly.
     *
     * @return the logger.
     */
    static System.Logger logger() {
        return System.getLogger(LOGGER_NAME);
    }
}
