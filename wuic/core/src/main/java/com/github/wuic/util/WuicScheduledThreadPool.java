/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
public enum WuicScheduledThreadPool {

    /**
     * Singleton.
     */
    INSTANCE;

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
        pool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        Runtime.getRuntime().addShutdownHook(new Shutdown());
    }

    /**
     * <p>
     * Executes the given job in the given delay in seconds.
     * </p>
     *
     * @param job the job
     * @param seconds the delay in seconds
     * @return the future result
     */
    public synchronized ScheduledFuture<?> execute(final Runnable job, final int seconds) {
        return pool.schedule(new ExceptionLogger(job), seconds, TimeUnit.SECONDS);
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
     * This {@code Thread} calls {@link #shutdown()}
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class Shutdown extends Thread {

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            shutdown();
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
