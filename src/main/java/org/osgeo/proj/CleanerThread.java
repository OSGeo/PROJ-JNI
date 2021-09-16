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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A thread processing all {@link Reference} instances enqueued in a {@link ReferenceQueue}.
 * This is the central place where every soft references produced by the PROJ-JNI library
 * are consumed. This thread will invoke the {@link SharedPointer#release()} method for
 * each references enqueued by the garbage collector.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 */
final class CleanerThread extends Thread {
    /**
     * List of references collected by the garbage collector.
     * This reference shall be given to {@link Reference} constructors.
     */
    static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    /**
     * Creates the singleton instance of the {@code ReferenceQueueConsumer} thread.
     */
    static {
        final CleanerThread thread = new CleanerThread();
        /*
         * Call to Thread.start() must be outside the constructor
         * (Reference: Goetz et al.: "Java Concurrency in Practice").
         */
        thread.start();
    }

    /**
     * Constructs a new thread as a daemon thread. This thread will be sleeping most of the time.
     * It will run only only a few nanoseconds every time a new {@link Reference} is enqueued.
     *
     * <p>We give to this thread a priority higher than the normal one since this thread shall
     * execute only tasks to be completed very shortly. Quick execution of those tasks is at
     * the benefit of the rest of the system, since they make more resources available sooner.</p>
     */
    private CleanerThread() {
        super(null, null, "PROJ objects disposer", 16*1024);        // Small (16 kb) stack size is sufficient.
        setPriority(Thread.MAX_PRIORITY - 2);
        setDaemon(true);
    }

    /**
     * Loop to be run during the virtual machine lifetime.
     * Public as an implementation side-effect; <strong>do not invoke explicitly!</strong>
     */
    @Override
    public final void run() {
        /*
         * The reference queue should never be null. However some strange cases have been
         * observed at shutdown time. If the field become null, assume that a shutdown is
         * under way and let the thread terminate.
         */
        ReferenceQueue<Object> queue;
        while ((queue = QUEUE) != null) {
            try {
                /*
                 * Block until a reference is enqueued. The reference should never be null
                 * when using the method without timeout (it could be null if we specified
                 * a timeout). If the remove() method behaves as if a timeout occurred, we
                 * may be in the middle of a shutdown. Continue anyway as long as we didn't
                 * received the kill event.
                 */
                final SharedObjects.Entry ref = (SharedObjects.Entry) queue.remove();
                if (ref != null) {
                    /*
                     * If the reference does not implement the SharedObjects.Entry class, we want
                     * the ClassCastException to be logged in the "catch" block since it would be
                     * a programming error that we want to know about.
                     */
                    ref.cleaner.release();
                    SharedObjects.CACHE.remove(ref);
                }
            } catch (Throwable exception) {
                Logger.getLogger(NativeResource.LOGGER_NAME).log(Level.WARNING, exception.getLocalizedMessage(), exception);
            }
        }
        // Do not log anything at this point, since the loggers may be shutdown now.
    }
}
