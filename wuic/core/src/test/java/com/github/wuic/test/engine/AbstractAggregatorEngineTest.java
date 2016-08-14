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

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.AbstractAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.test.ProcessContextRule;
import com.github.wuic.util.FutureLong;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This class tests {@link AbstractAggregatorEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
@RunWith(JUnit4.class)
public class AbstractAggregatorEngineTest {

    /**
     * <p>
     * This implementation just returns a mocked nut when aggregation is performed.
     * </p>
     *
     * @author Guillaume DROUET
          * @since 0.5.2
     */
    final class A extends AbstractAggregatorEngine {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        A() {
            init(Boolean.TRUE);
            setNutTypeFactory(new NutTypeFactory("UTF-8"));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<ConvertibleNut> aggregationParse(final EngineRequest request) throws WuicException {
            return Arrays.asList(Mockito.mock(ConvertibleNut.class));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<NutType> getNutTypes() {
            return getNutTypeFactory().getNutType(EnumNutType.values());
        }
    }

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
     * Checks that a dynamic nut is never aggregated.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void dynamicNutTest() throws WuicException {
        final Engine e = new A();
        final List<Nut> nuts = new ArrayList<Nut>();

        for (int i = 0; i < 6; i++) {
            final Nut nut = Mockito.mock(Nut.class);
            Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
            Mockito.when(nut.getInitialName()).thenReturn(i + ".js");
            Mockito.when(nut.isDynamic()).thenReturn(i % 2 == 0);
            Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
            nuts.add(nut);
        }

        final NutsHeap h = Mockito.mock(NutsHeap.class);
        Mockito.when(h.getNuts()).thenReturn(nuts);

        final List<ConvertibleNut> res = e.parse(new EngineRequestBuilder("", h, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .processContext(processContext.getProcessContext()).build());
        Assert.assertEquals(4, res.size());
    }
}
