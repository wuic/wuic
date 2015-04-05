/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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

package com.github.wuic.test.engine;

import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.AbstractCacheEngine;
import com.github.wuic.engine.core.MemoryMapCacheEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.FutureLong;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link MemoryMapCacheEngineTest} test.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.3
 */
@RunWith(JUnit4.class)
public class MemoryMapCacheEngineTest {

    /**
     * <p>
     * Make sure the cache is notified only by one listener for several call with the same key.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void invalidationTest() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine(true, -1, false) {

            /**
             * {@inheritDoc}
             */
            @Override
            public void removeFromCache(final EngineRequest.Key request) {
                counter.incrementAndGet();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void putToCache(final EngineRequest.Key request, final CacheResult nuts) {
                // Do not add to cache to ignore for test purpose
            }
        };

        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(nut.getInitialName()).thenReturn("foo.js");
        Mockito.when(nut.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(ProcessContext.class))).thenReturn(Arrays.asList(nut));

        final NutsHeap heap = new NutsHeap(this, Arrays.asList(""), dao, "heap");
        heap.checkFiles(null);

        // Registers the InvalidateCache multiple time
        engine.parse(new EngineRequestBuilder("", heap).build());
        engine.parse(new EngineRequestBuilder("", heap).build());
        engine.parse(new EngineRequestBuilder("", heap).build());

        // Call listeners
        heap.nutUpdated(heap);

        // Only one listener is notified
        Assert.assertEquals(1, counter.get());
    }

    /**
     * Add an element then clears the cache.
     *
     * @throws Exception if test fails
     */
    @Test
    public void addThenClearTest() throws Exception {
        final EngineRequest.Key req = new EngineRequest.Key("wid", Arrays.asList(Mockito.mock(ConvertibleNut.class)));
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine(true, -1, false);
        final Map<String, ConvertibleNut> nuts = new HashMap<String, ConvertibleNut>();
        nuts.put("", Mockito.mock(ConvertibleNut.class));
        AbstractCacheEngine.CacheResult result = new AbstractCacheEngine.CacheResult(null, nuts);
        engine.putToCache(req, result);
        engine.clearCache();
        Assert.assertNull(engine.getFromCache(req));
    }

    /**
     * Add an element then removes the cache.
     *
     * @throws Exception if test fails
     */
    @Test
    public void addThenRemoveTest() throws Exception {
        final EngineRequest.Key req = new EngineRequest.Key("wid", Arrays.asList(Mockito.mock(ConvertibleNut.class)));
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine(true, -1, false);
        final Map<String, ConvertibleNut> nuts = new HashMap<String, ConvertibleNut>();
        nuts.put("", Mockito.mock(ConvertibleNut.class));
        AbstractCacheEngine.CacheResult result = new AbstractCacheEngine.CacheResult(null, nuts);
        engine.putToCache(req, result);
        engine.removeFromCache(req);
        Assert.assertNull(engine.getFromCache(req));
    }
}
