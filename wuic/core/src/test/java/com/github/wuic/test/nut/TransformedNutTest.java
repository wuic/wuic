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


package com.github.wuic.test.nut;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.TransformedNut;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Output;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <p>
 * {@link com.github.wuic.nut.TransformedNut} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class TransformedNutTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Tests transformation handling.
     *
     * @throws IOException if test fails
     */
    @Test
    public void transformTest() throws IOException {
        final TransformedNut nut = new TransformedNut(
                new InMemoryNut(".foo{}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false));
        final Output bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        nut.transform(new Pipe.DefaultOnReady(bos));
        Assert.assertEquals(bos.execution().toString(), ".foo{}");
    }

    /**
     * Nut name can't be changed.
     */
    @Test(expected = IllegalStateException.class)
    public void setNutNameTest() {
        final TransformedNut nut = new TransformedNut(
                new InMemoryNut(".foo{}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false));
        nut.setNutName("");
    }

    /**
     * New transformer can't be added.
     */
    @Test(expected = IllegalStateException.class)
    public void addTransformerTest() {
        final TransformedNut nut = new TransformedNut(
                new InMemoryNut(".foo{}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false));
        nut.addTransformer(Mockito.mock(Pipe.Transformer.class));
    }

    /**
     * New referenced nut can't be added.
     */
    @Test(expected = IllegalStateException.class)
    public void addReferencedNutTest() {
        final TransformedNut nut = new TransformedNut(
                new InMemoryNut(".foo{}".getBytes(), "foo.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false));
        nut.addReferencedNut(Mockito.mock(ConvertibleNut.class));
    }
}
