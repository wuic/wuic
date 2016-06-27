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


package com.github.wuic.test.dao;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.core.DiskNutDao;
import com.github.wuic.nut.dao.core.PathNutDao;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.util.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
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
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Test with a wildcard + regex setting turned on.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void wildcardAndRegexTest() throws Exception {
        final ClasspathNutDao dao = new ClasspathNutDao();
        dao.init("/", null, -1, true, true);
        dao.init(false, true, null);
    }

    /**
     * <p>
     * Make sure an exception is raised when we configure a DAO for both wildcard and regex.
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void regexAndWildcardTest() {
        final PathNutDao dao = new PathNutDao() {

            /**
             * {@inheritDoc}
             */
            @Override
            protected DirectoryPath createBaseDirectory() throws IOException {
                return null;
            }
        };

        dao.init("", null, -1, true, true);
        dao.init(true, false, null);
    }

    /**
     * <p>
     * Test with a wildcard setting.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void wildcardClasspathScanningTest() throws Exception {
        folder.newFolder("1", "classpathScanningTest", "1");
        folder.newFolder("2", "classpathScanningTest", "2");
        final File file1 = folder.newFile("1/classpathScanningTest/1/foo.css");
        final File file2 = folder.newFile("2/classpathScanningTest/2/bar.css");

        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        method.setAccessible(true);
        method.invoke(getClass().getClassLoader(), new Object[]{file1.getParentFile().getParentFile().getParentFile().toURI().toURL()});
        method.invoke(getClass().getClassLoader(), new Object[]{file2.getParentFile().getParentFile().getParentFile().toURI().toURL()});

        final ClasspathNutDao dao = new ClasspathNutDao();
        dao.init("/classpathScanningTest", null, -1, false, true);
        dao.init(false, true, null);
        Assert.assertEquals(2, dao.create("*.css", ProcessContext.DEFAULT).size());
        Assert.assertEquals(1, dao.create("1/*.css", ProcessContext.DEFAULT).size());
        Assert.assertEquals(1, dao.create("2/*.css", ProcessContext.DEFAULT).size());
    }

    /**
     * Tests when one base directory corresponds to multiple classpath entries for {@link ClasspathNutDao}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void regexClasspathScanningTest() throws Exception {
        folder.newFolder("1", "classpathScanningTest");
        folder.newFolder("2", "classpathScanningTest");
        final File file1 = folder.newFile("1/classpathScanningTest/foo.css");
        final File file2 = folder.newFile("2/classpathScanningTest/bar.css");

        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        method.setAccessible(true);
        method.invoke(getClass().getClassLoader(), new Object[]{file1.getParentFile().getParentFile().toURI().toURL()});
        method.invoke(getClass().getClassLoader(), new Object[]{file2.getParentFile().getParentFile().toURI().toURL()});

        final ClasspathNutDao dao = new ClasspathNutDao();
        dao.init("/classpathScanningTest", null, -1, true, false);
        dao.init(false, true, null);
        Assert.assertEquals(2, dao.create(".*.css", ProcessContext.DEFAULT).size());
    }

    /**
     * Tests one direct entry for {@link ClasspathNutDao}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void noScanningTest() throws Exception {
        folder.newFolder("no", "scanning", "test");
        final File file = folder.newFile("no/scanning/test/foo.css");

        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        method.setAccessible(true);
        method.invoke(getClass().getClassLoader(), new Object[]{file.getParentFile().getParentFile().getParentFile().toURI().toURL()});

        final ClasspathNutDao dao = new ClasspathNutDao();
        dao.init("/scanning", null, -1, false, false);
        dao.init(false, false, null);
        Assert.assertEquals(1, dao.create("test/foo.css", ProcessContext.DEFAULT).size());
    }

    /**
     * <p>
     * Test exists implementation for {@link ClasspathNutDao}.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test
    public void classpathExistsTest() throws IOException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(ClasspathNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.property(ApplicationConfig.BASE_PATH, "/images").build();
        Assert.assertTrue(dao.exists("reject-block.png", null));
        Assert.assertFalse(dao.exists("unknown.png", ProcessContext.DEFAULT));
    }

    /**
     * <p>
     * Test stream for {@link ClasspathNutDao}.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test
    public void classpathReadTest() throws IOException {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, ClasspathNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(ClasspathNutDao.class.getSimpleName() + "Builder");
        final NutDao dao = builder.property(ApplicationConfig.BASE_PATH, "/images").build();
        Assert.assertTrue(dao.exists("reject-block.png", ProcessContext.DEFAULT));
        final InputStream is = dao.create("reject-block.png", ProcessContext.DEFAULT).get(0).openStream();
        IOUtils.copyStream(is, new ByteArrayOutputStream());
        is.close();
    }

    /**
     * <p>
     * Test with {@link DiskNutDao}.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void diskReadTest() throws Exception {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, DiskNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create(DiskNutDao.class.getSimpleName() + "Builder");
        builder.property(ApplicationConfig.BASE_PATH, getClass().getResource("/images").toURI().getPath());
        final NutDao dao = builder.build();
        Assert.assertTrue(dao.exists("reject-block.png", ProcessContext.DEFAULT));
        final InputStream is = dao.create("reject-block.png", ProcessContext.DEFAULT).get(0).openStream();
        IOUtils.copyStream(is, new ByteArrayOutputStream());
        is.close();
    }
}
