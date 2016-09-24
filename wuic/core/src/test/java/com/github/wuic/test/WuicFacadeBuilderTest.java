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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ProcessContext;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.context.SimpleContextBuilderConfigurator;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.core.HttpNutDao;
import com.github.wuic.util.MapPropertyResolver;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.PropertyResolver;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Tests for {@link com.github.wuic.WuicFacadeBuilder}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class WuicFacadeBuilderTest {

    /**
     * Process context.
     */
    @ClassRule
    public static ProcessContextRule processContext = new ProcessContextRule();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Asserts that wuic.xml not loaded.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void noXmlTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        Assert.assertTrue(builder.noConfigurationPath().build().workflowIds().isEmpty());
    }

    /**
     * <p>
     * Tests that properties are applied.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void propertiesFileTest() throws Exception {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder()
                .wuicPropertiesPath(getClass().getResource("/wuic-test.properties"));

        final List<ConvertibleNut> res = builder.build().runWorkflow("css-scripts", Mockito.mock(ProcessContext.class));
        Assert.assertNotNull(res);
        Assert.assertEquals(NumberUtils.TWO, res.size());

        for (final ConvertibleNut c : res) {
            Assert.assertEquals("UTF-16", c.getNutType().getCharset());
        }
    }

    /**
     * Asserts that wuic.xml loaded.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void xmlTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        Assert.assertFalse(builder.build().workflowIds().isEmpty());
    }

    /**
     * <p>
     * Test when an {@link IllegalStateException} is thrown because of bad URL.
     * </p>
     *
     * @throws WuicException if test fails
     */
    @Test(expected = IllegalStateException.class)
    public void testBadUrl() throws WuicException {
        new WuicFacadeBuilder(new PropertyResolver() {
            @Override
            public String resolveProperty(final String first) {
                return ApplicationConfig.WUIC_SERVLET_XML_PATH_PARAM.equals(first) ? "" : null;
            }
        }).build();
    }

    /**
     * <p>
     * Test facade when a bad default {@link com.github.wuic.nut.dao.NutDao} class is specified.
     * </p>
     *
     * @throws WuicException if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBadDefaultNutDaoClass() throws WuicException {
        new WuicFacadeBuilder(new PropertyResolver() {
            @Override
            public String resolveProperty(final String first) {
                return ApplicationConfig.WUIC_DEFAULT_NUT_DAO_CLASS.equals(first) ? String.class.getName() : null;
            }
        }).build();
    }

    /**
     * <p>
     * Test facade when a different default {@link com.github.wuic.nut.dao.NutDao} class is specified.
     * Since the {@link HttpNutDao} is supposed to be used, an {@link IllegalStateException} is expected as no HTTP
     * server is running.
     * </p>
     *
     * @throws WuicException if test fails
     */
    @Test(expected = IllegalStateException.class)
    public void testDefaultNutDaoClass() throws WuicException {
        new WuicFacadeBuilder(new PropertyResolver() {
            @Override
            public String resolveProperty(final String first) {
                return ApplicationConfig.WUIC_DEFAULT_NUT_DAO_CLASS.equals(first) ? HttpNutDao.class.getName() : null;
            }
        }).wuicConfigurationPath(getClass().getResource("/wuic-conventions.json")).build().runWorkflow("wf-heap", processContext.getProcessContext());
    }

    /**
     * Asserts that context path is correct.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void contextPathTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        Assert.assertEquals("/foo", builder.noConfigurationPath().contextPath("/foo").build().getContextPath());
    }

    /**
     * Asserts that {@link WuicFacadeBuilder#multipleConfigInTagSupport} is correct.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void disableMultipleConfigInTagSupportTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        Assert.assertTrue(builder.build().allowsMultipleConfigInTagSupport());
        builder.disableMultipleConfigInTagSupport();
        Assert.assertFalse(builder.build().allowsMultipleConfigInTagSupport());
    }

    /**
     * Asserts that additional configurator is called.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void configuratorTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        final AtomicInteger count = new AtomicInteger(0);
        builder.noConfigurationPath().contextBuilderConfigurators(new SimpleContextBuilderConfigurator() {
            @Override
            public int internalConfigure(final ContextBuilder ctxBuilder) {
                count.incrementAndGet();
                return 0;
            }
        }).contextPath("/foo").build();

        Assert.assertEquals(1, count.get());
    }

    /**
     * Executes several settings.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void propertiesTest() throws WuicException {
        final PropertyResolver propsTrue = Mockito.mock(PropertyResolver.class);
        Mockito.when(propsTrue.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT))).thenReturn("true");
        Mockito.when(propsTrue.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM))).thenReturn("/");
        Mockito.when(propsTrue.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_WARMUP_STRATEGY))).thenReturn(WuicFacade.WarmupStrategy.NONE.name());
        Mockito.when(propsTrue.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS))).thenReturn("true");
        Mockito.when(propsTrue.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_ADDITIONAL_BUILDER_CONFIGURATORS))).thenReturn("");
        Mockito.when(propsTrue.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_ADDITIONAL_BUILDER_INSPECTOR))).thenReturn("");
        new WuicFacadeBuilder(propsTrue).build();

        final PropertyResolver propsFalse = Mockito.mock(PropertyResolver.class);
        Mockito.when(propsFalse.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_SERVLET_MULTIPLE_CONG_IN_TAG_SUPPORT))).thenReturn("false");
        Mockito.when(propsFalse.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM))).thenReturn("/");
        Mockito.when(propsFalse.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_WARMUP_STRATEGY))).thenReturn(WuicFacade.WarmupStrategy.NONE.name());
        Mockito.when(propsFalse.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_USE_DEFAULT_CONTEXT_BUILDER_CONFIGURATORS))).thenReturn("false");
        Mockito.when(propsFalse.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_ADDITIONAL_BUILDER_CONFIGURATORS))).thenReturn("");
        Mockito.when(propsFalse.resolveProperty(Mockito.eq(ApplicationConfig.WUIC_ADDITIONAL_BUILDER_INSPECTOR))).thenReturn("");
        new WuicFacadeBuilder(propsFalse).build();
    }

    /**
     * <p>
     * Test when internal context builder is used.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test
    public void useInternalContextBuilderTest() throws WuicException, IOException {
        WuicFacade wuicFacade = new WuicFacadeBuilder()
                .contextBuilder()
                .tag("internal")
                .processContext(processContext.getProcessContext())
                .contextEngineBuilder(TextAggregatorEngine.class)
                .property(ApplicationConfig.AGGREGATE, false)
                .toContext()
                .contextNutDaoBuilder(ClasspathNutDao.class)
                .property(ApplicationConfig.BASE_PATH, "/skipped/deep")
                .toContext()
                .heap("heap", ContextBuilder.getDefaultBuilderId(ClasspathNutDao.class), new String[] { "baz.js" })
                .releaseTag()
                .toFacade()
                .build();

        Assert.assertEquals(1, wuicFacade.runWorkflow("heap", processContext.getProcessContext()).size());
        Assert.assertNotEquals(wuicFacade.runWorkflow("heap", processContext.getProcessContext()).get(0).getName(), "aggregate.js");

        wuicFacade = new WuicFacadeBuilder()
                .objectBuilderInspector(new ObjectBuilderInspector() {
                    @Override
                    public <T> T inspect(T object) {
                        return object;
                    }
                })
                .contextBuilder()
                .tag("internal")
                .processContext(processContext.getProcessContext())
                .contextEngineBuilder(TextAggregatorEngine.class)
                .property(ApplicationConfig.AGGREGATE, false)
                .toContext()
                .contextNutDaoBuilder(ClasspathNutDao.class)
                .property(ApplicationConfig.BASE_PATH, "/skipped/deep")
                .toContext()
                .heap("heap", ContextBuilder.getDefaultBuilderId(ClasspathNutDao.class), new String[] { "baz.js" })
                .releaseTag()
                .toFacade()
                .build();

        Assert.assertEquals(1, wuicFacade.runWorkflow("heap", processContext.getProcessContext()).size());
        Assert.assertNotEquals(wuicFacade.runWorkflow("heap", processContext.getProcessContext()).get(0).getName(), "aggregate.js");
    }

    /**
     * <p>
     * Checks that profiles impact property files selection.
     * </p>
     *
     * @throws WuicException if test fails
     */
    @Test
    public void propertyProfilesTest() throws WuicException {
        Map<Object, Object> map = new HashMap<Object, Object>();
        WuicFacadeBuilder b = new WuicFacadeBuilder(new MapPropertyResolver(map)).noConfigurationPath();
        b.build();
        Assert.assertEquals("baz", b.getPropertyResolver().resolveProperty("foo"));
        Assert.assertEquals("test", b.getPropertyResolver().resolveProperty("test"));

        map = new HashMap<Object, Object>();
        b = new WuicFacadeBuilder(new MapPropertyResolver(map)).noConfigurationPath();
        map.put(ApplicationConfig.PROFILES, "foo");
        b.build();
        Assert.assertEquals("bar", b.getPropertyResolver().resolveProperty("foo"));
        Assert.assertEquals("test", b.getPropertyResolver().resolveProperty("test"));
    }

    /**
     * <p>
     * Checks that profiles impact confguration files selection.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void configurationProfileTest() throws WuicException, IOException {
        WuicFacadeBuilder b = new WuicFacadeBuilder();
        b.contextBuilder().enableProfile("configurationProfileTest").toFacade();
        Assert.assertEquals(9, b.build().workflowIds().size());
    }

    /**
     * <p>
     * Tests JMX operations.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void jmxTest() throws Exception {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        String charset = null;

        for (final Map.Entry<String, Charset> cs : Charset.availableCharsets().entrySet()) {
            if (!Charset.defaultCharset().equals(cs.getValue())) {
                charset = cs.getKey();
            }
        }

        map.put(ApplicationConfig.CHARSET, charset);
        final WuicFacadeBuilder b = new WuicFacadeBuilder(new MapPropertyResolver(map)).noConfigurationPath();
        final WuicFacade f = b.build();
        Assert.assertEquals(f.getNutTypeFactory().getCharset(), charset);

        // Exposes JMX bean to change global properties
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ObjectName propertyName = new ObjectName("com.github.wuic.jmx:type=PropertyResolver");
        mbs.invoke(propertyName,
                "addProperty",
                new Object[] { ApplicationConfig.CHARSET, Charset.defaultCharset().name() },
                new String[] {"java.lang.String", "java.lang.String"} );
        Assert.assertEquals(f.getNutTypeFactory().getCharset(), Charset.defaultCharset().name());
    }
}
