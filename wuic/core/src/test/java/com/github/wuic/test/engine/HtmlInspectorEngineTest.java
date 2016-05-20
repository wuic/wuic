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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.Workflow;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.context.Context;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.core.AbstractCacheEngine;
import com.github.wuic.engine.core.AssetsMarkupHandler;
import com.github.wuic.engine.core.AssetsMarkupParser;
import com.github.wuic.engine.core.HtmlInspectorEngine;
import com.github.wuic.engine.core.MemoryMapCacheEngine;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.core.DiskNutDao;
import com.github.wuic.nut.dao.core.ProxyNutDao;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.StringUtils;
import com.github.wuic.util.UrlProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <p>
 * Tests the {@link com.github.wuic.engine.core.HtmlInspectorEngine} class.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
@RunWith(JUnit4.class)
public class HtmlInspectorEngineTest {

    /**
     * The regex that should match HTML when transformed during tests.
     */
    private static final String REGEX = ".*?<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<script type=\"text/javascript\" src=\"/.*?aggregate.js\"></script>.*?" +
            "<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<img width=\"50%\" height=\"60%\" src=\".*?\\d.*?png\" />.*?" +
            "<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<script type=\"text/javascript\" src=\"/.*?aggregate.js\"></script>.*?" +
            "<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<script type=\"text/javascript\" src=\"/.*?aggregate.js\"></script>.*?";


    /**
     * <p>
     * Creates a {@link com.github.wuic.context.Context} for test purpose.
     * </p>
     *
     * @return the context
     * @throws com.github.wuic.exception.WorkflowNotFoundException won't occurs
     * @throws java.io.IOException won't occurs
     */
    private Context newContext() throws WorkflowNotFoundException, IOException {
        final Context ctx = Mockito.mock(Context.class);
        final NutsHeap h = Mockito.mock(NutsHeap.class);
        final Nut n = Mockito.mock(Nut.class);
        Mockito.when(n.openStream()).thenReturn(new ByteArrayInputStream("var workflow = '';".getBytes()));
        Mockito.when(n.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(n.getInitialName()).thenReturn("workflow.js");
        Mockito.when(n.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(h.getNuts()).thenReturn(Arrays.asList(n));
        Mockito.when(h.getId()).thenReturn("workflow");
        final Workflow workflow = new Workflow(null, new HashMap<NutType, NodeEngine>(), h);
        Mockito.when(ctx.getWorkflow(Mockito.anyString())).thenReturn(workflow);
        return ctx;
    }

    /**
     * <p>
     * Tests server hint.
     * </p>
     *
     * @throws Exception if test fauls
     */
    @Test(timeout = 60000)
    public void hintTest() throws Exception {
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        final ProxyNutDao proxy = new ProxyNutDao("", dao);
        final String html = IOUtils.readString(new InputStreamReader(getClass().getResourceAsStream("/html/index.html")));
        Assert.assertTrue(html.contains("<html "));
        Assert.assertTrue(html.contains("<head>"));
        assertHintAppCache(proxy, html);
    }

    /**
     * <p>
     * Tests server hint.
     * </p>
     *
     * @throws Exception if test fauls
     */
    @Test(timeout = 60000)
    public void hintNoHeadTest() throws Exception {
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        final ProxyNutDao proxy = new ProxyNutDao("", dao);
        final String html = IOUtils.readString(new InputStreamReader(getClass().getResourceAsStream("/html/index.html")));
        Assert.assertTrue(html.contains("<html "));
        Assert.assertTrue(html.contains("<head>"));
        assertHintAppCache(proxy, html.replace("<head>", ""));
    }

    /**
     * <p>
     * Performs transformations and hint/application cache detection.
     * </p>
     *
     * @param proxy the proxy
     * @param html the HTML content
     * @throws com.github.wuic.exception.WuicException if test fails
     * @throws java.io.IOException if test fails
     */
    private void assertHintAppCache(final ProxyNutDao proxy, final String html) throws WuicException, IOException {
        final Nut bytes = new ByteArrayNut(html.getBytes(), "index.html", NutType.HTML, 1L, false);
        proxy.addRule("index.html", bytes);

        final NutsHeap heap = new NutsHeap(this, Arrays.asList("index.html"), proxy, "heap");
        heap.checkFiles(ProcessContext.DEFAULT);
        final EngineRequest request = new EngineRequestBuilder("workflow", heap, newContext()).processContext(ProcessContext.DEFAULT).build();
        final HtmlInspectorEngine e = new HtmlInspectorEngine();
        e.init(true, true);
        final List<ConvertibleNut> nuts = e.parse(request);

        Assert.assertEquals(1, nuts.size());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final ConvertibleNut nut = nuts.get(0);
        nut.transform(new Pipe.DefaultOnReady(os));
        final String content = new String(os.toByteArray());
        Assert.assertNotNull(nut.getReferencedNuts());
        Assert.assertFalse(nut.getReferencedNuts().isEmpty());
        final UrlProvider p = request.getUrlProviderFactory().create("workflow");

        Assert.assertTrue(content.contains("<html manifest="));
        for (final ConvertibleNut n : nut.getReferencedNuts()) {
            if (n.getNutType() != NutType.APP_CACHE) {
                Assert.assertTrue(content.contains("<link rel=\"preload\" href=\"/" + p.getUrl(n) + "\""));
            }
        }
    }

    /**
     * <p>
     * Complete parse test.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void parseTest() throws Exception {
        final Context ctx = newContext();
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        final NutsHeap heap = new NutsHeap(this, Arrays.asList("index.html"), dao, "heap");
        heap.checkFiles(ProcessContext.DEFAULT);
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        final TextAggregatorEngine css = new TextAggregatorEngine();
        css.init(true);
        css.async(true);
        chains.put(NutType.CSS, css);

        final TextAggregatorEngine jse = new TextAggregatorEngine();
        jse.init(true);
        jse.async(true);
        chains.put(NutType.JAVASCRIPT, jse);
        final EngineRequest request = new EngineRequestBuilder("workflow", heap, ctx).chains(chains).processContext(ProcessContext.DEFAULT).build();

        final HtmlInspectorEngine e = new HtmlInspectorEngine();
        e.init(true, true);
        final List<ConvertibleNut> nuts = e.parse(request);

        Assert.assertEquals(1, nuts.size());
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final ConvertibleNut nut = nuts.get(0);
        nut.transform(new Pipe.DefaultOnReady(os));
        final String content = new String(os.toByteArray());
        Assert.assertTrue(content, Pattern.compile(REGEX, Pattern.DOTALL).matcher(content).matches());
        Assert.assertNotNull(nut.getReferencedNuts());
        Assert.assertEquals(13, nut.getReferencedNuts().size());

        final ConvertibleNut js = nut.getReferencedNuts().get(11);
        Assert.assertEquals(js.getInitialNutType(), NutType.JAVASCRIPT);
        final String script = IOUtils.readString(new InputStreamReader(js.openStream()));

        Assert.assertTrue(script, script.contains("console.log(i);"));
        Assert.assertTrue(script, script.contains("i+=3"));
        Assert.assertTrue(script, script.contains("i+=4"));
    }

    /**
     * <p>
     * Tests cached transformation by added transformer.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void transformationCacheTest() throws Exception {
        final String[] script = new String[] {"<script src='foo.js'></script>",
                "<script src='bar.js'></script>",
                "<script src='baz.js'></script>" };
        final byte[] bytes = StringUtils.merge(script, "\n").getBytes();

        final HtmlInspectorEngine engine = new HtmlInspectorEngine();
        engine.init(true, true);
        engine.setParser(new AssetsMarkupParser() {
            @Override
            public void parse(final Reader reader, final AssetsMarkupHandler handler) {
                handler.handleScriptLink("foo.js", new HashMap<String, String>(), 1, 1, 1, script[0].length() + 1);
                handler.handleScriptLink("bar.js", new HashMap<String, String>(), 2, 1, 2, script[1].length() + 1);
                handler.handleScriptLink("baz.js", new HashMap<String, String>(), 3, 1, 3, script[2].length() + 1);
            }
        });

        ConvertibleNut nut = new ByteArrayNut(bytes, "index.html", NutType.HTML, 1L, true);
        final ConvertibleNut finalNut = nut;
        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(ProcessContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final String name = invocationOnMock.getArguments()[0].toString();

                if (finalNut.getInitialName().equals(name)) {
                    return Arrays.asList(finalNut);
                }

                final Nut n = Mockito.mock(Nut.class);
                Mockito.when(n.getVersionNumber()).thenReturn(new FutureLong(1L));
                Mockito.when(n.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
                Mockito.when(n.getInitialName()).thenReturn(name);
                Mockito.when(n.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                return Arrays.asList(n);
            }
        });

        final NutsHeap heap = new NutsHeap(this, Arrays.asList("index.html"), dao, "heap");
        heap.checkFiles(ProcessContext.DEFAULT);

        // First call
        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("", heap, null).build());
        Assert.assertEquals(1, res.size());
        String content = NutUtils.readTransform(res.get(0));
        Assert.assertNotNull(res.get(0).getReferencedNuts());
        Assert.assertEquals(3, res.get(0).getReferencedNuts().size());
        Assert.assertTrue(content.contains("1/000000004034EFC6foo.js"));
        Assert.assertTrue(content.contains("1/000000004034EFC6bar.js"));
        Assert.assertTrue(content.contains("1/000000004034EFC6baz.js"));

        // Second call
        final Pipe.Transformer<ConvertibleNut> transformer = res.get(0).getTransformers().iterator().next();
        nut = new ByteArrayNut(bytes, "index.html", NutType.HTML, 1L, true);
        nut.addTransformer(transformer);
        content = NutUtils.readTransform(nut);
        Assert.assertNull(nut.getReferencedNuts());
        Assert.assertTrue(content.contains("1/000000004034EFC6foo.js"));
        Assert.assertTrue(content.contains("1/000000004034EFC6bar.js"));
        Assert.assertTrue(content.contains("1/000000004034EFC6baz.js"));
    }

    /**
     * <p>
     * Tests the best effort support.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void bestEffortTest() throws Exception {
        final String r1 = "<script type=\"text/javascript\" src=\"/0/000000002B702562foo.ts.js\"></script>";
        final String r2 = "<script type=\"text/javascript\" src=\"/1/best-effort/workflowbest-effort/aggregate.js\"></script>";
        final String content = IOUtils.readString(new InputStreamReader(getClass().getResourceAsStream("/html/index.html")))
                .replace("<script src=\"script/foo.ts\"></script>", r1)
                .replace("<wuic:html-import workflowId=\"heap\"/>", r2);

        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final AbstractCacheEngine engine = new AbstractCacheEngine() {

            private CacheResult cache;

            /**
             * {@inheritDoc}
             */
            @Override
            public void putToCache(final EngineRequest.Key request, final CacheResult nuts) {
                cache = nuts;

                if (nuts.getDefaultResult() != null) {
                    countDownLatch.countDown();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void removeFromCache(final EngineRequest.Key request) {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CacheResult getFromCache(final EngineRequest.Key request) {
                return cache;
            }
        };

        engine.init(true, true);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        final Nut nut = new ByteArrayNut(content.getBytes(), "index.html", NutType.HTML, 1L, false);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        Mockito.when(heap.getNutDao()).thenReturn(dao);
        Mockito.when(heap.findDaoFor(Mockito.any(Nut.class))).thenReturn(dao);
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        final HtmlInspectorEngine e = new HtmlInspectorEngine();
        e.init(true, true);
        e.setParser(new AssetsMarkupParser() {
            @Override
            public void parse(final Reader reader, final AssetsMarkupHandler handler) {
                handler.handleLink("http://bar.com/script.css", new HashMap<String, String>(), 3, 9, 3, 49);

                Map<String, String> attr = new LinkedHashMap<String, String>();
                attr.put("type", "text/javascript");
                handler.handleScriptLink("http://foo.com/script.js", attr, 4, 9, 4, 72);

                handler.handleLink("favicon.ico", new HashMap<String, String>(), 5, 9, 5, 36);
                handler.handleLink("script/script1.css", new HashMap<String, String>(), 6, 9, 6, 44);
                handler.handleComment("<!-- some comments -->".toCharArray(), 7, 9, 24);

                attr = new LinkedHashMap<String, String>();
                attr.put("rel", "text/css");
                handler.handleLink("script/script2.css", new HashMap<String, String>(), 8, 9, 8, 59);

                attr = new LinkedHashMap<String, String>();
                attr.put("type", "text/javascript");
                handler.handleScriptLink("script/script1.js", attr, 10, 9, 10, 68);

                handler.handleScriptLink("/0/000000002B702562foo.ts.js", new HashMap<String, String>(), 12, 9, 12, 84);
                handler.handleScriptLink("/1/best-effort/workflowbest-effort/aggregate.js", new HashMap<String, String>(), 13, 9, 13, 103);
                handler.handleCssContent("\n            .inner {\n            }\n        ".toCharArray(),
                        new HashMap<String, String>(), 14, 9, 16, 17);

                handler.handleImgLink("earth.jpg", new HashMap<String, String>(), 21, 9, 21, 31);

                attr = new LinkedHashMap<String, String>();
                attr.put("height", "60%");
                attr.put("width", "50%");
                handler.handleImgLink("template-img.png", attr, 22, 9, 22, 70);

                attr = new LinkedHashMap<String, String>();
                attr.put("rel", "stylesheet");
                handler.handleLink("script/script3.css?foo", new HashMap<String, String>(), 24, 5, 24, 59);

                handler.handleScriptLink("script/script2.js#bar", new HashMap<String, String>(), 25, 5, 25, 43);
                handler.handleLink("script/script4.css", new HashMap<String, String>(), 26, 5, 26, 35);
                handler.handleJavascriptContent("console.log(i);".toCharArray(), new HashMap<String, String>(), 27, 5, 27, 37);

                attr = new LinkedHashMap<String, String>();
                attr.put("type", "text/javascript");
                handler.handleScriptLink("script/script3.js", attr, 28, 5, 28, 69);

                handler.handleScriptLink("script/script4.js", new HashMap<String, String>(), 29, 5, 29, 37);
            }
        });
        chains.put(NutType.HTML, e);

        final TextAggregatorEngine css = new TextAggregatorEngine();
        css.init(true);
        css.async(true);
        chains.put(NutType.CSS, css);

        final TextAggregatorEngine jse = new TextAggregatorEngine();
        jse.init(true);
        jse.async(true);
        chains.put(NutType.JAVASCRIPT, jse);

        chains.put(NutType.TYPESCRIPT, new NodeEngine() {
            @Override
            public List<NutType> getNutTypes() {
                return Arrays.asList(NutType.TYPESCRIPT);
            }

            @Override
            public EngineType getEngineType() {
                return EngineType.CONVERTER;
            }

            @Override
            protected List<ConvertibleNut> internalParse(EngineRequest request) throws WuicException {
                final ConvertibleNut nut = new ByteArrayNut("".getBytes(), "foo.ts.js", NutType.JAVASCRIPT, 0L, false);
                return Arrays.asList(nut);
            }

            @Override
            public Boolean works() {
                return true;
            }
        });

        List<ConvertibleNut> nuts = engine.parse(new EngineRequestBuilder("", heap, newContext()).processContext(ProcessContext.DEFAULT).chains(chains).build());
        String res = NutUtils.readTransform(nuts.get(0));
        Assert.assertEquals(content.replace("<html ", "<html manifest=\"/1/index.html.appcache\" ").replace("\r\n", "\n"), res);

        countDownLatch.await(5, TimeUnit.SECONDS);

        nuts = engine.parse(new EngineRequestBuilder("", heap, null).chains(chains).build());
        res = NutUtils.readTransform(nuts.get(0));
        Assert.assertTrue(res, Pattern.compile(REGEX, Pattern.DOTALL).matcher(res).matches());
    }

    /**
     * <p>
     * Memory map support test for head engine.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void memoryMapSupportTest() throws Exception {
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, MemoryMapCacheEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("MemoryMapCacheEngineBuilder");
        builder.property(ApplicationConfig.BEST_EFFORT, true);
        final MemoryMapCacheEngine cache = (MemoryMapCacheEngine) builder.build();
        concurrencyTest(cache);
    }

    /**
     * <p>
     * Runs a concurrency test with given engine.
     * </p>
     *
     * @param cache the head engine
     */
    public void concurrencyTest(final Engine cache) throws Exception {
        final Context ctx = newContext();
        final String content = IOUtils.readString(new InputStreamReader(getClass().getResourceAsStream("/html/index.html")));
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        final CountDownLatch countDownLatch = new CountDownLatch(400);

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        final Nut nut = new ByteArrayNut(content.getBytes(), "index.html", NutType.HTML, 1L, false);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        Mockito.when(heap.getNutDao()).thenReturn(dao);
        Mockito.when(heap.findDaoFor(Mockito.any(Nut.class))).thenReturn(dao);
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        final HtmlInspectorEngine e = new HtmlInspectorEngine();
        e.init(true, true);
        chains.put(NutType.HTML, e);

        for (long i = countDownLatch.getCount(); i > 0; i--) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        cache.parse(new EngineRequestBuilder("", heap, ctx).processContext(ProcessContext.DEFAULT).chains(chains).build());
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail(e.getMessage());
                    }
                }
            }).run();
        }

        Assert.assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
    }

    /**
     * <p>
     * Tests when heap created during transformation should be strongly referenced and when it should not be the case.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void heapListenerHolderTest() throws Exception {
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        final NutsHeap heap = new NutsHeap(this, Arrays.asList("index.html"), dao, "heap");
        heap.checkFiles(ProcessContext.DEFAULT);
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        final TextAggregatorEngine css = new TextAggregatorEngine();
        css.init(true);
        css.async(true);
        chains.put(NutType.CSS, css);

        final TextAggregatorEngine jse = new TextAggregatorEngine();
        jse.init(true);
        jse.async(true);
        chains.put(NutType.JAVASCRIPT, jse);

        final EngineRequest request = new EngineRequestBuilder("workflow", heap, newContext()).processContext(ProcessContext.DEFAULT).chains(chains).build();
        final HtmlInspectorEngine h = new HtmlInspectorEngine();
        h.init(true, true);
        final List<ConvertibleNut> nuts = h.parse(request);
        Assert.assertEquals(1, nuts.size());
        final ConvertibleNut nut = nuts.get(0);
        nut.transform(new Pipe.DefaultOnReady(new OutputStream() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void write(final int b) throws IOException {
            }
        }));

        // Make sure heap created on the fly are strongly referenced somewhere
        Assert.assertEquals(5, dao.getNutObservers().size());
        System.gc();
        Thread.sleep(500L);
        Assert.assertEquals(5, dao.getNutObservers().size());
        NutsHeap.ListenerHolder.INSTANCE.clear();
        heap.notifyListeners(heap);
        System.gc();
        Thread.sleep(500L);
        Assert.assertEquals(3, dao.getNutObservers().size());
    }
}
