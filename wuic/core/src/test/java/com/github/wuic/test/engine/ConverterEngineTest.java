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


package com.github.wuic.test.engine;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.core.AbstractCompressorEngine;
import com.github.wuic.engine.core.AbstractConverterEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.Input;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Output;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link AbstractConverterEngine} base class.
 */
@RunWith(JUnit4.class)
public class ConverterEngineTest {

    /**
     * An inspector that must be invoked by the converter.
     */
    public static class I extends AbstractCompressorEngine {

        public I() {
            init(true);
        }

        @Override
        public List<NutType> getNutTypes() {
            return Arrays.asList(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        }

        @Override
        public EngineType getEngineType() {
            return EngineType.MINIFICATION;
        }

        @Override
        public boolean transform(final Input is, final Output os, final ConvertibleNut convertible, EngineRequest request) throws IOException {
            os.writer().write("function myReplacedFunctionSetsCssStyle() {}");
            return true;
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
            init(convert, false);
            setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<NutType> getNutTypes() {
            return Arrays.asList(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected NutType targetNutType() {
            return getNutTypeFactory().getNutType(EnumNutType.JAVASCRIPT);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Input transform(final Input is,
                               final ConvertibleNut convertible,
                               final EngineRequest e)
                throws IOException {
            return new InMemoryInput("function myFunctionSetsCssStyle() {}", Charset.defaultCharset().displayName());
        }
    }

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

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
        final Nut nut = new InMemoryNut(".myClass {}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final NodeEngine converter = new C(true) {
            @Override
            protected NutType targetNutType() {
                return getNutTypeFactory().getNutType(EnumNutType.CSS);
            }
        };

        final List<ConvertibleNut> nuts = converter.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .chain(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), converter)
                .chain(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), new I())
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
        final Nut nut = new InMemoryNut(".myClass {}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final NodeEngine converter = new C(true);
        final List<ConvertibleNut> nuts = converter.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .chain(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), converter)
                .chain(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), new I())
                .build());
        Assert.assertEquals(1, nuts.size());
        final String content = NutUtils.readTransform(nuts.get(0));
        Assert.assertEquals(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), nuts.get(0).getNutType());
        Assert.assertEquals(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), nuts.get(0).getInitialNutType());
        Assert.assertEquals("aggregate.css.js", nuts.get(0).getName());
        Assert.assertEquals("foo.css", nuts.get(0).getInitialName());
        Assert.assertEquals("function myReplacedFunctionSetsCssStyle() {}", content);
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
        final Nut nut = new InMemoryNut(".myClass {}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final NodeEngine converter = new C(false);
        final List<ConvertibleNut> nuts = converter.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .chain(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), converter)
                .chain(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), new I())
                .build());
        Assert.assertEquals(1, nuts.size());
        final String content = NutUtils.readTransform(nuts.get(0));
        Assert.assertEquals(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), nuts.get(0).getNutType());
        Assert.assertEquals(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), nuts.get(0).getInitialNutType());
        Assert.assertEquals("foo.css", nuts.get(0).getName());
        Assert.assertEquals("foo.css", nuts.get(0).getInitialName());
        Assert.assertEquals(".myClass {}", content);
    }
}
