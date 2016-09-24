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

import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Tests the facade with different {@link com.github.wuic.WuicFacade.WarmupStrategy}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class WarmupTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Creates a facade that invokes given callback each time a workflow is invoked.
     * </p>
     *
     * @param strategy the strategy
     * @param r the callback
     * @throws WuicException if test fails
     */
    private void createFacade(final WuicFacade.WarmupStrategy strategy, final Runnable r) throws WuicException {
        new WuicFacadeBuilder().objectBuilderInspector(new ObjectBuilderInspector() {

            @Override
            public <T> T inspect(final T object) {
                if (HeadEngine.class.isAssignableFrom(object.getClass())) {
                    return (T) new HeadEngine() {

                        @Override
                        public ConvertibleNut parse(final EngineRequest request, final String path) throws WuicException {
                            return HeadEngine.class.cast(object).parse(request, path);
                        }

                        @Override
                        public EngineType getEngineType() {
                            return HeadEngine.class.cast(object).getEngineType();
                        }

                        @Override
                        protected List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
                            r.run();
                            return request.getNuts();
                        }

                        @Override
                        public Boolean works() {
                            return HeadEngine.class.cast(object).works();
                        }
                    };
                }
                return object;
            }
        }).warmUpStrategy(strategy).build();
    }

    /**
     * Tests with {@link com.github.wuic.WuicFacade.WarmupStrategy#NONE}.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void noneWarmupStrategyTest() throws WuicException {
        final AtomicInteger count = new AtomicInteger();
        createFacade(WuicFacade.WarmupStrategy.NONE, new Runnable() {
            @Override
            public void run() {
               count.incrementAndGet();
            }
        });

        Assert.assertEquals(0, count.get());
    }

    /**
     * Tests with {@link com.github.wuic.WuicFacade.WarmupStrategy#SYNC}.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void syncWarmupStrategyTest() throws WuicException {
        final AtomicInteger count = new AtomicInteger();
        createFacade(WuicFacade.WarmupStrategy.SYNC, new Runnable() {
            @Override
            public void run() {
                count.incrementAndGet();
            }
        });

        Assert.assertNotEquals(0, count.get());
    }

    /**
     * Tests with {@link com.github.wuic.WuicFacade.WarmupStrategy#ASYNC}.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void asyncWarmupStrategyTest() throws Exception {
        final CountDownLatch count = new CountDownLatch(1);
        createFacade(WuicFacade.WarmupStrategy.ASYNC, new Runnable() {
            @Override
            public void run() {
                count.countDown();
            }
        });

        Assert.assertEquals(1, count.getCount());
        Assert.assertTrue(count.await(20L, TimeUnit.SECONDS));
    }
}
