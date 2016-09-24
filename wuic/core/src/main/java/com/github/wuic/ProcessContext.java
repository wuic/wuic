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


package com.github.wuic;

import com.github.wuic.util.WuicScheduledThreadPool;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * <p>
 * A request context is an object giving more information about the context in which a process is executed.
 * For instance, operations could be run in a scheduled job, when the application starts or when an HTTP request is sent
 * to a web container.
 * </p>
 *
 * <p>
 * This class should be specialized in order to give details that are specific to a particular context.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class ProcessContext {

    /**
     * Default install that could be installed anywhere.
     */
    public static final ProcessContext DEFAULT = new ProcessContext();

    /**
     * <p>
     * Executes as soon as possible the given job and returns the related {@link Future}.
     * This method delegates the job to {@link WuicScheduledThreadPool} singleton.
     * </p>
     *
     * @param job the job to execute
     * @return the future result
     */
    public synchronized <T> Future<T> executeAsap(final Callable<T> job) {
        return WuicScheduledThreadPool.INSTANCE.executeAsap(job);
    }
}
