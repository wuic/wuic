/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.engine.Engine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.SpriteProvider;
import com.github.wuic.engine.core.BinPacker;
import com.github.wuic.engine.core.ImageAggregatorEngine;
import com.github.wuic.engine.core.ImageCompressorEngine;
import com.github.wuic.engine.core.SpriteInspectorEngine;
import com.github.wuic.nut.ConvertibleNut;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * <p>
 * {@link Engine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class EngineTest {

    /**
     * Nominal test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainTest() {
        final NodeEngine engine1 = new SpriteInspectorEngine(false, new SpriteProvider[] {});
        final NodeEngine engine2 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine engine3 = new ImageCompressorEngine(false);
        final NodeEngine chain = NodeEngine.chain(engine1, engine2, engine3);
        assertChainTest(chain, engine1, engine2, engine3);
    }

    /**
     * Test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)} with null.
     */
    @Test
    public void chainTestWithNull() {
        final NodeEngine engine1 = new SpriteInspectorEngine(false, new SpriteProvider[] {});
        final NodeEngine engine2 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine engine3 = new ImageCompressorEngine(false);
        final NodeEngine chain = NodeEngine.chain(null, engine1, null, engine2, null, engine3);
        assertChainTest(chain, engine1, engine2, engine3);
    }

    /**
     * Union test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainUnionTest() {
        final NodeEngine engine1 = new SpriteInspectorEngine(false, new SpriteProvider[] {});
        final NodeEngine engine2 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine engine3 = new ImageCompressorEngine(false);
        assertChainTest(NodeEngine.chain(NodeEngine.chain(engine1, engine2), NodeEngine.chain(engine2, engine3)), engine1, engine2, engine3);
    }

    /**
     * Chaining duplicate engines test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainDuplicateTest() {
        final NodeEngine engine1 = new SpriteInspectorEngine(false, new SpriteProvider[] {});
        final NodeEngine engine2 = engine1;
        final NodeEngine engine3 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine engine4 = new ImageCompressorEngine(false);
        assertChainTest(NodeEngine.chain(engine1, engine2, engine3, engine4), engine2, engine3, engine4);
    }

    /**
     * Chaining engines with a replacement test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainReplaceWithLast() {
        final NodeEngine engine1 =new SpriteInspectorEngine(false, new SpriteProvider[] {});
        final NodeEngine engine2 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine engine3 = new ImageCompressorEngine(false);
        final NodeEngine engine4 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine chain = NodeEngine.chain(engine1, engine2, engine3);
        assertChainTest(NodeEngine.chain(chain, engine4), engine1, engine4, engine3);
    }

    /**
     * Chaining with the first engine replaced test for {@link NodeEngine#chain(com.github.wuic.engine.NodeEngine...)}.
     */
    @Test
    public void chainReplaceFirst() {
        final NodeEngine engine1 = new SpriteInspectorEngine(false, new SpriteProvider[] {});
        final NodeEngine engine2 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
        final NodeEngine engine3 = new ImageCompressorEngine(false);
        final NodeEngine engine4 = new ImageAggregatorEngine(false, new BinPacker<ConvertibleNut>());
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
