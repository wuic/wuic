/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.core.AbstractCacheEngine;
import com.github.wuic.engine.core.MemoryMapCacheEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.SizableNut;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.test.ProcessContextRule;
import com.github.wuic.test.TemporaryFileManagerRule;
import com.github.wuic.test.WuicTest;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.TemporaryFileManager;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link MemoryMapCacheEngineTest} test.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.3
 */
@RunWith(JUnit4.class)
public class MemoryMapCacheEngineTest {

    /**
     * Process context.
     */
    @ClassRule
    public static ProcessContextRule processContext = new ProcessContextRule();

    /**
     * Temporary file manager.
     */
    @ClassRule
    public static TemporaryFileManagerRule temporaryFileManager = new TemporaryFileManagerRule();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Creates a new mocked nut.
     * </p>
     *
     * @param name the name
     * @return the nut
     * @throws if any I/O error occurs
     */
    private Nut newNut(final String name) throws IOException {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(nut.getInitialName()).thenReturn(name + ".js");
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(nut.openStream()).thenReturn(new InMemoryInput("", Charset.defaultCharset().displayName()));
        return nut;
    }

    /**
     * <p>
     * Tests that any dynamic nut is not cached and that transformer is reused.
     * </p>
     *
     * @throws Exception exception
     */
    @Test
    public void dynamicTest() throws Exception {
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "10MB");
        final AtomicInteger counter1 = new AtomicInteger();
        final Nut nut1 = newNut("foo");
        Mockito.when(nut1.isDynamic()).thenReturn(false);
        Mockito.when(nut1.openStream()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return new InMemoryInput(("var c = " + counter1.getAndIncrement() + ';'), Charset.defaultCharset().displayName());
            }
        });

        final AtomicInteger counter2 = new AtomicInteger();
        final Nut nut2 = newNut("bar");
        Mockito.when(nut2.isDynamic()).thenReturn(true);
        Mockito.when(nut2.openStream()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return new InMemoryInput(("var c = " + counter2.getAndIncrement() + ';'), Charset.defaultCharset().displayName());
            }
        });

        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(ProcessContext.class))).thenReturn(Arrays.asList(nut1, nut2));
        final NutsHeap heap = new NutsHeap(this, Arrays.asList(""), dao, "heap", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(processContext.getProcessContext());

        final Pipe.Transformer[] transformers = new Pipe.Transformer[3];

        for (int i = 0; i < transformers.length; i++) {
            transformers[i] = new Pipe.DefaultTransformer<ConvertibleNut>();
        }

        for (int i = 0; i < transformers.length; i++) {
            final Pipe.Transformer transformer = transformers[i];
            final List<ConvertibleNut> nuts = engine.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                    .chain(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), new NodeEngine() {
                        @Override
                        public List<NutType> getNutTypes() {
                            return Arrays.asList(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
                        }

                        @Override
                        public EngineType getEngineType() {
                            return EngineType.INSPECTOR;
                        }

                        @Override
                        protected List<ConvertibleNut> internalParse(EngineRequest request) throws WuicException {
                            for (final ConvertibleNut convertibleNut : request.getNuts()) {
                                if (convertibleNut.isDynamic()) {
                                    convertibleNut.addTransformer(transformer);
                                }
                            }

                            return request.getNuts();
                        }

                        @Override
                        public Boolean works() {
                            return true;
                        }
                    }).build());

            for (final ConvertibleNut n : nuts) {
                final String s = NutUtils.readTransform(n);

                if (n.isDynamic()) {
                    Assert.assertTrue(s.contains(String.valueOf(counter2.get() - 1)));
                } else {
                    Assert.assertTrue(s.contains(String.valueOf(1)));
                }
            }
        }
    }

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
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine() {

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

        engine.init(true, -1, false, "10MB");

        final Nut nut = newNut("foo");
        Mockito.when(nut.openStream()).thenAnswer(WuicTest.openStreamAnswer(""));

        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(ProcessContext.class))).thenReturn(Arrays.asList(nut));

        final NutsHeap heap = new NutsHeap(this, Arrays.asList(""), dao, "heap", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(processContext.getProcessContext());

        // Registers the InvalidateCache multiple time
        for (int i = 0; i < 3; i++) {
            engine.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
        }

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
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "10KB");
        final Map<String, AbstractCacheEngine.CacheResult.Entry> nuts = new HashMap<String, AbstractCacheEngine.CacheResult.Entry>();
        nuts.put("", new AbstractCacheEngine.CacheResult.Entry(Mockito.mock(ConvertibleNut.class)));
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
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "1024");
        final Map<String, AbstractCacheEngine.CacheResult.Entry> nuts = new HashMap<String, AbstractCacheEngine.CacheResult.Entry>();
        nuts.put("", new AbstractCacheEngine.CacheResult.Entry(Mockito.mock(ConvertibleNut.class)));
        AbstractCacheEngine.CacheResult result = new AbstractCacheEngine.CacheResult(nuts, null);
        engine.putToCache(req, result);
        engine.removeFromCache(req);
        Assert.assertNull(engine.getFromCache(req));
    }

    /**
     * <p>
     * Tests when size limit unit is not correct.
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void badUnitLimitTest() {
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "1GB");
    }

    /**
     * <p>
     * Tests when size limit is not a number.
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void badNumberLimitTest() {
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "foo");
    }

    /**
     * <p>
     * Tests when size limit is not a number when a unit is specified.
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void badNumberWithUnitLimitTest() {
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "fookb");
    }

    /**
     * Tests that cache fallback to disk when memory size limit has been reached.
     */
    @Test
    public void testDiskStore() {
        final MemoryMapCacheEngine engine = new MemoryMapCacheEngine();
        engine.init(true, -1, false, "16");
        engine.setTemporaryFileManager(temporaryFileManager.getTemporaryFileManager());

        final SizableNut r = Mockito.mock(SizableNut.class, Mockito.withSettings().serializable());
        Mockito.when(r.size()).thenReturn(4);
        Mockito.when(r.getSource()).thenReturn(new SourceImpl());

        final SizableNut o = Mockito.mock(SizableNut.class, Mockito.withSettings().serializable());
        Mockito.when(o.size()).thenReturn(4);
        Mockito.when(o.getSource()).thenReturn(new SourceImpl());

        final SizableNut n = Mockito.mock(SizableNut.class, Mockito.withSettings().serializable());
        Mockito.when(n.size()).thenReturn(4);
        Mockito.when(n.getReferencedNuts()).thenReturn(Arrays.asList((ConvertibleNut) r));
        Mockito.when(n.getSource()).thenReturn(new SourceImpl() { { addOriginalNut(o); } });

        final Map<String, AbstractCacheEngine.CacheResult.Entry> n1 = new HashMap<String, AbstractCacheEngine.CacheResult.Entry>();
        n1.put("a", new AbstractCacheEngine.CacheResult.Entry(n));

        final Map<String, AbstractCacheEngine.CacheResult.Entry> n2 = new HashMap<String, AbstractCacheEngine.CacheResult.Entry>();
        n2.put("b", new AbstractCacheEngine.CacheResult.Entry(o));

        final Map<String, AbstractCacheEngine.CacheResult.Entry> n3 = new HashMap<String, AbstractCacheEngine.CacheResult.Entry>();
        n3.put("c", new AbstractCacheEngine.CacheResult.Entry(r));

        final EngineRequest.Key k1 = new EngineRequest.Key("a", Arrays.asList(Mockito.mock(ConvertibleNut.class)));
        engine.putToCache(k1, new AbstractCacheEngine.CacheResult(n1, null));
        Assert.assertEquals(12, engine.getMemoryInUse());

        final EngineRequest.Key k2 = new EngineRequest.Key("b", Arrays.asList(Mockito.mock(ConvertibleNut.class)));
        engine.putToCache(k2, new AbstractCacheEngine.CacheResult(n2, null));
        Assert.assertEquals(16, engine.getMemoryInUse());

        final EngineRequest.Key k3 = new EngineRequest.Key("c", Arrays.asList(Mockito.mock(ConvertibleNut.class)));
        engine.putToCache(k3, new AbstractCacheEngine.CacheResult(n3, null));
        Assert.assertEquals(16, engine.getMemoryInUse());

        Assert.assertNotNull(engine.getFromCache(k1));
        Assert.assertNotNull(engine.getFromCache(k2));
        Assert.assertNotNull(engine.getFromCache(k3));

        engine.removeFromCache(k1);
        Assert.assertEquals(4, engine.getMemoryInUse());

        engine.removeFromCache(k3);
        Assert.assertEquals(4, engine.getMemoryInUse());

        Assert.assertNull(engine.getFromCache(k1));
        Assert.assertNotNull(engine.getFromCache(k2));
        Assert.assertNull(engine.getFromCache(k3));
    }
}
