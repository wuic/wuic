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
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.SpriteProvider;
import com.github.wuic.engine.core.BinPacker;
import com.github.wuic.engine.core.ImageAggregatorEngine;
import com.github.wuic.engine.core.ImageCompressorEngine;
import com.github.wuic.engine.core.SpriteInspectorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link Engine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class EngineTest {

    /**
     * Test purpose class.
     */
    private static final class E extends NodeEngine {

        /**
         * Nut type to return.
         */
        private final NutType nutType;

        /**
         * Engine type to return.
         */
        private final EngineType engineType;

        /**
         * Increment each time parse method is invoked.
         */
        private final AtomicInteger invocationCount;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param nutType the nut type
         * @param engineType the engine type
         * @param invocationCount the counter
         */
        private E(final NutType nutType, final EngineType engineType, final AtomicInteger invocationCount) {
            this.nutType = nutType;
            this.engineType = engineType;
            this.invocationCount = invocationCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<NutType> getNutTypes() {
            return Arrays.asList(nutType);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EngineType getEngineType() {
            return engineType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
            invocationCount.incrementAndGet();
            return request.getNuts();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean works() {
            return true;
        }
    }

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Tests when an engine is skipped.
     *
     * @throws Exception if test fails
     */
    @Test
    public void skipTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final NodeEngine a = new E(NutType.GIF, EngineType.CACHE, count);
        final NodeEngine b = new E(NutType.GIF, EngineType.INSPECTOR, count);
        final NodeEngine c = new E(NutType.GIF, EngineType.BINARY_COMPRESSION, count);

        a.setNext(b);
        b.setNext(c);

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialNutType()).thenReturn(NutType.GIF);
        Mockito.when(nut.getInitialName()).thenReturn("foo.gif");
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final EngineRequestBuilder builder = new EngineRequestBuilder("", heap, null);
        final EngineRequest request = builder.build();

        a.parse(request);
        Assert.assertEquals(3, count.get());

        builder.skip(EngineType.CACHE);
        a.parse(request);
        Assert.assertEquals(5, count.get());

        builder.skip(EngineType.CACHE, EngineType.BINARY_COMPRESSION);
        a.parse(request);
        Assert.assertEquals(6, count.get());
    }

    /**
     * Nominal test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainTest() {
        final SpriteInspectorEngine engine1 = new SpriteInspectorEngine();
        engine1.init(false, new SpriteProvider[] {});
        final ImageAggregatorEngine engine2 = new ImageAggregatorEngine();
        engine2.init(false);
        engine2.init(new BinPacker<ConvertibleNut>());
        final ImageCompressorEngine engine3 = new ImageCompressorEngine();
        engine3.init(false);
        final NodeEngine chain = NodeEngine.chain(engine1, engine2, engine3);
        assertChainTest(chain, engine1, engine2, engine3);
    }

    /**
     * Test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)} with null.
     */
    @Test
    public void chainTestWithNull() {
        final SpriteInspectorEngine engine1 = new SpriteInspectorEngine();
        engine1.init(false, new SpriteProvider[] {});
        final ImageAggregatorEngine engine2 = new ImageAggregatorEngine();
        engine2.init(false);
        engine2.init(new BinPacker<ConvertibleNut>());

        final ImageCompressorEngine engine3 = new ImageCompressorEngine();
        engine3.init(false);

        final NodeEngine chain = NodeEngine.chain(null, engine1, null, engine2, null, engine3);
        assertChainTest(chain, engine1, engine2, engine3);
    }

    /**
     * Union test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainUnionTest() {
        final SpriteInspectorEngine engine1 = new SpriteInspectorEngine();
        engine1.init(false, new SpriteProvider[] {});

        final ImageAggregatorEngine engine2 = new ImageAggregatorEngine();
        engine2.init(false);
        engine2.init(new BinPacker<ConvertibleNut>());

        final ImageCompressorEngine engine3 = new ImageCompressorEngine();
        engine3.init(false);

        assertChainTest(NodeEngine.chain(NodeEngine.chain(engine1, engine2), NodeEngine.chain(engine2, engine3)), engine1, engine2, engine3);
    }

    /**
     * Chaining duplicate engines test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainDuplicateTest() {
        final SpriteInspectorEngine engine1 = new SpriteInspectorEngine();
        engine1.init(false, new SpriteProvider[] {});

        final NodeEngine engine2 = engine1;
        final ImageAggregatorEngine engine3 = new ImageAggregatorEngine();
        engine3.init(false);
        engine3.init(new BinPacker<ConvertibleNut>());

        final ImageCompressorEngine engine4 = new ImageCompressorEngine();
        engine4.init(false);

        assertChainTest(NodeEngine.chain(engine1, engine2, engine3, engine4), engine2, engine3, engine4);
    }

    /**
     * Chaining engines with a replacement test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainReplaceWithLast() {
        final SpriteInspectorEngine engine1 = new SpriteInspectorEngine();
        engine1.init(false, new SpriteProvider[] {});

        final ImageAggregatorEngine engine2 = new ImageAggregatorEngine();
        engine2.init(false);
        engine2.init(new BinPacker<ConvertibleNut>());

        final ImageCompressorEngine engine3 = new ImageCompressorEngine();
        engine3.init(false);

        final ImageAggregatorEngine engine4 = new ImageAggregatorEngine();
        engine4.init(false);
        engine4.init(new BinPacker<ConvertibleNut>());

        final NodeEngine chain = NodeEngine.chain(engine1, engine2, engine3);
        assertChainTest(NodeEngine.chain(chain, engine4), engine1, engine4, engine3);
    }

    /**
     * Chaining with the first engine replaced test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainReplaceFirst() {
        final SpriteInspectorEngine engine1 = new SpriteInspectorEngine();
        engine1.init(false, new SpriteProvider[] {});

        final ImageAggregatorEngine engine2 = new ImageAggregatorEngine();
        engine2.init(false);
        engine2.init(new BinPacker<ConvertibleNut>());

        final ImageCompressorEngine engine3 = new ImageCompressorEngine();
        engine3.init(false);

        final ImageAggregatorEngine engine4 = new ImageAggregatorEngine();
        engine4.init(false);
        engine4.init(new BinPacker<ConvertibleNut>());

        NodeEngine chain = NodeEngine.chain(engine1, engine2, engine3);
        chain = NodeEngine.chain(engine4, chain);
        assertChainTest(chain, engine1, engine2, engine3);
    }

    /**
     * <p>
     * Tests some assertions on given engines.
     * </p>
     *
     * @param chain the chain of engines
     * @param e1 first engine in chain
     * @param e2 second engine in chain
     * @param e3 third engine in chain
     */
    private void assertChainTest(final NodeEngine chain, final NodeEngine e1, final NodeEngine e2, final NodeEngine e3) {
        // Test null/not null assertion on next and previous
        Assert.assertNull(chain.getPrevious());
        Assert.assertNotNull(chain.getNext());
        Assert.assertNotNull(chain.getNext().getNext());
        Assert.assertNotNull(chain.getNext().getPrevious());
        Assert.assertNotNull(chain.getNext().getNext().getPrevious());
        Assert.assertNull(chain.getNext().getNext().getNext());

        // Test right position in the chain
        Assert.assertEquals(chain, e1);
        Assert.assertEquals(chain.getNext(), e2);
        Assert.assertEquals(chain.getNext().getNext(), e3);
        Assert.assertEquals(chain.getNext().getPrevious(), e1);
        Assert.assertEquals(chain.getNext().getNext().getPrevious(), e2);

        Assert.assertTrue(e1.compareTo(e2) < 0);
        Assert.assertTrue(e2.compareTo(e3) < 0);
        Assert.assertTrue(e3.compareTo(e2) > 0);
        Assert.assertTrue(e2.compareTo(e1) > 0);
    }
}
