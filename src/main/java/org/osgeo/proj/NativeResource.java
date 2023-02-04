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

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.lang.annotation.Native;
import java.util.logging.Logger;
import org.opengis.util.FactoryException;
import javax.measure.Unit;


/**
 * Base class of all objects having a reference to a native resource.
 * The resource is referenced in a pointer of type {@code long} and named {@code "ptr"}.
 * The field name matter, since native code searches for a field having exactly that name.
 * It is subclass or caller responsibility to manage the release of the resource referenced
 * by {@link #ptr} when no longer referenced.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
abstract class NativeResource {
    /**
     * Name of the native library, without the {@code "lib"} prefix used on Unix system
     * and without the platform-specific suffix.
     */
    private static final String NATIVE_LIB = "proj-binding";

    /**
     * Name of the logger to use for all warnings or debug messages emitted by this package.
     */
    static final String LOGGER_NAME = "org.osgeo.proj";

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
     * Creates a new instance with no native resource allocated on the heap.
     * This constructor is used when the native resource consists in calls to static C/C++ functions only.
     */
    NativeResource() {
        ptr = 0;
    }

    /**
     * Wraps the PROJ resource at the given address.
     * A null pointer is assumed caused by a failure to allocate the PROJ object from C/C++ code
     * (not necessarily because out of memory).
     *
     * @param  ptr  pointer to the PROJ resource, or 0 if it can not be allocated.
     * @throws FactoryException if {@code ptr} is 0.
     */
    NativeResource(final long ptr) throws FactoryException {
        this.ptr = ptr;
        if (ptr == 0) {
            throw new FactoryException("Can not allocate PROJ object.");
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
        final String prefix, suffix;
        if (os.contains("Windows")) {
            prefix = "";
            suffix = ".dll";
        } else if (os.contains("Mac OS")) {
            prefix = "lib";
            suffix = ".dylib";
        } else {
            prefix = "lib";
            suffix = ".so";
        }
        /*
         * The resource may be a URL to an ordinary file (typically in the Maven "target/classes" directory)
         * or a path to an entry inside the JAR file. The later case can be recognized by its "jar" protocol
         * like this:
         *
         *      jar:file:/home/…/proj.jar!/org/…/libproj-binding.so
         *
         * The URL can be used directly only if it is an ordinary file (without "jar" protocol).
         */
        final String nativeFile = prefix + NATIVE_LIB + suffix;
        final URL res = NativeResource.class.getResource(nativeFile);
        if (res == null) {
            throw new UnsatisfiedLinkError("Missing native file: " + nativeFile);
        }
        if (!"jar".equals(res.getProtocol()) && !"bundleresource".equals(res.getProtocol())) {
           return Paths.get(res.toURI());
        }
        /*
         * The native file is inside the JAR file. We need to extract it somewhere on the file system.
         * If we can locate the directory containing JAR file and if we have write permission for it,
         * we will copy the native file there so we can reuse it next time the application is launched.
         */
        Path location = null;
        String file = res.getPath();
        final int s = file.indexOf('!');
        if (s >= 0) {
            location = Paths.get(new URI(file.substring(0, s)));
            final Path directory = location.getParent();
            location = directory.resolve(nativeFile);
            if (Files.isReadable(location)) {
                return location;
            }
            if (Files.exists(location) || !Files.isWritable(directory)) {
                location = null;
            }
        }
        /*
         * If we can not copy the native library close to the JAR file, copy in a temporary file.
         * That file will be deleted on JVM exists, so a new file will be copied every time the
         * application is launched.
         */
        if (location == null) {
            location = Files.createTempFile(prefix + NATIVE_LIB, suffix);
            location.toFile().deleteOnExit();
        }
        try (InputStream in = res.openStream()) {
            Files.copy(in, location, StandardCopyOption.REPLACE_EXISTING);
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
        System.load(path);
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
     * for {@code rawPointer}.
     *
     * @param  type        one of the {@link Type#COORDINATE_REFERENCE_SYSTEM}, <i>etc.</i> constants.
     * @param  rawPointer  pointer to the shared pointer for the object allocated by PROJ.
     * @return the Java object wrapping the PROJ object.
     * @throws FactoryException if the given type is not recognized.
     */
    private IdentifiableObject wrapGeodeticObject(final short type, final long rawPointer) throws FactoryException {
        final org.osgeo.proj.IdentifiableObject obj;
        switch (type) {
            case Type.IDENTIFIER:                  obj = new ObjectIdentifier        (rawPointer); break;
            case Type.AXIS:                        obj = new Axis                    (rawPointer); break;
            case Type.COORDINATE_SYSTEM:           obj = new CS                      (rawPointer); break;
            case Type.CARTESIAN_CS:                obj = new CS.Cartesian            (rawPointer); break;
            case Type.SPHERICAL_CS:                obj = new CS.Spherical            (rawPointer); break;
            case Type.ELLIPSOIDAL_CS:              obj = new CS.Ellipsoidal          (rawPointer); break;
            case Type.VERTICAL_CS:                 obj = new CS.Vertical             (rawPointer); break;
            case Type.TEMPORAL_CS:                 obj = new CS.Time                 (rawPointer); break;
            case Type.DATUM:                       obj = new Datum                   (rawPointer); break;
            case Type.GEODETIC_REFERENCE_FRAME:    obj = new Datum.Geodetic          (rawPointer); break;
            case Type.VERTICAL_REFERENCE_FRAME:    obj = new Datum.Vertical          (rawPointer); break;
            case Type.TEMPORAL_DATUM:              obj = new Datum.Temporal          (rawPointer); break;
            case Type.ENGINEERING_DATUM:           obj = new Datum.Engineering       (rawPointer); break;
            case Type.ELLIPSOID:                   obj = new Datum.Ellipsoid         (rawPointer); break;
            case Type.PRIME_MERIDIAN:              obj = new Datum.PrimeMeridian     (rawPointer); break;
            case Type.COORDINATE_REFERENCE_SYSTEM: obj = new CRS                     (rawPointer); break;
            case Type.GEODETIC_CRS:                obj = new CRS.Geodetic            (rawPointer); break;
            case Type.GEOGRAPHIC_CRS:              obj = new CRS.Geographic          (rawPointer); break;
            case Type.GEOCENTRIC_CRS:              obj = new CRS.Geocentric          (rawPointer); break;
            case Type.PROJECTED_CRS:               obj = new CRS.Projected           (rawPointer); break;
            case Type.VERTICAL_CRS:                obj = new CRS.Vertical            (rawPointer); break;
            case Type.TEMPORAL_CRS:                obj = new CRS.Temporal            (rawPointer); break;
            case Type.ENGINEERING_CRS:             obj = new CRS.Engineering         (rawPointer); break;
            case Type.COMPOUND_CRS:                obj = new CRS.Compound            (rawPointer); break;
            case Type.COORDINATE_OPERATION:        obj = new Operation               (rawPointer); break;
            case Type.OPERATION_METHOD:            obj = new Operation.Method        (rawPointer); break;
            case Type.CONVERSION:                  obj = new Operation.Conversion    (rawPointer); break;
            case Type.TRANSFORMATION:              obj = new Operation.Transformation(rawPointer); break;
            case Type.PARAMETER:                   obj = new Parameter               (rawPointer); break;
            case Type.PARAMETER_VALUE:             obj = new Parameter.Value         (rawPointer); break;
            default: throw new FactoryException("Unknown object type.");
        }
        return obj.releaseWhenUnreachable();
    }

    /**
     * Returns a unit of the given type with the given scale factor, or {@code null} if the caller
     * should instantiate an {@link UnitOfMeasure} itself. This method is invoked from native code;
     * it shall not be moved, renamed or have method signature modified unless the C++ bindings are
     * updated accordingly.
     *
     * @param  ordinal  ordinal value of the {@link UnitType}.
     * @param  scale    scale factor from the desired unit to its system unit.
     * @return the unit, or {@code null} if caller should instantiate {@link UnitOfMeasure} itself.
     *
     * @see UnitType#getUserDefinedTypeAndScale(int)
     */
    private static Unit<?> getPredefinedUnit(final int ordinal, final double scale) {
        return UnitType.forOrdinal(ordinal).getPredefinedUnit(scale);
    }

    /**
     * Invoked by native code for getting the logger where to send a message.
     * If this method is renamed, then the native C++ code needs to be updated accordingly.
     *
     * @return the logger.
     */
    static Logger logger() {
        return Logger.getLogger(LOGGER_NAME);
    }
}
