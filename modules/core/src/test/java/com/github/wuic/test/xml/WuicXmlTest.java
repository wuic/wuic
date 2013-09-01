/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.Context;
import com.github.wuic.ContextBuilder;
import com.github.wuic.ContextBuilderConfigurator;
import com.github.wuic.NutType;
import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.nut.NutDaoBuilderFactory;
import com.github.wuic.engine.AbstractEngineBuilder;
import com.github.wuic.engine.Engine;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.EngineBuilderPropertyNotSupportedException;
import com.github.wuic.exception.NutDaoBuilderPropertyNotSupportedException;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDaoBuilder;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.PropertySetter;
import com.github.wuic.xml.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * <p>
 * Test the wuic.xml support.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class WuicXmlTest {

    /**
     * <p>
     * Mocked DAO builder.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    public static final class MockEngineBuilder extends AbstractEngineBuilder {

        /**
         * <p>
         * Builds with a mocked property setter.
         * </p>
         */
        public MockEngineBuilder() {
            final PropertySetter<String> setter = mock(PropertySetter.class);
            when(setter.getPropertyKey()).thenReturn("c.g.engine.foo");
            addPropertySetter(setter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Engine internalBuild() throws BuilderPropertyNotSupportedException {
            final Engine engine = mock(Engine.class);
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
     * @since 0.4.0
     */
    public static final class MockDaoBuilder extends AbstractNutDaoBuilder {

        /**
         * <p>
         * Builds with a mocked setter.
         * </p>
         */
        public MockDaoBuilder() {
            final PropertySetter<String> setter = mock(PropertySetter.class);
            when(setter.getPropertyKey()).thenReturn("c.g.dao.foo");
            addPropertySetter(setter);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected NutDao internalBuild() throws BuilderPropertyNotSupportedException {
            final NutDao retval = mock(NutDao.class);

            try {
                final Nut nut = mock(Nut.class);
                when(nut.getNutType()).thenReturn(NutType.CSS);
                when(nut.getName()).thenReturn("foo.css");
                when(retval.create(anyString())).thenReturn(Arrays.asList(nut));
                when(retval.saveSupported()).thenReturn(true);
                when(nut.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
                when(nut.isAggregatable()).thenReturn(true);
            } catch (StreamException se) {
                Assert.fail();
            } catch (NutNotFoundException se) {
                Assert.fail();
            }

            return retval;
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
     * Test configurator with a custom DAO/engine previously registered.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void configuratorWithCustomDaoAndEngineTest() throws Exception {
        // Add custom DAO and engine
        NutDaoBuilderFactory.getInstance().addBuilderClass(MockDaoBuilder.class.getName());
        EngineBuilderFactory.getInstance().addBuilderClass(MockEngineBuilder.class.getName());

        // Load configuration
        final ContextBuilderConfigurator cfg = new WuicXmlContextBuilderConfigurator(getClass().getResource("/wuic-full.xml"));
        final ContextBuilder builder = new ContextBuilder();
        cfg.configure(builder);
    }

    /**
     * <p>
     * Detailed assertions tests on bean state after mapping with JAXB.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
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
        Assert.assertNotNull(xml.getWorkflows());
        Assert.assertNotNull(xml.getPollingInterleaveSeconds());

        // Number of elements
        Assert.assertEquals(NumberUtils.TWO, xml.getHeaps().size());
        Assert.assertEquals(NumberUtils.TWO, xml.getEngineBuilders().size());
        Assert.assertEquals(NumberUtils.TWO, xml.getDaoBuilders().size());
        Assert.assertEquals(NumberUtils.TWO, xml.getWorkflows().size());

        // Heap field
        for (XmlHeapBean heap : xml.getHeaps()) {
            Assert.assertNotNull(heap.getId());
            Assert.assertNotNull(heap.getDaoBuilderId());
            Assert.assertNotNull(heap.getNutPaths());
            Assert.assertEquals(NumberUtils.TWO, heap.getNutPaths().size());
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

        // Workflow field
        for (int i = 0; i < xml.getWorkflows().size(); i++) {
            final XmlWorkflowBean workflow = xml.getWorkflows().get(i);

            Assert.assertNotNull(workflow.getId());
            Assert.assertNotNull(workflow.getEngineBuilderIds());
            Assert.assertNotNull(workflow.getHeapId());

            if (i == 1) {
                Assert.assertNull(workflow.getDaoBuilderIds());
                continue;
            }

            Assert.assertNotNull(workflow.getDaoBuilderIds());
            Assert.assertEquals(NumberUtils.TWO, workflow.getEngineBuilderIds().size());
            Assert.assertEquals(1, workflow.getDaoBuilderIds().size());
        }
    }

    /**
     * <p>
     * Tests when default DAO.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void withDefaultDaoTest() throws Exception {
        final ContextBuilder builder = new ContextBuilder();

        // File required default configuration
        NutDaoBuilderFactory.getInstance().newContextBuilderConfigurator().configure(builder);
        final ContextBuilderConfigurator cfg = new WuicXmlContextBuilderConfigurator(getClass().getResource("/wuic-with-default-builder.xml"));
        cfg.configure(builder);
        builder.build().process("simpleWorkflow", "");
    }

    /**
     * Tests a wuic.xml file referencing default DAO.
     *
     * @throws Exception if test fails
     */
    @Test
    public void withPollingTest() throws Exception {

        // Add custom DAO and engine required
        NutDaoBuilderFactory.getInstance().addBuilderClass(MockDaoBuilder.class.getName());
        EngineBuilderFactory.getInstance().addBuilderClass(MockEngineBuilder.class.getName());

        // By default we use this file
        final URL full = getClass().getResource("/wuic-full.xml");
        final File tmp = File.createTempFile("wuic", "xml");
        IOUtils.copyStream(full.openStream(), new FileOutputStream(tmp));

        // Load configuration
        final ContextBuilderConfigurator cfg = new WuicXmlContextBuilderConfigurator(tmp.toURI().toURL());
        final ContextBuilder builder = new ContextBuilder();
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
        ctx.process("simpleWorkflow", "");

        // Remove test file
        tmp.delete();
    }
}
