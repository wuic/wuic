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

import com.github.wuic.config.bean.json.FileJsonContextBuilderConfigurator;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.nut.ConvertibleNut;

import java.io.IOException;
import java.util.List;

import com.github.wuic.util.UrlUtils;
import com.github.wuic.config.bean.xml.FileXmlContextBuilderConfigurator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

/**
 * <p>
 * This class tests {@link com.github.wuic.engine.core.ImageAggregatorEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.3
 */
@RunWith(Theories.class)
public class ImageAggregatorEngineTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Data points for a set of sprite configurations.
     * </p>
     *
     * @return the data points
     * @throws Exception if initialization fails
     */
    @DataPoints
    public static Context[] sprites() throws Exception {
        final ContextBuilder b1 = new ContextBuilder().configureDefault();
        new FileXmlContextBuilderConfigurator(ImageAggregatorEngineTest.class.getResource("/wuic-sprite.xml")).configure(b1);

        final ContextBuilder b2 = new ContextBuilder().configureDefault();
        new FileJsonContextBuilderConfigurator(ImageAggregatorEngineTest.class.getResource("/wuic-sprite.json")).configure(b2);

        return new Context[] { b1.build(), b2.build() };
    }

    /**
     * <p>
     * Tests engine when aggregation is enabled.
     * </p>
     *
     * @param ctx tested context
     * @throws Exception if test fails
     */
    @Theory
    public void withoutAggregation(final Context ctx) throws Exception {
        final List<ConvertibleNut> nuts = ctx.process("", "jsSpriteNotAggregate", UrlUtils.urlProviderFactory(), null);
        Assert.assertEquals(3, nuts.size());
        assertOneReference(nuts);
    }

    /**
     * <p>
     * Tests engine when aggregation is disabled.
     * </p>
     *
     * @param ctx the tested context
     * @throws Exception if test fails
     */
    @Theory
    public void withAggregation(final Context ctx) throws Exception {
        final List<ConvertibleNut> nuts = ctx.process("", "cssSpriteAggregate", UrlUtils.urlProviderFactory(), null);
        Assert.assertEquals(1, nuts.size());
        assertOneReference(nuts);
    }

    /**
     * <p>
     * Asserts that each nut in the given list references one nut after transformation.
     * </p>
     *
     * @param nuts the nuts
     * @throws IOException if transformation fails
     */
    private void assertOneReference(final List<ConvertibleNut> nuts) throws IOException {
        for (final ConvertibleNut n : nuts) {
            n.transform();
            Assert.assertNotNull(n.getReferencedNuts());
            Assert.assertEquals(1, n.getReferencedNuts().size());
        }
    }
}
