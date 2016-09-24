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


package com.github.wuic.test;

import com.github.wuic.ProcessContext;
import com.github.wuic.util.WuicScheduledThreadPool;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * <p>
 * This rule produces standalone {@link ProcessContext} with dedicated thread pool.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class ProcessContextRule implements TestRule {

    /**
     * The process context.
     */
    private final ProcessContext processContext;

    /**
     * The thread pool.
     */
    private final ScheduledExecutorService pool;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public ProcessContextRule() {
        this.pool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.processContext = new ProcessContext() {
            @Override
            public synchronized <T> Future<T> executeAsap(final Callable<T> job) {
                return pool.submit(new WuicScheduledThreadPool.CallExceptionLogger<T>(job));
            }
        };
    }

    /**
     * <p>
     * Gets the process context.
     * </p>
     *
     * @return the process context
     */
    public ProcessContext getProcessContext() {
        return processContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    pool.shutdown();
                }
            }
        };
    }
}
