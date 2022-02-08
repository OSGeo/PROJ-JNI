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

import java.util.Map;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.lang.annotation.Native;
import java.util.concurrent.locks.ReentrantLock;

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;


/**
 * Wrapper for {@code PJ_CONTEXT}, the PROJ threading context.
 * A {@code PJ_CONTEXT} can be used by only one thread at a time, not necessarily the creator thread.
 * Contexts are stored in a pool so any {@link Context} not in current use can be taken by any thread.
 * Contexts that have not been used for at least {@value #TIMEOUT} nanoseconds may be disposed.
 *
 * <p>This class holds also all PROJ resources that depends on that particular {@code PJ_CONTEXT} instance.
 * For example {@code osgeo::proj::io::AuthorityFactory} contains indirectly a pointer to {@code PJ_CONTEXT},
 * so it should be used in the same thread than that {@code PJ_CONTEXT}. All those resources shall be used in
 * a try-with-resource block. Example:</p>
 *
 * <pre>
 * try (Context c = Context.acquire()) {
 *     AuthorityFactory factory = c.factory("EPSG");
 *     // Do not use above factory ouside this block.
 * }</pre>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class Context extends NativeResource implements AutoCloseable {
    /**
     * Timeout after which to discard unused contexts, in nanoseconds.
     * There is no guarantees that contexts will be discarded soon after this timeout;
     * the only guarantee is that contexts will not be discarded before this timeout.
     * Current setting is one minute (may change in any future version).
     */
    private static final long TIMEOUT = 1 * 60 * 1000_000_000L;

    /**
     * The previously created {@code PJ_CONTEXT} instances.
     * Those instances are pushed back to the pool after usage for
     * allowing the same thread or another thread to use them again.
     */
    private static final Deque<Context> CONTEXTS = new ConcurrentLinkedDeque<>();

    /**
     * Ensure that only one thread at a time is destroying expired Context instances otherwise
     * this can lead to NullPointerExceptions when accessing long primitives.
     */
    private static final ReentrantLock DESTROY_EXPIRED_LOCK = new ReentrantLock();

    /**
     * Timestamp (as given by {@link System#nanoTime()}) of last use of this context.
     * Used for determining if the {@link #TIMEOUT} has been elapsed for this context.
     */
    private long lastUse;

    /**
     * Wrapper around the {@code osgeo::proj::io::DatabaseContext}, created when first needed.
     * Database contexts are needed by {@link #factories} and by {@link #createFromUserInput(String)} method.
     */
    @Native
    private long database;

    /**
     * Wrappers for {@code osgeo::proj::io::AuthorityFactory}, created when first needed.
     * Keys are authority names and values are wrappers for corresponding PROJ factories.
     * Values shall be used inside a try-with-resource block as documented in class javadoc.
     */
    private final Map<String,AuthorityFactory> factories = new HashMap<>();

    /**
     * Creates and wraps a new {@code PJ_CONTEXT}.
     */
    private Context() {
        super(create());
    }

    /**
     * Invokes the C/C++ {@code proj_context_create()} method.
     * It is caller's responsibility to verify that the returned value is non-null.
     *
     * @return pointer to the {@code PJ_CONTEXT} allocated by PROJ, or 0 if out of memory.
     */
    private static native long create();

    /**
     * Gets a PROJ context, creating a new one if needed.
     * This method shall be invoked in a {@code try} block as below:
     *
     * <pre>
     * try (Context c = Context.acquire()) {
     *     AuthorityFactory factory = c.factory("EPSG");
     *     // Do not use above factory ouside this block.
     * }</pre>
     *
     * All objects obtained from {@link Context} shall be used inside the {@code try} block.
     *
     * @return  wrapper for the {@code PJ_CONTEXT} structure, together with resources that depends on it.
     */
    static Context acquire() {
        final Context c = CONTEXTS.pollLast();
        return (c != null) ? c : new Context();
    }

    /**
     * Returns a factory for the given authority, creating it when first needed.
     * The factory shall be used inside a try-with-resource block as shown in class javadoc.
     *
     * @param  authority  the authority name, for example {@code "EPSG"}.
     * @return factory backed by PROJ for the given authority.
     * @throws FactoryException if the factory can not be created.
     */
    final AuthorityFactory factory(final String authority) throws FactoryException {
        AuthorityFactory factory = factories.get(authority);
        if (factory == null) {
            factory = new AuthorityFactory(this, authority);
            try {
                factories.put(authority, factory);
            } catch (Throwable e) {
                factory.release();                  // For releasing native resource if OutOfMemoryError.
                throw e;
            }
        }
        return factory;
    }

    /**
     * Invokes the C++ {@code createFromUserInput(text, ctx)} method.
     *
     * @param  text  the text to parse. It is caller responsibility to ensure that this argument is non-null.
     * @return a coordinate reference system or other kind of object created from the given text.
     *
     * @see Proj#createFromUserInput(String)
     */
    native Object createFromUserInput(final String text);

    /**
     * Creates the PROJ {@code PJ} object for the given coordinate operation.
     * The {@code PJ} shall be used in the same thread than this {@code Context}.
     *
     * @param  operation  wrapper for the operation for which to create a {@code PJ}.
     * @return address of the {@code PJ} created by this method, or 0 if out of memory.
     * @throws TransformException if the construction failed.
     */
    native long createPJ(NativeResource operation) throws TransformException;

    /**
     * Disposes this context. This method returns the {@code PJ_CONTEXT} structure to the pool,
     * so it can be reused again by this thread or by another thread. Old {@code PJ_CONTEXT}s
     * not used for a long time are opportunistically discarded.
     *
     * <p>This method should not be invoked explicitly. Instead it is invoked in try-with-resource
     * statements as documented in {@linkplain Context class javadoc}.</p>
     */
    @Override
    public final void close() {
        try {
            destroyExpired();
            lastUse = System.nanoTime();
            CONTEXTS.add(this);
        } catch (Throwable e) {
            destroy();              // We will forget this instance (it has not been pushed back to the pool).
            throw e;
        }
    }

    /**
     * Disposes all {@code PJ_CONTEXT} structures which have not been used for at least {@value #TIMEOUT} nanoseconds.
     * This method should be invoked when there is a chance that some contexts are no longer needed, for example when
     * some PROJ objects are garbage collected.
     */
    private static void destroyExpired() {
        if(DESTROY_EXPIRED_LOCK.tryLock()) {
            try {
                Context c = CONTEXTS.peekFirst();
                if (c != null) {
                    final long time = System.nanoTime();
                    while (time - c.lastUse > TIMEOUT) {  // Accessing lastUse can lead to a NullpointerException when multiple threads are calling this method
                        c = CONTEXTS.pollFirst();                   // Verify again since it may have changed concurrently.
                        if (c == null) return;
                        if (time - c.lastUse <= TIMEOUT) try {
                            c.lastUse = time;                       // Pretend we just used that context, for consistent ordering.
                            CONTEXTS.add(c);
                            return;
                        } catch (Throwable e) {
                            c.destroy();
                            throw e;
                        }
                        c.destroy();
                        c = CONTEXTS.peekFirst();                   // Check if next context should also be disposed.
                    }
                }
            } finally {
                DESTROY_EXPIRED_LOCK.unlock();
            }
        }
    }

    /**
     * Destroys all {@code PJ_CONTEXT} instances. This is invoked at JVM shutdown time.
     */
    static void destroyAll() {
        Context c;
        while ((c = CONTEXTS.poll()) != null) {
            c.destroy();
        }
    }

    /**
     * Disposes all native resources associated to this context. First, this method releases all
     * {@code osgeo::proj::io::AuthorityFactory} or similar objects. Then {@code PJ_CONTEXT} is
     * destroyed last.
     */
    private void destroy() {
        factories.values().forEach(AuthorityFactory::release);
        /*
         * PJ_CONTEXT is not a pointer managed by C++ std::shared_ptr library, so we need to be
         * careful here. We destroy PJ_CONTEXT here on the assumption that above lines disposed
         * all objects that were using it. If an exception has been thrown before we reach this
         * line, we will have a memory leak. But the alternative (destroy PJ_CONTEXT in finally
         * block) may be worst since it could destroy a resource still used by live C++ objects.
         */
        destroyPJ();
    }

    /**
     * Invokes the C/C++ {@code proj_context_destroy(…)} method.
     * This method shall be invoked exactly once when {@link Context} is disposed.
     * It is the caller's responsibility to ensure that the {@code PJ_CONTEXT} is not
     * used anymore, for example that all {@link #factories} have been disposed.
     */
    private native void destroyPJ();
}
