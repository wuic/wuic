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

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * This {@link Future} wraps a result computed synchronously and consequently return it as soon as {@link #get()} is
 * called.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class SyncFuture<T> implements Future<T>, Serializable {

    /**
     * The wrapped value.
     */
    private T value;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param val the wrapped value
     */
    public SyncFuture(final T val) {
        this.value = val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }
}
