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


package com.github.wuic.test.config;

import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.config.bean.BuilderBean;
import com.github.wuic.config.bean.HeapBean;
import com.github.wuic.config.bean.PropertyBean;
import com.github.wuic.config.bean.WorkflowBean;
import com.github.wuic.config.bean.WorkflowTemplateBean;
import com.github.wuic.config.bean.WuicBean;
import com.github.wuic.config.bean.json.BeanContextBuilderConfigurator;
import com.github.wuic.config.bean.json.FileJsonContextBuilderConfigurator;
import com.github.wuic.config.bean.json.ReaderJsonContextBuilderConfigurator;
import com.github.wuic.config.bean.xml.ReaderXmlContextBuilderConfigurator;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CssInspectorEngine;
import com.github.wuic.engine.core.GzipEngine;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.core.DiskNutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.test.xml.MockDao;
import com.github.wuic.test.xml.MockEngine;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.config.bean.xml.FileXmlContextBuilderConfigurator;
import com.github.wuic.util.PropertyResolver;
import com.github.wuic.util.UrlUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * <p>
 * This class makes tests on top of configurations specified in JSON or XML.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@RunWith(Theories.class)
public class ConfigBeanTest {

    /**
     * To assert exception.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Classes that load configuration files for beans.
     */
    @DataPoints("cfgClass")
    public static Class[] cfgClass = new Class[] {
            FileXmlContextBuilderConfigurator.class, FileJsonContextBuilderConfigurator.class
    };

    /**
     * <p>
     * Data points for a full set of configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("full")
    public static BeanContextBuilderConfigurator[] full() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-full.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-full.json"))
        };
    }

    /**
     * <p>
     * Data points for a set of default builders configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("withDefaultBuilder")
    public static BeanContextBuilderConfigurator[] withDefaultBuilder() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-with-default-builder.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-with-default-builder.json"))
        };
    }

    /**
     * <p>
     * Data points for a set of configurations with placeholders.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("placeholder")
    public static BeanContextBuilderConfigurator[] placeholder() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-placeholder.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-placeholder.json"))
        };
    }

    /**
     * <p>
     * Data points for a set of configurations with bindings.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("bind")
    public static BeanContextBuilderConfigurator[] bind() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-bind.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-bind.json"))
        };
    }

    /**
     * <p>
     * Data points for a set of basic configurations loaded with a reader.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("basic-reader")
    public static BeanContextBuilderConfigurator[] basic() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new ReaderXmlContextBuilderConfigurator.Simple(new FileReader(new File(ConfigBeanTest.class.getResource("/wuic-basic.xml").getFile())), "tag", true, ProcessContext.DEFAULT),
                new ReaderJsonContextBuilderConfigurator.Simple(new FileReader(new File(ConfigBeanTest.class.getResource("/wuic-basic.json").getFile())), "tag")
        };
    }

    /**
     * <p>
     * Data points for a set of filter configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("filter")
    public static BeanContextBuilderConfigurator[] filter() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-filter.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-filter.json"))
        };
    }

    /**
     * <p>
     * Data points for a set of composition configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("composition")
    public static BeanContextBuilderConfigurator[] composition() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-heap-composition.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-heap-composition.json"))
        };
    }

    /**
     * <p>
     * Data points for a deep set of configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("composition-workflow")
    public static BeanContextBuilderConfigurator[] deep() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-deep.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-deep.json"))
        };
    }

    /**
     * <p>
     * Data points for a set of configurations without specific engine.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("without")
    public static BeanContextBuilderConfigurator[] without() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-without.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-without.json"))
        };
    }

    /**
     * <p>
     * Tests a workflow built on top of a composition.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void compositionByWorkflowTest(
            @FromDataPoints("composition-workflow") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        contextBuilderConfigurator.configure(builder);
        final Context ctx = builder.build();
        ctx.process("", "composite", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
    }

    /**
     * <p>
     * Data points for a set of conventional configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints("conventions")
    public static BeanContextBuilderConfigurator[] conventions() throws Exception {
        return new BeanContextBuilderConfigurator[] {
                new FileXmlContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-conventions.xml")),
                new FileJsonContextBuilderConfigurator(ConfigBeanTest.class.getResource("/wuic-conventions.json"))
        };
    }

    /**
     * <p>
     * Detailed assertions tests on bean state after mapping with JAXB.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void fullTest(@FromDataPoints("full") final BeanContextBuilderConfigurator contextBuilderConfigurator) throws Exception {
        final WuicBean bean = contextBuilderConfigurator.getWuicBean();

        // Non-null
        Assert.assertNotNull(bean.getHeaps());
        Assert.assertNotNull(bean.getEngineBuilders());
        Assert.assertNotNull(bean.getDaoBuilders());
        Assert.assertNotNull(bean.getWorkflowTemplates());
        Assert.assertNotNull(bean.getPollingIntervalSeconds());

        // Number of elements
        Assert.assertEquals(NumberUtils.TWO, bean.getHeaps().size());
        Assert.assertEquals(NumberUtils.TWO, bean.getEngineBuilders().size());
        Assert.assertEquals(NumberUtils.TWO, bean.getDaoBuilders().size());
        Assert.assertEquals(NumberUtils.TWO, bean.getWorkflowTemplates().size());

        // Heap field
        for (HeapBean heap : bean.getHeaps()) {
            Assert.assertNotNull(heap.getId());
            Assert.assertNotNull(heap.getDaoBuilderId());
            Assert.assertNotNull(heap.getElements());
            Assert.assertEquals(NumberUtils.TWO, heap.getElements().size());
        }

        // DAO field
        for (int i = 0; i < bean.getDaoBuilders().size(); i++) {
            BuilderBean dao = bean.getDaoBuilders().get(i);
            Assert.assertNotNull(dao.getId());
            Assert.assertNotNull(dao.getType());

            if (i == 1) {
                Assert.assertNull(dao.getProperties());
                continue;
            }

            Assert.assertNotNull(dao.getProperties());
            Assert.assertEquals(1, dao.getProperties().size());

            // Property field
            for (PropertyBean prop : dao.getProperties()) {
                Assert.assertNotNull(prop.getKey());
                Assert.assertNotNull(prop.getValue());
            }
        }

        // Engine field
        for (int i = 0; i < bean.getEngineBuilders().size(); i++) {
            BuilderBean engine = bean.getEngineBuilders().get(i);
            Assert.assertNotNull(engine.getId());
            Assert.assertNotNull(engine.getType());

            if (i == 1) {
                Assert.assertNull(engine.getProperties());
                continue;
            }

            Assert.assertNotNull(engine.getProperties());
            Assert.assertEquals(1, engine.getProperties().size());

            // Property field
            for (PropertyBean prop : engine.getProperties()) {
                Assert.assertNotNull(prop.getKey());
                Assert.assertNotNull(prop.getValue());
            }
        }

        // Workflow template field
        for (int i = 0; i < bean.getWorkflowTemplates().size(); i++) {
            final WorkflowTemplateBean workflow = bean.getWorkflowTemplates().get(i);

            Assert.assertNotNull(workflow.getId());
            Assert.assertNotNull(workflow.getEngineBuilderIds());

            if (i == 1) {
                Assert.assertNull(workflow.getDaoBuilderIds());
                continue;
            }

            Assert.assertNotNull(workflow.getDaoBuilderIds());
            Assert.assertEquals(NumberUtils.TWO, workflow.getEngineBuilderIds().size());
            Assert.assertEquals(1, workflow.getDaoBuilderIds().size());
        }

        for (int i = 0; i < bean.getWorkflows().size(); i++) {
            final WorkflowBean workflow = bean.getWorkflows().get(i);

            Assert.assertNotNull(workflow.getWorkflowTemplateId());
            Assert.assertNotNull(workflow.getIdPrefix());
            Assert.assertNotNull(workflow.getHeapIdPattern());
        }
    }

    /**
     * <p>
     * Test configurator with a custom DAO/engine previously registered.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void configuratorWithCustomDaoAndEngineTest(@FromDataPoints("full") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        // Add custom DAO and engine
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        contextBuilderConfigurator.configure(builder);
    }

    /**
     * <p>
     * Test when a bad file is detected.
     * </p>
     *
     * @param clazz the class
     * @throws Exception if test fails
     */
    @Theory
    public void fileReadExceptionTest(@FromDataPoints("cfgClass") final Class<? extends BeanContextBuilderConfigurator> clazz)
            throws Exception {
        thrown.expect(IllegalArgumentException.class);
        clazz.getConstructor(URL.class).newInstance(null);
    }

    /**
     * <p>
     * Tests when default DAO.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void withDefaultDaoTest(@FromDataPoints("withDefaultBuilder") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        // File required default configuration
        final ContextBuilder builder = new ContextBuilder().configureDefault();

        contextBuilderConfigurator.configure(builder);
        builder.build().process("", "simpleWorkflowsimpleHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
    }

    /**
     * <p>
     * Tests when placeholders are used.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void withPlaceholdersTest(@FromDataPoints("placeholder") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        final PropertyResolver resolver = Mockito.mock(PropertyResolver.class);
        Mockito.when(resolver.resolveProperty("debug")).thenReturn("true");

        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);

        // File required default configuration
        final ContextBuilder builder = new ContextBuilder(new ContextBuilder(ebf, nbf, fbf), resolver).configureDefault();
        contextBuilderConfigurator.configure(builder);
        final MockDao dao = (MockDao) builder.build().getWorkflow("simpleHeap").getHeap().getNutDao();
        Assert.assertEquals(dao.getFoo(), "true");
        Assert.assertEquals(dao.getBar(), "false");
        Assert.assertEquals(dao.getBaz(), "baz");
    }

    /**
     * Tests a file referencing binding.
     *
     * @param contextBuilderConfigurator the context builder configurator
     * @throws Exception if test fails
     */
    @Theory
    public void bindTest(@FromDataPoints("bind") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        contextBuilderConfigurator.configure(builder);
        final Context ctx = builder.build();

        // Process implicit workflow with composed heaps
        ctx.process("", "bind", UrlUtils.urlProviderFactory(), null);
    }

    /**
     * <p>
     * Tests bean configuration through a reader.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void readerTest(@FromDataPoints("basic-reader") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        final ContextBuilder builder = new ContextBuilder();
        contextBuilderConfigurator.configure(builder);
    }

    /**
     * <p>
     * Tests bean configuration with filters.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void filterTest(@FromDataPoints("filter") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        // Add custom DAO and engine required
        final ContextBuilder builder = new ContextBuilder();
        contextBuilderConfigurator.configure(builder);

        Assert.assertEquals(1, builder.build().process("", "wf-simpleHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT).size());
    }

    /**
     * <p>
     * Tests configuration with filters for referenced nuts.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void filterDeepTest(@FromDataPoints("filter") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        // Add custom DAO and engine required
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        contextBuilderConfigurator.configure(builder);
        builder.tag(this).processContext(ProcessContext.DEFAULT).heap("heap", "defaultDao", new String[] {"images/reject-block.png"}).releaseTag();

        final List<ConvertibleNut> nuts = builder.build().process("", "wf-refHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);

        // Keep only css, remove JS files
        Assert.assertEquals(1, nuts.size());
        nuts.get(0).transform();
        Assert.assertNotNull(nuts.get(0).getReferencedNuts());
        Assert.assertEquals(8, nuts.get(0).getReferencedNuts().size());

        // Assert that ref.css is removed
        final List<ConvertibleNut> ref = nuts.get(0).getReferencedNuts().get(1).getReferencedNuts();
        Assert.assertTrue(ref == null || ref.isEmpty());
    }

    /**
     * Tests a file referencing composed heaps.
     *
     * @param contextBuilderConfigurator the configurator
     * @throws Exception if test fails
     */
    @Theory
    public void heapCompositionTest(@FromDataPoints("composition") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws Exception {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        contextBuilderConfigurator.configure(builder);
        final Context ctx = builder.build();

        // Process implicit workflow with composed heaps
        ctx.process("", "simple", UrlUtils.urlProviderFactory(), null);
        ctx.process("", "nested", UrlUtils.urlProviderFactory(), null);
        ctx.process("", "referenced", UrlUtils.urlProviderFactory(), null);
        ctx.process("", "both", UrlUtils.urlProviderFactory(), null);
        ctx.process("", "full", UrlUtils.urlProviderFactory(), null);
        ctx.process("", "any-order", UrlUtils.urlProviderFactory(), null);
    }

    /**
     * <p>
     * Checks that conventions regarding {@code null} builder ID are applied.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws java.io.IOException if test fails
     * @throws com.github.wuic.exception.WuicException if test fails
     */
    @Theory
    public void conventionTest(@FromDataPoints("conventions") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws JAXBException, IOException, WuicException {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, GzipEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class, DiskNutDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        contextBuilderConfigurator.configure(builder);
        final List<ConvertibleNut> res = builder.build().process("", "wf-heap", UrlUtils.urlProviderFactory(), null);
        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size());
        final ConvertibleNut n = res.get(0);
        n.transform();
        Assert.assertFalse(n.isCompressed());
    }

    /**
     * <p>
     * Checks that some default engines are not applied.
     * </p>
     *
     * @param contextBuilderConfigurator the configurator
     * @throws java.io.IOException if test fails
     * @throws com.github.wuic.exception.WuicException if test fails
     */
    @Theory
    public void withoutTest(@FromDataPoints("without") final BeanContextBuilderConfigurator contextBuilderConfigurator)
            throws JAXBException, IOException, WuicException {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, GzipEngine.class, TextAggregatorEngine.class, CssInspectorEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class, DiskNutDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf).configureDefault();

        // Load configuration
        contextBuilderConfigurator.configure(builder);
        final List<ConvertibleNut> res = builder.build().process("", "simpleWorkflowsimpleHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size());
        final ConvertibleNut n = res.get(0);
        Assert.assertFalse(n.getName(), n.getName().contains("aggregate"));
    }
}
