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


package com.github.wuic.test;

import com.github.wuic.AnnotationProcessor;
import com.github.wuic.AnnotationScanner;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.engine.EngineType;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.path.FilePath;
import com.github.wuic.path.core.ZipEntryFilePath;
import com.github.wuic.util.AnnotationDetectorScanner;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutDiskStore;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.StringUtils;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.util.UrlMatcher;
import com.github.wuic.util.UrlUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * <p>
 * Utility class tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.4
 */
@RunWith(JUnit4.class)
public class UtilityTest extends WuicTest {

    /**
     * <p>
     * Test the nut research feature.
     * </p>
     */
    @Test
    public void searchNutTest() {
        final ConvertibleNut nut1 = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut1.getName()).thenReturn("./nut1");

        final ConvertibleNut nut2 = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut2.getName()).thenReturn("nut2/.");

        final ConvertibleNut nut3 = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut3.getName()).thenReturn("nut3");
        Mockito.when(nut3.getReferencedNuts()).thenReturn(Arrays.asList(nut1, nut2));

        Assert.assertNotNull(NutUtils.findByName(Arrays.asList(nut1, nut2, nut3), "nut1"));
        Assert.assertNotNull(NutUtils.findByName(Arrays.asList(nut1, nut2, nut3), "nut2"));
        Assert.assertNotNull(NutUtils.findByName(Arrays.asList(nut1, nut2, nut3), "nut3"));

        Assert.assertNotNull(NutUtils.findByName(Arrays.asList(nut3), "nut1"));
        Assert.assertNotNull(NutUtils.findByName(Arrays.asList(nut3), "nut2"));
        Assert.assertNotNull(NutUtils.findByName(Arrays.asList(nut3), "nut3"));
    }

    /**
     * Test when nuts with same name are merged.
     *
     * @throws IOException if test fails
     */
    @Test
    public void mergeNutTest() throws IOException {
        final ConvertibleNut first = Mockito.mock(ConvertibleNut.class);
        final ConvertibleNut second = Mockito.mock(ConvertibleNut.class);
        final ConvertibleNut third = Mockito.mock(ConvertibleNut.class);
        final ConvertibleNut fourth = Mockito.mock(ConvertibleNut.class);
        final ConvertibleNut fifth = Mockito.mock(ConvertibleNut.class);
        final List<ConvertibleNut> input = Arrays.asList(first, second, third, fourth, fifth);

        Mockito.when(first.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(second.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(third.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(fourth.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(fifth.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        Mockito.when(first.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(second.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(third.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(fourth.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(fifth.getVersionNumber()).thenReturn(new FutureLong(1L));

        Mockito.when(first.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(second.getInitialNutType()).thenReturn(NutType.CSS);
        Mockito.when(third.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(fourth.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(fifth.getInitialNutType()).thenReturn(NutType.CSS);

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.css");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("bar.js");
        Mockito.when(fifth.getName()).thenReturn("baz.css");

        Mockito.when(first.getInitialName()).thenReturn("foo.js");
        Mockito.when(second.getInitialName()).thenReturn("foo.css");
        Mockito.when(third.getInitialName()).thenReturn("bar.js");
        Mockito.when(fourth.getInitialName()).thenReturn("bar.js");
        Mockito.when(fifth.getInitialName()).thenReturn("baz.css");

        List<ConvertibleNut> output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input);

        Assert.assertEquals(4, output.size());
        Assert.assertEquals("foo.js", output.get(0).getName());
        Assert.assertEquals("foo.css", output.get(1).getName());
        Assert.assertEquals("bar.js", output.get(2).getName());
        Assert.assertEquals("baz.css", output.get(3).getName());

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.css");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("bar2.js");
        Mockito.when(fifth.getName()).thenReturn("baz.css");

        Mockito.when(first.getInitialName()).thenReturn("foo.js");
        Mockito.when(second.getInitialName()).thenReturn("foo.css");
        Mockito.when(third.getInitialName()).thenReturn("bar.js");
        Mockito.when(fourth.getInitialName()).thenReturn("bar2.js");
        Mockito.when(fifth.getInitialName()).thenReturn("baz.css");

        output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input);

        Assert.assertEquals(5, output.size());
        Assert.assertEquals("foo.js", output.get(0).getName());
        Assert.assertEquals("foo.css", output.get(1).getName());
        Assert.assertEquals("bar.js", output.get(2).getName());
        Assert.assertEquals("bar2.js", output.get(3).getName());
        Assert.assertEquals("baz.css", output.get(4).getName());

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.js");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("baz.css");
        Mockito.when(fifth.getName()).thenReturn("baz.css");

        Mockito.when(first.getInitialName()).thenReturn("foo.js");
        Mockito.when(second.getInitialName()).thenReturn("foo.js");
        Mockito.when(third.getInitialName()).thenReturn("bar.js");
        Mockito.when(fourth.getInitialName()).thenReturn("baz.css");
        Mockito.when(fifth.getInitialName()).thenReturn("baz.css");

        output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input);

        Assert.assertEquals(3, output.size());
        Assert.assertEquals("foo.js", output.get(0).getName());
        Assert.assertEquals("bar.js", output.get(1).getName());
        Assert.assertEquals("baz.css", output.get(2).getName());

        Mockito.when(first.getName()).thenReturn("foo.js");
        Mockito.when(second.getName()).thenReturn("foo.js");
        Mockito.when(third.getName()).thenReturn("bar.js");
        Mockito.when(fourth.getName()).thenReturn("baz.css");
        Mockito.when(fifth.getName()).thenReturn("foo.js");

        Mockito.when(first.getInitialName()).thenReturn("foo.js");
        Mockito.when(second.getInitialName()).thenReturn("foo.js");
        Mockito.when(third.getInitialName()).thenReturn("bar.js");
        Mockito.when(fourth.getInitialName()).thenReturn("baz.css");
        Mockito.when(fifth.getInitialName()).thenReturn("foo.js");

        output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input);

        Assert.assertEquals(4, output.size());
        Assert.assertEquals("foo.js", output.get(0).getName());
        Assert.assertEquals("bar.js", output.get(1).getName());
        Assert.assertEquals("baz.css", output.get(2).getName());
        Assert.assertEquals("3foo.js", output.get(3).getName());
    }

    /**
     * Test path simplification method.
     */
    @Test
    public void simplifyPathTest() {
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("foo/bar/../../baz.css"), "baz.css");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("./foo"), "foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/./foo"), "/bar/foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/./foo/."), "/bar/foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo"), "/foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo/"), "/foo/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo"), "foo");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo/"), "foo/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo/file"), "/foo/file");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/../foo/file/"), "/foo/file/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo/file"), "foo/file");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/../foo/file/"), "foo/file/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/foo/../.."), "/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/bar/foo/../../"), "/");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("bar/foo/../../path/../file"), "file");
        Assert.assertEquals(StringUtils.simplifyPathWithDoubleDot("/foo/../bar/../baz"), "/baz");
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("bar/foo/../../../"));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot(".."));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("/../"));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("../foo"));
        Assert.assertNull(StringUtils.simplifyPathWithDoubleDot("foo/../bar/../../path"));
    }

    /**
     * Test string merge.
     */
    @Test
    public void mergeTest() {
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", "oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo:", "oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo:", ":oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo", ":oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo", ":oof", "foo",}, ":"), "foo:oof:foo");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", "foo", }, null), "foo:ooffoo");
        Assert.assertEquals(StringUtils.merge(new String[] {":", "oof", }, ":"), ":oof");
        Assert.assertEquals(StringUtils.merge(new String[] {":", ":foo:", ":oof", }, ":"), ":foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {":", "foo:", ":oof:", }, ":"), ":foo:oof:");
        Assert.assertEquals(StringUtils.merge(new String[] {"/opt", "data", }, "/"), "/opt/data");
        Assert.assertEquals(StringUtils.merge(new String[] {"http://", "server:80", "", "root" }, "/"), "http://server:80/root");
    }

    /**
     * Be sure that the {@code Map} used internally keep the order of the keys.
     */
    @Test
    public void orderingKeyMapTest() {
        final Map<String, String> map = CollectionUtils.orderedKeyMap();

        map.put("toto", "");
        map.put("titi", "");
        map.put("tata", "");
        map.put("tutu", "");

        int cpt = 0;

        for (String key : map.keySet()) {
            Assert.assertTrue(cpt == 0 ? "toto".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 1 ? "titi".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 2 ? "tata".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 3 ? "tutu".equals(key) : Boolean.TRUE);
            cpt++;
        }
    }

    /**
     * Makes sure the research is correctly performed.
     * 
     * Actually if you have this directory : /foo/oof/js/path.js
     * You have a classpath where root is /foo i.e path.js is retrieved thanks to /oof/js/path.js
     * Your classpath protocol has base directory /oof
     * We need to be very clear about the path evaluated by the regex
     * For instance, /.*.js should returns /js/path.js since /oof is the base path
     * After, that /oof + /js/path.js will result in the exact classpath entry to retrieve
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void fileSearchTest() throws IOException {

        // Part 1
        final String nanoTime = String.valueOf(System.nanoTime());
        final String tmp = System.getProperty("java.io.tmpdir");
        final String path = IOUtils.mergePath(tmp, nanoTime, "foo");
        final File basePath = new File(path);
        Assert.assertTrue(basePath.mkdirs());

        final File file = File.createTempFile("file", ".js", basePath);

        final DirectoryPath parent = DirectoryPath.class.cast(IOUtils.buildPath(IOUtils.mergePath(tmp, nanoTime)));
        List<String> list = IOUtils.listFile(parent, Pattern.compile(".*js"));

        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), IOUtils.mergePath("foo", file.getName()));

        // Part 2
        final String str = getClass().getResource("/images").toString();
        final String baseDir = str.substring(str.indexOf(":/") + 1);
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir));
        final List<String> listFiles = IOUtils.listFile(directoryPath, Pattern.compile(".*.png"));
        Assert.assertEquals(40, listFiles.size());

        for (String f : listFiles) {
            Assert.assertEquals(directoryPath.getChild(f).getAbsolutePath(), IOUtils.mergePath(directoryPath.getAbsolutePath(), f));
        }
    }

    /**
     * <p>
     * Reads an archive with empty file. Issue #97.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void fileSearchInJarTest() throws IOException {
        final String resolve = "/META-INF/resources/webjars";
        String str = IOUtils.normalizePathSeparator(getClass().getResource(resolve).toString());

        final String baseDir = str.substring(str.indexOf(":/") + 1);
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir));
        IOUtils.listFile(directoryPath, Pattern.compile(".*.js"));
    }

    /**
     * <p>
     * Tests when reading unzipped entry. Also tests that unzipped entry is recreated when unzipped file is deleted.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void zipEntryTest() throws IOException {
        String str = IOUtils.normalizePathSeparator(getClass().getResource("/zip").toString());

        final String baseDir = str.substring(str.indexOf(":/") + 1);
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir));
        final List<String> list = IOUtils.listFile(directoryPath, Pattern.compile(".*.png"));
        ZipEntryFilePath.ZipFileInputStream is;

        for (final String s : list) {
            is = ZipEntryFilePath.ZipFileInputStream.class.cast(FilePath.class.cast(directoryPath.getChild(s)).openStream());
            IOUtils.copyStream(is, new ByteArrayOutputStream());
            is.close();

            if (!"img.zip".equals(is.getFile().getName())) {
                Assert.assertTrue(is.getFile().delete());
            }

            // Retry when file is deleted
            is = ZipEntryFilePath.ZipFileInputStream.class.cast(FilePath.class.cast(directoryPath.getChild(s)).openStream());
            IOUtils.copyStream(is, new ByteArrayOutputStream());
            is.close();

            if (!"img.zip".equals(is.getFile().getName())) {
                Assert.assertTrue(is.getFile().delete());
            }
        }

        Assert.assertEquals(5, list.size());
    }

    /**
     * <p>
     * Search with skipped files.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void fileSearchSkipStartWith() throws IOException {
        final String str = getClass().getResource("/skipped").toString();
        final String baseDir = str.substring(str.indexOf(":/") + 1);
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir));
        final List<String> res = IOUtils.listFile(directoryPath, Pattern.compile(".*.js"), Arrays.asList("ignore"));
        Assert.assertEquals(2, res.size());
    }

    /**
     * Test difference between sets.
     */
    @Test
    public void differenceTest() {
        final Set<String> res = diff(new HashSet<String>(), new HashSet<String>());
        Assert.assertTrue("must be instance of HashSet", res instanceof HashSet);
    }

    /**
     * Test difference with ordered sets.
     */
    @Test
    public void orderedDiffTest() {
        final Set<String> first = new LinkedHashSet<String>();
        final Set<String> second = new LinkedHashSet<String>();
        final Set<String> res = diff(first, second);
        Assert.assertTrue("must be instance of LinkedHashSet", res instanceof LinkedHashSet);
    }

    /**
     * Test that EngineType exclusion.
     */
    @Test
    public void withoutEngineTest() {
        Assert.assertEquals(EngineType.values().length - 2, EngineType.without(EngineType.INSPECTOR, EngineType.AGGREGATOR).length);
    }

    /**
     * Difference assumptions.
     *
     * @return diff result
     */
    public Set<String> diff(final Set<String> first, final Set<String> second) {
        Set<String> diff = CollectionUtils.difference(first, second);
        Assert.assertTrue(diff.isEmpty());

        first.addAll(Arrays.asList("a", "b", "c", "d"));
        second.addAll(Arrays.asList("a", "b"));
        diff = CollectionUtils.difference(first, second);
        Assert.assertEquals(diff.size(), 2);

        first.clear();
        second.clear();
        first.addAll(Arrays.asList("a", "b"));
        second.addAll(Arrays.asList("a", "b", "c"));
        diff = CollectionUtils.difference(first, second);
        Assert.assertEquals(diff.size(), 1);
        Assert.assertEquals("c", diff.iterator().next());

        first.add("c");
        diff = CollectionUtils.difference(first, second);
        Assert.assertTrue(diff.isEmpty());
        return diff;
    }

    /**
     * Test javascript import.
     *
     * @throws IOException if test fails
     */
    @Test
    public void htmlJavascriptImportTest() throws IOException {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getName()).thenReturn("foo.js");
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(nut.getNutType()).thenReturn(NutType.JAVASCRIPT);
        HtmlUtil.writeScriptImport(nut, "myPath");

        final String res = HtmlUtil.writeScriptImport(nut, "myPath", "param=param");
        Assert.assertTrue(res.contains("\"myPath/1/foo.js\""));
        Assert.assertTrue(res.startsWith("<script param=param "));
    }

    /**
     * Test CSS import.
     *
     * @throws IOException if test fails
     */
    @Test
    public void htmlCssImportTest() throws IOException {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getName()).thenReturn("foo.css");
        Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        final String res = HtmlUtil.writeScriptImport(nut, "myPath", "param=param");
        Assert.assertTrue(res.contains("\"myPath/1/foo.css\""));
        Assert.assertTrue(res.startsWith("<link param=param "));
    }

    /**
     * Test IMG import.
     *
     * @throws IOException if test fails
     */
    @Test
    public void htmlImgImportTest() throws IOException {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getName()).thenReturn("foo.png");
        Mockito.when(nut.getNutType()).thenReturn(NutType.PNG);
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        final String res = HtmlUtil.writeScriptImport(nut, "myPath", "param=param");
        Assert.assertTrue(res.contains("\"myPath/1/foo.png\""));
        Assert.assertTrue(res.startsWith("<img param=param "));
    }

    /**
     * Test URL generation.
     */
    @Test
    public void getUrltTest() {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getInitialNutType()).thenReturn(NutType.CSS);
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        // Served nut
        Mockito.when(nut.getName()).thenReturn("foo.css");
        Assert.assertTrue(UrlUtils.urlProviderFactory().create("myPath").getUrl(nut).contains("myPath/1/foo.css"));

        // Absolute path
        Mockito.when(nut.getName()).thenReturn("http:/domain.fr/foo.css");
        Assert.assertTrue(UrlUtils.urlProviderFactory().create("myPath").getUrl(nut).contains("http:/domain.fr/foo.css"));

        // Proxy path
        Mockito.when(nut.getProxyUri()).thenReturn("http://proxy.fr/foo.css");
        Mockito.when(nut.getName()).thenReturn("http:/domain.fr/foo.css");
        Assert.assertTrue(UrlUtils.urlProviderFactory().create("myPath").getUrl(nut).contains("http://proxy.fr/foo.css"));
    }

    /**
     * Tests nut type identification.
     */
    @Test
    public void getByMimeTypeTest() {
        NutType nutType = NutType.getNutTypeForMimeType("text/html;charset=UTF-8");
        Assert.assertEquals(nutType, NutType.HTML);

        nutType = NutType.getNutTypeForMimeType("text/html");
        Assert.assertEquals(nutType, NutType.HTML);

        nutType = NutType.getNutTypeForMimeType("bad-mime-type");
        Assert.assertNull(nutType);
    }

    /**
     * Default annotation scanner test.
     */
    @Test
    public void defaultAnnotationScannerTest() {
        final AnnotationScanner s = new AnnotationDetectorScanner();
        final AtomicInteger count = new AtomicInteger();
        s.scan("com", new AnnotationProcessor() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Class<? extends Annotation> requiredAnnotation() {
                return RunWith.class;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void handle(final Class<?> annotatedType) {
               count.incrementAndGet();
            }
        });

        Assert.assertNotEquals(0, count.intValue());
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} when no workflow is defined.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithNoWorkflowTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/4000/nut.js");
        Assert.assertFalse(urlMatcher.matches());
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} when no nut name is defined.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithNoNutNameTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("workflow/4000/");
        Assert.assertFalse(urlMatcher.matches());
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} with nominal case.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/workflow/4000/nut.js");
        Assert.assertTrue(urlMatcher.matches());
        Assert.assertEquals(urlMatcher.getWorkflowId(), "workflow");
        Assert.assertEquals(urlMatcher.getNutName(), "nut.js");
        Assert.assertEquals(urlMatcher.getVersionNumber(), "4000");
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} with deep nut name case.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithDeepNameTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/workflow/4000/deep/nut.js");
        Assert.assertTrue(urlMatcher.matches());
        Assert.assertEquals(urlMatcher.getWorkflowId(), "workflow");
        Assert.assertEquals(urlMatcher.getNutName(), "deep/nut.js");
        Assert.assertEquals(urlMatcher.getVersionNumber(), "4000");
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} with bad numeric nut name case.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithNumericNameTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/workflow/4000/4000/nut.js");
        Assert.assertFalse(urlMatcher.matches());
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} with good numeric nut name case.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithGoodNumericNameTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/workflow/4000/deep/4000/nut.js");
        Assert.assertTrue(urlMatcher.matches());
        Assert.assertEquals(urlMatcher.getWorkflowId(), "workflow");
        Assert.assertEquals(urlMatcher.getNutName(), "deep/4000/nut.js");
        Assert.assertEquals(urlMatcher.getVersionNumber(), "4000");
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} with deep nut name and not version number case.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithDeepNameAndWithoutVersionTest() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/workflow/deep/nut.js");
        Assert.assertTrue(urlMatcher.matches());
        Assert.assertEquals(urlMatcher.getWorkflowId(), "workflow");
        Assert.assertEquals(urlMatcher.getNutName(), "deep/nut.js");
        Assert.assertNull(urlMatcher.getVersionNumber());
    }

    /**
     * <p>
     * Tests {@link UrlMatcher} when no version is defined.
     * </p>
     *
     * @throws UnsupportedEncodingException if test fails
     */
    @Test
    public void urlMatcherWithNoVersion() throws UnsupportedEncodingException {
        final UrlMatcher urlMatcher = UrlUtils.urlMatcher("/workflow/nut.js");
        Assert.assertTrue(urlMatcher.matches());
        Assert.assertEquals(urlMatcher.getWorkflowId(), "workflow");
        Assert.assertEquals(urlMatcher.getNutName(), "nut.js");
        Assert.assertNull(urlMatcher.getVersionNumber());
    }

    /**
     * <p>
     * Tests the pipe.    
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void pipeTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(10);
        final Pipe p = new Pipe(Mockito.mock(ConvertibleNut.class), new ByteArrayInputStream(new byte[] { (byte) count.get() }));
        class T extends Pipe.DefaultTransformer {
            @Override
            public void transform(final InputStream is, final OutputStream os, final Object o) throws IOException {
                int r = is.read();
                os.write(r + count.decrementAndGet());
            }
        }

        int expect = 0;
        for (int i = 1; i <= count.get(); i++) {
            expect += i;
            p.register(new T());
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Pipe.executeAndWriteTo(p, new ArrayList<Pipe.OnReady>(), bos);
        Assert.assertEquals(expect, bos.toByteArray()[0]);
    }

    /**
     * <p>
     * Tests that file deletion supports null values and deep structure.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test
    public void deleteTest() throws IOException {
        final String now = String.valueOf(System.currentTimeMillis());
        final File parent = new File(IOUtils.mergePath(System.getProperty("java.io.tmpdir"), "wuicDeleteTest", now));
        Assert.assertTrue(parent.mkdirs());
        final File child1 = new File(parent, "child1");
        Assert.assertTrue(child1.mkdir());
        final File child2 = new File(parent, "child2");
        Assert.assertTrue(child2.createNewFile());
        final File child3 = new File(child1, "child3");
        Assert.assertTrue(child3.createNewFile());
        IOUtils.delete(parent);
        Assert.assertFalse(parent.exists());
        IOUtils.delete(null);
    }

    /**
     * <p>
     * Tests for {@link NutDiskStore}.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void nutDiskStoreTest() throws Exception {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getName()).thenReturn("/foo/bar.js");
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        File f = NutDiskStore.INSTANCE.store(nut);
        Assert.assertFalse(f.exists());
        f.createNewFile();

        f = NutDiskStore.INSTANCE.store(nut);
        Assert.assertTrue(f.exists());

        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(2L));
        f = NutDiskStore.INSTANCE.store(nut);
        Assert.assertFalse(f.exists());
    }

    /**
     * <p>
     * Tests number detection support.
     * </p>
     */
    @Test
    public void isNumberTest() {
        Assert.assertFalse(NumberUtils.isNumber(""));
        Assert.assertFalse(NumberUtils.isNumber("aa"));
        Assert.assertFalse(NumberUtils.isNumber("1aa"));
        Assert.assertTrue(NumberUtils.isNumber("11"));
        Assert.assertTrue(NumberUtils.isNumber("01"));
        Assert.assertTrue(NumberUtils.isNumber("100"));
    }

    /**
     * Common beginning string extraction test.
     */
    @Test
    public void extractCommonBeginningTest() {
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/foo", "/paths/bar", "/paths/baz")));
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/baz/foo", "/paths/bar")));
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/baz/foo", "/paths/bar/foo")));
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/foo", "/paths/bar/foo")));

        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/foo/", "/paths/bar/", "/paths/baz/")));
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/baz/foo/", "/paths/bar/")));
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/baz/foo/", "/paths/bar/foo/")));
        Assert.assertEquals("/paths", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/foo/", "/paths/bar/foo/")));

        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/foo", "paths/bar", "paths/baz")));
        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/baz/foo", "paths/bar")));
        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/baz/foo", "paths/bar/foo")));
        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/foo", "paths/bar/foo")));

        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/foo/", "paths/bar/", "paths/baz/")));
        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/baz/foo/", "paths/bar/")));
        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/baz/foo/", "paths/bar/foo/")));
        Assert.assertEquals("paths", StringUtils.computeCommonPathBeginning(Arrays.asList("paths/foo/", "paths/bar/foo/")));

        Assert.assertEquals("/paths/deep", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/deep/foo", "/paths/deep/bar")));
        Assert.assertEquals("/paths/deep", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths/deep/foo", "/paths/deep/bar/baz")));

        Assert.assertEquals("/", StringUtils.computeCommonPathBeginning(Arrays.asList("/foo")));
        Assert.assertEquals("/foo", StringUtils.computeCommonPathBeginning(Arrays.asList("/foo/bar")));
        Assert.assertEquals("/foo/bar", StringUtils.computeCommonPathBeginning(Arrays.asList("/foo/bar/baz")));

        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths", "/paths/bar", "/paths/foo")));
        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths", "/paths/bar")));
        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(Arrays.asList("/paths", "/paths")));
        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(Arrays.asList("paths", "paths")));
        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(Arrays.asList("/", "paths")));
        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(Arrays.asList("foo")));
        Assert.assertEquals("", StringUtils.computeCommonPathBeginning(new ArrayList<String>()));
    }

    /**
     * <p>
     * Tests {@link StringUtils#replaceAll(String, String, StringBuilder)}.
     * </p>
     */
    @Test
    public void replaceAllTest() {
        Assert.assertEquals("foo bar baz", StringUtils.replaceAll("|", " ", new StringBuilder("foo|bar|baz")).toString());
        Assert.assertEquals("foo,,,,bar,,,,baz", StringUtils.replaceAll("+=-", ",,,,", new StringBuilder("foo+=-bar+=-baz")).toString());
        Assert.assertEquals("foo bar baz", StringUtils.replaceAll(".", " ", new StringBuilder("foo bar baz")).toString());
    }

    /**
     * <p>
     * Tests {@link StringUtils#substringMatrix(String[], int, int, int, int)}.
     * </p>
     */
    @Test
    public void substringMatrixTest() {
        String[] lines = {
            "abcdefg", "hijk", "lmno", "pqrstu", "vwxyz"
        };

        Assert.assertEquals("h", StringUtils.substringMatrix(lines, 2, 1, 2, 2));
        Assert.assertEquals(lines[1], StringUtils.substringMatrix(lines, 2, 1, 2, 5));
        Assert.assertEquals("fg\nhi", StringUtils.substringMatrix(lines, 1, 6, 2, 3));
        Assert.assertEquals("abcdefg\nhijk\nlmno\npqrstu\nvwxyz", StringUtils.substringMatrix(lines, 1, 1, 5, 6));
        Assert.assertEquals("k\nlmno\np", StringUtils.substringMatrix(lines, 2, 4, 4, 2));
    }

    /**
     * <p>
     * Tests {@link StringUtils#reachEndLineAndColumn(String[], int, int, int, java.util.concurrent.atomic.AtomicInteger, java.util.concurrent.atomic.AtomicInteger)}.
     * </p>
     */
    @Test
    public void reachEndLineAndColumnTest() {
        String[] lines = {
                "abcdefg", "hijk", "lmno", "pqrstu", "vwxyz"
        };

        final AtomicInteger l = new AtomicInteger();
        final AtomicInteger c = new AtomicInteger();

        StringUtils.reachEndLineAndColumn(lines, 1, 1, 3, l, c);
        Assert.assertEquals(1, l.get());
        Assert.assertEquals(3, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 2, 2, 3, l, c);
        Assert.assertEquals(2, l.get());
        Assert.assertEquals(4, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 2, 2, 4, l, c);
        Assert.assertEquals(3, l.get());
        Assert.assertEquals(1, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 2, 2, 10, l, c);
        Assert.assertEquals(4, l.get());
        Assert.assertEquals(3, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 1, 1, 26, l, c);
        Assert.assertEquals(5, l.get());
        Assert.assertEquals(5, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 1, 2, 24, l, c);
        Assert.assertEquals(5, l.get());
        Assert.assertEquals(4, c.get());
    }
}
