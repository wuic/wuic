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

import com.github.wuic.AnnotationProcessor;
import com.github.wuic.AnnotationScanner;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ServiceLoaderAnnotationScanner;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.path.FilePath;
import com.github.wuic.path.core.ZipEntryFilePath;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Input;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutDiskStore;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Output;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.SequenceReader;
import com.github.wuic.util.StringUtils;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.util.TemporaryFileManager;
import com.github.wuic.util.Timer;
import com.github.wuic.util.TimerTreeFactory;
import com.github.wuic.util.UrlMatcher;
import com.github.wuic.util.UrlUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
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
 * @since 0.3.4
 */
@RunWith(JUnit4.class)
public class UtilityTest extends WuicTest {

    /**
     * Temporary file manager.
     */
    @ClassRule
    public static TemporaryFileManagerRule temporaryFileManager = new TemporaryFileManagerRule();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Temporary folder.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

        Mockito.when(first.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        Mockito.when(second.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        Mockito.when(third.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        Mockito.when(fourth.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        Mockito.when(fifth.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));

        Mockito.when(first.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(second.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(third.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(fourth.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(fifth.getVersionNumber()).thenReturn(new FutureLong(1L));

        Mockito.when(first.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(second.getInitialNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
        Mockito.when(third.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(fourth.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(fifth.getInitialNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));

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

        List<ConvertibleNut> output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input, Charset.defaultCharset().displayName());

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

        output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input, Charset.defaultCharset().displayName());

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

        output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input, Charset.defaultCharset().displayName());

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

        output = CompositeNut.mergeNuts(Mockito.mock(ProcessContext.class), input, Charset.defaultCharset().displayName());

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
        Assert.assertEquals(StringUtils.merge(new String[]{"foo", "oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo:", "oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo:", ":oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo", ":oof",}, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo", ":oof", "foo",}, ":"), "foo:oof:foo");
        Assert.assertEquals(StringUtils.merge(new String[]{"foo", ":oof", "foo",}, null), "foo:ooffoo");
        Assert.assertEquals(StringUtils.merge(new String[]{":", "oof",}, ":"), ":oof");
        Assert.assertEquals(StringUtils.merge(new String[]{":", ":foo:", ":oof",}, ":"), ":foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[]{":", "foo:", ":oof:",}, ":"), ":foo:oof:");
        Assert.assertEquals(StringUtils.merge(new String[]{"/opt", "data",}, "/"), "/opt/data");
        Assert.assertEquals(StringUtils.merge(new String[]{"http://", "server:80", "", "root"}, "/"), "http://server:80/root");
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

        final DirectoryPath parent = DirectoryPath.class.cast(IOUtils.buildPath(IOUtils.mergePath(tmp, nanoTime),
                Charset.defaultCharset().displayName(),
                temporaryFileManager.getTemporaryFileManager()));
        List<String> list = IOUtils.listFile(parent, Pattern.compile(".*js"));

        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), IOUtils.mergePath("foo", file.getName()));

        // Part 2
        final String str = getClass().getResource("/images").toString();
        final String baseDir = str.substring(str.indexOf(":/") + 1);
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir,
                Charset.defaultCharset().displayName(),
                temporaryFileManager.getTemporaryFileManager()));
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
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir,
                Charset.defaultCharset().displayName(),
                temporaryFileManager.getTemporaryFileManager()));
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
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir,
                Charset.defaultCharset().displayName(),
                temporaryFileManager.getTemporaryFileManager()));
        final List<String> list = IOUtils.listFile(directoryPath, Pattern.compile(".*.png"));
        ZipEntryFilePath.ZipFileInputStream is;

        for (final String s : list) {
            is = ZipEntryFilePath.ZipFileInputStream.class.cast(FilePath.class.cast(directoryPath.getChild(s)).openStream().inputStream());
            IOUtils.copyStream(is, new ByteArrayOutputStream());
            is.close();

            if (!"img.zip".equals(is.getFile().getName())) {
                Assert.assertTrue(is.getFile().delete());
            }

            // Retry when file is deleted
            is = ZipEntryFilePath.ZipFileInputStream.class.cast(FilePath.class.cast(directoryPath.getChild(s)).openStream().inputStream());
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
        final DirectoryPath directoryPath = DirectoryPath.class.cast(IOUtils.buildPath(baseDir,
                Charset.defaultCharset().displayName(),
                temporaryFileManager.getTemporaryFileManager()));
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
        Mockito.when(nut.getNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        HtmlUtil.writeScriptImport(nut, "myPath");

        final Map<String, String> a = new HashMap<String, String>();
        a.put("param", "param");

        final String res = HtmlUtil.writeScriptImport(nut, "myPath", a);
        Assert.assertTrue(res.contains("\"myPath/1/foo.js\""));
        Assert.assertTrue(res.startsWith("<script type=\"text/javascript\" param=\"param\" "));
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
        Mockito.when(nut.getNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        final Map<String, String> a = new HashMap<String, String>();
        a.put("param", "param");
        final String res = HtmlUtil.writeScriptImport(nut, "myPath", a);
        Assert.assertTrue(res.contains("\"myPath/1/foo.css\""));
        Assert.assertTrue(res.startsWith("<link type=\"text/css\" rel=\"stylesheet\" param=\"param\" "));
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
        Mockito.when(nut.getNutType()).thenReturn(new NutType(EnumNutType.PNG, Charset.defaultCharset().displayName()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        final Map<String, String> a = new HashMap<String, String>();
        a.put("param", "param");

        final String res = HtmlUtil.writeScriptImport(nut, "myPath", a);
        Assert.assertTrue(res.contains("\"myPath/1/foo.png\""));
        Assert.assertTrue(res.startsWith("<img param=\"param\" "));
    }

    /**
     * Test URL generation.
     */
    @Test
    public void getUrltTest() {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
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
        final NutTypeFactory f = new NutTypeFactory(Charset.defaultCharset().displayName());
        NutType nutType = f.getNutTypeForMimeType("text/html;charset=UTF-8");
        Assert.assertTrue(nutType.isBasedOn(EnumNutType.HTML));

        nutType = f.getNutTypeForMimeType("text/html");
        Assert.assertTrue(nutType.isBasedOn(EnumNutType.HTML));

        nutType = f.getNutTypeForMimeType("bad-mime-type");
        Assert.assertNull(nutType);
    }

    /**
     * Default annotation scanner test.
     */
    @Test
    public void defaultAnnotationScannerTest() {
        final AnnotationScanner s = new ServiceLoaderAnnotationScanner();
        final AtomicInteger count = new AtomicInteger();
        final AnnotationProcessor processor = new AnnotationProcessor() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Class<? extends Annotation> requiredAnnotation() {
                return EngineService.class;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void handle(final Class<?> annotatedType) {
                count.incrementAndGet();
            }
        };

        s.scan("com", processor);
        Assert.assertNotEquals(0, count.intValue());
        count.set(0);
        s.scan("net", processor);
        Assert.assertEquals(0, count.intValue());
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
        final Pipe<ConvertibleNut> p = new Pipe<ConvertibleNut>(Mockito.mock(ConvertibleNut.class), new InMemoryInput(new byte[] { (byte) count.get() }, Charset.defaultCharset().displayName()));
        class T extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                int r = is.inputStream().read();
                os.outputStream().write(r + count.decrementAndGet());
                return true;
            }
        }

        int expect = 0;
        for (int i = 1; i <= count.get(); i++) {
            expect += i;
            p.register(new T());
        }

        final InMemoryOutput bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        Pipe.executeAndWriteTo(p, new ArrayList<Pipe.OnReady>(), bos);
        Assert.assertEquals(expect, bos.execution().getByteResult()[0]);
    }

    /**
     * <p>
     * Tests the pipe with skipped transformer.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void skipTransformerTest() throws Exception {
        final Pipe<ConvertibleNut> p = new Pipe<ConvertibleNut>(Mockito.mock(ConvertibleNut.class), new InMemoryInput("content", Charset.defaultCharset().displayName()));
        class T extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                return false;
            }
        }
        p.register(new T());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.execute(new Pipe.OnReady() {
            @Override
            public void ready(final Pipe.Execution e) throws IOException {
                e.writeResultTo(out);
            }
        });

        Assert.assertEquals("content", out.toString());
    }

    /**
     * <p>
     * Tests the pipe with no transformer.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void noTransformerTest() throws Exception {
        final Pipe<ConvertibleNut> p = new Pipe<ConvertibleNut>(Mockito.mock(ConvertibleNut.class), new InMemoryInput("content", Charset.defaultCharset().displayName()));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.execute(new Pipe.OnReady() {
            @Override
            public void ready(final Pipe.Execution e) throws IOException {
                e.writeResultTo(out);
            }
        });

        Assert.assertEquals("content", out.toString());
    }

    /**
     * <p>
     * Tests the pipe with first skipped transformer.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void firstTransformerSkippedTest() throws Exception {
        final Pipe<ConvertibleNut> p = new Pipe<ConvertibleNut>(Mockito.mock(ConvertibleNut.class), new InMemoryInput("content", Charset.defaultCharset().displayName()));
        class T1 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                return false;
            }
        }

        class T2 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                os.writer().write("T2");
                return true;
            }
        }
        p.register(new T1());
        p.register(new T2());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.execute(new Pipe.OnReady() {
            @Override
            public void ready(final Pipe.Execution e) throws IOException {
                e.writeResultTo(out);
            }
        });

        Assert.assertEquals("T2", out.toString());
    }

    /**
     * <p>
     * Tests the pipe with last skipped transformer.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void lastTransformerSkippedTest() throws Exception {
        final Pipe<ConvertibleNut> p = new Pipe<ConvertibleNut>(Mockito.mock(ConvertibleNut.class), new InMemoryInput("content", Charset.defaultCharset().displayName()));
        class T2 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                return false;
            }
        }

        class T1 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                os.writer().write("T1");
                return true;
            }
        }
        p.register(new T1());
        p.register(new T2());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.execute(new Pipe.OnReady() {
            @Override
            public void ready(final Pipe.Execution e) throws IOException {
                e.writeResultTo(out);
            }
        });

        Assert.assertEquals("T1", out.toString());
    }

    /**
     * <p>
     * Tests the pipe with middle skipped transformer.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void middleTransformerSkippedTest() throws Exception {
        final Pipe<ConvertibleNut> p = new Pipe<ConvertibleNut>(Mockito.mock(ConvertibleNut.class), new InMemoryInput("content", Charset.defaultCharset().displayName()));
        class T1 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                os.writer().write("T1");
                return true;
            }
        }

        class T2 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                os.writer().write("T2");
                return false;
            }
        }

        class T3 extends Pipe.DefaultTransformer<ConvertibleNut> {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut o) throws IOException {
                Writer.class.cast(IOUtils.copyStream(is, os)).write("T3");
                return true;
            }
        }

        p.register(new T1());
        p.register(new T2());
        p.register(new T3());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.execute(new Pipe.OnReady() {
            @Override
            public void ready(final Pipe.Execution e) throws IOException {
                e.writeResultTo(out);
            }
        });

        Assert.assertEquals("T1T3", out.toString());
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
        Assert.assertEquals(4, c.get());

        StringUtils.reachEndLineAndColumn(lines, 1, 3, 3, l, c);
        Assert.assertEquals(1, l.get());
        Assert.assertEquals(6, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 2, 2, 3, l, c);
        Assert.assertEquals(2, l.get());
        Assert.assertEquals(5, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 2, 2, 4, l, c);
        Assert.assertEquals(3, l.get());
        Assert.assertEquals(2, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 2, 2, 10, l, c);
        Assert.assertEquals(4, l.get());
        Assert.assertEquals(4, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 1, 1, 26, l, c);
        Assert.assertEquals(5, l.get());
        Assert.assertEquals(6, c.get());

        l.set(0);
        c.set(0);
        StringUtils.reachEndLineAndColumn(lines, 1, 2, 24, l, c);
        Assert.assertEquals(5, l.get());
        Assert.assertEquals(5, c.get());
    }

    /**
     * <p>
     * Test {@link StringUtils#indexOf(char[], int, int, char[])}.
     * </p>
     */
    @Test
    public void indexOfTest() {
        Assert.assertEquals(0, StringUtils.indexOf("foo".toCharArray(), 0, 3, "foo".toCharArray()));
        Assert.assertEquals(1, StringUtils.indexOf("foo".toCharArray(), 0, 3, "o".toCharArray()));
        Assert.assertEquals(-1, StringUtils.indexOf("foo".toCharArray(), 0, 3, "fooo".toCharArray()));
        Assert.assertEquals(0, StringUtils.indexOf("foobar".toCharArray(), 0, 6, "foo".toCharArray()));
        Assert.assertEquals(3, StringUtils.indexOf("foobar".toCharArray(), 0, 6, "bar".toCharArray()));
        Assert.assertEquals(-1, StringUtils.indexOf("foobar".toCharArray(), 0, 5, "bar".toCharArray()));
        Assert.assertEquals(-1, StringUtils.indexOf("foo".toCharArray(), 1, 3, "foo".toCharArray()));
        Assert.assertEquals(3, StringUtils.indexOf("foobarbaz".toCharArray(), 0, 9, "bar".toCharArray()));
        Assert.assertEquals(3, StringUtils.indexOf("foobarbaz".toCharArray(), 3, 6, "bar".toCharArray()));
        Assert.assertEquals(-1, StringUtils.indexOf("foobarbaz".toCharArray(), 4, 5, "bar".toCharArray()));
    }

    /**
     * <p>
     * Test {@link StringUtils#replace(char[], int, int, char)}.
     * </p>
     */
    @Test
    public void replaceTest() {
        final char[] array = new char[] { 'a', 'b', 'c', 'd' };
        StringUtils.replace(array, 1, 3, ' ');
        Assert.assertArrayEquals(array, new char[]{'a', ' ', ' ', 'd'});
        StringUtils.replace(array, 1, 1, ' ');
        Assert.assertArrayEquals(array, new char[]{'a', ' ', ' ', 'd'});
        StringUtils.replace(array, 3, 4, ' ');
        Assert.assertArrayEquals(array, new char[]{'a', ' ', ' ', ' '});
        StringUtils.replace(array, 0, 1, ' ');
        Assert.assertArrayEquals(array, new char[]{' ', ' ', ' ', ' '});
    }

    /**
     * <p>
     * Test {@link IOUtils#checkCharset(String)}.
     * </p>
     */
    @Test
    public void checkCharsetTest() {
        Assert.assertEquals(Charset.defaultCharset().displayName(), IOUtils.checkCharset(Charset.defaultCharset().displayName()));
        Assert.assertEquals(Charset.defaultCharset().name(), IOUtils.checkCharset(""));
    }

    /**
     * <p>
     * Tests {@link SequenceReader}.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test
    public void sequenceReaderTest() throws IOException {
        final Reader r1 = new StringReader("a");
        final Reader r2 = new StringReader("b");
        final Reader r3 = new StringReader("c");
        final Reader seq = new SequenceReader(Arrays.asList(r1, r2, r3));
        Assert.assertEquals("abc", IOUtils.readString(seq));
        seq.close();
    }

    /**
     * <p>
     * Nominal test for {@link TemporaryFileManager}.
     * </p>
     *
     * @throws IOException if test fails
     * @throws InterruptedException if a thread fails
     */
    @Test
    public void temporaryFileTest() throws IOException, InterruptedException {
        final TemporaryFileManager temporaryFileManager = new TemporaryFileManager(temporaryFolder.newFolder(), 1);
        final File file = temporaryFileManager.createTempFile(getClass().getSimpleName(), "temporaryFileTest", ".test");
        Assert.assertTrue(file.exists());
        Thread.sleep(1500L);
        Assert.assertFalse(file.exists());
    }

    /**
     * <p>
     * Test eternal file with {@link TemporaryFileManager}.
     * </p>
     *
     * @throws IOException if test fails
     * @throws InterruptedException if a thread fails
     */
    @Test
    public void eternalTemporaryFileTest() throws IOException, InterruptedException {
        final TemporaryFileManager temporaryFileManager = new TemporaryFileManager(temporaryFolder.newFolder(), 1);
        final File file = temporaryFileManager.createTempFile(getClass().getSimpleName(), "temporaryFileTest", ".test", -1);
        Assert.assertTrue(file.exists());
        Thread.sleep(1500L);
        Assert.assertTrue(file.exists());
    }

    /**
     * <p>
     * Tests when test directory is already cleans.
     * </p>
     *
     * @throws IOException if test fails
     * @throws InterruptedException if a thread issue occurs
     */
    @Test
    public void temporaryFileAlreadyDeletedTest() throws IOException, InterruptedException {
        final TemporaryFileManager temporaryFileManager = new TemporaryFileManager(temporaryFolder.newFolder(), 1);
        final File file = temporaryFileManager.createTempFile(getClass().getSimpleName(), "temporaryFileTest", ".test");
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.delete());
        Thread.sleep(1500L);
    }

    /**
     * <p>
     * Tests when temporary directory is a file.
     * </p>
     *
     * @throws IOException if test fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void badTemporaryFileTest() throws IOException {
        new TemporaryFileManager(temporaryFolder.newFile(), 1);
    }

    /**
     * <p>
     * Tests when temporary file can't be deleted.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     */
    @Test(expected = IllegalArgumentException.class)
    public void temporaryFileOpenTest() throws IOException {
        final File d = temporaryFolder.newFolder();
        final File f = new File(d, "t");
        OutputStream os = null;

        try {
            os = new FileOutputStream(f);
            new TemporaryFileManager(d, 1);
        } finally {
            IOUtils.close(os);
        }
    }

    /**
     * <p>
     * Test for {@link com.github.wuic.util.TimerTreeFactory.TimerTree} class.
     * </p>
     *
     * @throws InterruptedException if test fails
     */
    @Test
    public void timerTreeTest() throws InterruptedException {
        final TimerTreeFactory timerTreeFactory = new TimerTreeFactory();

        final Timer t1 = timerTreeFactory.getTimerTree();
        t1.start();
        Thread.sleep(100L);

        final Timer t2 = timerTreeFactory.getTimerTree();
        t2.start();
        Thread.sleep(100L);

        final Timer t3 = timerTreeFactory.getTimerTree();
        t3.start();
        Thread.sleep(100L);

        Assert.assertTrue(t3.end() < 200);
        Assert.assertTrue(t2.end() < 200);
        Assert.assertTrue(t1.end() < 200);
    }

    /**
     * <p>
     * Tests {@link NumberUtils#remainingLength(int, int, int)}.
     * </p>
     */
    @Test
    public void remainingLengthTest() {
        Assert.assertEquals(5, NumberUtils.remainingLength(10, 10, 15));
    }
}
