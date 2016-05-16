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


package com.github.wuic.test.engine;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ProcessContext;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.NutType;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.LineInspectorListener;
import com.github.wuic.engine.LineMatcherInspector;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.ScriptLineInspector;
import com.github.wuic.engine.core.AngularTemplateInspector;
import com.github.wuic.engine.core.CssInspectorEngine;
import com.github.wuic.engine.core.JavascriptInspectorEngine;
import com.github.wuic.engine.core.MemoryMapCacheEngine;
import com.github.wuic.engine.core.SourceMapLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.UrlUtils;
import com.github.wuic.xml.FileXmlContextBuilderConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link com.github.wuic.engine.core.TextInspectorEngine} and subclass tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
@RunWith(JUnit4.class)
public class InspectorTest {

    /**
     * Called only if ServiceLoader detects the configurator.
     */
    public static boolean called = false;

    /**
     * Makes sure the object builder inspector are installed thanks to the {@link java.util.ServiceLoader}.
     */
    @Test(timeout = 60000)
    public void serviceTest() {
        new ContextBuilder();
        Assert.assertTrue(called);
    }

    /**
     * <p>
     * Asserts that the result of the inspections of a stream built on top of given collections contains the specified
     * count.
     * </p>
     *
     * @param collection the collection of content to parse
     * @param builder the builder used to append the content
     * @param message failure message
     * @param count expected number of detected nut
     * @param autoCreate creates a nut each time the engine calls the DAO
     * @return the transformation result
     * @throws Exception if test fails
     */
    public String assertInspection(final String[][] collection,
                                   final StringBuilder builder,
                                   final String message,
                                   final int count,
                                   final Engine engine,
                                   final boolean autoCreate)
            throws Exception {
        final List<String> createdPaths = new ArrayList<String>();
        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.withRootPath(Mockito.anyString())).thenReturn(dao);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(NutDao.PathFormat.class), Mockito.any(ProcessContext.class))).thenAnswer(new Answer<Object>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                if (!autoCreate) {
                    return Collections.emptyList();
                }

                final List<Nut> retval = new ArrayList<Nut>();
                final Nut nut = Mockito.mock(Nut.class);

                Mockito.when(nut.getInitialName()).thenReturn(String.valueOf(invocationOnMock.getArguments()[0]));
                Mockito.when(nut.getInitialNutType()).thenReturn(NutType.getNutType(String.valueOf(invocationOnMock.getArguments()[0])));
                Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

                createdPaths.add(nut.getInitialName());

                if (nut.getInitialNutType().equals(NutType.MAP)) {
                    Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream(("{"
                            + "  \"version\": 3,"
                            + "  \"file\": \"testcode.js\","
                            + "  \"sections\": ["
                            + "    {"
                            + "      \"map\": {"
                            + "         \"version\": 3,"
                            + "         \"mappings\": \"AAAAA,QAASA,UAAS,EAAG;\","
                            + "         \"sources\": [\"testcode.js\"],"
                            + "         \"names\": [\"foo\"]"
                            + "      },"
                            + "      \"offset\": {"
                            + "        \"line\": 1,"
                            + "        \"column\": 1"
                            + "      }"
                            + "    }"
                            + "  ]"
                            + "}").getBytes()));
                } else {
                    Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream(("content of " + invocationOnMock.getArguments()[0]).getBytes()));
                }

                retval.add(nut);
                return retval;
            }
        });

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getId()).thenReturn("heap");
        Mockito.when(heap.hasCreated(Mockito.any(Nut.class))).thenReturn(true);
        Mockito.when(heap.findDaoFor(Mockito.any(Nut.class))).thenReturn(dao);
        final NutsHeap h = new NutsHeap(this, null, dao, "heap", heap);
        h.checkFiles(ProcessContext.DEFAULT);
        final List<Nut> nuts = new ArrayList<Nut>();

        for (final String[] c : collection) {
            final String rule = c[0];
            final String path = c[1];
            builder.append(String.format(rule, path));
        }

        final Nut nut = new PipedConvertibleNut(new ByteArrayNut(builder.toString().getBytes(), "", NutType.JAVASCRIPT, 1L, false)) {
            @Override
            public String getName() {
                final String retval = createdPaths.size() + ".css";
                h.addCreate(retval, retval);
                return retval;
            }

            @Override
            public String getInitialName() {
                return getName();
            }
        };
        nuts.add(nut);

        Mockito.when(heap.getNuts()).thenReturn(nuts);
        Mockito.when(heap.getNutDao()).thenReturn(dao);
        Mockito.when(heap.findDaoFor(Mockito.mock(Nut.class))).thenReturn(dao);

        final EngineRequest request = new EngineRequestBuilder("wid", h, null).contextPath("cp").build();
        final List<ConvertibleNut> res = engine.parse(request);
        final StringBuilder sb = new StringBuilder();

        for (final ConvertibleNut convertibleNut : res) {
            sb.append(NutUtils.readTransform(convertibleNut));
        }

        Assert.assertEquals(message + "\n" + Arrays.toString(createdPaths.toArray()), count, createdPaths.size());

        return sb.toString();
    }

    /**
     * <p>
     * Test when multiple @import/background are declared on the same line.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test//(timeout = 60000)
    public void multipleImportPerLineTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"@import url(\"%s\");", "jquery.ui.core.css"},
                new String[]{"@import \"%s\";", "jquery.ui.accordion.css"},
                new String[]{"@import '%s';", "jquery.ui.autocomplete.css"},
                new String[]{"@import url('%s');", "jquery.ui.button.css"},
                new String[]{"@import \"%s\";", "jquery.ui.datepicker.css"},
                new String[]{"@import '%s';", "jquery.ui.dialog.css"},
                new String[]{"@import url(  \"%s\");", "jquery.ui.menu.css"},
                new String[]{"foo{background: url(\"%s\") }", "sprite.png"},
                new String[]{"background: /* comment */ url(%s);", "sprite2.png"},
                new String[]{"background:url('%s');", "sprite3.png"},
                new String[]{"background: #FFF url('%s');", "sprite4.png"},
                new String[]{"@import /* some comments */ url(\"%s\");", "jquery.ui.spinner.css"},
                new String[]{"background: #dadada/*{bgColorHover}*/ url(%s)/*{bgImgUrlHover}*/ 50/*{bgHoverXPos}*/ 50/*{bgHoverYPos}*/ repeat-x/*{bgHoverRepeat}*/;", "images/ui-bg_glass_75_dadada_1x400.png"},
                new String[]{"background-image:url(%s);", "images/ui-icons_454545_256x240.png"},
        };

        final StringBuilder builder = new StringBuilder();

        // ignore comments
        builder.append("/*background: url('sprite5.png');*/");
        builder.append("/*background:\n url('sprite6.png');*/");
        final CssInspectorEngine e = new CssInspectorEngine();
        e.init(true);
        Assert.assertNotEquals(-1, assertInspection(collection, builder, null, collection.length, e, true));
    }

    /**
     * <p>
     * Tests when @font-face is used?
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void fontUrlTest() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("a {\n" +
                "  background: transparent;\n" +
                "}\n" +
                "a:active,\n" +
                "a:hover {\n" +
                "  outline: 0;\n" +
                "}\n" +
                "abbr[title] {\n" +
                "  border-bottom: 1px dotted;\n" +
                "}");
        sb.append("@font-face {\nfont-family: 'Glyphicons Halflings';");

        String[][] collection = new String[][]{
                new String[]{"src: url(\"%s\");", "../fonts/glyphicons-halflings-regular.eot"},
                new String[]{"src: url(\"%s\") format('embedded-opentype'),", "../fonts/glyphicons-halflings-regular.eot?#iefix"},
                new String[]{"src: url(\"%s\") format('woff'),", "../fonts/glyphicons-halflings-regular.woff"},
                new String[]{"src: url(\"%s\") format('truetype'),", "../fonts/glyphicons-halflings-regular.ttf"},
                new String[]{"src: url(\"%s\") format('svg');}", "../fonts/glyphicons-halflings-regular.svg#glyphicons_halflingsregular"},
        };

        final CssInspectorEngine e = new CssInspectorEngine();
        e.init(true);
        assertInspection(collection, sb, "Must handle font URLs.", collection.length, e, true);
    }

    /**
     * <p>
     * Test when sourceMappingURL is used.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void sourceMappingUrlTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"//sourceMappingURL=%s ", "sourcemap.js.map"},
                new String[]{"//# sourceMappingURL=%s ", "sourcemap1.js.map"},
                new String[]{"//@ sourceMappingURL=%s ", "sourcemap2.js.map"},
                new String[]{"// #sourceMappingURL=%s ", "sourcemap3.js.map"},
                new String[]{"// @sourceMappingURL=%s ", "sourcemap4.js.map"},
        };

        final CssInspectorEngine e = new CssInspectorEngine();
        e.init(true);
        assertInspection(collection, new StringBuilder(), "Should create nuts for sourceMap urls.", collection.length * 2, e, true);
    }

    /**
     * <p>
     * Tests that sourcemaps are not captured inside string literals.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test(timeout = 60000)
    public void sourceMappingInLiteralTest() throws WuicException, IOException {
        final String content = "function inlineSourceMap(sourceMap, sourceCode, sourceFilename) {\n" +
                "  ////# sourceMappingURL=url.js.map\n" +
                "  // This can be used with a sourcemap that has already has toJSON called on it.\n" +
                "  // Check first.\n" +
                "  var json = sourceMap;\n" +
                "  if (typeof sourceMap.toJSON === 'function') {\n" +
                "    json = sourceMap.toJSON();\n" +
                "  }\n" +
                "  json.sources = [sourceFilename];\n" +
                "  json.sourcesContent = [sourceCode];\n" +
                "  var base64 = Buffer(JSON.stringify(json)).toString('base64');\n" +
                "  return '//# sourceMappingURL=data:application/json;base64,' + base64;\n" +
                "}";

        final AtomicInteger count = new AtomicInteger();

        final ConvertibleNut n = Mockito.mock(ConvertibleNut.class);
        Mockito.when(n.getName()).thenReturn("foo.js");
        Mockito.when(n.getNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(n.getInitialName()).thenReturn("foo.js");
        Mockito.when(n.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(n.getSource()).thenReturn(new SourceImpl());
        Mockito.when(n.getVersionNumber()).thenReturn(new FutureLong(1L));

        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(ProcessContext.class))).thenReturn(Arrays.asList((Nut) n));

        final NutsHeap h = new NutsHeap(this, Arrays.asList("foo"), dao, "heap");
        h.checkFiles(ProcessContext.DEFAULT);
        final EngineRequest r = new EngineRequestBuilder("", h, Mockito.mock(Context.class)).build();

        SourceMapLineInspector.newInstance(Mockito.mock(NodeEngine.class)).inspect(new LineInspectorListener() {

            @Override
            public void onMatch(final char[] data,
                                final int offset,
                                final int length,
                                final String replacement,
                                final List<? extends ConvertibleNut> extracted)
                    throws WuicException {
                count.incrementAndGet();
            }
        }, content.toCharArray(), r, null, n);

        Assert.assertEquals(1, count.get());
    }

    /**
     * <p>
     * Test for AngularJS with no wrap pattern.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void angularNoWrapTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{
                        "angular.module('docsTemplateUrlDirective', [])\n" +
                                ".directive('myCustomer', function() {\n" +
                                "  return {\n" +
                                "    templateUrl  : '%s'\n" +
                                "  };\n" +
                                "})", "my-customer.html"}, {
                ".directive('myCustomer2', function() {\n" +
                "  return {\n\" +\n" +
                "    // comment templateUrl  : '%s'\n" +
                "  };\n" +
                "})", "my-customer2.html"}, {".directive('myCustomer3', function() {\n" +
                "  return {\n" +
                "    /*templateUrl  : '%s'*/\n" +
                "  };\n" +
                "});", "my-customer3.html"}
        };

        final JavascriptInspectorEngine e = new JavascriptInspectorEngine();
        e.init(true, "");

        assertInspection(collection,
                new StringBuilder(),
                "Should create nuts for templateUrl urls.",
                collection.length - 2,
                e,
                true);
    }

    /**
     * <p>
     * Tests an inspection performed on very large file.
     * </p>
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void largeInspection() throws Exception {
        String c = IOUtils.readString(new InputStreamReader(getClass().getResourceAsStream("/largeInspection.js")));
        final LineInspectorListener i = new LineInspectorListener() {
            @Override
            public void onMatch(final char[] data,
                                final int offset,
                                final int length,
                                final String replacement,
                                final List<? extends ConvertibleNut> extracted) throws WuicException {
            }
        };

        SourceMapLineInspector.newInstance(new JavascriptInspectorEngine()).inspect(i, c.toCharArray(), null, null, null);
        AngularTemplateInspector.newInstance(null).inspect(i, c.toCharArray(), null, null, null);
    }

    /**
     * <p>
     * Test for AngularJS with wrap pattern.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void angularWrapTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"angular.module('docsTemplateUrlDirective', [])\n" +
                        ".directive('myCustomer', function() {\n" +
                        "  return {\n" +
                        "    templateUrl  : fn(%s)\n" +
                        "  };\n" +
                        "});", "'my-customer.html'"},
        };

        final JavascriptInspectorEngine e = new JavascriptInspectorEngine();
        e.init(true, "fn(%s)");
        assertInspection(collection, new StringBuilder(), "Should create nuts for templateUrl urls.", collection.length, e, true);
    }

    /**
     * <p>
     * Test for URL rewrite when nut has not been found.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void angularFallbackTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"angular.module('docsTemplateUrlDirective', [])\n" +
                        ".directive('myCustomer', function() {\n" +
                        "  return {\n" +
                        "    templateUrl  : 'template.html'\n" +
                        "  };\n" +
                        "});", "my-customer.html"},
                {"angular.module('docsTemplateUrlDirective2', [])\n" +
                        ".directive('myCustomer2', function() {\n" +
                        "  return {\n" +
                        "    templateUrl  : 'template2.html?foo'\n" +
                        "  };\n" +
                        "});", "my-customer2.html"},
                {"angular.module('docsTemplateUrlDirective3', [])\n" +
                        ".directive('myCustomer3', function() {\n" +
                        "  return {\n" +
                        "    'templateUrl'  : 'template3.html?foo'\n" +
                        "  };\n" +
                        "});", "my-customer3.html"},
                {"angular.module('docsTemplateUrlDirective4', [])\n" +
                        ".directive('myCustomer4', function() {\n" +
                        "  return {\n" +
                        "    \"templateUrl\"  : 'template4.html?foo'\n" +
                        "  };\n" +
                        "});", "my-customer4.html"}
        };

        final JavascriptInspectorEngine e = new JavascriptInspectorEngine();
        e.init(true, "");
        final String value = assertInspection(collection, new StringBuilder(), "Should create nuts for templateUrl urls.", 0, e, false);

        Assert.assertTrue(value.contains("template.html?versionNumber=1"));
        Assert.assertTrue(value.contains("template2.html?foo&versionNumber=1"));
        Assert.assertTrue(value.contains("template3.html?foo&versionNumber=1"));
        Assert.assertTrue(value.contains("template4.html?foo&versionNumber=1"));

        int index = 0;
        int count = 0;

        while ((index = value.indexOf("templateUrl", index)) != -1) {
            count++;
            index++;
        }

        Assert.assertEquals(NumberUtils.FOUR, count);
    }

    /**
     * Test source map inspection.
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void sourceMapInspection() throws Exception {
        final NodeEngine engine = Mockito.mock(NodeEngine.class);
        final NodeEngine next = Mockito.mock(NodeEngine.class);
        Mockito.when(next.getEngineType()).thenReturn(EngineType.AGGREGATOR);
        Mockito.when(engine.getNext()).thenReturn(next);
        final SourceMapLineInspector inspector = new SourceMapLineInspector(engine);
        final LineMatcherInspector.LineMatcher matcher = inspector.lineMatcher("//# sourceMappingURL=foo.map");
        matcher.find();

        final NutDao dao = Mockito.mock(NutDao.class);

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getId()).thenReturn("heap");
        Mockito.when(heap.hasCreated(Mockito.any(Nut.class))).thenReturn(true);
        Mockito.when(heap.findDaoFor(Mockito.any(Nut.class))).thenReturn(dao);
        Mockito.when(heap.getNutDao()).thenReturn(dao);
        final NutsHeap h = new NutsHeap(this, null, dao, "heap", heap);
        h.checkFiles(ProcessContext.DEFAULT);

        final EngineRequest req = new EngineRequestBuilder("", h, null).build();
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        Mockito.when(nut.getSource()).thenReturn(new SourceImpl());
        Mockito.when(nut.getName()).thenReturn("nut");
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(next.works()).thenReturn(true);

        // rewrite statement
        inspector.appendTransformation(matcher, req, null, nut);

        // do not rewrite statement
        Mockito.when(next.getEngineType()).thenReturn(EngineType.MINIFICATION);
        Mockito.when(next.works()).thenReturn(false);
        final List<LineInspector.AppendedTransformation> a2 = inspector.appendTransformation(matcher, req, null, nut);
        Assert.assertNotNull(a2);
        Assert.assertEquals(1, a2.size());
        Assert.assertNotEquals(0, a2.get(0).getReplacement().length());
    }

    /**
     * <p>
     * Test for AngularJS with bad wrap pattern.
     * </p>
     *
     * @throws Exception if test succeed
     */
    @Test(timeout = 60000, expected = IllegalArgumentException.class)
    public void badAngularWrapTest() throws Exception {
        new JavascriptInspectorEngine().init(true, "fn('foo')");
    }

    /**
     * <p>
     * Test when @import with "data:" url.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void dataUrlTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"@import url(\"%s\");", "data:image/gif;base64,R0lGODlhCwAHAIAAACgoKP///yH5BAEAAAEALAAAAAALAAcAAAIORI4JlrqN1oMSnmmZDQUAOw=="},
                new String[]{"@import url(\"%s\");", "data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\'><filter id=\'jstree-grayscale\'><feColorMatrix type=\'matrix\' values=\'0.3333 0.3333 0.3333 0 0 0.3333 0.3333 0.3333 0 0 0.3333 0.3333 0.3333 0 0 0 0 0 1 0\'/></filter></svg>#jstree-grayscale"},
        };

        final CssInspectorEngine e = new CssInspectorEngine();
        e.init(true);
        assertInspection(collection, new StringBuilder(), "Shouldn't create nuts for 'data:' urls.", 0, e, true);
    }

    /**
     * Test when file is referenced with '../'.
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void parentRefTest() throws Exception {
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        builder.tag("parentRefTest")
                .contextEngineBuilder(MemoryMapCacheEngine.class).property(ApplicationConfig.BEST_EFFORT, true).toContext()
                .releaseTag();
        new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-deep.xml")).configure(builder);
        final Context ctx = builder.build();

        // ../ refers a file inside base directory hierarchy
        List<ConvertibleNut> group = ctx.process("", "css-inner", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
        Assert.assertEquals(2, group.size());
        group.get(0).transform();
        Assert.assertEquals(2, group.get(0).getReferencedNuts().size());
        Assert.assertEquals(1, group.get(1).getReferencedNuts().size());

        // ../ refers a file outside base directory hierarchy
        group = ctx.process("", "css-outer", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
        Assert.assertEquals(1, group.size());
        group.get(0).transform();
        Assert.assertEquals(2, group.get(0).getReferencedNuts().size());
    }

    /**
     * <p>
     * Tests a workflow built on top of a composition.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
    public void compositionByWorkflowTest() throws Exception {
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-deep.xml")).configure(builder);
        final Context ctx = builder.build();
        ctx.process("", "composite", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
    }

    /**
     * Tests that single comments are properly handled by script inspector.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void handleSingleCommentTest() throws WuicException {
        check("foo // comment1\n// comment2\n// bar\n// comment3\nbaz//comment4",
                Arrays.asList("// comment1\n", "// comment2\n", "// bar\n", "// comment3\n", "//comment4"),
                ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT);
        check("/*foo*/ // comment1\n/*// comment2\n*/// /*bar*/\n// comment3\nbaz//comment4",
                Arrays.asList("// comment1\n", "// /*bar*/\n", "// comment3\n", "//comment4"),
                ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT);
        check("var y = '\\\\\\'';/*foo*/ var j = \"// comment1\";\nvar i = 'hello';/*// comment2\n*/var z = '\"';// /*bar*/\n// comment3\nvar k = '\\'';baz//comment4",
                Arrays.asList("// /*bar*/\n", "// comment3\n", "//comment4"),
                ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT);
        check("/*var y = '\\\\\\'';/*foo* var j = \"// comment1\";\nvar i = 'hello';* // comment2\n*var z = '\"';// /*bar/\n// comment3\nvar k = '\\'';baz//comment4",
                Collections.EMPTY_LIST,
                ScriptLineInspector.ScriptMatchCondition.SINGLE_LINE_COMMENT);
    }

    /**
     * Tests that multiple line comments are properly handled by script inspector.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void handleMultiCommentTest() throws WuicException {
        check("foo // comment1\n// comment2\n// bar\n// comment3\nbaz//comment4",
                Collections.EMPTY_LIST,
                ScriptLineInspector.ScriptMatchCondition.MULTI_LINE_COMMENT);
        check("/*foo*/ // comment1\n/*// comment2\n*/// /*bar*/\n// comment3\nbaz//comment4",
                Arrays.asList("/*foo*/", "/*// comment2\n*/"),
                ScriptLineInspector.ScriptMatchCondition.MULTI_LINE_COMMENT);
        check("var y = '\\\\\\'';/*foo*/ var j = \"// comment1\";\nvar i = 'hello';/*// comment2\n*/var z = '\"';// /*bar*/\n// comment3\nvar k = '\\'';baz//comment4",
                Arrays.asList("/*foo*/", "/*// comment2\n*/"),
                ScriptLineInspector.ScriptMatchCondition.MULTI_LINE_COMMENT);
        final String c = "/*var y = '\\\\\\'';/*foo* var j = \"// comment1\";\nvar i = 'hello';* // comment2\n*var z = '\"';// /*bar/\n// comment3\nvar k = '\\'';baz//comment4";
        check(c, Arrays.asList(c), ScriptLineInspector.ScriptMatchCondition.MULTI_LINE_COMMENT);
    }

    /**
     * Tests that no comments are properly handled by script inspector.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void handleNoCommentTest() throws WuicException {
        check("foo // comment1\n// comment2\n// bar\n// comment3\nbaz//comment4",
                Arrays.asList("foo                                            baz"),
                ScriptLineInspector.ScriptMatchCondition.NO_COMMENT);
        check("/*foo*/ // comment1\n/*// comment2\n*/// /*bar*/\n// comment3\nbaz//comment4",
                Arrays.asList("                                                           baz"),
                ScriptLineInspector.ScriptMatchCondition.NO_COMMENT);
        check("var y = '\\\\\\'';/*foo*/ var j = \"// comment1\";\nvar i = 'hello';/*// comment2\n*/var z = '\"';// /*bar*/\n// comment3\nvar k = '\\'';baz//comment4",
                Arrays.asList("var y = '\\\\\\'';        var j = \"// comment1\";\nvar i = 'hello';                var z = '\"';                       var k = '\\'';baz"),
                ScriptLineInspector.ScriptMatchCondition.NO_COMMENT);
        final String c = "/*var y = '\\\\\\'';/*foo* var j = \"// comment1\";\nvar i = 'hello';* // comment2\n*var z = '\"';// /*bar/\n// comment3\nvar k = '\\'';baz//comment4";
        check(c, Collections.EMPTY_LIST, ScriptLineInspector.ScriptMatchCondition.NO_COMMENT);
    }

    /**
     * Tests that all tokens are properly handled by script inspector.
     *
     * @throws WuicException if test fails
     */
    @Test(timeout = 60000)
    public void handleAllTest() throws WuicException {
        String c = "foo // comment1\n// comment2\n// bar\n// comment3\nbaz//comment4";
        check(c, Arrays.asList(c), ScriptLineInspector.ScriptMatchCondition.ALL);
        c = "/*foo*/ // comment1\n/*// comment2\n*/// /*bar*/\n// comment3\nbaz//comment4";
        check(c, Arrays.asList(c), ScriptLineInspector.ScriptMatchCondition.ALL);
        c = "var y = '\\\\\\'';/*foo*/ var j = \"// comment1\";\nvar i = 'hello';/*// comment2\n*/var z = '\"';// /*bar*/\n// comment3\nvar k = '\\'';baz//comment4";
        check(c, Arrays.asList(c), ScriptLineInspector.ScriptMatchCondition.ALL);
        c = "/*var y = '\\\\\\'';/*foo* var j = \"// comment1\";\nvar i = 'hello';* // comment2\n*var z = '\"';// /*bar/\n// comment3\nvar k = '\\'';baz//comment4";
        check(c, Arrays.asList(c), ScriptLineInspector.ScriptMatchCondition.ALL);
    }

    /**
     * <p>
     * Checks that the inspection of the given content and the option specified in parameter generates notification
     * for all expected string.
     * </p>
     *
     * @param content content to inspect
     * @param expected string to be notified
     * @param opt the option
     * @throws WuicException if any error occurs
     */
    private void check(final String content,
                       final List<String> expected,
                       final ScriptLineInspector.ScriptMatchCondition opt) throws WuicException {
        final List<String> captured = new ArrayList<String>();
        final ScriptLineInspector i = new ScriptLineInspector(opt) {

            /**
             * {@inheritDoc}
             */
            @Override
            public Range doFind(final char[] buffer,
                                final int offset,
                                final int length,
                                final EngineRequest request,
                                final CompositeNut.CompositeInputStream cis,
                                final ConvertibleNut originalNut) {
                return new Range(Range.Delimiter.EOF, offset, offset + length);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String toString(final ConvertibleNut convertibleNut) throws IOException {
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected List<AppendedTransformation> appendTransformation(final char[] data,
                                                                        final int offset,
                                                                        final int length,
                                                                        final EngineRequest request,
                                                                        final CompositeNut.CompositeInputStream cis,
                                                                        final ConvertibleNut originalNut)
                    throws WuicException {
                return Arrays.asList(new AppendedTransformation(offset, offset + length, null, ""));
            }
        };

        i.newInspection();
        i.inspect(new LineInspectorListener() {
            @Override
            public void onMatch(final char[] data,
                                final int offset,
                                final int length,
                                final String replacement,
                                final List<? extends ConvertibleNut> extracted)
                    throws WuicException {
                captured.add(new String(data, offset, length));
            }
        }, content.toCharArray(), null, null, null);

        Assert.assertEquals(expected, captured);
    }
}