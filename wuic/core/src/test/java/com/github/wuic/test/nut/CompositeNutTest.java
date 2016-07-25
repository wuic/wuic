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


package com.github.wuic.test.nut;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.test.WuicTest;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Input;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Output;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * <p>
 * {@link CompositeNutTest} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.3
 */
@RunWith(JUnit4.class)
public class CompositeNutTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Expected exception.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Nominal test.
     *
     * @throws IOException if test fails
     */
    @Test
    public void compositeTest() throws IOException {
        final ConvertibleNut n1 = newNut("n1", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), "some css rules");
        final ConvertibleNut n2 = newNut("n2", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), "some css rules");
        final Nut composite = new CompositeNut(Charset.defaultCharset().displayName(), true, "composite", null, Mockito.mock(ProcessContext.class),
                ConvertibleNut[].class.cast(Arrays.asList(n1, n2).toArray()));
        IOUtils.copyStream(composite.openStream().inputStream(), new ByteArrayOutputStream());
    }

    /**
     * Tests when stream is read as character stream or byte stream.
     *
     * @throws IOException if test fails
     */
    @Test
    public void readCharOrByteTest() throws IOException {
        final int len = 4;

        // on 3 bytes
        final char character = '€';
        String content = "";

        for (int i = 0; i < len; i++) {
            content += character;
        }

        ConvertibleNut n1 = newNut("n1", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), content);
        ConvertibleNut n2 = newNut("n2", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), content);

        Nut composite = new CompositeNut(Charset.defaultCharset().displayName(), true, "composite", null, Mockito.mock(ProcessContext.class),
                ConvertibleNut[].class.cast(Arrays.asList(n1, n2).toArray()));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CompositeNut.CompositeInput is = CompositeNut.CompositeInput.class.cast(composite.openStream());
        IOUtils.copyStream(is.inputStream(), bos);
        String message = String.format("Info: %s\nContent: %s\n", is.toString(), new String(bos.toByteArray()));

        for (int i = 0; i < content.length(); i++) {
            Assert.assertEquals(message + "Index: " + i + "\n", n1, is.nutAt(i));
        }

        for (int i = content.length(); i < content.length() * 2; i++) {
            Assert.assertEquals(message + "Index: " + i + "\n", n2, is.nutAt(i));
        }

        Assert.assertNull(is.toString(), is.nutAt(content.length() * 2));

        CharArrayWriter caw = new CharArrayWriter();
        is = CompositeNut.CompositeInput.class.cast(composite.openStream());
        IOUtils.copyStream(is.reader(), caw);
        message = String.format("Info: %s\nContent: %s\n", is.toString(), caw.toString());

        for (int i = 0; i < content.length(); i++) {
            Assert.assertEquals(message + "Index: " + i + "\n", n1, is.nutAt(i));
        }

        for (int i = content.length(); i < content.length() * 2; i++) {
            Assert.assertEquals(message + "Index: " + i + "\n", n2, is.nutAt(i));
        }

        Assert.assertNull(is.toString(), is.nutAt(content.length() * 2));

        InMemoryOutput inMemoryOutput = new InMemoryOutput(Charset.defaultCharset().displayName());
        is = CompositeNut.CompositeInput.class.cast(composite.openStream());
        IOUtils.copyStream(is, inMemoryOutput);
        message = String.format("Info: %s\nContent: %s\n", is.toString(), inMemoryOutput.execution().toString());

        for (int i = 0; i < content.length(); i++) {
            Assert.assertEquals(message + "Index: " + i + "\n", n1, is.nutAt(i));
        }

        for (int i = content.length(); i < content.length() * 2; i++) {
            Assert.assertEquals(message + "Index: " + i + "\n", n2, is.nutAt(i));
        }

        Assert.assertNull(is.toString(), is.nutAt(content.length() * 2));

        n1 = newNut("n1", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.PNG), content);
        n2 = newNut("n2", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.PNG), content);

        composite = new CompositeNut(Charset.defaultCharset().displayName(), true, "composite", null, Mockito.mock(ProcessContext.class),
                ConvertibleNut[].class.cast(Arrays.asList(n1, n2).toArray()));

        is = CompositeNut.CompositeInput.class.cast(composite.openStream());

        IOUtils.copyStream(is, new InMemoryOutput(Charset.defaultCharset().displayName()));
        expectedException.expect(UnsupportedOperationException.class);
        is.nutAt(0);
    }

    /**
     * <p>
     * Checks that transformer are correctly applied in a composition.
     * </p>
     *
     * @throws java.io.IOException if test fails
     */
    @Test
    public void aggregationTransformTest() throws IOException {
        final Pipe.Transformer<ConvertibleNut> t1 = new Pipe.DefaultTransformer<ConvertibleNut>() {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut convertible) throws IOException {
                Writer.class.cast(IOUtils.copyStream(is, os)).write(" t1");
                return true;
            }
        };

        final Pipe.Transformer<ConvertibleNut> t2 = new Pipe.DefaultTransformer<ConvertibleNut>() {
            @Override
            public boolean transform(final Input is, final Output os, final ConvertibleNut convertible) throws IOException {
                Writer.class.cast(IOUtils.copyStream(is, os)).write(" t2");
                return true;
            }
        };

        final Pipe.Transformer<ConvertibleNut> t3 = new Pipe.DefaultTransformer<ConvertibleNut>() {
            @Override
            public boolean transform(Input is, Output os, ConvertibleNut convertible) throws IOException {
                Writer.class.cast(IOUtils.copyStream(is, os)).write(" this is the end");
                return true;
            }

            @Override
            public boolean canAggregateTransformedStream() {
                return false;
            }
        };

        final ConvertibleNut n1 = newNut("n1", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), "some css rules");
        Mockito.when(n1.getTransformers()).thenReturn(new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>(Arrays.asList(t1, t3)));

        final ConvertibleNut n2 = newNut("n2", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), "other css rules");
        Mockito.when(n2.getTransformers()).thenReturn(new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>(Arrays.asList(t2, t3)));

        final ConvertibleNut composite = new CompositeNut(Charset.defaultCharset().displayName(), true, "composite", null, Mockito.mock(ProcessContext.class),
                ConvertibleNut[].class.cast(Arrays.asList(n1, n2).toArray()));

        Assert.assertEquals("some css rules t1other css rules t2 this is the end", NutUtils.readTransform(composite));
    }

    /**
     * <p>
     * Builds a new nut.
     * </p>
     *
     * @param nutType the type
     * @param content the content
     * @return the nut
     * @throws IOException if creation fails
     */
    private ConvertibleNut newNut(final String name, final NutType nutType, final String content) throws IOException {
        final ConvertibleNut n = Mockito.mock(ConvertibleNut.class);
        Mockito.when(n.openStream()).thenAnswer(WuicTest.openStreamAnswer(content));
        Mockito.when(n.getInitialNutType()).thenReturn(nutType);
        Mockito.when(n.getNutType()).thenReturn(nutType);
        Mockito.when(n.getName()).thenReturn(name + nutType.getExtensions()[0]);
        Mockito.when(n.getInitialName()).thenReturn(name + nutType.getExtensions()[0]);
        Mockito.when(n.getReferencedNuts()).thenReturn(CollectionUtils.newList(Mockito.mock(ConvertibleNut.class)));
        Mockito.when(n.getVersionNumber()).thenReturn(new FutureLong(1L));
        return n;
    }
}
