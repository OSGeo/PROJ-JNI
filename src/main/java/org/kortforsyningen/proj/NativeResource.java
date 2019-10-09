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


/**
 * Base class of all objects having a reference to a native resource.
 * The resource is referenced in a pointer of type {@code long} and named {@code "ptr"}.
 * The field name matter, since native code searches for a field having exactly that name.
 *
 * <p>{@code NativeResource} can be {@linkplain IdentifiableObject#cleanWhenUnreachable() registered}
 * for automatic release of C++ shared pointer when an instance of another object is garbage collected.
 * The other object is usually {@link IdentifiableObject}, but other objects could be used as well.
 * The navigation shall be in only one direction, from {@link IdentifiableObject} to {@code NativeResource}.
 * See {@link java.lang.ref.Cleaner} for explanation about why this class shall not contains any reference
 * to the {@code IdentifiableObject}.</p>
 *
 * <p>If above-cited automatic release is not used, then it is subclass or caller responsibility
 * to manage the release of the resource referenced by {@link #ptr} when no longer referenced.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
class NativeResource implements Runnable {
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
        try {
            System.load(libraryPath().toAbsolutePath().toString());
        } catch (URISyntaxException | IOException e) {
            throw (UnsatisfiedLinkError) new UnsatisfiedLinkError("Can not get path to native file.").initCause(e);
        }
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
     * Invoked by native code for getting the logger where to send a message.
     * If this method is renamed, then the native C++ code needs to be updated accordingly.
     *
     * @return the logger.
     */
    @SuppressWarnings("unused")
    private static System.Logger logger() {
        return System.getLogger(LOGGER_NAME);
    }

    /**
     * Returns a <cite>Well-Known Text</cite> (WKT) for this object.
     * This is allowed only if the wrapped PROJ object implements {@code osgeo::proj::io::IWKTExportable}.
     *
     * @param  convention  ordinal value of the {@link WKTFormat.Convention} to use.
     * @param  multiline   whether the WKT will use multi-line layout.
     * @param  strict      whether to enforce strictly standard format.
     * @return the Well-Known Text (WKT) for this object, or {@code null} if the PROJ object
     *         does not implement the {@code osgeo::proj::io::IWKTExportable} interface.
     * @throws FormattingException if an error occurred during formatting.
     */
    native String toWKT(int convention, boolean multiline, boolean strict);

    /**
     * Invoked by the cleaner thread when the {@link IdentifiableObject} has been garbage collected.
     * This method is invoked by the cleaner thread and shall never been invoked directly by us.
     */
    @Override
    public native void run();
}
