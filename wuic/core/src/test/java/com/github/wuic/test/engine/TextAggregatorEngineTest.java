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
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.test.ProcessContextRule;
import com.github.wuic.test.WuicTest;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.TimerTreeFactory;
import org.junit.Assert;
import org.junit.ClassRule;
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
     * Process context.
     */
    @ClassRule
    public static ProcessContextRule processContext = new ProcessContextRule();

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
        e.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final Nut a = Mockito.mock(Nut.class);
        Mockito.when(a.getInitialName()).thenReturn("a.js");
        Mockito.when(a.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(a.openStream()).thenAnswer(WuicTest.openStreamAnswer("var foo = '';//# sourceMappingURL=a.js.map\nvar bar = '//# sourceMappingURL=a.js.map';"));
        Mockito.when(a.getVersionNumber()).thenReturn(new FutureLong(1L));

        final Nut b = Mockito.mock(Nut.class);
        Mockito.when(b.getInitialName()).thenReturn("b.js");
        Mockito.when(b.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(b.openStream()).thenAnswer(WuicTest.openStreamAnswer("var baz = '//# sourceMappingURL=b.js.map';\n//# sourceMappingURL=b.js.map\n"));
        Mockito.when(b.getVersionNumber()).thenReturn(new FutureLong(1L));

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(a, b));
        final List<ConvertibleNut> res = e.aggregationParse(new EngineRequestBuilder("wid", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .processContext(processContext.getProcessContext()).build());

        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size());
        final Nut nut = res.get(0);
        Assert.assertTrue(nut instanceof CompositeNut);

        final InMemoryOutput bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        PipedConvertibleNut.transform(new TimerTreeFactory(),
                (CompositeNut) nut,
                CompositeNut.class.cast(nut).getCompositionList().get(0).getTransformers(),
                Arrays.asList((Pipe.OnReady) new Pipe.DefaultOnReady(bos)));

        final String content = bos.execution().toString();
        Assert.assertEquals("var foo = '';//# \n"
                + "var bar = '//# sourceMappingURL=a.js.map';\n"
                + "var baz = '//# sourceMappingURL=b.js.map';\n" +
                "//# \n\n//# sourceMappingURL=aggregate.js.map\n", content);
    }
}
