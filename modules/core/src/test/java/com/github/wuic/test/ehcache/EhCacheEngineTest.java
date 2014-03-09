/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.test.ehcache;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.core.EhCacheEngineBuilder;
import com.github.wuic.engine.impl.ehcache.EhCacheEngine;
import com.github.wuic.engine.impl.ehcache.WuicEhcacheProvider;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.*;
import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link com.github.wuic.engine.Engine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class EhCacheEngineTest {

    /**
     * <p>
     * Mocked cache provider.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    public static final class CacheFactory implements WuicEhcacheProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public Cache getCache() {
            final Cache retval = new Cache(String.valueOf(System.currentTimeMillis()), 400, false, false, 20, 20);
            CacheManager.getInstance().addCache(retval);
            return retval;
        }
    }

    /**
     * Counter.
     */
    private AtomicInteger count;

    /**
     * Initializes counter.
     */
    @Before
    public void init() {
        count = new AtomicInteger();
    }

    /**
     * <p>
     * Creates a mocked engine that increments a counter each time its parse method is invoked.
     * </p>
     *
     * @return the mock
     * @throws WuicException if test fails
     */
    private NodeEngine mock() throws WuicException {
        final NodeEngine mock = Mockito.mock(NodeEngine.class);
        Mockito.when(mock.getEngineType()).thenReturn(EngineType.INSPECTOR);
        Mockito.when(mock.parse(Mockito.any(EngineRequest.class))).then(new Answer<Object>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                count.incrementAndGet();
                return ((EngineRequest)invocationOnMock.getArguments()[0]).getNuts();
            }
        });

        return mock;
    }

    /**
     * Test that content is cached.
     *
     * @throws Exception if test fails
     */
    @Test
    public void cacheTest() throws Exception {
        final EhCacheEngineBuilder builder = new EhCacheEngineBuilder();
        final Engine e = builder.build();
        final NodeEngine chain = mock();
        final Map<NutType, NodeEngine> map = new HashMap<NutType, NodeEngine>();
        map.put(NutType.CSS, chain);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getName()).thenReturn("foo.css");
        Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        e.parse(new EngineRequest("", "", heap, map));
        Assert.assertEquals(1, count.get());
        e.parse(new EngineRequest("", "", heap, map));
        Assert.assertEquals(1, count.get());
    }

    /**
     * Test that content is not cached.
     *
     * @throws Exception if test fails
     */
    @Test
    public void noCacheTest() throws Exception {
        final EhCacheEngineBuilder builder = new EhCacheEngineBuilder();
        builder.property(ApplicationConfig.CACHE, false);
        final Engine chain = mock();
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        chain.parse(new EngineRequest("", "", heap, new HashMap<NutType, NodeEngine>()));
        Assert.assertEquals(1, count.get());
        chain.parse(new EngineRequest("", "", heap, new HashMap<NutType, NodeEngine>()));
        Assert.assertEquals(2, count.get());
    }


    /**
     * Test that cached content is invalidated when changes are notified.
     *
     * @throws Exception if test fails
     */
    @Test
    public void invalidateCacheTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(nut.getName()).thenReturn("foo.js");
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final List<HeapListener> listeners = new ArrayList<HeapListener>();

        Mockito.doAnswer(new Answer() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                listeners.add((HeapListener) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(heap).addObserver(Mockito.any(HeapListener.class));

        final EhCacheEngineBuilder builder = new EhCacheEngineBuilder();
        builder.property(ApplicationConfig.CACHE_PROVIDER_CLASS, CacheFactory.class.getName());
        final EhCacheEngine cache = (EhCacheEngine) builder.build();
        final Map<NutType, NodeEngine> map = new HashMap<NutType, NodeEngine>();
        map.put(NutType.JAVASCRIPT, mock());

        cache.parse(new EngineRequest("", "", heap, map));
        Assert.assertEquals(1, count.get());
        cache.parse(new EngineRequest("", "", heap, map));
        Assert.assertEquals(1, count.get());
        Assert.assertEquals(listeners.size(), 1);

        // Invalidate cache
        listeners.get(0).nutUpdated(heap);

        cache.parse(new EngineRequest("", "", heap, map));
        Assert.assertEquals(2, count.get());
        cache.parse(new EngineRequest("", "", heap, map));
        Assert.assertEquals(2, count.get());
    }
}
