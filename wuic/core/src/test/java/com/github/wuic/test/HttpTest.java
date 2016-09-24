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
import com.github.wuic.NutTypeFactory;
import com.github.wuic.NutTypeFactoryHolder;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.core.HttpNutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;
import com.github.wuic.util.UrlUtils;
import com.github.wuic.config.bean.xml.FileXmlContextBuilderConfigurator;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * <p>
 * HTTP tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.3.1
 */
@RunWith(JUnit4.class)
@WuicRunnerConfiguration(webApplicationPath = "/", port = 9876)
public class HttpTest extends WuicTest {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Process context.
     */
    @ClassRule
    public static ProcessContextRule processContext = new ProcessContextRule();

    /**
     * The server running during tests.
     */
    @ClassRule
    public static Server server = new com.github.wuic.test.Server();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Test exists implementation.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test
    public void httpExistsTest() throws IOException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, HttpNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(HttpNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.property(ApplicationConfig.SERVER_PORT, 9876).build();
        Assert.assertTrue(dao.exists("images/reject-block.png", processContext.getProcessContext()));
        Assert.assertFalse(dao.exists("images/unknw.png", processContext.getProcessContext()));
    }

    /**
     * <p>
     * Test stream.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test
    public void httpReadTest() throws IOException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, HttpNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(HttpNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.property(ApplicationConfig.SERVER_PORT, 9876).build();
        NutTypeFactoryHolder.class.cast(dao).setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final InputStream is = dao.create("images/reject-block.png", processContext.getProcessContext()).get(0).openStream().inputStream();
        IOUtils.copyStream(is, new ByteArrayOutputStream());
        is.close();
    }

    /**
     * Test HTTP nuts.
     *
     * @throws Exception if test fails
     */
    @Test
    public void httpNutTest() throws Exception {
        Long startTime = System.currentTimeMillis();
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-http.xml")).configure(builder);
        final Context facade = builder.build();
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        startTime = System.currentTimeMillis();
        final Collection<ConvertibleNut> group = facade.process("", "css-imagecss-image", UrlUtils.urlProviderFactory(), processContext.getProcessContext());
        loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        Assert.assertFalse(group.isEmpty());
        Input is;

        for (Nut res : group) {
            is = res.openStream();
            Assert.assertTrue(is.execution().toString().length() > 0);
        }
    }
}
