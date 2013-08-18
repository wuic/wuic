package com.github.wuic.test;

import com.github.wuic.ContextBuilder;
import com.github.wuic.ContextBuilderConfigurator;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.NumberUtils;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Tests suite for {@link ContextBuilderConfigurator}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class ContextBuilderConfiguratorTest {

    /**
     * Tests basic configuration with polling activation.
     *
     * @throws StreamException if test fails
     */
    @Test
    public void configureWithPollingTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);

        final ContextBuilderConfigurator cfg = new ContextBuilderConfigurator() {

            /**
             * {@inheritDoc}
             */
            @Override
            public int internalConfigure(final ContextBuilder ctxBuilder) {
                return 1;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTag() {
                return "test";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
                synchronized (ContextBuilderConfiguratorTest.class) {
                    count.incrementAndGet();
                    ContextBuilderConfiguratorTest.class.notify();
                }

                return System.currentTimeMillis();
            }
        };

        final ContextBuilder builder = Mockito.mock(ContextBuilder.class);
        cfg.configure(builder);

        synchronized (ContextBuilderConfiguratorTest.class) {
            ContextBuilderConfiguratorTest.class.wait(1500L);
        }

        Assert.assertEquals(count.intValue(), NumberUtils.TWO);
    }
}
