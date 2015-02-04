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

import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.core.AbstractConverterEngine;
import com.github.wuic.engine.core.JavascriptInspectorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.NutUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests {@link AbstractConverterEngine} base class.
 */
@RunWith(JUnit4.class)
public class ConverterEngineTest {

    /**
     * An inspector that must be invoked by the converter.
     */
    public static class I extends JavascriptInspectorEngine {
        public I() {
            super(true, "UTF-8");
            addInspector(new LineInspector(Pattern.compile(Pattern.quote("function myFunctionSetsCssStyle() {}"))) {
                protected String toString(final ConvertibleNut convertibleNut) throws IOException {
                    return null;
                }

                public List<? extends ConvertibleNut> appendTransformation(final Matcher matcher,
                                                                           final StringBuilder replacement,
                                                                           final EngineRequest request,
                                                                           final CompositeNut.CompositeInputStream cis,
                                                                           final ConvertibleNut originalNut) throws WuicException {
                    replacement.append("function myReplacedFunctionSetsCssStyle() {}");
                    return null;
                }
            });
        }
    }

    /**
     * A basic converter.
     */
    public static class C extends AbstractConverterEngine {

        /**
         * <p>
         * Builds a new engine.
         * </p>
         *
         * @param convert enabled conversion or not
         */
        public C(final Boolean convert) {
            super(convert, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<NutType> getNutTypes() {
            return Arrays.asList(NutType.CSS);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected NutType targetNutType() {
            return NutType.JAVASCRIPT;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final ConvertibleNut convertible, final EngineRequest e)
                throws IOException {
            os.write("function myFunctionSetsCssStyle() {}".getBytes());
            convertible.setNutType(NutType.JAVASCRIPT);
        }
    }

    /**
     * <p>
     * Base class must checks that conversion is done.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test(expected = IllegalStateException.class)
    public void nutTypeNotChangedTest() throws WuicException, IOException {
        final Nut nut = new ByteArrayNut(".myClass {}".getBytes(), "foo.css", NutType.CSS, 1L);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final NodeEngine converter = new C(true) {
            @Override
            protected NutType targetNutType() {
                return NutType.CSS;
            }
        };

        final List<ConvertibleNut> nuts = converter.parse(new EngineRequestBuilder("", heap)
                .chain(NutType.CSS, converter)
                .chain(NutType.JAVASCRIPT, new I())
                .build());
        Assert.assertEquals(1, nuts.size());
        NutUtils.readTransform(nuts.get(0));
    }

    /**
     * <p>
     * Checks that conversion is performed: type and name must change. Converted nut must also be inspected.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test
    public void enableConversionTest() throws WuicException, IOException {
        final Nut nut = new ByteArrayNut(".myClass {}".getBytes(), "foo.css", NutType.CSS, 1L);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final NodeEngine converter = new C(true);
        final List<ConvertibleNut> nuts = converter.parse(new EngineRequestBuilder("", heap)
                .chain(NutType.CSS, converter)
                .chain(NutType.JAVASCRIPT, new I())
                .build());
        Assert.assertEquals(1, nuts.size());
        final String content = NutUtils.readTransform(nuts.get(0));
        Assert.assertEquals(NutType.JAVASCRIPT, nuts.get(0).getNutType());
        Assert.assertEquals(NutType.CSS, nuts.get(0).getInitialNutType());
        Assert.assertEquals("aggregate.css.js", nuts.get(0).getName());
        Assert.assertEquals("foo.css", nuts.get(0).getInitialName());
        Assert.assertEquals("function myReplacedFunctionSetsCssStyle() {}\n", content);
    }

    /**
     * <p>
     * Checks that nothing is performed when converter is disabled.
     * </p>
     *
     * @throws WuicException if test fails
     * @throws IOException if test fails
     */
    @Test
    public void disableConversionTest() throws WuicException, IOException {
        final Nut nut = new ByteArrayNut(".myClass {}".getBytes(), "foo.css", NutType.CSS, 1L);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final NodeEngine converter = new C(false);
        final List<ConvertibleNut> nuts = converter.parse(new EngineRequestBuilder("", heap)
                .chain(NutType.CSS, converter)
                .chain(NutType.JAVASCRIPT, new I())
                .build());
        Assert.assertEquals(1, nuts.size());
        final String content = NutUtils.readTransform(nuts.get(0));
        Assert.assertEquals(NutType.CSS, nuts.get(0).getNutType());
        Assert.assertEquals(NutType.CSS, nuts.get(0).getInitialNutType());
        Assert.assertEquals("foo.css", nuts.get(0).getName());
        Assert.assertEquals("foo.css", nuts.get(0).getInitialName());
        Assert.assertEquals(".myClass {}", content);
    }
}
