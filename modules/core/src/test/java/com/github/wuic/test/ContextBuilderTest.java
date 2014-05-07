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


package com.github.wuic.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.Context;
import com.github.wuic.ContextBuilder;
import com.github.wuic.NutType;
import com.github.wuic.engine.*;
import com.github.wuic.engine.core.TextAggregatorEngineBuilder;
import com.github.wuic.exception.*;
import com.github.wuic.nut.*;

import com.github.wuic.nut.filter.RegexRemoveNutFilterBuilder;
import com.github.wuic.util.AbstractBuilderFactory;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.*;

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
    private static final String NUT_NAME_ONE = "foo.js";

    /**
     * Test JS.
     */
    private static final String NUT_NAME_TWO = "test.js";

    /**
     * <p>
     * Mocked DAO builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public static final class MockEngineBuilder extends AbstractEngineBuilder {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Engine internalBuild() throws BuilderPropertyNotSupportedException {
            final NodeEngine engine = mock(NodeEngine.class);

            // Prepare Engine mock
            when(engine.getNutTypes()).thenReturn(Arrays.asList(NutType.JAVASCRIPT));
            when(engine.getEngineType()).thenReturn(EngineType.AGGREGATOR);
            try {
                when(engine.parse(any(EngineRequest.class))).thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        return ((EngineRequest) invocationOnMock.getArguments()[0]).getNuts();
                    }
                });
            } catch (WuicException e) {
                throw new IllegalStateException(e);
            }

            // Prepare Engine builder mock
            return engine;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void throwPropertyNotSupportedException(final String key) throws EngineBuilderPropertyNotSupportedException {

        }
    }

    /**
     * <p>
     * Mocked DAO builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public static final class MockDaoBuilder extends AbstractNutDaoBuilder {

        /**
         * {@inheritDoc}
         */
        @Override
        protected NutDao internalBuild() throws BuilderPropertyNotSupportedException {
            try {
                // Prepare Nut mock
                final Nut mockNutOne = mock(Nut.class);
                when(mockNutOne.getName()).thenReturn("foo.js");
                when(mockNutOne.getNutType()).thenReturn(NutType.JAVASCRIPT);
                when(mockNutOne.isAggregatable()).thenReturn(true);
                when(mockNutOne.isTextCompressible()).thenReturn(true);
                when(mockNutOne.isCacheable()).thenReturn(true);
                when(mockNutOne.isBinaryCompressible()).thenReturn(true);
                when(mockNutOne.openStream()).thenReturn(new ByteArrayInputStream("var foo;".getBytes()));
                when(mockNutOne.getVersionNumber()).thenReturn(new BigInteger("1"));

                final Nut mockNutTwo = mock(Nut.class);
                when(mockNutTwo.getName()).thenReturn("test.js");
                when(mockNutTwo.getNutType()).thenReturn(NutType.JAVASCRIPT);
                when(mockNutTwo.isAggregatable()).thenReturn(true);
                when(mockNutTwo.isTextCompressible()).thenReturn(true);
                when(mockNutTwo.isCacheable()).thenReturn(true);
                when(mockNutTwo.isBinaryCompressible()).thenReturn(true);
                when(mockNutTwo.openStream()).thenReturn(new ByteArrayInputStream("var test;".getBytes()));
                when(mockNutTwo.getVersionNumber()).thenReturn(new BigInteger("1"));

                // Prepare DAO mock
                final NutDao mockDao = mock(NutDao.class);


                final List<Nut> nutsOne = new ArrayList<Nut>();
                nutsOne.add(mockNutOne);
                when(mockDao.create(ContextBuilderTest.NUT_NAME_ONE)).thenReturn(nutsOne);

                final List<Nut> nutTwo = new ArrayList<Nut>();
                nutTwo.add(mockNutTwo);
                when(mockDao.create(NUT_NAME_TWO)).thenReturn(nutTwo);
                when(mockDao.saveSupported()).thenReturn(false);
                final NutDao mockStore = mock(NutDao.class);
                when(mockStore.saveSupported()).thenReturn(true);

                // Prepare DAO builder mock
                return mockDao;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void throwPropertyNotSupportedException(final String key) throws NutDaoBuilderPropertyNotSupportedException {
        }
    }

    /**
     * <p>
     * Mocked store DAO builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    public static final class MockStoreDaoBuilder extends AbstractNutDaoBuilder {

        /**
         * {@inheritDoc}
         */
        @Override
        protected NutDao internalBuild() throws BuilderPropertyNotSupportedException {
            final NutDao mockStore = mock(NutDao.class);
            when(mockStore.saveSupported()).thenReturn(true);

            // Prepare DAO builder mock
            return mockStore;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void throwPropertyNotSupportedException(final String key) throws NutDaoBuilderPropertyNotSupportedException {
        }
    }

    /**
     * Create some mocked object.
     */
    @Before
    public void prepareMocks() throws Exception {
        NutDaoBuilderFactory.getInstance().addBuilderClass(MockStoreDaoBuilder.class.getName());
        NutDaoBuilderFactory.getInstance().addBuilderClass(MockDaoBuilder.class.getName());
        EngineBuilderFactory.getInstance().addBuilderClass(MockEngineBuilder.class.getName());
    }

    /**
     * Nominal test with classic use.
     *
     * @throws Exception if test fails
     */
    @Test
    public void nominalTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                .toContext()
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        // Should be aggregated now
        Assert.assertTrue(context.process("", "workflow-heap").size() == 1);
    }

    /**
     * Nominal test with classic use but without default engine inclusion.
     *
     * @throws Exception if test fails
     */
    @Test
    public void withoutDefaultEnginesTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                .toContext()
                .template("tpl", new String[]{"engine"}, null, false)
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap").size() > 1);
    }

    /**
     * Test that uses filter.
     *
     * @throws Exception if test fails
     */
    @Test
    public void withFilterTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .contextNutFilterBuilder("exclude", RegexRemoveNutFilterBuilder.class.getSimpleName())
                .property(ApplicationConfig.REGEX_EXPRESSIONS, NUT_NAME_ONE)
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                .toContext()
                .template("tpl", new String[]{"engine"}, null, false)
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap").size() == 1);
    }


    /**
     * Test when a default engine is overridden.
     *
     * @throws Exception if test fails
     */
    @Test
    public void overrideDefaultEnginesTest() throws Exception {
        final String defaultName = AbstractBuilderFactory.ID_PREFIX + TextAggregatorEngineBuilder.class.getSimpleName();

        // Typical use : no exception should be thrown
        final ContextBuilder builder = new ContextBuilder();
        EngineBuilderFactory.getInstance().newContextBuilderConfigurator().configure(builder);
        builder.tag("test")
               .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
               .toContext()
               .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
               .contextEngineBuilder(defaultName, TextAggregatorEngineBuilder.class.getSimpleName())
               .property(ApplicationConfig.AGGREGATE, false)
               .toContext()
               .template("tpl", new String[]{defaultName})
               .workflow("workflow-", true, "heap", "tpl")
               .releaseTag();

        Assert.assertTrue(builder.build().process("", "workflow-heap").size() > 1);
    }

    /**
     * Tests with many heaps referenced by a workflow in a regex.
     *
     * @throws Exception if test fails
     */
    @Test
    public void regexTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap-one", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .heap("heap-two", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                .toContext()
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap-.*", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap-one").size() == 1);
        Assert.assertTrue(context.process("", "workflow-heap-two").size() == 1);
    }


    /**
     * Tests with an implicit heap created when no workflow refers it.
     *
     * @throws Exception if test fails
     */
    @Test
    public void implicitWorkflowTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap-one", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .heap("heap-two", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                .toContext()
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap-one", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap-one").size() == 1);
        Assert.assertTrue(context.process("", "heap-two").size() == 1);
    }

    /**
     * Tests with an empry chain.
     *
     * @throws Exception if test fails
     */
    @Test
    public void emptyChainTest() throws Exception {
        // Typical use : no exception should be thrown
        final Context context = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .template("tpl", new String[] {}, null, false)
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build();

        Assert.assertTrue(context.process("", "workflow-heap").size() > 1);
    }

    /**
     * Checks when the context is up to date or not.
     */
    @Test
    public void upToDateTest() {
        final ContextBuilder builder = new ContextBuilder();
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
            new ContextBuilder()
                    .tag("test")
                    .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                    .toContext()
                    .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                    .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                    .toContext()
                    .template("tpl", new String[]{"engine"}, "dao")
                    .workflow("workflow", true, "heap", "tpl")
                    .releaseTag()
                    .build();
        } catch (Exception e) {
            // Normal behavior : mockDaoBuilder does not supports save()
        }

        new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .contextNutDaoBuilder("store", MockStoreDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
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
        final ContextBuilder builder = new ContextBuilder()
                .tag("test")
                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                .toContext()
                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
                .toContext()
                .releaseTag()
                .tag("tag")
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag();
        builder.build().process("", "workflow-heap");

        try {
            builder.clearTag("tag").build().process("workflow-heap", "");
            Assert.fail();
        } catch (WorkflowNotFoundException wnfe) {
            // Exception normally raised
        }

        builder.tag("test")
                .template("tpl", new String[]{"engine"})
                .workflow("workflow-", true, "heap", "tpl")
                .releaseTag()
                .build()
                .process("", "workflow-heap");

        try {
            builder.clearTag("test").build().process("workflow-heap", "");
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
            new ContextBuilder().contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName()).toContext();
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
        final ContextBuilder builder = new ContextBuilder();
        final int threadNumber = 2000;
        final Thread[] threads = new Thread[threadNumber];

        for (int i = 0; i < threadNumber; i = i + 4) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        builder.tag("test")
                            .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                            .toContext()
                            .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                            .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
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
                                .contextNutDaoBuilder("dao", MockDaoBuilder.class.getSimpleName())
                                .toContext()
                                .heap("heap", "dao", NUT_NAME_ONE, NUT_NAME_TWO)
                                .contextEngineBuilder("engine", MockEngineBuilder.class.getSimpleName())
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
