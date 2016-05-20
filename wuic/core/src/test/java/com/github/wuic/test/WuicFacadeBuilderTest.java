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
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.PropertyResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
     * Asserts that wuic.xml not loaded.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void noXmlTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        Assert.assertTrue(builder.noXmlConfiguration().build().workflowIds().isEmpty());
    }

    /**
     * <p>
     * Tests that properties are applied.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void propertiesFileTest() throws Exception {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder()
                .wuicPropertiesPath(getClass().getResource("/wuic-test.properties"));

        final List<ConvertibleNut> res = builder.build().runWorkflow("css-scripts", Mockito.mock(ProcessContext.class));
        Assert.assertNotNull(res);
        Assert.assertEquals(NumberUtils.TWO, res.size());

        for (final ConvertibleNut c : res) {
            Assert.assertEquals(1L, c.getVersionNumber().get().longValue());
        }
    }

    /**
     * Asserts that wuic.xml loaded.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
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
    @Test(timeout = 60000, expected = IllegalStateException.class)
    public void testBadUrl() throws WuicException {
        new WuicFacadeBuilder(new PropertyResolver() {
            @Override
            public String resolveProperty(final String first) {
                return ApplicationConfig.WUIC_SERVLET_XML_PATH_PARAM.equals(first) ? "" : null;
            }

            @Override
            public Collection<Object> getKeys() {
                return Arrays.asList((Object) ApplicationConfig.WUIC_SERVLET_XML_PATH_PARAM);
            }
        }).build();
    }

    /**
     * Asserts that context path is correct.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void contextPathTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        Assert.assertEquals("/foo", builder.noXmlConfiguration().contextPath("/foo").build().getContextPath());
    }

    /**
     * Asserts that additional configurator is called.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void configuratorTest() throws WuicException {
        final WuicFacadeBuilder builder = new WuicFacadeBuilder();
        final AtomicInteger count = new AtomicInteger(0);
        builder.noXmlConfiguration().contextBuilderConfigurators(new SimpleContextBuilderConfigurator(getClass().getName()) {
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
    @Test(timeout = 60000)
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
    @Test(timeout = 60000)
    public void useInternalContextBuilderTest() throws WuicException, IOException {
        WuicFacade wuicFacade = new WuicFacadeBuilder()
                .contextBuilder()
                .tag("internal")
                .processContext(ProcessContext.DEFAULT)
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

        Assert.assertEquals(1, wuicFacade.runWorkflow("heap", ProcessContext.DEFAULT).size());
        Assert.assertNotEquals(wuicFacade.runWorkflow("heap", ProcessContext.DEFAULT).get(0).getName(), "aggregate.js");

        wuicFacade = new WuicFacadeBuilder()
                .objectBuilderInspector(new ObjectBuilderInspector() {
                    @Override
                    public <T> T inspect(T object) {
                        return object;
                    }
                })
                .contextBuilder()
                .tag("internal")
                .processContext(ProcessContext.DEFAULT)
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

        Assert.assertEquals(1, wuicFacade.runWorkflow("heap", ProcessContext.DEFAULT).size());
        Assert.assertNotEquals(wuicFacade.runWorkflow("heap", ProcessContext.DEFAULT).get(0).getName(), "aggregate.js");
    }
}
