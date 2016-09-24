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


package com.github.wuic.servlet;

import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.SyncFuture;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * <p>
 * A {@link ProcessContext} created in a {@link javax.servlet.http.HttpServletRequest} execution scope.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class ServletProcessContext extends ProcessContext {

    /**
     * The wrapped request.
     */
    private final HttpServletRequest httpServletRequest;

    /**
     * The logger.
     */
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * <p>
     * Builds a new instance with a {@link HttpServletRequest request} to wrap.
     * </p>
     *
     * @param request the request to wrap
     */
    public ServletProcessContext(final HttpServletRequest request) {
        httpServletRequest = new SynchronizedHttpServletRequestWrapper(request);
    }

    /**
     * <p>
     * Try to cast the given object in the instance of this class. An {@link IllegalStateException} will be thrown if
     * the process context is {@code null} or an instance of a different class.
     * </p>
     *
     * @param processContext the process context
     * @return the process context
     */
    public static ServletProcessContext cast(final ProcessContext processContext) {
        if (!(processContext instanceof ServletProcessContext)) {
            WuicException.throwBadStateException(new IllegalStateException(
                    "Process context must wrap an HTTP request when WUIC is deployed in any servlet container."));
        }

        return ServletProcessContext.class.cast(processContext);
    }

    /**
     * <p>
     * Gets the wrapped request.
     * </p>
     *
     * @return the request
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    /**
     * <p>
     * Executes as soon as possible the given job and returns the related {@link java.util.concurrent.Future}.
     * The method uses the {@link javax.servlet.AsyncContext} started from the given request.
     * </p>
     *
     * @param job the job to execute
     * @return the future result
     */
    @Override
    public <T> Future<T> executeAsap(final Callable<T> job) {

        // Asynchronous service not available
        if (!httpServletRequest.isAsyncSupported()) {
            final Exception ex = new IllegalStateException("isAsyncSupported() returns false in the current request.");
            logger.warn("Trying to directly use the internal executor, which is possibly not allowed by the servlet container.");
            logger.warn("Make sure all your filter/servlet have their async-supported flag turned on. Otherwise disable asynchronous operations.");
            logger.debug("Detailed cause.", ex);
            return super.executeAsap(job);
        } else {
            synchronized (httpServletRequest) {

                // Already running in an asynchronous job
                if (!httpServletRequest.isAsyncStarted() || httpServletRequest.getAttribute(getClass().getName()) != null) {
                    logger.debug("This thread is already running asynchronously. The job will be run now synchronously.");

                    try {
                        // Run synchronously
                        return new SyncFuture<T>(job.call());
                    } catch (Exception e) {
                        WuicException.throwBadStateException(e);
                        return null;
                    }
                } else {
                    // Make sure we track the fact that we run in an asynchronous job
                    httpServletRequest.setAttribute(getClass().getName(), "");

                    // Call the service
                    final AsyncContext asyncContext = httpServletRequest.startAsync();
                    final FutureTask<T> task = new FutureTask<T>(new WuicScheduledThreadPool.CallExceptionLogger<T>(
                            new WuicScheduledThreadPool.CallExceptionLogger<T>(job))) {

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        protected void done() {
                            asyncContext.complete();
                        }
                    };

                    // Schedules the job to run asynchronously
                    asyncContext.start(task);
                    return task;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ProcessContext[request=%s]", httpServletRequest);
    }
}
