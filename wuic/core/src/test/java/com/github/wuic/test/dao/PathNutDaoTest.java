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


package com.github.wuic.test.dao;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.core.DiskNutDao;
import com.github.wuic.util.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * <p>
 * Tests for classes related to {@link com.github.wuic.nut.dao.core.PathNutDao}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class PathNutDaoTest {

    /**
     * Temporary folder factory.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Tests when one base directory corresponds to multiple classpath entries for {@link ClasspathNutDao}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void classpathScanningTest() throws Exception {
        folder.newFolder("1", "classpathScanningTest");
        folder.newFolder("2", "classpathScanningTest");
        final File file1 = folder.newFile("1/classpathScanningTest/foo.css");
        final File file2 = folder.newFile("2/classpathScanningTest/bar.css");

        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        method.setAccessible(true);
        method.invoke(getClass().getClassLoader(), new Object[]{file1.getParentFile().getParentFile().toURI().toURL()});
        method.invoke(getClass().getClassLoader(), new Object[]{file2.getParentFile().getParentFile().toURI().toURL()});

        final ClasspathNutDao dao = new ClasspathNutDao("/classpathScanningTest", false, null, -1, true, false);
        Assert.assertEquals(2, dao.create(".*.css").size());
    }

    /**
     * <p>
     * Test exists implementation for {@link ClasspathNutDao}.
     * </p>
     *
     * @throws StreamException if test fails
     * @throws BuilderPropertyNotSupportedException if test fails
     */
    @Test
    public void classpathExistsTest() throws StreamException, BuilderPropertyNotSupportedException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(ClasspathNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.property(ApplicationConfig.BASE_PATH, "/images").build();
        Assert.assertTrue(dao.exists("reject-block.png"));
        Assert.assertFalse(dao.exists("unknown.png"));
    }

    /**
     * <p>
     * Test stream for {@link ClasspathNutDao}.
     * </p>
     *
     * @throws StreamException if test fails
     * @throws BuilderPropertyNotSupportedException if test fails
     * @throws IOException if test fails
     * @throws NutNotFoundException if test fails
     */
    @Test
    public void classpathReadTest() throws StreamException, BuilderPropertyNotSupportedException, NutNotFoundException, IOException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(ClasspathNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.property(ApplicationConfig.BASE_PATH, "/images").build();
        Assert.assertTrue(dao.exists("reject-block.png"));
        final InputStream is = dao.create("reject-block.png").get(0).openStream();
        IOUtils.copyStream(is, new ByteArrayOutputStream());
        is.close();
    }

    /**
     * <p>
     * Test with {@link DiskNutDao}.
     * </p>
     *
     * @throws StreamException if test fails
     * @throws BuilderPropertyNotSupportedException if test fails
     * @throws IOException if test fails
     * @throws NutNotFoundException if test fails
     */
    @Test
    public void diskReadTest() throws StreamException, BuilderPropertyNotSupportedException, NutNotFoundException, IOException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, DiskNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(DiskNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.build();
        Assert.assertTrue(dao.exists("src/test/resources/images/reject-block.png"));
        final InputStream is = dao.create("src/test/resources/images/reject-block.png").get(0).openStream();
        IOUtils.copyStream(is, new ByteArrayOutputStream());
        is.close();
    }
}
