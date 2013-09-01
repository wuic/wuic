/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.engine.impl.ehcache.EhCacheEngine;
import com.github.wuic.engine.impl.embedded.CGBinPacker;
import com.github.wuic.engine.impl.embedded.CGCssSpriteProvider;
import com.github.wuic.engine.impl.embedded.CGImageAggregatorEngine;
import com.github.wuic.engine.impl.embedded.CGImageCompressorEngine;
import com.github.wuic.nut.Nut;
import junit.framework.Assert;
import net.sf.ehcache.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * <p>
 * {@link Engine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class EngineTest {

    /**
     * Nominal test for {@link Engine#chain(com.github.wuic.engine.Engine...)}.
     */
    @Test
    public void chainTest() {
        final Engine engine1 = new EhCacheEngine(false, Mockito.mock(Cache.class));
        final Engine engine2 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        final Engine engine3 = new CGImageCompressorEngine(false);
        Engine chain = Engine.chain(engine1, engine2, engine3);
        assertChainTest(chain, engine1, engine2, engine3);
    }

    /**
     * Union test for {@link Engine#chain(com.github.wuic.engine.Engine...)}.
     */
    @Test
    public void chainUnionTest() {
        final Engine engine1 = new EhCacheEngine(false, Mockito.mock(Cache.class));
        final Engine engine2 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        final Engine engine3 = new CGImageCompressorEngine(false);
        assertChainTest(Engine.chain(Engine.chain(engine1, engine2), Engine.chain(engine2, engine3)), engine1, engine2, engine3);
    }

    /**
     * Chaining duplicate engines test for {@link Engine#chain(com.github.wuic.engine.Engine...)}.
     */
    @Test
    public void chainDuplicateTest() {
        final Engine engine1 = new EhCacheEngine(false, Mockito.mock(Cache.class));
        final Engine engine2 = engine1;
        final Engine engine3 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        final Engine engine4 = new CGImageCompressorEngine(false);
        assertChainTest(Engine.chain(engine1, engine2, engine3, engine4), engine2, engine3, engine4);
    }

    /**
     * Chaining engines with a replacement test for {@link Engine#chain(com.github.wuic.engine.Engine...)}.
     */
    @Test
    public void chainReplaceWithLast() {
        final Engine engine1 = new EhCacheEngine(false, Mockito.mock(Cache.class));
        final Engine engine2 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        final Engine engine3 = new CGImageCompressorEngine(false);
        final Engine engine4 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        final Engine chain = Engine.chain(engine1, engine2, engine3);
        assertChainTest(Engine.chain(chain, engine4), engine1, engine4, engine3);
    }

    /**
     * Chaining with the first engine replaced test for {@link Engine#chain(com.github.wuic.engine.Engine...)}.
     */
    @Test
    public void chainReplaceFirst() {
        final Engine engine1 = new EhCacheEngine(false, Mockito.mock(Cache.class));
        final Engine engine2 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        final Engine engine3 = new CGImageCompressorEngine(false);
        final Engine engine4 = new CGImageAggregatorEngine(false, new CGBinPacker<Nut>(), new CGCssSpriteProvider());
        Engine chain = Engine.chain(engine1, engine2, engine3);
        chain = Engine.chain(engine4, chain);
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
    private void assertChainTest(final Engine chain, final Engine e1, final Engine e2, final Engine e3) {
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
