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

import com.github.wuic.NutType;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.TransformedNut;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p>
 * {@link com.github.wuic.nut.TransformedNut} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class TransformedNutTest {

    /**
     * Tests transformation handling.
     *
     * @throws IOException if test fails
     */
    @Test(timeout = 60000)
    public void transformTest() throws IOException {
        final TransformedNut nut = new TransformedNut(new ByteArrayNut(".foo{}".getBytes(), "foo.css", NutType.CSS, 1L, false));
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        nut.transform(new Pipe.DefaultOnReady(bos));
        Assert.assertEquals(new String(bos.toByteArray()), ".foo{}");
    }

    /**
     * Transformed nut must be serializable.
     */
    @Test(timeout = 60000, expected = IllegalArgumentException.class)
    public void notSerializableTest() {
        final ConvertibleNut mock = Mockito.mock(ConvertibleNut.class);
        Mockito.when(mock.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(mock.getInitialName()).thenReturn("foo.js");
        new TransformedNut(mock);
    }

    /**
     * Nut name can't be changed.
     */
    @Test(timeout = 60000, expected = IllegalStateException.class)
    public void setNutNameTest() {
        final TransformedNut nut = new TransformedNut(new ByteArrayNut(".foo{}".getBytes(), "foo.css", NutType.CSS, 1L, false));
        nut.setNutName("");
    }

    /**
     * New transformer can't be added.
     */
    @Test(timeout = 60000, expected = IllegalStateException.class)
    public void addTransformerTest() {
        final TransformedNut nut = new TransformedNut(new ByteArrayNut(".foo{}".getBytes(), "foo.css", NutType.CSS, 1L, false));
        nut.addTransformer(Mockito.mock(Pipe.Transformer.class));
    }

    /**
     * New referenced nut can't be added.
     */
    @Test(timeout = 60000, expected = IllegalStateException.class)
    public void addReferencedNutTest() {
        final TransformedNut nut = new TransformedNut(new ByteArrayNut(".foo{}".getBytes(), "foo.css", NutType.CSS, 1L, false));
        nut.addReferencedNut(Mockito.mock(ConvertibleNut.class));
    }


}
