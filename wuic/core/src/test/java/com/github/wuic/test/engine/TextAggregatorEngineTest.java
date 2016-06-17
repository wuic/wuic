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

import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * {@link TextAggregatorEngine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@RunWith(JUnit4.class)
public class TextAggregatorEngineTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Tests when sourcemap are removed.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     * @throws WuicException if inspection fails
     */
    @Test
    public void removeSourceMapTest() throws IOException, WuicException {
        final TextAggregatorEngine e = new TextAggregatorEngine();
        e.init(true);
        e.async(true);
        final Nut a = Mockito.mock(Nut.class);
        Mockito.when(a.getInitialName()).thenReturn("a.js");
        Mockito.when(a.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(a.openStream()).thenReturn(new ByteArrayInputStream("var foo = '';//# sourceMappingURL=a.js.map\nvar bar = '//# sourceMappingURL=a.js.map';".getBytes()));
        Mockito.when(a.getVersionNumber()).thenReturn(new FutureLong(1L));

        final Nut b = Mockito.mock(Nut.class);
        Mockito.when(b.getInitialName()).thenReturn("b.js");
        Mockito.when(b.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
        Mockito.when(b.openStream()).thenReturn(new ByteArrayInputStream("var baz = '//# sourceMappingURL=b.js.map';\n//# sourceMappingURL=b.js.map\n".getBytes()));
        Mockito.when(b.getVersionNumber()).thenReturn(new FutureLong(1L));

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(a, b));
        final List<ConvertibleNut> res = e.aggregationParse(new EngineRequestBuilder("wid", heap, null).processContext(ProcessContext.DEFAULT).build());

        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size());
        final Nut nut = res.get(0);
        Assert.assertTrue(nut instanceof CompositeNut);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PipedConvertibleNut.transform((CompositeNut) nut, CompositeNut.class.cast(nut).getCompositionList().get(0).getTransformers(), Arrays.asList((Pipe.OnReady) new Pipe.DefaultOnReady(bos)));

        final String content = new String(bos.toByteArray());
        Assert.assertEquals("var foo = '';//# \n"
                + "var bar = '//# sourceMappingURL=a.js.map';\n"
                + "var baz = '//# sourceMappingURL=b.js.map';\n" +
                "//# \n\n//# sourceMappingURL=aggregate.js.map\n", content);
    }
}
