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

import com.github.wuic.Context;
import com.github.wuic.ContextBuilder;
import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.nut.Nut;

import java.util.List;

import com.github.wuic.xml.FileXmlContextBuilderConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * <p>
 * This class tests {@link com.github.wuic.engine.impl.embedded.CGImageAggregatorEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.3
 */
@RunWith(JUnit4.class)
public class ImageAggregatorEngineTest {

    /**
     * Tested context.
     */
    private Context ctx;

    /**
     * <p>
     * Creates the context.
     * </p>
     *
     * @throws Exception if context can't be created
     */
    @Before
    public void context() throws Exception {
        final ContextBuilder builder = new ContextBuilder();
        EngineBuilderFactory.getInstance().newContextBuilderConfigurator().configure(builder);
        new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic-sprite.xml")).configure(builder);
        ctx = builder.build();
    }

    /**
     * <p>
     * Tests engine when aggregation is enabled.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void withoutAggregation() throws Exception {
        final List<Nut> nuts = ctx.process("", "jsSpriteNotAggregate");
        Assert.assertEquals(3, nuts.size());
        assertOneReference(nuts);
    }

    /**
     * <p>
     * Tests engine when aggregation is disabled.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void withAggregation() throws Exception {
        final List<Nut> nuts = ctx.process("", "cssSpriteAggregate");
        Assert.assertEquals(1, nuts.size());
        assertOneReference(nuts);


    }

    private void assertOneReference(final List<Nut> nuts) {
        for (final Nut n : nuts) {
            Assert.assertNotNull(n.getReferencedNuts());
            Assert.assertEquals(1, n.getReferencedNuts().size());
        }
    }
}
