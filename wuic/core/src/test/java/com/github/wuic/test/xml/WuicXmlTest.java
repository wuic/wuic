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


package com.github.wuic.test.xml;

import com.github.wuic.ProcessContext;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.core.GzipEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.core.DiskNutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.PropertyResolver;
import com.github.wuic.util.UrlUtils;
import com.github.wuic.xml.FileXmlContextBuilderConfigurator;
import com.github.wuic.xml.ReaderXmlContextBuilderConfigurator;
import com.github.wuic.xml.XmlBuilderBean;
import com.github.wuic.xml.XmlHeapBean;
import com.github.wuic.xml.XmlPropertyBean;
import com.github.wuic.xml.XmlWorkflowBean;
import com.github.wuic.xml.XmlWorkflowTemplateBean;
import com.github.wuic.xml.XmlWuicBean;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.List;

/**
 * <p>
 * Test the wuic.xml support.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class WuicXmlTest {

    /**
     * Temporary.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * <p>
     * Test configurator with a custom DAO/engine previously registered.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void configuratorWithCustomDaoAndEngineTest() throws Exception {
        // Add custom DAO and engine
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-full.xml"));
        cfg.configure(builder);
    }

    /**
     * <p>
     * Detailed assertions tests on bean state after mapping with JAXB.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void beanTest() throws Exception {
        
        // Load with JAXB
        final JAXBContext jc = JAXBContext.newInstance(XmlWuicBean.class);
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/wuic.xsd")));
        final XmlWuicBean xml = (XmlWuicBean) unmarshaller.unmarshal(getClass().getResource("/wuic-full.xml"));

        // Non-null
        Assert.assertNotNull(xml.getHeaps());
        Assert.assertNotNull(xml.getEngineBuilders());
        Assert.assertNotNull(xml.getDaoBuilders());
        Assert.assertNotNull(xml.getWorkflowTemplates());
        Assert.assertNotNull(xml.getPollingIntervalSeconds());

        // Number of elements
        Assert.assertEquals(NumberUtils.TWO, xml.getHeaps().size());
        Assert.assertEquals(NumberUtils.TWO, xml.getEngineBuilders().size());
        Assert.assertEquals(NumberUtils.TWO, xml.getDaoBuilders().size());
        Assert.assertEquals(NumberUtils.TWO, xml.getWorkflowTemplates().size());

        // Heap field
        for (XmlHeapBean heap : xml.getHeaps()) {
            Assert.assertNotNull(heap.getId());
            Assert.assertNotNull(heap.getDaoBuilderId());
            Assert.assertNotNull(heap.getElements());
            Assert.assertEquals(NumberUtils.TWO, heap.getElements().size());
        }

        // DAO field
        for (int i = 0; i < xml.getDaoBuilders().size(); i++) {
            XmlBuilderBean dao = xml.getDaoBuilders().get(i);
            Assert.assertNotNull(dao.getId());
            Assert.assertNotNull(dao.getType());

            if (i == 1) {
                Assert.assertNull(dao.getProperties());
                continue;
            }

            Assert.assertNotNull(dao.getProperties());
            Assert.assertEquals(1, dao.getProperties().size());

            // Property field
            for (XmlPropertyBean prop : dao.getProperties()) {
                Assert.assertNotNull(prop.getKey());
                Assert.assertNotNull(prop.getValue());
            }
        }

        // Engine field
        for (int i = 0; i < xml.getEngineBuilders().size(); i++) {
            XmlBuilderBean engine = xml.getEngineBuilders().get(i);
            Assert.assertNotNull(engine.getId());
            Assert.assertNotNull(engine.getType());

            if (i == 1) {
                Assert.assertNull(engine.getProperties());
                continue;
            }

            Assert.assertNotNull(engine.getProperties());
            Assert.assertEquals(1, engine.getProperties().size());

            // Property field
            for (XmlPropertyBean prop : engine.getProperties()) {
                Assert.assertNotNull(prop.getKey());
                Assert.assertNotNull(prop.getValue());
            }
        }

        // Workflow template field
        for (int i = 0; i < xml.getWorkflowTemplates().size(); i++) {
            final XmlWorkflowTemplateBean workflow = xml.getWorkflowTemplates().get(i);

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

        for (int i = 0; i < xml.getWorkflows().size(); i++) {
            final XmlWorkflowBean workflow = xml.getWorkflows().get(i);

            Assert.assertNotNull(workflow.getWorkflowTemplateId());
            Assert.assertNotNull(workflow.getIdPrefix());
            Assert.assertNotNull(workflow.getHeapIdPattern());
        }
    }

    /**
     * <p>
     * Test when a bad XML file is detected.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000, expected = IllegalArgumentException.class)
    public void xmlReadExceptionTest() throws Exception {
        new FileXmlContextBuilderConfigurator(null);
    }

    /**
     * <p>
     * Tests when default DAO.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void withDefaultDaoTest() throws Exception {
        // File required default configuration
        final ContextBuilder builder = new ContextBuilder().configureDefault();

        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-with-default-builder.xml"));
        cfg.configure(builder);
        builder.build().process("", "simpleWorkflowsimpleHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
    }

    /**
     * <p>
     * Tests when placeholders are used.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void withPlaceholdersTest() throws Exception {
        final PropertyResolver resolver = Mockito.mock(PropertyResolver.class);
        Mockito.when(resolver.resolveProperty("debug")).thenReturn("true");

        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);

        // File required default configuration
        final ContextBuilder builder = new ContextBuilder(new ContextBuilder(ebf, nbf, fbf), resolver).configureDefault();
        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-placeholder.xml"));
        cfg.configure(builder);
        final MockDao dao = (MockDao) builder.build().getWorkflow("simpleHeap").getHeap().getNutDao();
        Assert.assertEquals(dao.getFoo(), "true");
        Assert.assertEquals(dao.getBar(), "false");
        Assert.assertEquals(dao.getBaz(), "baz");
    }

    /**
     * Tests a wuic.xml file referencing default DAO.
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void withPollingTest() throws Exception {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // By default we use this file
        final URL full = getClass().getResource("/wuic-full.xml");
        final File tmp = temporaryFolder.newFile("wuic.xml");
        IOUtils.copyStream(full.openStream(), new FileOutputStream(tmp));

        // Load configuration
        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(tmp.toURI().toURL());
        cfg.configure(builder);
        Context ctx = builder.build();
        Assert.assertTrue(ctx.isUpToDate());

        // We change the content before polling
        final URL wwdb = getClass().getResource("/wuic-simple.xml");
        IOUtils.copyStream(wwdb.openStream(), new FileOutputStream(tmp));

        // Polling is done every seconds
        Thread.sleep(1300L);
        Assert.assertFalse(ctx.isUpToDate());

        // Check new context which contains new workflow
        ctx = builder.build();
        Assert.assertTrue(ctx.isUpToDate());
        ctx.process("", "simpleWorkflowsimpleHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);

        // Remove test file
        tmp.delete();
    }

    /**
     * Tests a wuic.xml file referencing binding.
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void bindTest() throws Exception {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-bind.xml"));
        cfg.configure(builder);
        final Context ctx = builder.build();

        // Process implicit workflow with composed heaps
        ctx.process("", "bind", UrlUtils.urlProviderFactory(), null);
    }

    /**
     * <p>
     * Tests XML configuration through a reader.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void readerTest() throws Exception {
        final Reader reader = new FileReader(new File(getClass().getResource("/wuic-basic.xml").getFile()));
        final ContextBuilderConfigurator cfg = new ReaderXmlContextBuilderConfigurator(reader, "tag", true, ProcessContext.DEFAULT);
        final ContextBuilder builder = new ContextBuilder();
        cfg.configure(builder);
    }

    /**
     * <p>
     * Tests XML configuration with filters.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void filterTest() throws Exception {
        // Add custom DAO and engine required
        final Reader reader = new FileReader(new File(getClass().getResource("/wuic-filter.xml").getFile()));
        final ContextBuilderConfigurator cfg = new ReaderXmlContextBuilderConfigurator(reader, "tag", true, null);
        final ContextBuilder builder = new ContextBuilder();
        cfg.configure(builder);

        Assert.assertEquals(1, builder.build().process("", "wf-simpleHeap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT).size());
    }

    /**
     * <p>
     * Tests XML configuration with filters for referenced nuts.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void filterDeepTest() throws Exception {
        // Add custom DAO and engine required
        final Reader reader = new FileReader(new File(getClass().getResource("/wuic-filter.xml").getFile()));
        final ContextBuilderConfigurator cfg = new ReaderXmlContextBuilderConfigurator(reader, "tag", true, ProcessContext.DEFAULT);
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        cfg.configure(builder);
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
     * Tests a wuic.xml file referencing composed heaps.
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void heapCompositionTest() throws Exception {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, MockEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, MockDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-heap-composition.xml"));
        cfg.configure(builder);
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
     * @throws java.io.IOException if test fails
     * @throws com.github.wuic.exception.WuicException if test fails
     */
    @Test(timeout = 60000)
    public void conventionTest() throws JAXBException, IOException, WuicException {
        // Add custom DAO and engine required
        final ObjectBuilderFactory<Engine> ebf = new ObjectBuilderFactory<Engine>(EngineService.class, GzipEngine.class);
        final ObjectBuilderFactory<NutDao> nbf = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class, DiskNutDao.class);
        final ObjectBuilderFactory<NutFilter> fbf = new ObjectBuilderFactory<NutFilter>(NutFilterService.class);
        final ContextBuilder builder = new ContextBuilder(ebf, nbf, fbf);

        // Load configuration
        final ContextBuilderConfigurator cfg = new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-conventions.xml"));
        cfg.configure(builder);
        final List<ConvertibleNut> res = builder.build().process("", "wf-heap", UrlUtils.urlProviderFactory(), null);
        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size());
        final ConvertibleNut n = res.get(0);
        n.transform();
        Assert.assertFalse(n.isCompressed());
    }
}
