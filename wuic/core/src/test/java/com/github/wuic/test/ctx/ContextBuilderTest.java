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


package com.github.wuic.test.ctx;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.Context;
import com.github.wuic.ContextBuilder;

import com.github.wuic.ContextInterceptor;
import com.github.wuic.ContextInterceptorAdapter;
import com.github.wuic.Workflow;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.util.UrlUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link com.github.wuic.ContextBuilder} and {@link Context} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class ContextBuilderTest {

    /**
     * Foo JS.
     */
    static final String NUT_NAME_ONE = "foo.js";

    /**
     * Test JS.
     */
    static final String NUT_NAME_TWO = "test.js";

    /**
     * Engine builder.
     */
    private static ObjectBuilderFactory<Engine> engineBuilderFactory;

    /**
     * NutDao builder.
     */
    private static ObjectBuilderFactory<NutDao> nutDaoBuilderFactory;

    /**
     * NutFilter builder.
     */
    private static ObjectBuilderFactory<NutFilter> nutFilterBuilderFactory;

    /**
     * Create some mocked object.
     */
    @BeforeClass
    public static void prepareMocks() throws Exception {
        engineBuilderFactory = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class, TextAggregatorEngine.class);
        nutDaoBuilderFactory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class, MockStoreDao.class);
        nutFilterBuilderFactory = new ObjectBuilderFactory<NutFilter>(NutFilterService.class, NutFilterService.DEFAULT_SCAN_PACKAGE);
    }

    /**
     * Nominal test with classic use.
     *
     * @throws Exception if test fails
     */
    @Test
    public void nominalTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .configureDefault()
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        // Should be aggregated now
        Assert.assertTrue(context.process("", "workflow-heap", UrlUtils.urlProviderFactory()).size() == 1);
    }

    /**
     * <p>
     * Test {@link ContextInterceptor} usage.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void interceptorTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(4);
        final List<ConvertibleNut> nuts = new ArrayList<ConvertibleNut>();
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);

        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .interceptor(new ContextInterceptorAdapter() {
                    @Override
                    public EngineRequest beforeProcess(final EngineRequest request) {
                        super.beforeProcess(request);
                        count.decrementAndGet();
                        return request;
                    }

                    @Override
                    public EngineRequest beforeProcess(final EngineRequest request, final String path) {
                        super.beforeProcess(request, path);
                        count.decrementAndGet();
                        return request;
                    }

                    @Override
                    public List<ConvertibleNut> afterProcess(final List<ConvertibleNut> n) {
                        super.afterProcess(n);
                        count.decrementAndGet();
                        return nuts;
                    }

                    @Override
                    public String beforeGetWorkflow(final String wId) {
                        super.beforeGetWorkflow(wId);
                        return wId;
                    }

                    @Override
                    public Workflow afterGetWorkflow(final String id, final Workflow workflow) {
                        super.afterGetWorkflow(id, workflow);
                        return workflow;
                    }

                    @Override
                    public ConvertibleNut afterProcess(final ConvertibleNut n, final String path) {
                        super.afterProcess(n, path);
                        count.decrementAndGet();
                        return nut;
                    }
                }).releaseTag().build();

        Assert.assertEquals(nuts, context.process("", "heap", UrlUtils.urlProviderFactory()));
        Assert.assertEquals(2, count.get());

        Assert.assertEquals(nut, context.process("", "heap", "", UrlUtils.urlProviderFactory()));
        Assert.assertEquals(0, count.get());
    }

    /**
     * Nominal test with classic use but without default engine inclusion.
     *
     * @throws Exception if test fails
     */
    @Test
    public void withoutDefaultEnginesTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .template("tpl", new String[]{"engine"}, null, false)
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap", UrlUtils.urlProviderFactory()).size() > 1);
    }

    /**
     * Test that uses filter.
     *
     * @throws Exception if test fails
     */
    @Test
    public void withFilterTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .contextNutFilterBuilder("exclude", "RegexRemoveNutFilterBuilder")
                .property(ApplicationConfig.REGEX_EXPRESSIONS, NUT_NAME_ONE)
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .template("tpl", new String[]{"engine"}, null, false)
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap", UrlUtils.urlProviderFactory()).size() == 1);
    }

    /**
     * Test when a default engine is overridden.
     *
     * @throws Exception if test fails
     */
    @Test
    public void overrideDefaultEnginesTest() throws Exception {
        // Typical use : no exception should be thrown
        final ContextBuilder builder = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory).configureDefault();
        builder.tag("test")
               .contextNutDaoBuilder("dao", "MockDaoBuilder")
               .toContext()
               .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
               .contextEngineBuilder(TextAggregatorEngine.class)
               .property(ApplicationConfig.AGGREGATE, false)
               .toContext()
               .template("tpl", new String[]{ ContextBuilder.getDefaultBuilderId(TextAggregatorEngine.class) })
               .workflow("workflow-", true, "heap", "tpl")
               .releaseTag();

        Assert.assertTrue(builder.build().process("", "workflow-heap", UrlUtils.urlProviderFactory()).size() > 1);
    }

    /**
     * Tests with many heaps referenced by a workflow in a regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void regexTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .configureDefault()
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap-one", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .heap("heap-two", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap-.*", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap-one", UrlUtils.urlProviderFactory()).size() == 1);
        Assert.assertTrue(context.process("", "workflow-heap-two", UrlUtils.urlProviderFactory()).size() == 1);
    }


    /**
     * Test when a template is not found.
     *
     * @throws Exception if test fails
     */
    @Test(expected = WorkflowTemplateNotFoundException.class)
    public void workflowTemplateNotFoundTest() throws Exception {
        new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .configureDefault()
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap-one", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .workflow("workflow-", true, "heap-one", "tpl")
                .releaseTag()
                .build();
    }

    /**
     * Tests with an implicit heap created when no workflow refers it.
     *
     * @throws Exception if test fails
     */
    @Test
    public void implicitWorkflowTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .configureDefault()
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap-one", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .heap("heap-two", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap-one", "tpl")
                .releaseTag()
                .build();

        Assert.assertEquals(1, context.process("", "workflow-heap-one", UrlUtils.urlProviderFactory()).size());
        Assert.assertEquals(1, context.process("", "heap-two", UrlUtils.urlProviderFactory()).size());
    }

    /**
     * Tests with an empry chain.
     *
     * @throws Exception if test fails
     */
    @Test
    public void emptyChainTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .template("tpl", new String[] {}, null, false)
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap", UrlUtils.urlProviderFactory()).size() > 1);
    }

    /**
     * Checks when the context is up to date or not.
     */
    @Test
    public void upToDateTest() {
        final ContextBuilder builder = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory);
        final Context context = builder.build();
        Assert.assertTrue(context.isUpToDate());

        Assert.assertTrue(builder.tag("test").releaseTag().build().isUpToDate());
        Assert.assertFalse(context.isUpToDate());
    }


    /**
     * Checks when the context is configured with DAO stores.
     *
     * @throws Exception if test fails
     */
    @Test
    public void withStoreTest() throws Exception {
        try {
            new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                    .tag("test")
                    .contextNutDaoBuilder("dao", "MockDaoBuilder")
                    .toContext()
                    .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                    .contextEngineBuilder("engine", "MockEngineBuilder")
                    .toContext()
                    .template("tpl", new String[]{"engine"}, "dao")
                    .workflow("workflow", true, "heap", "tpl")
                    .releaseTag()
                    .build();
        } catch (Exception e) {
            // Normal behavior : mockDaoBuilder does not supports save()
        }

        new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .contextNutDaoBuilder("store", "MockStoreDaoBuilder")
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .template("tpl", new String[]{"engine"}, "store")
                .workflow("workflow", true, "heap", "tpl")
                .releaseTag()
                .build();
    }

    /**
     * Test settings erasure.
     * @throws Exception if test fails
     */
    @Test
    public void testClearTag() throws Exception {
        final ContextBuilder builder = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory)
                .tag("test")
                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", "MockEngineBuilder")
                .toContext()
                .releaseTag()
                .tag("tag")
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag();
        builder.build().process("", "workflow-heap", UrlUtils.urlProviderFactory());

        try {
            builder.clearTag("tag").build().process("workflow-heap", "", UrlUtils.urlProviderFactory());
            Assert.fail();
        } catch (WorkflowNotFoundException wnfe) {
            // Exception normally raised
        }

        builder.tag("test")
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build()
                .process("", "workflow-heap", UrlUtils.urlProviderFactory());

        try {
            builder.clearTag("test").build().process("workflow-heap", "", UrlUtils.urlProviderFactory());
            Assert.fail();
        } catch (WorkflowNotFoundException wnfe) {
            // Exception normally raised
        }
    }

    /**
     * Test when a builder is used without any tag.
     */
    @Test
    public void unTaggedUsageTest() {
        try {
            new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory).contextNutDaoBuilder("dao", "MockDaoBuilder").toContext();
        } catch (Exception e) {
            // Normal behavior
        }
    }

    /**
     * Test some concurrent accesses.
     *
     * @throws InterruptedException if test fails
     */
    @Test
    public void concurrentTest() throws InterruptedException {
        final ContextBuilder builder = new ContextBuilder(engineBuilderFactory, nutDaoBuilderFactory, nutFilterBuilderFactory);
        final int threadNumber = 2000;
        final Thread[] threads = new Thread[threadNumber];

        for (int i = 0; i < threadNumber; i = i + 4) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        builder.tag("test")
                            .contextNutDaoBuilder("dao", "MockDaoBuilder")
                            .toContext()
                            .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                            .contextEngineBuilder("engine", "MockEngineBuilder")
                            .toContext()
                            .template("tpl", new String[]{"engine"})
                            .workflow("workflow", true, "heap", "tpl")
                            .releaseTag()
                            .build();
                    } catch (Exception e) {
                        System.out.println(Thread.currentThread().toString());
                        e.printStackTrace(System.out);
                        Assert.fail();
                    }
                }
            });

            threads[i + 1] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        builder.tag("foo")
                                .contextNutDaoBuilder("dao", "MockDaoBuilder")
                                .toContext()
                                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                                .contextEngineBuilder("engine", "MockEngineBuilder")
                                .toContext()
                                .template("tpl", new String[]{"engine"})
                                .workflow("workflow", true, "heap", "tpl")
                                .releaseTag()
                                .build();
                    } catch (Exception e) {
                        Assert.fail();
                    }
                }
            });

            threads[i + 2] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        builder.clearTag("test");
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        Assert.fail();
                    }
                }
            });

            threads[i + 3] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        builder.clearTag("foo");
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        Assert.fail();
                    }
                }
            });
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait all threads
        for (Thread t : threads) {
            t.join();
        }
    }
}
