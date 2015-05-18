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


package com.github.wuic.test.engine;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ProcessContext;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.NutType;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.CssInspectorEngine;
import com.github.wuic.engine.core.JavascriptInspectorEngine;
import com.github.wuic.engine.core.MemoryMapCacheEngine;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link com.github.wuic.engine.core.TextInspectorEngine} and subclass tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.4.1
 */
@RunWith(JUnit4.class)
public class InspectorTest {

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
        final AtomicInteger createCount = new AtomicInteger(0);
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
                Mockito.when(nut.getInitialName()).thenReturn(String.valueOf(createCount.incrementAndGet()));
                Mockito.when(nut.getInitialNutType()).thenReturn(NutType.CSS);
                Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream(("content of " + invocationOnMock.getArguments()[0]).getBytes()));
                Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

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

        final Nut nut = new PipedConvertibleNut(new ByteArrayNut(builder.toString().getBytes(), "", NutType.CSS, 1L, false)) {
            @Override
            public String getName() {
                final String retval = createCount.get() + ".css";
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

        Assert.assertEquals(message, count, createCount.get());

        return sb.toString();
    }

    /**
     * <p>
     * Test when multiple @import/background are declared on the same line.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
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
        Assert.assertNotEquals(-1, assertInspection(collection, builder, null, collection.length, new CssInspectorEngine(true, "UTF-8"), true));
    }

    /**
     * <p>
     * Tests when @font-face is used?
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
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


        assertInspection(collection, sb, "Must handle font URLs.", collection.length, new CssInspectorEngine(true, "UTF-8"), true);
    }

    /**
     * <p>
     * Test when sourceMappingURL is used.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void sourceMappingUrlTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"//sourceMappingURL=%s", "sourcemap.js.map"},
                new String[]{"//# sourceMappingURL=%s", "sourcemap1.js.map"},
                new String[]{"//@ sourceMappingURL=%s", "sourcemap2.js.map"},
                new String[]{"// #sourceMappingURL=%s", "sourcemap3.js.map"},
                new String[]{"// @sourceMappingURL=%s", "sourcemap4.js.map"}
        };

        assertInspection(collection, new StringBuilder(), "Should create nuts for sourceMap urls.", collection.length, new CssInspectorEngine(true, "UTF-8"), true);
    }

    /**
     * <p>
     * Test for AngularJS with no wrap pattern.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
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

        assertInspection(collection,
                new StringBuilder(),
                "Should create nuts for templateUrl urls.",
                collection.length - 2,
                new JavascriptInspectorEngine(true, "UTF-8", ""), true);
    }

    /**
     * <p>
     * Test for AngularJS with wrap pattern.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void angularWrapTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"angular.module('docsTemplateUrlDirective', [])\n" +
                        ".directive('myCustomer', function() {\n" +
                        "  return {\n" +
                        "    templateUrl  : fn(%s)\n" +
                        "  };\n" +
                        "});", "'my-customer.html'"},
        };

        assertInspection(collection,
                new StringBuilder(),
                "Should create nuts for templateUrl urls.",
                collection.length,
                new JavascriptInspectorEngine(true, "UTF-8", "fn(%s)"), true);
    }

    /**
     * <p>
     * Test for URL rewrite when nut has not been found.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
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
                        "});", "my-customer2.html"}
        };

        final String value = assertInspection(collection,
                new StringBuilder(),
                "Should create nuts for templateUrl urls.",
                0,
                new JavascriptInspectorEngine(true, "UTF-8", ""), false);

        Assert.assertTrue(value.contains("template.html?versionNumber=1"));
        Assert.assertTrue(value.contains("template2.html?foo&versionNumber=1"));

    }

    /**
     * <p>
     * Test for AngularJS with bad wrap pattern.
     * </p>
     *
     * @throws Exception if test succeed
     */
    @Test(expected = IllegalArgumentException.class)
    public void badAngularWrapTest() throws Exception {
        new JavascriptInspectorEngine(true, "UTF-8", "fn('foo')");
    }

    /**
     * <p>
     * Test when @import with "data:" url.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void dataUrlTest() throws Exception {
        String[][] collection = new String[][]{
                new String[]{"@import url(\"%s\");", "data:image/gif;base64,R0lGODlhCwAHAIAAACgoKP///yH5BAEAAAEALAAAAAALAAcAAAIORI4JlrqN1oMSnmmZDQUAOw=="},
                new String[]{"@import url(\"%s\");", "data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\'><filter id=\'jstree-grayscale\'><feColorMatrix type=\'matrix\' values=\'0.3333 0.3333 0.3333 0 0 0.3333 0.3333 0.3333 0 0 0.3333 0.3333 0.3333 0 0 0 0 0 1 0\'/></filter></svg>#jstree-grayscale"},
        };

        assertInspection(collection, new StringBuilder(), "Shouldn't create nuts for 'data:' urls.", 0, new CssInspectorEngine(true, "UTF-8"), true);
    }

    /**
     * Test when file is referenced with '../'.
     *
     * @throws Exception if test fails
     */
    @Test
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
    @Test
    public void compositionByWorkflowTest() throws Exception {
        final ContextBuilder builder = new ContextBuilder().configureDefault();
        new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-deep.xml")).configure(builder);
        final Context ctx = builder.build();
        ctx.process("", "composite", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
    }
}
