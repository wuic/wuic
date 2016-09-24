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
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.context.SimpleContextBuilderConfigurator;
import com.github.wuic.util.NumberUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Tests suite for {@link ContextBuilderConfigurator}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class ContextBuilderConfiguratorTest {

    /**
     * Called only if ServiceLoader detects the configurator.
     */
    public static boolean called = false;

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Makes sure the context builder configurators are installed thanks to the {@link java.util.ServiceLoader}.
     */
    @Test
    public void serviceTest() {
        new ContextBuilder();
        Assert.assertTrue(called);
    }

    /**
     * Tests configurations when multiple and non-multiple executions are set.
     *
     * @throws IOException if test fails
     */
    @Test
    public void configureMultipleTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final StringBuilder tagBuilder = new StringBuilder("test");

        final ContextBuilderConfigurator cfg = new ContextBuilderConfigurator() {

            /**
             * {@inheritDoc}
             */
            @Override
            public int internalConfigure(final ContextBuilder ctxBuilder) {
                count.incrementAndGet();
                return -1;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTag() {
                return tagBuilder.toString();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Long getLastUpdateTimestampFor(final String path) throws IOException {
                return -1L;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ProcessContext getProcessContext() {
                return null;
            }
        };

        final ContextBuilder builder = Mockito.mock(ContextBuilder.class);
        Mockito.when(builder.tag(Mockito.anyObject())).thenReturn(builder);

        cfg.setMultipleConfigurations(Boolean.FALSE);
        cfg.configure(builder);
        cfg.configure(builder);
        cfg.configure(builder);
        Assert.assertEquals(1, count.get());

        cfg.setMultipleConfigurations(Boolean.TRUE);
        cfg.configure(builder);
        Assert.assertEquals(NumberUtils.TWO, count.get());

        cfg.setMultipleConfigurations(Boolean.FALSE);
        tagBuilder.append("1");
        cfg.configure(builder);
        tagBuilder.append("1");
        cfg.configure(builder);
        tagBuilder.append("1");
        cfg.configure(builder);
        tagBuilder.append("1");
        cfg.configure(builder);
        Assert.assertEquals(NumberUtils.SIX, count.get());
    }

    /**
     * Tests basic configuration with polling activation.
     *
     * @throws IOException if test fails
     */
    @Test
    public void configureWithPollingTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NumberUtils.TWO);

        final ContextBuilderConfigurator cfg = new SimpleContextBuilderConfigurator() {

            /**
             * {@inheritDoc}
             */
            @Override
            public int internalConfigure(final ContextBuilder ctxBuilder) {
                return 3 - (int) latch.getCount();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Long getLastUpdateTimestampFor(final String path) throws IOException {
                synchronized (ContextBuilderConfiguratorTest.class) {
                    latch.countDown();
                    ContextBuilderConfiguratorTest.class.notify();
                }

                return System.currentTimeMillis();
            }
        };

        final ContextBuilder builder = Mockito.mock(ContextBuilder.class);
        Mockito.when(builder.tag(Mockito.anyObject())).thenReturn(builder);
        cfg.configure(builder);

        Assert.assertTrue(latch.await(1500, TimeUnit.MILLISECONDS));
    }
}
