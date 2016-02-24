/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

/**
 * <p>
 * This class is used internally by WUIC to control the number of created threads for different tasks. When a
 * peace of code needs to create asynchronous task in WUIC, it always uses this singleton which wraps a thread
 * pool. The class is tread safe and don't need to be called with a mutex.
 * </p>
 *
 * <p>
 * The singleton adds a hook with {@link Runtime#addShutdownHook(Thread)} to shutdown the pool when executed.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
public final class WuicScheduledThreadPool extends Thread {

    /**
     * We just create 1 thread per processor.
     */
    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * The unique instance.
     */
    private static WuicScheduledThreadPool instance = null;

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The thread pool.
     */
    private ScheduledExecutorService pool;

    /**
     * <p>
     * Creates an unique instance.
     * </p>
     */
    private WuicScheduledThreadPool() {
        pool = Executors.newScheduledThreadPool(POOL_SIZE);
        Runtime.getRuntime().addShutdownHook(this);
    }

    /**
     * <p>
     * Gets the unique instance.
     * </p>
     *
     * @return the unique instance
     */
    public static synchronized WuicScheduledThreadPool getInstance() {
        if (instance == null) {
            instance = new WuicScheduledThreadPool();
        }

        return instance;
    }

    /**
     * <p>
     * Schedules an execution in a specified delay of a given job. Once the job is executed, its execution will
     * be repeated in the initial delay, and so on.
     * </p>
     *
     * @param job the job to execute
     * @param delay the delay between executions
     * @return an object which gives control over scheduled executions
     */
    public synchronized ScheduledFuture<?> executeEveryTimeInSeconds(final Runnable job, final int delay) {
        return pool.scheduleWithFixedDelay(new ExceptionLogger(job), delay, delay, TimeUnit.SECONDS);
    }

    /**
     * <p>
     * Executes as soon as possible the given job and returns the related {@link Future}.
     * </p>
     *
     * @param job the job to execute
     * @return the future result
     */
    public synchronized <T> Future<T> executeAsap(final Callable<T> job) {
        return pool.submit(new CallExceptionLogger(job));
    }

    /**
     * <p>
     * Executes as soon as possible the given job and returns the related {@link Future}.
     * </p>
     *
     * @param job the job to execute
     * @return the future result
     */
    public synchronized Future<?> executeAsap(final Runnable job) {
        return pool.submit(job);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        shutdown();
    }

    /**
     * <p>
     * Stops the pool.
     * </p>
     */
    public void shutdown() {
        pool.shutdownNow();
    }

    /**
     * <p>
     * Logs any exception which occurs when running a delegated {@link Runnable}.
     * </p>
     *
     * @author Guillaume DROUET
          * @since 0.4.0
     */
    private final class ExceptionLogger implements Runnable {

        /**
         * Wrapped runnable.
         */
        private Runnable delegate;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param r the runnable
         */
        private ExceptionLogger(final Runnable r) {
            delegate = r;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                delegate.run();
            } catch (Exception e) {
                log.error("A thread execution has failed in WUIC", e);
            }
        }
    }

    /**
     * <p>
     * Logs any exception which occurs when running a delegated {@link Callable}.
     * </p>
     *
     * @author Guillaume DROUET
          * @since 0.4.4
     */
    public static final class CallExceptionLogger<T> implements Callable<T> {

        /**
         * Wrapped callable.
         */
        private Callable<T> delegate;

        /**
         * Logger.
         */
        private Logger log = LoggerFactory.getLogger(getClass());

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param c the callable
         */
        public CallExceptionLogger(final Callable<T> c) {
            delegate = c;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T call() {
            try {
                return delegate.call();
            } catch (Exception e) {
                log.error("A thread execution has failed in WUIC", e);
                return null;
            }
        }
    }
}
