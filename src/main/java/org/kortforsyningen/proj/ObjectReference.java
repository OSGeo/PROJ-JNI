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
import java.lang.ref.Cleaner;


/**
 * Base class of all wrappers around PROJ objects referenced by a raw pointer.
 * The type of pointer (and consequently the action to take at disposal time)
 * depends on the subclass: it may be a pointer to a traditional C/C++ object
 * allocated by {@code malloc(…)}, or it may be a C++ shared pointer.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
abstract class ObjectReference {
    /**
     * Manager of objects having native resources to be released after the Java object has been garbage-collected.
     * This manager will invoke a {@code proj_xxx_destroy(…)} method where <var>xxx</var> depends on the resource
     * which has been registered, or decrement the reference count of a shared pointer.
     */
    private static final Cleaner DISPOSER = Cleaner.create(ObjectReference::cleanerThread);

    /**
     * Creates a new thread for disposing PROJ objects after their Java wrappers have been garbage-collected.
     * We create a thread ourselves mostly for specifying a more explicit name than the default name.
     *
     * @param  cleaner  provided by {@link Cleaner}.
     * @return the thread to use for disposing PROJ objects.
     */
    private static Thread cleanerThread(final Runnable cleaner) {
        final Thread t = new Thread(cleaner);
        t.setPriority(Thread.MAX_PRIORITY - 2);
        t.setName("PROJ objects disposer");
        return t;
    }

    /**
     * The pointer to PROJ structure allocated in the C/C++ heap. This value has no meaning in Java code.
     * <strong>Do not modify</strong>, since this value is required for using PROJ. Do not rename neither,
     * unless potential usage of this field is also verified in the C/C++ source code.
     */
    final long ptr;

    /**
     * Task invoked when the enclosing {@link ObjectReference} instance has been garbage-collected.
     * {@code Disposer} shall not contain any reference to the enclosing {@code ObjectReference}.
     * Instead, this class holds a copy of the {@link ObjectReference#ptr} value.
     * See {@link Cleaner} for explanation about why this separation is required.
     *
     * <p>The default implementation assumes that {@link #ptr} is a shared pointer and that releasing
     * the object consists in decrementing the references count. If {@link #ptr} is a different kind
     * of pointer, then the {@link #run()} method must be overridden.</p>
     */
    static class Disposer implements Runnable {
        /**
         * Pointer to PROJ structure to release after the enclosing object has been garbage collected.
         * This is a copy of {@link ObjectReference#ptr}. Releasing is done by calling {@link #run()}.
         */
        final long ptr;

        /**
         * Creates a disposer for the PROJ object referenced by the given pointer.
         * The type of pointer (classical or shared) depends on the class of this {@code Disposer}.
         *
         * @param  ptr  copy of {@link ObjectReference#ptr} value.
         */
        Disposer(final long ptr) {
            this.ptr = ptr;
        }

        /**
         * Invoked by the cleaner thread when the enclosing {@link ObjectReference} is no longer reachable.
         * This method shall never been invoked by us. The default implementation assumes that {@link #ptr}
         * is a shared pointer and decrement the references count. If {@link #ptr} is another kind of pointer,
         * then this method must be overridden.
         */
        @Override
        public void run() {
            release(ptr);
        }
    }

    /**
     * Decrements the references count of the given shared pointer. This method is invoked automatically by
     * the default {@link Disposer} implementation when this {@link ObjectReference} is garbage collected.
     * This method will be ignored if this {@link ObjectReference} uses another disposal mechanism.
     *
     * @param  ptr  the shared pointer allocated by PROJ.
     */
    private static native void release(long ptr);

    /**
     * Creates a wrapper for the given pointer to a PROJ structure. If the given pointer is null,
     * then this constructor assumes that the PROJ object allocation failed because of out of memory.
     *
     * <p>If {@code isShared} is true, then this constructor automatically register a handler for
     * decrementing the references count after this {@link ObjectReference} is garbage collected.
     * Otherwise the caller is responsible for invoking {@link #onDispose(Disposer)} after construction.</p>
     *
     * @param  ptr       pointer to the PROJ structure to wrap.
     * @param  isShared  whether the given pointer is a shared pointer.
     * @throws OutOfMemoryError if the given {@code ptr} is null.
     */
    ObjectReference(final long ptr, final boolean isShared) {
        this.ptr = ptr;
        if (ptr == 0) {
            throw new OutOfMemoryError();
        }
        if (isShared) try {
            onDispose(new Disposer(ptr));
        } catch (Throwable e) {
            release(ptr);
            throw e;
        }
    }

    /**
     * Sets the handler to invoke when this {@link ObjectReference} is garbage collected.
     * This method shall be invoked at most once during construction in a code like below:
     *
     * <pre>
     * try {
     *     onDispose(new Disposer(ptr));
     * } catch (Throwable e) {
     *     release(ptr);
     *     throw e;
     * }
     * </pre>
     *
     * This method shall not be invoked after {@link #ObjectReference(long, boolean)} if the
     * {@code isShared} flag was {@code true} because that constructor has automatically
     * invoked this method in such case.
     *
     * @param  handler  a task to execute when this {@link ObjectReference} is garbage-collected.
     */
    final void onDispose(final Disposer handler) {
        DISPOSER.register(this, handler);
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
         * Example of URL for a JAR entry: jar:file:/home/…/proj.jar!/org/…/ObjectReference.class
         */
        final String nativeFile = libdir + "/libproj-binding." + suffix;
        final URL res = ObjectReference.class.getResource(nativeFile);
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
     * Loads the native library. If this initialization fails, a message is logged at fatal error level
     * (because the library will be unusable) but no exception is thrown.  We do not throw an exception
     * from this static initializer because doing so would result in {@link NoClassDefFoundError} to be
     * thrown on all subsequent attempts to use this class, which may be confusing.
     */
    static {
        try {
            System.load(libraryPath().toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError | Exception e) {
            System.getLogger("org.kortforsyningen.proj").log(System.Logger.Level.ERROR, e);
        }
    }
}
